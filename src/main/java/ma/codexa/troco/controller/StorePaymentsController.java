package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.CmiCheckoutDTO;
import ma.codexa.troco.dto.StorePaymentsConfigDTO;
import ma.codexa.troco.dto.StoreSettingsDTO;
import ma.codexa.troco.dto.request.PaymentCredentialsTestRequest;
import ma.codexa.troco.dto.request.StoreCmiInitRequest;
import ma.codexa.troco.dto.request.StorePayPalCreateOrderRequest;
import ma.codexa.troco.dto.request.StoreStripeChargeRequest;
import ma.codexa.troco.dto.request.StoreStripePaymentIntentRequest;
import ma.codexa.troco.service.FournisseurService;
import ma.codexa.troco.service.StorePaymentGatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StorePaymentsController {

    private final StorePaymentGatewayService paymentGatewayService;
    private final FournisseurService fournisseurService;

    /** Admin — tester & activer Stripe pour la boutique courante. */
    @PostMapping("/store-settings/me/payments/test-stripe")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testStripe(
            @RequestBody(required = false) PaymentCredentialsTestRequest request) {
        String message = paymentGatewayService.testStripe(request);
        StoreSettingsDTO settings = fournisseurService.getMyStoreSettings();
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", message, "settings", settings)));
    }

    @PostMapping("/store-settings/me/payments/test-paypal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testPaypal(
            @RequestBody(required = false) PaymentCredentialsTestRequest request) {
        String message = paymentGatewayService.testPaypal(request);
        StoreSettingsDTO settings = fournisseurService.getMyStoreSettings();
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", message, "settings", settings)));
    }

    @PostMapping("/store-settings/me/payments/test-cmi")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testCmi(
            @RequestBody(required = false) PaymentCredentialsTestRequest request) {
        String message = paymentGatewayService.testCmi(request);
        StoreSettingsDTO settings = fournisseurService.getMyStoreSettings();
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", message, "settings", settings)));
    }

    /** Public — config paiements (gateways ready uniquement). */
    @GetMapping("/store-payments/config")
    public ResponseEntity<ApiResponse<StorePaymentsConfigDTO>> publicConfig(
            @RequestParam(required = false) String slug) {
        return ResponseEntity.ok(ApiResponse.success(paymentGatewayService.getPublicConfig(slug)));
    }

    @PostMapping("/store-payments/stripe/payment-intent")
    public ResponseEntity<ApiResponse<Map<String, String>>> stripeIntent(
            @Valid @RequestBody StoreStripePaymentIntentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentGatewayService.createStripePaymentIntent(request)));
    }

    /** Public — débit carte (PaymentMethod issu de Stripe Elements). */
    @PostMapping("/store-payments/stripe/charge")
    public ResponseEntity<ApiResponse<Map<String, String>>> stripeCharge(
            @Valid @RequestBody StoreStripeChargeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentGatewayService.chargeStripePaymentMethod(request)));
    }

    @PostMapping("/store-payments/paypal/create-order")
    public ResponseEntity<ApiResponse<Map<String, String>>> paypalCreate(
            @Valid @RequestBody StorePayPalCreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentGatewayService.createPayPalOrder(request)));
    }

    @PostMapping("/store-payments/paypal/capture/{orderId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> paypalCapture(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentGatewayService.capturePayPalOrder(orderId)));
    }

    @PostMapping("/store-payments/cmi/init")
    public ResponseEntity<ApiResponse<CmiCheckoutDTO>> cmiInit(
            @Valid @RequestBody StoreCmiInitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentGatewayService.initCmiCheckout(request)));
    }
}
