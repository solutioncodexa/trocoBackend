package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.CartItemDTO;
import ma.codexa.goldyara.dto.CustomerDTO;
import ma.codexa.goldyara.dto.OrderDTO;
import ma.codexa.goldyara.entity.Customer;
import ma.codexa.goldyara.entity.Order;
import ma.codexa.goldyara.entity.OrderItem;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.repository.CustomerRepository;
import ma.codexa.goldyara.repository.OrderRepository;
import ma.codexa.goldyara.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders with details");
        return orderRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findByIdWithDetails(id);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public Order createOrder(Order order) {
        // Associate each OrderItem with Order
        for (OrderItem item : order.getOrderItems()) {
            item.setOrder(order);
        }
        order.calculateTotal();
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        orderRepository.delete(order);
    }

    public Order createOrderFromDTO(OrderDTO orderDTO) {
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
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSelectedSize(cartItem.getSelectedSize());
            orderItem.setSelectedGoldType(cartItem.getSelectedGoldType());
            orderItem.setOrder(order);
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        order.calculateTotal();

        log.info("Creating new order for customer: {}", customer.getFullName());
        return orderRepository.save(order);
    }
}
