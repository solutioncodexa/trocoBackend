package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.StoreLeadDTO;
import ma.codexa.troco.dto.request.CreateStoreLeadRequest;
import ma.codexa.troco.entity.StoreLead;
import ma.codexa.troco.repository.StoreLeadRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreLeadService {

    private static final Set<String> TYPES = Set.of("newsletter", "lead", "devis");

    private final StoreLeadRepository repository;
    private final StoreWebhookDispatcher storeWebhookDispatcher;

    @Transactional
    public StoreLeadDTO submit(CreateStoreLeadRequest req) {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Boutique non résolue");
        }
        String type = req.getLeadType() == null ? "" : req.getLeadType().trim().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de formulaire invalide");
        }
        if ((req.getEmail() == null || req.getEmail().isBlank())
                && (req.getPhone() == null || req.getPhone().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email ou téléphone requis");
        }
        StoreLead lead = new StoreLead();
        lead.setFournisseurId(fid);
        lead.setLeadType(type);
        lead.setFullName(blank(req.getFullName()));
        lead.setEmail(blank(req.getEmail()));
        lead.setPhone(blank(req.getPhone()));
        lead.setMessage(blank(req.getMessage()));
        lead.setSourcePath(blank(req.getSourcePath()));
        StoreLead saved = repository.save(lead);
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("leadId", saved.getId());
            payload.put("leadType", saved.getLeadType());
            payload.put("fullName", saved.getFullName());
            payload.put("email", saved.getEmail());
            payload.put("phone", saved.getPhone());
            payload.put("message", saved.getMessage());
            payload.put("sourcePath", saved.getSourcePath());
            storeWebhookDispatcher.dispatchAsync(fid, StoreWebhookDispatcher.EVENT_LEAD_CREATED, payload);
        } catch (Exception ignored) {
            /* non-blocking */
        }
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StoreLeadDTO> list() {
        TenantContext.requireFournisseurId();
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        StringBuilder sb = new StringBuilder("id,type,name,email,phone,message,source,createdAt\n");
        for (StoreLeadDTO l : list()) {
            sb.append(csv(l.id())).append(',')
                    .append(csv(l.leadType())).append(',')
                    .append(csv(l.fullName())).append(',')
                    .append(csv(l.email())).append(',')
                    .append(csv(l.phone())).append(',')
                    .append(csv(l.message())).append(',')
                    .append(csv(l.sourcePath())).append(',')
                    .append(csv(l.createdAt() != null ? l.createdAt().toString() : ""))
                    .append('\n');
        }
        return sb.toString();
    }

    private StoreLeadDTO toDto(StoreLead l) {
        return new StoreLeadDTO(
                l.getId(), l.getLeadType(), l.getFullName(), l.getEmail(),
                l.getPhone(), l.getMessage(), l.getSourcePath(), l.getCreatedAt());
    }

    private static String blank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v).replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) return "\"" + s + "\"";
        return s;
    }
}
