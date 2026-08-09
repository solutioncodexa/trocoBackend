package ma.codexa.troco.unit;

import ma.codexa.troco.entity.StockSettings;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.repository.ProductVariantRepository;
import ma.codexa.troco.repository.StockMovementRepository;
import ma.codexa.troco.repository.StockSettingsRepository;
import ma.codexa.troco.service.AuditLogService;
import ma.codexa.troco.service.NotificationService;
import ma.codexa.troco.service.StockService;
import ma.codexa.troco.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit — StockService resolveSettings / getOrCreateSettings")
class StockServiceResolveSettingsTest {

    @Mock StockSettingsRepository stockSettingsRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock ProductVariantRepository productVariantRepository;
    @Mock ProductRepository productRepository;
    @Mock NotificationService notificationService;
    @Mock AuditLogService auditLog;

    @InjectMocks StockService stockService;

    @BeforeEach
    void setTenant() {
        TenantContext.setFournisseurId(7L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.setFournisseurId(null);
    }

    @Test
    @DisplayName("resolveSettings sans ligne DB → defaults en mémoire, aucun INSERT")
    void resolveSettingsDoesNotPersist() {
        when(stockSettingsRepository.findFirstByFournisseurId(7L)).thenReturn(Optional.empty());

        StockSettings settings = stockService.resolveSettings();

        assertThat(settings.getFournisseurId()).isEqualTo(7L);
        assertThat(settings.getDefaultSafetyStock()).isEqualTo(10);
        assertThat(settings.getId()).isNull();
        verify(stockSettingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateSettings sans ligne DB → persiste les defaults")
    void getOrCreatePersists() {
        when(stockSettingsRepository.findFirstByFournisseurId(7L)).thenReturn(Optional.empty());
        when(stockSettingsRepository.save(any(StockSettings.class))).thenAnswer(inv -> {
            StockSettings s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        StockSettings settings = stockService.getOrCreateSettings();

        assertThat(settings.getId()).isEqualTo(99L);
        verify(stockSettingsRepository).save(any(StockSettings.class));
    }

    @Test
    @DisplayName("resolveSettings réutilise la ligne existante")
    void resolveSettingsReturnsExisting() {
        StockSettings existing = new StockSettings();
        existing.setId(3L);
        existing.setFournisseurId(7L);
        existing.setDefaultSafetyStock(15);
        when(stockSettingsRepository.findFirstByFournisseurId(7L)).thenReturn(Optional.of(existing));

        StockSettings settings = stockService.resolveSettings();

        assertThat(settings.getId()).isEqualTo(3L);
        assertThat(settings.getDefaultSafetyStock()).isEqualTo(15);
        verify(stockSettingsRepository, never()).save(any());
    }
}
