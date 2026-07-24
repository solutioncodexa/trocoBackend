package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.NotificationDTO;
import ma.codexa.troco.entity.Notification;
import ma.codexa.troco.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "API des notifications admin")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Liste des notifications")
    @GetMapping
    public ResponseEntity<ma.codexa.troco.common.ApiResponse<List<NotificationDTO>>> getAll() {
        List<NotificationDTO> list = notificationService.getAllNotifications().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(ma.codexa.troco.common.ApiResponse.success(list));
    }

    @Operation(summary = "Notifications non lues")
    @GetMapping("/unread")
    public ResponseEntity<ma.codexa.troco.common.ApiResponse<List<NotificationDTO>>> getUnread() {
        List<NotificationDTO> list = notificationService.getUnreadNotifications().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(ma.codexa.troco.common.ApiResponse.success(list));
    }

    @Operation(summary = "Nombre de notifications non lues")
    @GetMapping("/unread/count")
    public ResponseEntity<ma.codexa.troco.common.ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ma.codexa.troco.common.ApiResponse.success(Map.of("count", count)));
    }

    @Operation(summary = "Marquer comme lue")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Marquer toutes comme lues")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
