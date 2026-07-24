package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.SocialNetworkDTO;
import ma.codexa.troco.service.SocialNetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/social-networks")
@RequiredArgsConstructor
@Tag(name = "Social Networks", description = "Gestion des réseaux sociaux du site")
public class SocialNetworkController {

    private final SocialNetworkService socialNetworkService;

    @GetMapping("/public")
    @Operation(summary = "Réseaux actifs (storefront)")
    public ResponseEntity<ApiResponse<List<SocialNetworkDTO>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.success(socialNetworkService.getPublicEnabled()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tous les réseaux (admin)")
    public ResponseEntity<ApiResponse<List<SocialNetworkDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(socialNetworkService.getAll()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour les réseaux (batch)")
    public ResponseEntity<ApiResponse<List<SocialNetworkDTO>>> updateBatch(
            @RequestBody List<SocialNetworkDTO> updates) {
        return ResponseEntity.ok(ApiResponse.success(
                socialNetworkService.updateBatch(updates),
                "Réseaux sociaux mis à jour"));
    }
}
