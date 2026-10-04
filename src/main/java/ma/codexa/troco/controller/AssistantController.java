package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.request.AssistantChatRequest;
import ma.codexa.troco.service.assistant.AssistantAgentService;
import ma.codexa.troco.service.assistant.AssistantReply;
import ma.codexa.troco.service.assistant.AssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantAgentService agentService;

    /** Permet au front de masquer le bouton de chat quand l'assistant est désactivé. */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("enabled", assistantService.isEnabled())));
    }

    /** Réponse texte, plus les actions exécutées ou à confirmer lorsque les outils sont activés. */
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@Valid @RequestBody AssistantChatRequest request) {
        AssistantReply r = agentService.chat(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("reply", r.reply(), "actions", r.actions())));
    }

    @PostMapping("/actions/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<AssistantReply.ActionResult>> confirm(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(agentService.confirm(id)));
    }

    /** Annule une action déjà faite par l'assistant (valable 30 minutes). */
    @PostMapping("/actions/{id}/undo")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<AssistantReply.ActionResult>> undo(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(agentService.undo(id)));
    }

    @PostMapping("/actions/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancel(@PathVariable String id) {
        agentService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("cancelled", true)));
    }
}
