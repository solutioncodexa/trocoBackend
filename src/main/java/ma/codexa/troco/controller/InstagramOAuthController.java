package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.request.InstagramRequests;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.instagram.InstagramImportService;
import ma.codexa.troco.service.instagram.InstagramOAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Connexion du compte Instagram de la boutique et import de ses posts (API officielle). */
@RestController
@RequestMapping("/instagram-import/oauth")
@RequiredArgsConstructor
@Tag(name = "Instagram OAuth", description = "Connexion du compte Instagram et import de ses posts")
public class InstagramOAuthController {

    private final InstagramOAuthService oauth;
    private final InstagramImportService importService;

    @Operation(summary = "État de la connexion Instagram")
    @RequirePermission(AppPermissions.PRODUCTS_CREATE)
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<InstagramOAuthService.Status>> status() {
        return ResponseEntity.ok(ApiResponse.success(oauth.status()));
    }

    @Operation(summary = "Adresse d'autorisation Instagram")
    @RequirePermission(AppPermissions.PRODUCTS_CREATE)
    @GetMapping("/authorize-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> authorizeUrl(@RequestParam("returnTo") String returnTo) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", oauth.authorizeUrl(returnTo))));
    }

    @Operation(summary = "Retour d'Instagram après autorisation (public, protégé par le state signé)")
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(value = "code", required = false) String code,
                                         @RequestParam(value = "state", required = false) String state,
                                         @RequestParam(value = "error", required = false) String error) {
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, oauth.handleCallback(code, state, error))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @Operation(summary = "Rappel Meta : l'utilisateur a retiré l'app (public, signed_request vérifié)")
    @PostMapping(value = "/deauthorize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> deauthorize(@RequestParam(value = "signed_request", required = false) String signedRequest) {
        return oauth.deauthorize(signedRequest) ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Rappel Meta : demande de suppression des données (public, signed_request vérifié)")
    @PostMapping(value = "/data-deletion", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, String>> dataDeletion(@RequestParam(value = "signed_request", required = false) String signedRequest) {
        return oauth.deleteData(signedRequest)
                .map(r -> ResponseEntity.ok(Map.of("url", r[0], "confirmation_code", r[1])))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @Operation(summary = "Déconnecter le compte Instagram")
    @RequirePermission(AppPermissions.PRODUCTS_CREATE)
    @DeleteMapping
    public ResponseEntity<Void> disconnect() {
        oauth.disconnect();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Posts du compte connecté (24 par page)")
    @RequirePermission(AppPermissions.PRODUCTS_CREATE)
    @GetMapping("/media")
    public ResponseEntity<ApiResponse<InstagramImportService.AccountPage>> media(
            @RequestParam(value = "after", required = false) String after) {
        return ResponseEntity.ok(ApiResponse.success(importService.accountPosts(after)));
    }

    @Operation(summary = "Importer des posts du compte connecté (un brouillon par post)")
    @RequirePermission(AppPermissions.PRODUCTS_CREATE)
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<List<InstagramImportService.Item>>> importPosts(
            @RequestBody @Valid InstagramRequests.AccountImport body) {
        return ResponseEntity.ok(ApiResponse.success(importService.importAccountPosts(body.ids())));
    }
}
