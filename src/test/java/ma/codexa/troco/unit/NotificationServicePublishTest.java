package ma.codexa.troco.unit;

import ma.codexa.troco.dto.NotificationDTO;
import ma.codexa.troco.entity.Notification;
import ma.codexa.troco.repository.NotificationRepository;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.service.EmailService;
import ma.codexa.troco.service.NotificationService;
import ma.codexa.troco.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit — NotificationService push WebSocket")
class NotificationServicePublishTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks NotificationService notificationService;

    @BeforeEach
    void setTenant() {
        TenantContext.setFournisseurId(42L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.setFournisseurId(null);
    }

    @Test
    @DisplayName("notifyStockAlert publie sur /topic/store.{fournisseurId}")
    void stockAlertPublishesToTenantTopic() {
        when(notificationRepository.existsByTypeAndReferenceIdAndReadFalse(any(), any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(11L);
            return n;
        });

        notificationService.notifyStockAlert("LOW_STOCK", "Stock bas", "Variante XS", 5L);

        ArgumentCaptor<NotificationDTO> dtoCap = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/store.42"), dtoCap.capture());
        NotificationDTO dto = dtoCap.getValue();
        assertThat(dto.id()).isEqualTo(11L);
        assertThat(dto.type()).isEqualTo("LOW_STOCK");
        assertThat(dto.title()).isEqualTo("Stock bas");
        assertThat(dto.referenceId()).isEqualTo(5L);
        assertThat(dto.read()).isFalse();
    }

    @Test
    @DisplayName("notifyStockAlert ignore les doublons non lus")
    void stockAlertSkipsDuplicate() {
        when(notificationRepository.existsByTypeAndReferenceIdAndReadFalse("OUT_OF_STOCK", 9L)).thenReturn(true);

        notificationService.notifyStockAlert("OUT_OF_STOCK", "Rupture", "msg", 9L);

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
        verify(messagingTemplate, org.mockito.Mockito.never()).convertAndSend(any(String.class), any(Object.class));
    }
}
