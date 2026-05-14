package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.CustomOrderDTO;
import ma.codexa.goldyara.dto.CustomerDTO;
import ma.codexa.goldyara.entity.CustomOrder;
import ma.codexa.goldyara.entity.Customer;
import ma.codexa.goldyara.entity.Image;
import ma.codexa.goldyara.mapper.CustomOrderMapper;
import ma.codexa.goldyara.repository.CustomOrderRepository;
import ma.codexa.goldyara.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
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
        CustomOrder hydrated = customOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        return customOrderMapper.toDTO(hydrated);
    }

    public CustomOrderDTO updateEstimatedPrice(Long id, Double estimatedPrice) {
        CustomOrder customOrder = customOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        customOrder.setEstimatedPrice(estimatedPrice);
        customOrderRepository.save(customOrder);
        CustomOrder hydrated = customOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        return customOrderMapper.toDTO(hydrated);
    }

    public void deleteCustomOrder(Long id) {
        CustomOrder customOrder = customOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande personnalisée", id));
        customOrderRepository.delete(customOrder);
    }

    /** Cree la demande puis materialise le DTO dans la transaction (referenceImages/customer lazies). */
    public CustomOrderDTO createCustomOrderFromDTO(CustomOrderDTO dto) {
        CustomOrder customOrder = new CustomOrder();

        CustomerDTO customerDTO = dto.getCustomer();
        Customer customer = new Customer();
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
        notificationService.notifyNewCustomOrder(savedOrder);

        CustomOrder hydrated = customOrderRepository.findDetailedById(savedOrder.getId())
                .orElse(savedOrder);
        return customOrderMapper.toDTO(hydrated);
    }
}
