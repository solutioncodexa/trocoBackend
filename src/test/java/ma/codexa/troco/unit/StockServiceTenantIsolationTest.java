package ma.codexa.troco.unit;

import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.dto.StockVariantRowDTO;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.entity.ProductVariant;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit — StockService tenant isolation")
class StockServiceTenantIsolationTest {

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
    @DisplayName("listVariantRows charge uniquement les variantes du fournisseur courant")
    void listVariantsUsesTenantQuery() {
        StockSettings settings = new StockSettings();
        settings.setDefaultSafetyStock(10);
        settings.setExpiryAlertDays(30);
        when(stockSettingsRepository.findFirstByFournisseurId(7L)).thenReturn(Optional.of(settings));

        Product product = new Product();
        product.setId(1L);
        product.setName("Mine");
        product.setFournisseurId(7L);

        ProductVariant variant = new ProductVariant();
        variant.setId(11L);
        variant.setProduct(product);
        variant.setLabel("Standard");
        variant.setStock(5);
        variant.setPrice(10.0);

        when(productVariantRepository.findAllActiveWithProductForTenant(7L)).thenReturn(List.of(variant));

        PageResponse<StockVariantRowDTO> page = stockService.listVariantRows("all", 0, 50);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getProductName()).isEqualTo("Mine");
        verify(productVariantRepository).findAllActiveWithProductForTenant(7L);
        verify(productVariantRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("sans tenant → liste vide (pas de fuite cross-tenant)")
    void listVariantsEmptyWithoutTenant() {
        TenantContext.setFournisseurId(null);

        PageResponse<StockVariantRowDTO> page = stockService.listVariantRows("all", 0, 50);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        verify(productVariantRepository, never()).findAllActiveWithProductForTenant(anyLong());
    }
}
