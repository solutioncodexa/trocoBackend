package ma.codexa.goldyara.service;

import ma.codexa.goldyara.dto.CustomOrderDTO;
import ma.codexa.goldyara.dto.CustomerDTO;
import ma.codexa.goldyara.entity.CustomOrder;
import ma.codexa.goldyara.entity.Customer;
import ma.codexa.goldyara.entity.Image;
import ma.codexa.goldyara.repository.CustomOrderRepository;
import ma.codexa.goldyara.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOrderService {

    private final CustomOrderRepository customOrderRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    public List<CustomOrder> getAllCustomOrders() {
        return customOrderRepository.findAll();
    }

    public Optional<CustomOrder> getCustomOrderById(Long id) {
        return customOrderRepository.findById(id);
    }

    public List<CustomOrder> getCustomOrdersByStatus(String status) {
        return customOrderRepository.findByStatus(status);
    }

    public CustomOrder createCustomOrder(CustomOrder customOrder) {
        return customOrderRepository.save(customOrder);
    }

    public CustomOrder updateCustomOrderStatus(Long id, String status) {
        CustomOrder customOrder = customOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Commande personnalisée non trouvée avec l'id: " + id));

        customOrder.setStatus(status);
        return customOrderRepository.save(customOrder);
    }

    public CustomOrder updateEstimatedPrice(Long id, Double estimatedPrice) {
        CustomOrder customOrder = customOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Commande personnalisée non trouvée avec l'id: " + id));

        customOrder.setEstimatedPrice(estimatedPrice);
        return customOrderRepository.save(customOrder);
    }

    public void deleteCustomOrder(Long id) {
        CustomOrder customOrder = customOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Commande personnalisée non trouvée avec l'id: " + id));
        customOrderRepository.delete(customOrder);
    }

    public CustomOrder createCustomOrderFromDTO(CustomOrderDTO dto) {
        CustomOrder customOrder = new CustomOrder();
        
        // Create or find customer
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
        customOrder.setWeightEstimation(dto.getWeight());
        customOrder.setDescription(dto.getDescription());
        customOrder.setStatus("PENDING");
        
        // Add image if provided
        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            Image image = new Image();
            image.setUrl(dto.getImageUrl());
            image.setCustomOrder(customOrder);
            image.setIsPrimary(true);
            image.setDisplayOrder(0);
            List<Image> images = new ArrayList<>();
            images.add(image);
            customOrder.setReferenceImages(images);
        }
        
        CustomOrder savedOrder = customOrderRepository.save(customOrder);
        notificationService.notifyNewCustomOrder(savedOrder);
        return savedOrder;
    }
}
