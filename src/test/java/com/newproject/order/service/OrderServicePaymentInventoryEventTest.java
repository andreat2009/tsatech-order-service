package com.newproject.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newproject.order.domain.Order;
import com.newproject.order.domain.OrderItem;
import com.newproject.order.dto.PagedResponse;
import com.newproject.order.events.EventPublisher;
import com.newproject.order.repository.OrderCustomFieldValueRepository;
import com.newproject.order.repository.OrderItemRepository;
import com.newproject.order.repository.OrderRepository;
import com.newproject.order.repository.OrderReturnRecordRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OrderServicePaymentInventoryEventTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCustomFieldValueRepository orderCustomFieldValueRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderReturnRecordRepository orderReturnRecordRepository;

    @Mock
    private EventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderCustomFieldValueRepository, orderItemRepository, orderReturnRecordRepository, eventPublisher);
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

    @Test
    void paymentEventsDoNotOverrideAdminManagedStatuses() {
        Order order = orderWithStatus("Delivered");

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.syncStatusFromPayment(10L, "PAID");

        org.junit.jupiter.api.Assertions.assertEquals("Delivered", order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publish(eq("ORDER_UPDATED"), eq("order"), eq("10"), any());
    }

    @Test
    void paymentEventsStillUpdatePaymentLifecycleStatuses() {
        Order order = orderWithStatus("PAID");

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.syncStatusFromPayment(10L, "REFUNDED");

        org.junit.jupiter.api.Assertions.assertEquals("REFUNDED", order.getStatus());
        verify(orderRepository).save(order);
        verify(eventPublisher).publish(eq("ORDER_UPDATED"), eq("order"), eq("10"), any());
    }

    @Test
    void deletePendingOrderReleasesInventoryAndDeletesDependents() {
        Order order = orderWithStatus("PENDING_PAYMENT");
        OrderItem item = orderItem(order, 11L, 1008L, 2);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));

        orderService.delete(10L);

        verify(eventPublisher).publish(eq("ORDER_ITEM_RELEASED"), eq("order_item"), eq("11"), any());
        verify(orderReturnRecordRepository).deleteByOrderId(10L);
        verify(orderCustomFieldValueRepository).deleteByOrderId(10L);
        verify(orderItemRepository).deleteByOrderId(10L);
        verify(orderRepository).delete(order);
        verify(eventPublisher).publish("ORDER_CANCELLED", "order", "10", null);
    }

    @Test
    void deletePaidOrderDoesNotReleaseInventoryAgain() {
        Order order = orderWithStatus("PAID");
        OrderItem item = orderItem(order, 11L, 1008L, 2);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));

        orderService.delete(10L);

        verify(eventPublisher, never()).publish(eq("ORDER_ITEM_RELEASED"), eq("order_item"), eq("11"), any());
        verify(orderRepository).delete(order);
    }

    @Test
    void listPagedReturnsMetadataAndContent() {
        Order order = orderWithStatus("PENDING_PAYMENT");
        order.setCreatedAt(OffsetDateTime.now());
        when(orderRepository.findByCustomerId(eq(10L), any())).thenReturn(new PageImpl<>(List.of(order), PageRequest.of(1, 5), 12));
        when(orderCustomFieldValueRepository.findByOrderIdOrderByIdAsc(10L)).thenReturn(List.of());

        PagedResponse<?> response = orderService.listPaged(10L, 1, 5);

        verify(orderRepository).findByCustomerId(eq(10L), any());
        org.junit.jupiter.api.Assertions.assertEquals(1, response.getPage());
        org.junit.jupiter.api.Assertions.assertEquals(5, response.getSize());
        org.junit.jupiter.api.Assertions.assertEquals(12, response.getTotalElements());
        org.junit.jupiter.api.Assertions.assertEquals(3, response.getTotalPages());
        org.junit.jupiter.api.Assertions.assertEquals(1, response.getContent().size());
    }

    private Order orderWithStatus(String status) {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(status);
        order.setCustomerId(10L);
        order.setCurrency("EUR");
        order.setTotal(new BigDecimal("10.00"));
        order.setGuestCheckout(false);
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
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
