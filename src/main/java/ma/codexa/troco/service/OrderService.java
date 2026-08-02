package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.CartItemDTO;
import ma.codexa.troco.dto.CustomerDTO;
import ma.codexa.troco.dto.OrderCreatedDTO;
import ma.codexa.troco.dto.OrderDTO;
import ma.codexa.troco.mapper.OrderMapper;
import ma.codexa.troco.mapper.ProductMapper;
import ma.codexa.troco.entity.Customer;
import ma.codexa.troco.entity.Order;
import ma.codexa.troco.entity.OrderItem;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.entity.PromoCode;
import ma.codexa.troco.entity.ShippingCarrier;
import ma.codexa.troco.entity.StoreSettings;
import ma.codexa.troco.repository.CustomerRepository;
import ma.codexa.troco.repository.OrderRepository;
import ma.codexa.troco.repository.PromoCodeRepository;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.repository.StoreSettingsRepository;

import java.math.BigDecimal;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    private final StockService stockService;
    private final PromoCodeRepository promoCodeRepository;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final AuditLogService auditLog;
    private final StoreWebhookDispatcher storeWebhookDispatcher;
    private final ShippingCarrierService shippingCarrierService;
    private final LoyaltyService loyaltyService;
    private final PaymentAuditService paymentAuditService;
    private final StoreSettingsRepository storeSettingsRepository;
    private final PlanEntitlementService planEntitlementService;
    private final StorePaymentGatewayService storePaymentGatewayService;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders with details");
        List<Order> orders = orderRepository.findAllWithDetails();
        hydrateProductGraphs(orders);
        return orders;
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersPage(String status, String keyword, Pageable pageable) {
        String s = (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) ? status.toUpperCase() : null;
        boolean applyKeywordFilter = keyword != null && !keyword.isBlank();
        String keywordPattern = applyKeywordFilter ? "%" + keyword.trim().toLowerCase() + "%" : "%";
        Page<Order> page = orderRepository.findWithFilters(s, applyKeywordFilter, keywordPattern, pageable);
        hydrateProductGraphs(page.getContent());
        return page;
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return orderRepository.countByStatus(status.toUpperCase());
    }

    @Transactional(readOnly = true)
    public Optional<OrderDTO> getOrderDtoById(Long id) {
        return orderRepository.findByIdWithDetails(id).map(this::toOrderDto);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        Optional<Order> opt = orderRepository.findByIdWithDetails(id);
        opt.ifPresent(o -> hydrateProductGraphs(List.of(o)));
        return opt;
    }

    @Transactional(readOnly = true)
    public Optional<OrderDTO> getOrderDtoByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).map(this::toOrderDto);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        Optional<Order> opt = orderRepository.findByOrderNumber(orderNumber);
        opt.ifPresent(o -> hydrateProductGraphs(List.of(o)));
        return opt;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(String status) {
        List<Order> orders = orderRepository.findByStatus(status);
        hydrateProductGraphs(orders);
        return orders;
    }

    /**
     * Materialise le DTO encore dans la transaction (open-in-view=false) :
     * category / images / variants sont lazy et provoquent un 500 sinon.
     */
    private OrderDTO toOrderDto(Order order) {
        hydrateProductGraphs(List.of(order));
        return orderMapper.toDTO(order, productMapper);
    }

    /** Initialise les associations lazy nécessaires au mapping produit. */
    private static void hydrateProductGraphs(Iterable<Order> orders) {
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
                if (p == null) {
                    continue;
                }
                Hibernate.initialize(p.getCategory());
                Hibernate.initialize(p.getImages());
                Hibernate.initialize(p.getVariants());
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
        String previous = order.getStatus();
        String newStatus = status != null ? status.toUpperCase() : status;
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        if ("CANCELLED".equalsIgnoreCase(newStatus) && !"CANCELLED".equalsIgnoreCase(previous)) {
            stockService.restoreForOrder(saved);
        }
        log.info("Order status updated orderId={} orderNumber={} previous={} newStatus={}",
                id, saved.getOrderNumber(), previous, newStatus);
        auditLog.record(AuditLogService.Action.ORDER_UPDATE_STATUS, "ORDER", String.valueOf(id),
                "Commande " + saved.getOrderNumber() + ": " + previous + " → " + newStatus);
        return toOrderDto(saved);
    }

    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        String num = order.getOrderNumber();
        orderRepository.delete(order);
        auditLog.record(AuditLogService.Action.ORDER_DELETE, "ORDER", String.valueOf(id),
                "Suppression commande " + num);
    }

    /**
     * Crée la commande et retourne un accusé allégé (pas de lignes / client complets).
     */
    public OrderCreatedDTO createOrderFromDTO(OrderDTO orderDTO) {
        planEntitlementService.assertCanCreateOrder();
        Order order = new Order();
        Long fid = ma.codexa.troco.tenant.TenantContext.getFournisseurId();
        if (fid == null) {
            throw new BusinessException(
                    "Boutique non résolue — impossible de créer la commande",
                    HttpStatus.BAD_REQUEST);
        }
        order.setFournisseurId(fid);

        // Create customer
        CustomerDTO customerDTO = orderDTO.getCustomer();
        Customer customer = new Customer();
        customer.setFournisseurId(fid);
        customer.setFullName(customerDTO.getFullName());
        customer.setPhone(customerDTO.getPhone());
        customer.setAddress(customerDTO.getAddress());
        customer.setCity(customerDTO.getCity());
        customer.setEmail(customerDTO.getEmail());
        customer = customerRepository.save(customer);

        order.setCustomer(customer);
        String paymentMethod = normalizePaymentMethod(orderDTO.getPaymentMethod());
        storePaymentGatewayService.assertGatewayReadyForOrder(paymentMethod);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(resolveInitialPaymentStatus(paymentMethod));
        order.setStatus("NEW");
        order.setNotes(customerDTO.getAddress() + ", " + customerDTO.getCity());
        if (orderDTO.getCarrierCode() != null && !orderDTO.getCarrierCode().isBlank()) {
            order.setCarrierCode(orderDTO.getCarrierCode().trim().toUpperCase());
        }

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
            if (cartItem.getCustomLogoUrl() != null && !cartItem.getCustomLogoUrl().isBlank()) {
                orderItem.setCustomLogoUrl(cartItem.getCustomLogoUrl().trim());
            }
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

        // Shipping quote
        if (order.getCarrierCode() != null) {
            BigDecimal fee = shippingCarrierService.quote(
                    order.getCarrierCode(), BigDecimal.valueOf(order.getTotalAmount()));
            order.setShippingFee(fee.doubleValue());
            order.setTotalAmount(order.getTotalAmount() + fee.doubleValue());
        } else {
            order.setShippingFee(0.0);
        }

        // Loyalty redeem then earn
        LoyaltyService.RedeemResult redeemed = loyaltyService.redeem(
                customer.getPhone(),
                orderDTO.getLoyaltyPointsToRedeem(),
                order.getTotalAmount());
        if (redeemed.pointsRedeemed() > 0) {
            order.setLoyaltyPointsRedeemed(redeemed.pointsRedeemed());
            order.setDiscountAmount((order.getDiscountAmount() != null ? order.getDiscountAmount() : 0)
                    + redeemed.discountMad());
            order.setTotalAmount(Math.max(0, order.getTotalAmount() - redeemed.discountMad()));
        }
        int earned = loyaltyService.earn(customer.getPhone(), customer.getEmail(), order.getTotalAmount());
        order.setLoyaltyPointsEarned(earned);

        Order savedOrder = orderRepository.save(order);
        stockService.consumeForOrder(savedOrder);

        StoreSettings settings = storeSettingsRepository.findByFournisseurId(fid).orElse(null);
        String currency = settings != null && settings.getCurrency() != null ? settings.getCurrency() : "MAD";
        paymentAuditService.record(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                paymentProvider(paymentMethod),
                "ORDER_CREATED",
                BigDecimal.valueOf(savedOrder.getTotalAmount()),
                currency,
                savedOrder.getPaymentStatus(),
                "{\"paymentMethod\":\"" + paymentMethod + "\"}");

        log.info("Order created orderId={} orderNumber={} customerEmail={} totalAmount={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), customer.getEmail(), savedOrder.getTotalAmount());
        notificationService.notifyNewOrder(savedOrder);
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("orderId", savedOrder.getId());
            payload.put("orderNumber", savedOrder.getOrderNumber());
            payload.put("totalAmount", savedOrder.getTotalAmount());
            payload.put("customerName", customer.getFullName());
            payload.put("customerPhone", customer.getPhone());
            payload.put("customerEmail", customer.getEmail());
            payload.put("itemCount", savedOrder.getOrderItems() != null ? savedOrder.getOrderItems().size() : 0);
            storeWebhookDispatcher.dispatchAsync(fid, StoreWebhookDispatcher.EVENT_ORDER_CREATED, payload);
        } catch (Exception e) {
            log.warn("webhook_order_hook_failed: {}", e.getMessage());
        }
        return new OrderCreatedDTO(
                savedOrder.getId() != null ? savedOrder.getId().toString() : null,
                savedOrder.getOrderNumber(),
                savedOrder.getTotalAmount() != null ? savedOrder.getTotalAmount() : 0,
                savedOrder.getStatus() != null ? savedOrder.getStatus().toLowerCase() : "new",
                savedOrder.getPaymentStatus(),
                savedOrder.getLoyaltyPointsEarned()
        );
    }

    public OrderDTO updateTracking(Long id, String trackingNumber) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        order.setTrackingNumber(trackingNumber);
        if (order.getCarrierCode() != null && trackingNumber != null) {
            ShippingCarrier carrier = shippingCarrierService.requireEnabled(order.getCarrierCode());
            order.setTrackingUrl(shippingCarrierService.buildTrackingUrl(carrier, trackingNumber));
        }
        return toOrderDto(orderRepository.save(order));
    }

    private static String normalizePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) return "cash_on_delivery";
        String m = raw.trim().toLowerCase();
        return switch (m) {
            case "online", "card_cmi", "bnpl", "cash_on_delivery", "card_stripe", "stripe", "paypal" ->
                    "stripe".equals(m) ? "card_stripe" : m;
            default -> "cash_on_delivery";
        };
    }

    private static String resolveInitialPaymentStatus(String paymentMethod) {
        if (paymentMethod.startsWith("cash")) return "cod";
        // Stripe / PayPal : payés côté client avant création commande (capture confirmée).
        if ("card_stripe".equals(paymentMethod) || "paypal".equals(paymentMethod)) return "paid";
        // CMI confirmé côté client (retour ok / carte test) → traité comme payé à la création.
        if ("card_cmi".equals(paymentMethod) || "online".equals(paymentMethod)) return "paid";
        return "pending";
    }

    private static String paymentProvider(String method) {
        return switch (method) {
            case "card_cmi", "online" -> "CMI";
            case "card_stripe" -> "STRIPE";
            case "paypal" -> "PAYPAL";
            case "bnpl" -> "BNPL";
            default -> "COD";
        };
    }
}
