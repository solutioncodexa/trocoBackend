package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.rental.RentalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    /** Calendrier de disponibilité d'un produit à louer (public : GET /products/** est déjà ouvert). */
    @GetMapping("/products/{id}/rental-availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> availability(
            @PathVariable Long id,
            @RequestParam(required = false) Long variantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(rentalService.availability(id, variantId, from, to)));
    }
}
