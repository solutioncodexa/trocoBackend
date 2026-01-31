package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.dto.GoldPriceDTO;
import ma.codexa.goldyara.service.GoldPriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/gold-prices")
@RequiredArgsConstructor
@Tag(name = "Gold Prices", description = "API des prix de l'or (XAU/MAD) depuis sources externes")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class GoldPriceController {

    private final GoldPriceService goldPriceService;

    @Operation(summary = "Récupérer les prix de l'or", description = "Retourne le cours actuel et l'historique de l'or en MAD/oz (or.fr puis goldbroker.com en fallback)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prix récupérés avec succès"),
            @ApiResponse(responseCode = "503", description = "Sources de prix indisponibles")
    })
    @GetMapping
    public ResponseEntity<ma.codexa.goldyara.common.ApiResponse<GoldPriceDTO>> getGoldPrices() {
        log.debug("Récupération des prix de l'or XAU/MAD");
        GoldPriceDTO dto = goldPriceService.getGoldPrices();
        return ResponseEntity.ok(ma.codexa.goldyara.common.ApiResponse.success(dto));
    }
}
