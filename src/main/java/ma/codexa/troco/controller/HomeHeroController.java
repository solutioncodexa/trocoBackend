package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.HomeHeroSettingsDTO;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.HomeHeroSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/home-hero")
@RequiredArgsConstructor
@Tag(name = "Home Hero", description = "Photo du hero de la page d'accueil")
public class HomeHeroController {

    private final HomeHeroSettingsService homeHeroSettingsService;

    @GetMapping("/public")
    @Operation(summary = "Récupérer la photo du hero (public)")
    public ResponseEntity<ApiResponse<HomeHeroSettingsDTO>> getPublic() {
        return ResponseEntity.ok(ApiResponse.success(homeHeroSettingsService.getPublic()));
    }

    @PutMapping
    @RequirePermission(AppPermissions.CATALOG_MANAGE)
    @Operation(summary = "Mettre à jour la photo du hero")
    public ResponseEntity<ApiResponse<HomeHeroSettingsDTO>> update(
            @Valid @RequestBody HomeHeroSettingsDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                homeHeroSettingsService.update(dto),
                "Photo du hero mise à jour"));
    }
}
