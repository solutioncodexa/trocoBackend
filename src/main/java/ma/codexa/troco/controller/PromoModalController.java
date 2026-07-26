package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.PromoModalDTO;
import ma.codexa.troco.service.PromoModalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promo-modals")
@RequiredArgsConstructor
@Slf4j
public class PromoModalController {
    
    private final PromoModalService promoModalService;
    
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PromoModalDTO>> getActivePromoModal(
            @RequestParam(required = false) String path) {
        PromoModalDTO modal = promoModalService.getFirstActivePromoModalForPath(path);
        if (modal == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "Aucun promo modal actif"));
        }
        return ResponseEntity.ok(ApiResponse.success(modal, "Promo modal actif récupéré avec succès"));
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PromoModalDTO>>> getAllPromoModals() {
        List<PromoModalDTO> modals = promoModalService.getAllPromoModals();
        return ResponseEntity.ok(ApiResponse.success(modals, "Promo modals récupérés avec succès"));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoModalDTO>> getPromoModalById(@PathVariable String id) {
        try {
            Long idLong = Long.parseLong(id);
            PromoModalDTO modal = promoModalService.getPromoModalById(idLong);
            return ResponseEntity.ok(ApiResponse.success(modal, "Promo modal récupéré avec succès"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ID invalide", 400));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoModalDTO>> createPromoModal(@Valid @RequestBody PromoModalDTO modalDTO) {
        PromoModalDTO createdModal = promoModalService.createPromoModal(modalDTO);
        return ResponseEntity.ok(ApiResponse.success(createdModal, "Promo modal créé avec succès"));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoModalDTO>> updatePromoModal(@PathVariable String id, @Valid @RequestBody PromoModalDTO modalDTO) {
        try {
            Long idLong = Long.parseLong(id);
            PromoModalDTO updatedModal = promoModalService.updatePromoModal(idLong, modalDTO);
            return ResponseEntity.ok(ApiResponse.success(updatedModal, "Promo modal mis à jour avec succès"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ID invalide", 400));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePromoModal(@PathVariable String id) {
        try {
            Long idLong = Long.parseLong(id);
            promoModalService.deletePromoModal(idLong);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoModalDTO>> toggleActive(@PathVariable String id, @RequestParam Boolean isActive) {
        try {
            Long idLong = Long.parseLong(id);
            PromoModalDTO updatedModal = promoModalService.toggleActive(idLong, isActive);
            return ResponseEntity.ok(ApiResponse.success(updatedModal, "Statut du promo modal mis à jour avec succès"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ID invalide", 400));
        }
    }
}
