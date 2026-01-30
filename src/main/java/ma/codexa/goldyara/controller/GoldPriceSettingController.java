package ma.codexa.goldyara.controller;

import jakarta.validation.Valid;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.GoldPriceSettingDTO;
import ma.codexa.goldyara.dto.request.UpdateGoldPriceSettingRequest;
import ma.codexa.goldyara.entity.GoldPriceSetting;
import ma.codexa.goldyara.service.GoldPriceSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gold-price-settings")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class GoldPriceSettingController {

    private final GoldPriceSettingService goldPriceSettingService;

    public GoldPriceSettingController(GoldPriceSettingService goldPriceSettingService) {
        this.goldPriceSettingService = goldPriceSettingService;
    }

    /**
     * Récupère les paramètres de calcul du prix (prix au gramme + gain).
     * Public pour permettre au formulaire produit d'afficher le prix calculé.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<GoldPriceSettingDTO>> getSettings() {
        GoldPriceSetting s = goldPriceSettingService.getSettings();
        GoldPriceSettingDTO dto = new GoldPriceSettingDTO(s.getId(), s.getPricePerGram());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Met à jour le prix au gramme (admin). La marge est définie par produit.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<GoldPriceSettingDTO>> updateSettings(
            @Valid @RequestBody UpdateGoldPriceSettingRequest request) {
        GoldPriceSetting s = goldPriceSettingService.updateSettings(request.getPricePerGram());
        GoldPriceSettingDTO dto = new GoldPriceSettingDTO(s.getId(), s.getPricePerGram());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
