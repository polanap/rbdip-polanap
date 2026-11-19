package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Оформление заказа: координирует валидацию, расчёт цены, сохранение и
 * уведомление клиента, делегируя каждую ответственность отдельному классу.
 */
@Service
public class OrderService {

    private static final String DEFAULT_CUSTOMER_TYPE = "regular";
    private static final int DEFAULT_QUANTITY = 1;
    private static final String NEW_ORDER_STATUS = "new";

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderValidator orderValidator;
    private final PricingCalculator pricingCalculator;
    private final OrderNotifier orderNotifier;

    public OrderService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderValidator orderValidator,
            PricingCalculator pricingCalculator,
            OrderNotifier orderNotifier) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderValidator = orderValidator;
        this.pricingCalculator = pricingCalculator;
        this.orderNotifier = orderNotifier;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        orderValidator.validate(request);
        List<OrderLine> lines = request.items().stream().map(this::toOrderLine).toList();

        BigDecimal total = calculateTotal(lines, request);

        Order order = orderRepository.save(new Order(
                request.customerFullName(), request.customerAddress(), request.customerPhone(), NEW_ORDER_STATUS));
        saveItems(order.getId(), lines);

        orderNotifier.sendConfirmation(request.customerFullName(), order.getId(), total);
        return order;
    }

    private OrderLine toOrderLine(CreateOrderRequest.Item raw) {
        Product product = productRepository.findById(raw.productId())
                .orElseThrow(() -> new IllegalArgumentException("product " + raw.productId() + " not found"));
        int quantity = raw.quantity() == null ? DEFAULT_QUANTITY : raw.quantity();
        orderValidator.validateQuantity(quantity);
        return new OrderLine(product, quantity);
    }

    private BigDecimal calculateTotal(List<OrderLine> lines, CreateOrderRequest request) {
        List<PricingCalculator.LineItem> lineItems = lines.stream()
                .map(line -> new PricingCalculator.LineItem(line.product().getPrice(), line.quantity()))
                .toList();
        String customerType = request.customerType() == null ? DEFAULT_CUSTOMER_TYPE : request.customerType();
        return pricingCalculator.calculateOrderTotal(lineItems, customerType, request.couponCode());
    }

    private void saveItems(Long orderId, List<OrderLine> lines) {
        for (OrderLine line : lines) {
            Product product = line.product();
            orderItemRepository.save(new OrderItem(orderId, product.getName(), product.getPrice(), line.quantity()));
        }
    }

    private record OrderLine(Product product, int quantity) {
    }
}
