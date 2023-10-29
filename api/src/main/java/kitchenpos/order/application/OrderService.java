package kitchenpos.order.application;

import java.util.List;
import java.util.stream.Collectors;
import kitchenpos.order.application.dto.MenuQuantityDto;
import kitchenpos.order.application.dto.OrderRequest;
import kitchenpos.order.application.dto.OrderResponse;
import kitchenpos.order.application.dto.OrderStatusChangeRequest;
import kitchenpos.order.domain.Order;
import kitchenpos.order.domain.OrderLineItem;
import kitchenpos.order.domain.OrderRepository;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.order.domain.OrderValidator;
import kitchenpos.order.domain.OrderedItem;
import kitchenpos.order.domain.OrderedItemGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderValidator orderValidator;
    private final OrderedItemGenerator orderedItemGenerator;

    public OrderService(final OrderRepository orderRepository,
                        final OrderValidator orderValidator,
                        final OrderedItemGenerator orderedItemGenerator) {
        this.orderRepository = orderRepository;
        this.orderValidator = orderValidator;
        this.orderedItemGenerator = orderedItemGenerator;
    }

    @Transactional
    public OrderResponse create(final OrderRequest orderRequest) {
        final Order order = Order.createDefault(orderRequest.getOrderTableId(), convertToOrderLineItems(orderRequest.getOrderLineItems()));
        orderValidator.validate(order);
        orderRepository.save(order);

        return OrderResponse.from(order);
    }

    private List<OrderLineItem> convertToOrderLineItems(final List<MenuQuantityDto> menuQuantities) {
        return menuQuantities
            .stream()
            .map(menuIdWithQuantity -> new OrderLineItem(
                createOrderedItemFromOrderedMenu(menuIdWithQuantity.getMenuId()),
                menuIdWithQuantity.getQuantity())
            )
            .collect(Collectors.toList());
    }

    private OrderedItem createOrderedItemFromOrderedMenu(final Long menuId) {
        return orderedItemGenerator.generate(menuId);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        final List<Order> orders = orderRepository.findAll();

        return orders.stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse changeOrderStatus(final Long orderId, final OrderStatusChangeRequest changeRequest) {
        final Order order = orderRepository.findById(orderId)
            .orElseThrow(IllegalArgumentException::new);

        final OrderStatus orderStatus = OrderStatus.from(changeRequest.getOrderStatus());
        order.changeOrderStatus(orderStatus);

        return OrderResponse.from(order);
    }
}