package com.newproject.order.service;

import com.newproject.order.domain.Order;
import com.newproject.order.domain.OrderItem;
import com.newproject.order.dto.OrderItemRequest;
import com.newproject.order.dto.OrderItemResponse;
import com.newproject.order.events.EventPublisher;
import com.newproject.order.exception.NotFoundException;
import com.newproject.order.repository.OrderItemRepository;
import com.newproject.order.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderItemService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EventPublisher eventPublisher;

    public OrderItemService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderItemResponse addItem(Long orderId, OrderItemRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(request.getProductId());
        item.setSku(request.getSku());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());

        OrderItem saved = orderItemRepository.save(item);
        eventPublisher.publish("ORDER_ITEM_ADDED", "order_item", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public OrderItemResponse updateItem(Long itemId, OrderItemRequest request) {
        OrderItem item = orderItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Order item not found"));

        item.setProductId(request.getProductId());
        item.setSku(request.getSku());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());

        OrderItem saved = orderItemRepository.save(item);
        eventPublisher.publish("ORDER_ITEM_UPDATED", "order_item", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> listItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void removeItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Order item not found"));
        orderItemRepository.delete(item);
        eventPublisher.publish("ORDER_ITEM_REMOVED", "order_item", itemId.toString(), null);
    }

    private OrderItemResponse toResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setOrderId(item.getOrder().getId());
        response.setProductId(item.getProductId());
        response.setSku(item.getSku());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        return response;
    }
}
