package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.request.AssistantChatRequest;
import ma.codexa.troco.service.assistant.AssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    /** Permet au front de masquer le bouton de chat quand l'assistant est désactivé. */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("enabled", assistantService.isEnabled())));
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("reply", assistantService.chat(request))));
    }
}
