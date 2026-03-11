package com.newproject.order.service;

import com.newproject.order.domain.Order;
import com.newproject.order.dto.OrderRequest;
import com.newproject.order.dto.OrderResponse;
import com.newproject.order.events.EventPublisher;
import com.newproject.order.exception.NotFoundException;
import com.newproject.order.repository.OrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setCurrency(request.getCurrency());
        order.setTotal(request.getTotal());
        order.setStatus(request.getStatus() != null ? request.getStatus() : "NEW");
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerFirstName(request.getCustomerFirstName());
        order.setCustomerLastName(request.getCustomerLastName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerLocale(request.getCustomerLocale());
        order.setOrderComment(request.getOrderComment());
        order.setGuestCheckout(Boolean.TRUE.equals(request.getGuestCheckout()));
        OffsetDateTime now = OffsetDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        Order saved = orderRepository.save(order);
        eventPublisher.publish("ORDER_CREATED", "order", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found"));

        order.setCurrency(request.getCurrency());
        order.setTotal(request.getTotal());
        order.setStatus(request.getStatus() != null ? request.getStatus() : order.getStatus());
        if (request.getCustomerEmail() != null) {
            order.setCustomerEmail(request.getCustomerEmail());
        }
        if (request.getCustomerFirstName() != null) {
            order.setCustomerFirstName(request.getCustomerFirstName());
        }
        if (request.getCustomerLastName() != null) {
            order.setCustomerLastName(request.getCustomerLastName());
        }
        if (request.getCustomerPhone() != null) {
            order.setCustomerPhone(request.getCustomerPhone());
        }
        if (request.getCustomerLocale() != null) {
            order.setCustomerLocale(request.getCustomerLocale());
        }
        if (request.getOrderComment() != null) {
            order.setOrderComment(request.getOrderComment());
        }
        if (request.getGuestCheckout() != null) {
            order.setGuestCheckout(request.getGuestCheckout());
        }
        order.setUpdatedAt(OffsetDateTime.now());

        Order saved = orderRepository.save(order);
        eventPublisher.publish("ORDER_UPDATED", "order", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(Long customerId) {
        if (customerId != null) {
            return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
        return orderRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        orderRepository.delete(order);
        eventPublisher.publish("ORDER_CANCELLED", "order", id.toString(), null);
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setCurrency(order.getCurrency());
        response.setTotal(order.getTotal());
        response.setStatus(order.getStatus());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setCustomerFirstName(order.getCustomerFirstName());
        response.setCustomerLastName(order.getCustomerLastName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setCustomerLocale(order.getCustomerLocale());
        response.setOrderComment(order.getOrderComment());
        response.setGuestCheckout(order.getGuestCheckout());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
