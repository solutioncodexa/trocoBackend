package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.TopBarMessageDTO;
import ma.codexa.troco.service.TopBarMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/top-bar-messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Top Bar Messages", description = "API pour gérer les messages de la barre supérieure")
public class TopBarController {
    
    private final TopBarMessageService topBarMessageService;
    
    @GetMapping("/public")
    @Operation(summary = "Récupérer les messages actifs de la top bar", description = "Retourne la liste des messages actifs pour affichage public")
    public ResponseEntity<ApiResponse<List<TopBarMessageDTO>>> getActiveMessages() {
        log.info("Récupération des messages actifs de la top bar pour le public");
        List<TopBarMessageDTO> messages = topBarMessageService.getAllActiveMessages();
        return ResponseEntity.ok(ApiResponse.success(messages, "Messages actifs récupérés avec succès"));
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Récupérer tous les messages de la top bar", description = "Retourne la liste de tous les messages (actifs et inactifs)")
    public ResponseEntity<ApiResponse<List<TopBarMessageDTO>>> getAllMessages() {
        log.info("Récupération de tous les messages de la top bar");
        List<TopBarMessageDTO> messages = topBarMessageService.getAllMessages();
        return ResponseEntity.ok(ApiResponse.success(messages, "Messages récupérés avec succès"));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouveau message", description = "Crée un nouveau message pour la barre supérieure")
    public ResponseEntity<ApiResponse<TopBarMessageDTO>> createMessage(
            @Valid @RequestBody TopBarMessageDTO messageDTO) {
        log.info("Création d'un nouveau message de top bar");
        TopBarMessageDTO createdMessage = topBarMessageService.createMessage(messageDTO);
        return ResponseEntity.ok(ApiResponse.success(createdMessage, "Message créé avec succès"));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour un message", description = "Met à jour un message existant")
    public ResponseEntity<ApiResponse<TopBarMessageDTO>> updateMessage(
            @Parameter(description = "ID du message", required = true) @PathVariable String id,
            @Valid @RequestBody TopBarMessageDTO messageDTO) {
        log.info("Mise à jour du message de top bar avec l'id: {}", id);
        try {
            Long idLong = Long.parseLong(id);
            TopBarMessageDTO updatedMessage = topBarMessageService.updateMessage(idLong, messageDTO);
            return ResponseEntity.ok(ApiResponse.success(updatedMessage, "Message mis à jour avec succès"));
        } catch (NumberFormatException e) {
            log.error("ID invalide: {}", id);
            return ResponseEntity.badRequest().body(ApiResponse.error("ID invalide", 400));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un message", description = "Supprime un message existant")
    public ResponseEntity<Void> deleteMessage(
            @Parameter(description = "ID du message", required = true) @PathVariable String id) {
        log.info("Suppression du message de top bar avec l'id: {}", id);
        try {
            Long idLong = Long.parseLong(id);
            topBarMessageService.deleteMessage(idLong);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            log.error("ID invalide: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/Désactiver un message", description = "Change le statut actif d'un message")
    public ResponseEntity<ApiResponse<TopBarMessageDTO>> toggleActive(
            @Parameter(description = "ID du message", required = true) @PathVariable String id,
            @Parameter(description = "Statut actif", required = true) @RequestParam Boolean isActive) {
        log.info("Changement du statut actif du message {} à {}", id, isActive);
        try {
            Long idLong = Long.parseLong(id);
            TopBarMessageDTO updatedMessage = topBarMessageService.toggleActive(idLong, isActive);
            return ResponseEntity.ok(ApiResponse.success(updatedMessage, "Statut du message mis à jour avec succès"));
        } catch (NumberFormatException e) {
            log.error("ID invalide: {}", id);
            return ResponseEntity.badRequest().body(ApiResponse.error("ID invalide", 400));
        }
    }
}
