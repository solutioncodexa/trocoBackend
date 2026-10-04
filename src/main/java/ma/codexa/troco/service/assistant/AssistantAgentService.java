package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.config.AssistantToolsProperties;
import ma.codexa.troco.dto.request.AssistantChatRequest;
import ma.codexa.troco.service.AuditLogService;
import ma.codexa.troco.service.assistant.AssistantReply.ActionResult;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assistant qui agit : le modèle peut appeler des outils, mais c'est le code qui décide.
 *
 * <ul>
 *   <li>Lecture : exécutée aussitôt. Écriture limitée : exécutée aussitôt, journalisée.</li>
 *   <li>Écriture à fort impact : jamais exécutée sur la seule décision du modèle, le commerçant doit confirmer.</li>
 *   <li>Droits revérifiés à chaque exécution ; plafonds par message, par jour et par boutique ; interrupteur
 *       {@code app.assistant.tools.enabled}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantAgentService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AssistantToolsProperties toolsProps;
    private final AssistantService base;
    private final ChatModelClient client;
    private final AssistantToolRegistry registry;
    private final PendingActionStore pending;
    private final UndoStore undos;
    private final AuditLogService audit;

    private final Map<Long, AtomicInteger> writesToday = new ConcurrentHashMap<>();
    private volatile LocalDate writesDay = LocalDate.now();

    /** Les actions ne sont proposées qu'en conversation libre : pendant un parcours guidé, c'est l'interface qui mène. */
    public boolean toolsActive(AssistantChatRequest request) {
        return toolsProps.enabled() && (request.step() == null || request.step().isBlank());
    }

    public AssistantReply chat(AssistantChatRequest request) {
        long started = System.nanoTime();
        AssistantReply reply = converse(request);
        // Suivi d'usage et de coût : aucun contenu de conversation, seulement des compteurs.
        log.info("assistant_turn store={} tools={} actions={} ms={}", TenantContext.getFournisseurId(), toolsActive(request),
                reply.actions().stream().map(a -> a.tool() + ":" + a.status()).toList(),
                (System.nanoTime() - started) / 1_000_000);
        return reply;
    }

    private AssistantReply converse(AssistantChatRequest request) {
        if (!toolsActive(request)) {
            return AssistantReply.text(base.chat(request));
        }
        AssistantService.Prepared prepared = base.prepare(request, true);
        Long fid = prepared.fournisseurId();
        List<Map<String, Object>> specs = registry.specs();

        List<Map<String, Object>> convo = new ArrayList<>();
        for (ChatMessage m : prepared.messages()) {
            convo.add(Map.of("role", m.role(), "content", m.content()));
        }

        List<ActionResult> actions = new ArrayList<>();
        int writes = 0;
        boolean proposed = false;
        try {
            for (int round = 0; round < toolsProps.maxRounds(); round++) {
                ModelTurn turn = client.completeWithTools(convo, specs);
                if (turn.calls().isEmpty()) {
                    return new AssistantReply(finalText(turn.text(), actions, request.locale()), actions);
                }
                convo.add(MAPPER.convertValue(turn.rawMessage(), new com.fasterxml.jackson.core.type.TypeReference<>() {
                }));
                for (ToolCall call : turn.calls()) {
                    Map<String, Object> result;
                    AssistantTool tool = registry.find(call.name()).orElse(null);
                    if (tool == null || !registry.permitted(tool)) {
                        result = Map.of("error", "Outil indisponible");
                    } else if (tool.tier() == AssistantTool.Tier.READ) {
                        result = run(tool, call, actions, false);
                    } else if (tool.tier() == AssistantTool.Tier.AUTO) {
                        if (writes >= toolsProps.maxWritesPerTurn()) {
                            result = Map.of("error", "Limite d'actions atteinte pour ce message");
                        } else {
                            writes++;
                            result = run(tool, call, actions, true);
                        }
                    } else if (proposed) {
                        result = Map.of("error", "Une seule confirmation à la fois : attends la réponse du commerçant");
                    } else {
                        result = propose(tool, call, actions, fid);
                        proposed = actions.get(actions.size() - 1).status().equals("pending");
                    }
                    Map<String, Object> toolMsg = new LinkedHashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.id());
                    toolMsg.put("name", call.name());
                    toolMsg.put("content", json(result));
                    convo.add(toolMsg);
                }
            }
            // Trop d'allers-retours : on s'arrête là, les actions déjà faites restent affichées.
            return new AssistantReply(finalText("", actions, request.locale()), actions);
        } catch (AssistantUnavailableException e) {
            if (actions.isEmpty()) base.refundQuota(fid);
            if (!actions.isEmpty()) {
                return new AssistantReply(finalText("", actions, request.locale()), actions);
            }
            throw new BusinessException(AssistantService.msg(request.locale(), "unavailable"),
                    HttpStatus.SERVICE_UNAVAILABLE, "ASSISTANT_UNAVAILABLE");
        }
    }

    /** Le commerçant confirme une action proposée. Revérifie boutique, personne, droits et plafond du jour. */
    public ActionResult confirm(String actionId) {
        Long fid = TenantContext.requireFournisseurId();
        PendingActionStore.Pending p = pending.take(actionId, fid, userKey())
                .orElseThrow(() -> new BusinessException("Action expirée ou introuvable",
                        HttpStatus.NOT_FOUND, "ASSISTANT_ACTION_GONE"));
        if (!toolsProps.enabled()) {
            throw new BusinessException("Les actions de l'assistant sont désactivées",
                    HttpStatus.SERVICE_UNAVAILABLE, "ASSISTANT_TOOLS_DISABLED");
        }
        AssistantTool tool = registry.find(p.tool()).orElse(null);
        if (tool == null || !registry.permitted(tool)) {
            throw new BusinessException("Action non autorisée", HttpStatus.FORBIDDEN, "ASSISTANT_ACTION_DENIED");
        }
        ActionResult[] out = new ActionResult[1];
        if (!takeWriteSlot(fid)) {
            return ActionResult.failed(p.tool(), "Limite quotidienne d'actions atteinte");
        }
        execute(tool, p.args(), out, true, "confirmée");
        return out[0];
    }

    /** Le commerçant refuse : l'action est simplement oubliée. */
    public void cancel(String actionId) {
        pending.take(actionId, TenantContext.requireFournisseurId(), userKey());
    }

    /** Le commerçant annule une action de l'assistant : la boutique revient à l'état d'avant. */
    public ActionResult undo(String undoId) {
        Long fid = TenantContext.requireFournisseurId();
        UndoStore.Entry e = undos.take(undoId, fid, userKey())
                .orElseThrow(() -> new BusinessException("Annulation expirée ou introuvable",
                        HttpStatus.NOT_FOUND, "ASSISTANT_UNDO_GONE"));
        AssistantTool tool = registry.find(e.tool()).orElse(null);
        if (tool == null || !registry.permitted(tool)) {
            throw new BusinessException("Action non autorisée", HttpStatus.FORBIDDEN, "ASSISTANT_ACTION_DENIED");
        }
        try {
            e.undo().run();
        } catch (RuntimeException ex) {
            log.warn("Annulation assistant {} en erreur : {}", e.tool(), ex.toString());
            return ActionResult.failed(e.tool(), "L'annulation a échoué");
        }
        try {
            audit.record(AuditLogService.Action.ASSISTANT_ACTION, "ASSISTANT", e.tool(), "Assistant : annulation de " + e.tool());
        } catch (RuntimeException ex) {
            log.warn("Journal d'audit assistant indisponible : {}", ex.toString());
        }
        return ActionResult.done(e.tool(), Map.of(), null);
    }

    // ───────────── Exécution ─────────────

    private Map<String, Object> run(AssistantTool tool, ToolCall call, List<ActionResult> actions, boolean write) {
        JsonNode args = parse(call.arguments());
        if (args == null) {
            actions.add(ActionResult.failed(tool.name(), "Arguments invalides"));
            return Map.of("error", "Arguments invalides");
        }
        if (write && !takeWriteSlot(TenantContext.requireFournisseurId())) {
            ActionResult r = ActionResult.failed(tool.name(), "Limite quotidienne d'actions atteinte");
            actions.add(r);
            return Map.of("error", r.error());
        }
        ActionResult[] out = new ActionResult[1];
        Object data = execute(tool, args, out, write, "automatique");
        if (write || "failed".equals(out[0].status())) actions.add(out[0]);
        return "failed".equals(out[0].status()) ? Map.of("error", out[0].error()) : Map.of("result", data);
    }

    private Object execute(AssistantTool tool, JsonNode args, ActionResult[] out, boolean write, String mode) {
        try {
            ToolOutcome o = tool.execute(args);
            String undoId = write && o.undo() != null
                    ? undos.add(TenantContext.requireFournisseurId(), userKey(), tool.name(), o.undo()).id()
                    : null;
            out[0] = ActionResult.done(tool.name(), o.display(), undoId);
            if (write) auditWrite(tool, args, mode);
            return o.data();
        } catch (ToolFailure e) {
            out[0] = ActionResult.failed(tool.name(), e.getMessage());
            return null;
        } catch (BusinessException e) {
            out[0] = ActionResult.failed(tool.name(), clip(e.getMessage(), 160));
            return null;
        } catch (RuntimeException e) {
            log.warn("Outil assistant {} en erreur : {}", tool.name(), e.toString());
            out[0] = ActionResult.failed(tool.name(), "L'action a échoué");
            return null;
        }
    }

    private Map<String, Object> propose(AssistantTool tool, ToolCall call, List<ActionResult> actions, Long fid) {
        JsonNode args = parse(call.arguments());
        if (args == null) {
            actions.add(ActionResult.failed(tool.name(), "Arguments invalides"));
            return Map.of("error", "Arguments invalides");
        }
        try {
            Map<String, String> preview = tool.preview(args);
            PendingActionStore.Pending p = pending.add(fid, userKey(), tool.name(), args);
            actions.add(ActionResult.pending(tool.name(), p.id(), preview));
            return Map.of("status", "awaiting_user_confirmation");
        } catch (ToolFailure | BusinessException e) {
            actions.add(ActionResult.failed(tool.name(), clip(e.getMessage(), 160)));
            return Map.of("error", clip(e.getMessage(), 160));
        }
    }

    private void auditWrite(AssistantTool tool, JsonNode args, String mode) {
        try {
            audit.record(AuditLogService.Action.ASSISTANT_ACTION, "ASSISTANT", tool.name(),
                    clip("Assistant (" + mode + ") " + tool.name() + " " + args, 500));
        } catch (RuntimeException e) {
            log.warn("Journal d'audit assistant indisponible : {}", e.toString());
        }
    }

    // ───────────── Aides ─────────────

    /** Plafond quotidien d'écritures par boutique (réinitialisé chaque jour). */
    private boolean takeWriteSlot(Long fid) {
        LocalDate today = LocalDate.now();
        if (!today.equals(writesDay)) {
            synchronized (this) {
                if (!today.equals(writesDay)) {
                    writesToday.clear();
                    writesDay = today;
                }
            }
        }
        return writesToday.computeIfAbsent(fid, k -> new AtomicInteger()).incrementAndGet() <= toolsProps.dailyWriteLimit();
    }

    private static JsonNode parse(String arguments) {
        try {
            JsonNode n = MAPPER.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
            return n != null && n.isObject() ? n : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String json(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"résultat illisible\"}";
        }
    }

    /** Texte final : celui du modèle, sinon une phrase neutre pour que l'interface n'affiche jamais une bulle vide. */
    private static String finalText(String text, List<ActionResult> actions, String locale) {
        if (text != null && !text.isBlank()) return clip(text.trim(), 4000);
        boolean pendingAction = actions.stream().anyMatch(a -> "pending".equals(a.status()));
        return AssistantService.msg(locale, pendingAction ? "confirmPending" : "actionsDone");
    }

    private static String userKey() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "" : a.getName();
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
