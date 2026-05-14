package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.GoldTypeDTO;
import ma.codexa.goldyara.service.GoldTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gold-types")
@RequiredArgsConstructor
@Tag(name = "Gold Types", description = "API des types d'or (Or Jaune, Or Blanc, Or Rose)")
public class GoldTypeController {

    private final GoldTypeService goldTypeService;

    @Operation(summary = "Liste des types d'or")
    @GetMapping
    public ResponseEntity<ApiResponse<List<GoldTypeDTO>>> getAllGoldTypes() {
        List<GoldTypeDTO> list = goldTypeService.getAllGoldTypes();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @Operation(summary = "Créer un type d'or")
    @PostMapping
    public ResponseEntity<ApiResponse<GoldTypeDTO>> create(@Valid @RequestBody GoldTypeDTO dto) {
        GoldTypeDTO created = goldTypeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Type d'or créé"));
    }

    @Operation(summary = "Modifier un type d'or")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoldTypeDTO>> update(
            @Parameter(description = "ID du type d'or") @PathVariable Long id,
            @Valid @RequestBody GoldTypeDTO dto) {
        GoldTypeDTO updated = goldTypeService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Type d'or modifié"));
    }

    @Operation(summary = "Supprimer un type d'or")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du type d'or") @PathVariable Long id) {
        goldTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
