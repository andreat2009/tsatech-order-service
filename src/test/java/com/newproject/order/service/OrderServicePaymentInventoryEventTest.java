package com.newproject.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newproject.order.domain.Order;
import com.newproject.order.domain.OrderItem;
import com.newproject.order.events.EventPublisher;
import com.newproject.order.repository.OrderCustomFieldValueRepository;
import com.newproject.order.repository.OrderItemRepository;
import com.newproject.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServicePaymentInventoryEventTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCustomFieldValueRepository orderCustomFieldValueRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private EventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderCustomFieldValueRepository, orderItemRepository, eventPublisher);
    }

    @Test
    void failedPaymentPublishesInventoryReleaseEvents() {
        Order order = orderWithStatus("PENDING_PAYMENT");
        OrderItem item = orderItem(order, 11L, 1008L, 3);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));

        orderService.syncStatusFromPayment(10L, "PAYMENT_FAILED");

        verify(eventPublisher).publish(eq("ORDER_UPDATED"), eq("order"), eq("10"), any());
        verify(eventPublisher).publish(eq("ORDER_ITEM_RELEASED"), eq("order_item"), eq("11"), any());
    }

    @Test
    void paidPaymentPublishesInventoryCommitEvents() {
        Order order = orderWithStatus("PENDING_PAYMENT");
        OrderItem item = orderItem(order, 11L, 1008L, 3);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));

        orderService.syncStatusFromPayment(10L, "PAID");

        verify(eventPublisher).publish(eq("ORDER_UPDATED"), eq("order"), eq("10"), any());
        verify(eventPublisher).publish(eq("ORDER_ITEM_COMMITTED"), eq("order_item"), eq("11"), any());
    }


    @Test
    void cancelledPaymentPublishesInventoryReleaseEvents() {
        Order order = orderWithStatus("PENDING_PAYMENT");
        OrderItem item = orderItem(order, 11L, 1008L, 3);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));

        orderService.syncStatusFromPayment(10L, "PAYMENT_CANCELLED");

        verify(eventPublisher).publish(eq("ORDER_UPDATED"), eq("order"), eq("10"), any());
        verify(eventPublisher).publish(eq("ORDER_ITEM_RELEASED"), eq("order_item"), eq("11"), any());
    }

    private Order orderWithStatus(String status) {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(status);
        return order;
    }

    private OrderItem orderItem(Order order, Long itemId, Long productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setOrder(order);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setSku("SKU-1008");
        item.setName("Prodotto di test");
        item.setUnitPrice(new BigDecimal("2.00"));
        return item;
    }
}
