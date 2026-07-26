package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.CustomOrderDTO;
import ma.codexa.troco.dto.CustomOrderStatsDTO;
import ma.codexa.troco.dto.CustomerDTO;
import ma.codexa.troco.entity.CustomOrder;
import ma.codexa.troco.entity.Customer;
import ma.codexa.troco.entity.Image;
import ma.codexa.troco.mapper.CustomOrderMapper;
import ma.codexa.troco.repository.CustomOrderRepository;
import ma.codexa.troco.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomOrderService {

    private final CustomOrderRepository customOrderRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    private final CustomOrderMapper customOrderMapper;

    @Transactional(readOnly = true)
    public List<CustomOrder> getAllCustomOrders() {
        return customOrderRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Page<CustomOrder> getCustomOrdersPage(String status, String keyword, Pageable pageable) {
        String s = (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) ? status.toUpperCase() : null;
        boolean applyKeywordFilter = keyword != null && !keyword.isBlank();
        String keywordPattern = applyKeywordFilter ? "%" + keyword.trim().toLowerCase() + "%" : "%";
        return customOrderRepository.findWithFilters(s, applyKeywordFilter, keywordPattern, pageable);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return customOrderRepository.countByStatus(status.toUpperCase());
    }

    @Transactional(readOnly = true)
    public CustomOrderStatsDTO getCustomOrderStats() {
        return new CustomOrderStatsDTO(
                customOrderRepository.count(),
                customOrderRepository.countByStatus("PENDING"),
                customOrderRepository.countByStatus("CONTACTED"),
                customOrderRepository.countByStatus("COMPLETED")
        );
    }

    @Transactional(readOnly = true)
    public Optional<CustomOrder> getCustomOrderById(Long id) {
        return customOrderRepository.findDetailedById(id);
    }

    @Transactional(readOnly = true)
    public List<CustomOrder> getCustomOrdersByStatus(String status) {
        return customOrderRepository.findByStatusWithDetails(status);
    }

    public CustomOrder createCustomOrder(CustomOrder customOrder) {
        return customOrderRepository.save(customOrder);
    }

    public CustomOrderDTO updateCustomOrderStatus(Long id, String status) {
        CustomOrder customOrder = customOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        customOrder.setStatus(status);
        customOrderRepository.save(customOrder);
        log.info("custom_order_status_updated customOrderId={} status={}", id, status);
        CustomOrder hydrated = customOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        return customOrderMapper.toDTO(hydrated);
    }

    public CustomOrderDTO updateEstimatedPrice(Long id, Double estimatedPrice) {
        CustomOrder customOrder = customOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        customOrder.setEstimatedPrice(estimatedPrice);
        customOrderRepository.save(customOrder);
        log.info("custom_order_price_updated customOrderId={} estimatedPrice={}", id, estimatedPrice);
        CustomOrder hydrated = customOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        return customOrderMapper.toDTO(hydrated);
    }

    public void deleteCustomOrder(Long id) {
        CustomOrder customOrder = customOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        customOrderRepository.delete(customOrder);
        log.info("custom_order_deleted customOrderId={}", id);
    }

    /** Cree la demande puis materialise le DTO dans la transaction (referenceImages/customer lazies). */
    public CustomOrderDTO createCustomOrderFromDTO(CustomOrderDTO dto) {
        CustomOrder customOrder = new CustomOrder();
        Long fid = ma.codexa.troco.tenant.TenantContext.getFournisseurId();
        customOrder.setFournisseurId(fid);

        CustomerDTO customerDTO = dto.getCustomer();
        Customer customer = new Customer();
        customer.setFournisseurId(fid);
        customer.setFullName(customerDTO.getFullName());
        customer.setPhone(customerDTO.getPhone());
        customer.setAddress(customerDTO.getAddress());
        customer.setCity(customerDTO.getCity());
        customer.setEmail(customerDTO.getEmail());
        customer = customerRepository.save(customer);

        customOrder.setCustomer(customer);
        customOrder.setProductType(dto.getType() != null ? dto.getType().toUpperCase() : null);
        customOrder.setStyle(dto.getStyle() != null ? dto.getStyle().toUpperCase() : null);
        customOrder.setSize(dto.getSize());
        customOrder.setWeightEstimation(dto.getWeight());
        customOrder.setDescription(dto.getDescription());
        customOrder.setStatus("PENDING");

        List<String> urls = dto.getReferenceImageUrls();
        if (urls == null || urls.isEmpty()) {
            String singleUrl = dto.getImageUrl();
            if (singleUrl != null && !singleUrl.isEmpty()) {
                urls = List.of(singleUrl);
            }
        }
        if (urls != null && !urls.isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (int i = 0; i < urls.size(); i++) {
                Image image = new Image();
                image.setUrl(urls.get(i));
                image.setCustomOrder(customOrder);
                image.setIsPrimary(i == 0);
                image.setDisplayOrder(i);
                images.add(image);
            }
            customOrder.setReferenceImages(images);
        }

        CustomOrder savedOrder = customOrderRepository.save(customOrder);
        log.info("custom_order_created customOrderId={} productType={} style={}",
                savedOrder.getId(), savedOrder.getProductType(), savedOrder.getStyle());
        notificationService.notifyNewCustomOrder(savedOrder);

        CustomOrder hydrated = customOrderRepository.findDetailedById(savedOrder.getId())
                .orElse(savedOrder);
        return customOrderMapper.toDTO(hydrated);
    }
}
