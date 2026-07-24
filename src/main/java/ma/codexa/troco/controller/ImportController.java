package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.service.TrocoMaImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Import catalogue depuis troco.ma")
public class ImportController {

    private final TrocoMaImportService trocoMaImportService;

    @Operation(summary = "Importer catégories et produits depuis troco.ma (WooCommerce public API)")
    @PostMapping("/troco-ma")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importTrocoMa() {
        TrocoMaImportService.ImportResult result = trocoMaImportService.importAll();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "categoriesUpserted", result.categoriesUpserted(),
                "productsCreated", result.productsCreated(),
                "productsSkipped", result.productsSkipped(),
                "variantsCreated", result.variantsCreated()
        ), "Import troco.ma terminé"));
    }
}
