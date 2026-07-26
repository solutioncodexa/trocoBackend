package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.request.AiCopyRequest;
import ma.codexa.troco.service.AiCopyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai-copy")
@RequiredArgsConstructor
public class AiCopyController {

    private final AiCopyService aiCopyService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(@Valid @RequestBody AiCopyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(aiCopyService.generate(request)));
    }
}
