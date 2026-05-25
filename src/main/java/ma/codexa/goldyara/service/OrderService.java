package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.CartItemDTO;
import ma.codexa.goldyara.dto.CustomerDTO;
import ma.codexa.goldyara.dto.OrderDTO;
import ma.codexa.goldyara.mapper.OrderMapper;
import ma.codexa.goldyara.mapper.ProductMapper;
import ma.codexa.goldyara.entity.Customer;
import ma.codexa.goldyara.entity.Order;
import ma.codexa.goldyara.entity.OrderItem;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.entity.PromoCode;
import ma.codexa.goldyara.repository.CustomerRepository;
import ma.codexa.goldyara.repository.OrderRepository;
import ma.codexa.goldyara.repository.PromoCodeRepository;
import ma.codexa.goldyara.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final PromoCodeRepository promoCodeRepository;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders with details");
        List<Order> orders = orderRepository.findAllWithDetails();
        hydrateProductImages(orders);
        return orders;
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        Optional<Order> opt = orderRepository.findByIdWithDetails(id);
        opt.ifPresent(o -> hydrateProductImages(List.of(o)));
        return opt;
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        Optional<Order> opt = orderRepository.findByOrderNumber(orderNumber);
        opt.ifPresent(o -> hydrateProductImages(List.of(o)));
        return opt;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(String status) {
        List<Order> orders = orderRepository.findByStatus(status);
        hydrateProductImages(orders);
        return orders;
    }

    /** Sans graphe Hibernate sur deux bags : initialiser lazy images + variants encore dans la session. */
    private static void hydrateProductImages(Iterable<Order> orders) {
        Objects.requireNonNull(orders);
        for (Order order : orders) {
            if (order == null || order.getOrderItems() == null) {
                continue;
            }
            for (OrderItem item : order.getOrderItems()) {
                if (item == null) {
                    continue;
                }
                Product p = item.getProduct();
                if (p != null) {
                    if (p.getImages() != null) {
                        p.getImages().size();
                    }
                    if (p.getVariants() != null) {
                        p.getVariants().size();
                    }
                }
            }
        }
    }
    public Order createOrder(Order order) {
        // Associate each OrderItem with Order
        for (OrderItem item : order.getOrderItems()) {
            item.setOrder(order);
        }
        order.calculateTotal();
        return orderRepository.save(order);
    }

    public OrderDTO updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        log.info("Order status updated orderId={} orderNumber={} newStatus={}", id, saved.getOrderNumber(), status);
        hydrateProductImages(List.of(saved));
        return orderMapper.toDTO(saved, productMapper);
    }

    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        orderRepository.delete(order);
    }

    /**
     * Cree la commande et retourne un DTO deja materialise (chargement images produits
     * encore dans la transaction — evite LazyInitializationException dans le controleur).
     */
    public OrderDTO createOrderFromDTO(OrderDTO orderDTO) {
        Order order = new Order();

        // Create customer
        CustomerDTO customerDTO = orderDTO.getCustomer();
        Customer customer = new Customer();
        customer.setFullName(customerDTO.getFullName());
        customer.setPhone(customerDTO.getPhone());
        customer.setAddress(customerDTO.getAddress());
        customer.setCity(customerDTO.getCity());
        customer.setEmail(customerDTO.getEmail());
        customer = customerRepository.save(customer);

        order.setCustomer(customer);
        order.setPaymentMethod(orderDTO.getPaymentMethod());
        order.setStatus("NEW");
        order.setNotes(customerDTO.getAddress() + ", " + customerDTO.getCity());

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemDTO cartItem : orderDTO.getItems()) {
            OrderItem orderItem = new OrderItem();
            Product product = productRepository.findById(Long.parseLong(cartItem.getProduct().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", Long.parseLong(cartItem.getProduct().getId())));
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            double unitPrice = cartItem.getProduct() != null && cartItem.getProduct().getPrice() != null
                    && cartItem.getProduct().getPrice() > 0
                    ? cartItem.getProduct().getPrice()
                    : product.getPrice();
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(cartItem.getQuantity() * unitPrice);
            orderItem.setSelectedSize(cartItem.getSelectedSize());
            orderItem.setSelectedGoldType(cartItem.getSelectedGoldType());
            if (cartItem.getSelectedVariantId() != null && !cartItem.getSelectedVariantId().isBlank()) {
                try {
                    orderItem.setSelectedVariantId(Long.parseLong(cartItem.getSelectedVariantId()));
                } catch (NumberFormatException ignored) {
                    orderItem.setSelectedVariantId(null);
                }
            }
            if (cartItem.getProduct() != null && cartItem.getProduct().getWeight() != null) {
                orderItem.setSelectedWeight(cartItem.getProduct().getWeight());
            }
            orderItem.setOrder(order);
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        order.calculateTotal();

        // Apply promo code discount if provided
        if (orderDTO.getPromoCode() != null && !orderDTO.getPromoCode().isBlank()) {
            order.setPromoCode(orderDTO.getPromoCode());
            Double discount = orderDTO.getDiscount();
            if (discount != null && discount > 0) {
                order.setDiscountAmount(discount);
                order.setTotalAmount(order.getTotalAmount() - discount);
            }
            // Increment promo code usage
            promoCodeRepository.findByCodeIgnoreCase(orderDTO.getPromoCode().trim()).ifPresent(pc -> {
                pc.incrementUses();
                promoCodeRepository.save(pc);
                log.info("Promo code used: {} (uses={})", pc.getCode(), pc.getCurrentUses());
            });
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created orderId={} orderNumber={} customerEmail={} totalAmount={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), customer.getEmail(), savedOrder.getTotalAmount());
        notificationService.notifyNewOrder(savedOrder);
        hydrateProductImages(List.of(savedOrder));
        return orderMapper.toDTO(savedOrder, productMapper);
    }
}
