package com.newproject.order.service;

import com.newproject.order.domain.Order;
import com.newproject.order.domain.OrderCustomFieldValue;
import com.newproject.order.domain.OrderItem;
import com.newproject.order.dto.OrderCustomFieldRequest;
import com.newproject.order.dto.OrderCustomFieldResponse;
import com.newproject.order.dto.OrderItemResponse;
import com.newproject.order.dto.OrderRequest;
import com.newproject.order.dto.OrderResponse;
import com.newproject.order.dto.PagedResponse;
import com.newproject.order.events.EventPublisher;
import com.newproject.order.exception.NotFoundException;
import com.newproject.order.repository.OrderCustomFieldValueRepository;
import com.newproject.order.repository.OrderItemRepository;
import com.newproject.order.repository.OrderRepository;
import com.newproject.order.repository.OrderReturnRecordRepository;
import com.newproject.order.security.RequestActor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderCustomFieldValueRepository orderCustomFieldValueRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReturnRecordRepository orderReturnRecordRepository;
    private final EventPublisher eventPublisher;
    private final RequestActor requestActor;

    public OrderService(
        OrderRepository orderRepository,
        OrderCustomFieldValueRepository orderCustomFieldValueRepository,
        OrderItemRepository orderItemRepository,
        OrderReturnRecordRepository orderReturnRecordRepository,
        EventPublisher eventPublisher,
        RequestActor requestActor
    ) {
        this.orderRepository = orderRepository;
        this.orderCustomFieldValueRepository = orderCustomFieldValueRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderReturnRecordRepository = orderReturnRecordRepository;
        this.eventPublisher = eventPublisher;
        this.requestActor = requestActor;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        requestActor.assertCustomerAccessIfAuthenticated(request.getCustomerId());

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
        syncCustomFields(saved, request.getCustomFields());
        eventPublisher.publish("ORDER_CREATED", "order", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        assertOrderMutationAllowed(order);

        if (request.getCustomerId() != null && !request.getCustomerId().equals(order.getCustomerId())) {
            if (!requestActor.isAdmin()) {
                throw new AccessDeniedException("You cannot reassign another customer's order");
            }
            order.setCustomerId(request.getCustomerId());
        }

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
        if (request.getCustomFields() != null) {
            syncCustomFields(saved, request.getCustomFields());
        }
        eventPublisher.publish("ORDER_UPDATED", "order", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        requestActor.assertCustomerAccessIfAuthenticated(order.getCustomerId());
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(Long customerId) {
        Long scopedCustomerId = requestActor.resolveScopedCustomerId(customerId);
        if (scopedCustomerId != null) {
            return orderRepository.findByCustomerIdOrderByCreatedAtDesc(scopedCustomerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listPaged(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.max(1, Math.min(size, MAX_PAGE_SIZE)),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Long scopedCustomerId = requestActor.resolveScopedCustomerId(customerId);
        Page<Order> result = scopedCustomerId != null
            ? orderRepository.findByCustomerId(scopedCustomerId, pageable)
            : orderRepository.findAll(pageable);

        return PagedResponse.from(result.map(this::toResponse));
    }

    @Transactional
    public void delete(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        requestActor.assertCustomerAccessIfAuthenticated(order.getCustomerId());

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(id);
        if (shouldReleaseReservationsOnDelete(order.getStatus())) {
            for (OrderItem item : orderItems) {
                eventPublisher.publish("ORDER_ITEM_RELEASED", "order_item", item.getId().toString(), toOrderItemResponse(item));
            }
        }

        orderReturnRecordRepository.deleteByOrderId(id);
        orderCustomFieldValueRepository.deleteByOrderId(id);
        orderItemRepository.deleteByOrderId(id);
        orderRepository.delete(order);
        eventPublisher.publish("ORDER_CANCELLED", "order", id.toString(), null);
    }

    @Transactional
    public void syncStatusFromPayment(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        String previousStatus = order.getStatus();
        if (status == null || status.equalsIgnoreCase(previousStatus) || !canPaymentStatusOverride(previousStatus)) {
            return;
        }
        order.setStatus(status);
        order.setUpdatedAt(OffsetDateTime.now());
        Order saved = orderRepository.save(order);
        eventPublisher.publish("ORDER_UPDATED", "order", saved.getId().toString(), toResponse(saved));
        publishInventoryLifecycleEvents(saved.getId(), previousStatus, status);
    }

    private void assertOrderMutationAllowed(Order order) {
        if (requestActor.isAuthenticated()) {
            requestActor.assertCustomerAccessIfAuthenticated(order.getCustomerId());
            return;
        }
        if (!Boolean.TRUE.equals(order.getGuestCheckout())) {
            throw new AccessDeniedException("Only guest orders can be updated without authentication");
        }
    }

    private void publishInventoryLifecycleEvents(Long orderId, String previousStatus, String currentStatus) {
        if (orderId == null || currentStatus == null) {
            return;
        }

        if (isPaidStatus(currentStatus) && !isPaidStatus(previousStatus)) {
            publishOrderItemInventoryEvents(orderId, "ORDER_ITEM_COMMITTED");
        }

        if (isFailureStatus(currentStatus) && !isFailureStatus(previousStatus)) {
            publishOrderItemInventoryEvents(orderId, "ORDER_ITEM_RELEASED");
        }
    }

    private void publishOrderItemInventoryEvents(Long orderId, String eventType) {
        for (OrderItem item : orderItemRepository.findByOrderId(orderId)) {
            eventPublisher.publish(eventType, "order_item", item.getId().toString(), toOrderItemResponse(item));
        }
    }

    private boolean isPaidStatus(String status) {
        return status != null && "PAID".equalsIgnoreCase(status);
    }

    private boolean isFailureStatus(String status) {
        return status != null
            && ("PAYMENT_FAILED".equalsIgnoreCase(status)
            || "PAYMENT_CANCELLED".equalsIgnoreCase(status)
            || "PAYMENT_CANCELED".equalsIgnoreCase(status)
            || "CANCELLED".equalsIgnoreCase(status));
    }

    private boolean canPaymentStatusOverride(String currentStatus) {
        if (currentStatus == null || currentStatus.isBlank()) {
            return true;
        }
        return switch (currentStatus.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "NEW",
                "CREATED",
                "PENDING_PAYMENT",
                "REDIRECT_REQUIRED",
                "APPROVED",
                "CAPTURE_PENDING",
                "PENDING_OFFLINE",
                "PAYMENT_FAILED",
                "PAYMENT_CANCELLED",
                "PAYMENT_CANCELED",
                "PAID",
                "REFUNDED" -> true;
            default -> false;
        };
    }

    private boolean shouldReleaseReservationsOnDelete(String status) {
        if (status == null) {
            return true;
        }
        return "NEW".equalsIgnoreCase(status)
            || "PENDING_PAYMENT".equalsIgnoreCase(status)
            || "CREATED".equalsIgnoreCase(status)
            || "REDIRECT_REQUIRED".equalsIgnoreCase(status)
            || "APPROVED".equalsIgnoreCase(status)
            || "CAPTURE_PENDING".equalsIgnoreCase(status)
            || "PENDING_OFFLINE".equalsIgnoreCase(status);
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setOrderId(item.getOrder().getId());
        response.setProductId(item.getProductId());
        response.setVariantKey(item.getVariantKey());
        response.setVariantDisplayName(item.getVariantDisplayName());
        response.setSku(item.getSku());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        return response;
    }

    private void syncCustomFields(Order order, List<OrderCustomFieldRequest> requests) {
        orderCustomFieldValueRepository.deleteByOrderId(order.getId());
        if (requests == null || requests.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (OrderCustomFieldRequest request : requests) {
            if (request == null || isBlank(request.getFieldCode())) {
                continue;
            }
            OrderCustomFieldValue value = new OrderCustomFieldValue();
            value.setOrder(order);
            value.setFieldCode(trimToNull(request.getFieldCode()));
            value.setFieldLabel(trimToNull(request.getFieldLabel()) != null ? trimToNull(request.getFieldLabel()) : trimToNull(request.getFieldCode()));
            value.setFieldType(trimToNull(request.getFieldType()));
            value.setFieldScope(trimToNull(request.getFieldScope()));
            value.setFieldValue(trimToNull(request.getFieldValue()));
            value.setCreatedAt(now);
            orderCustomFieldValueRepository.save(value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        response.setCustomFields(orderCustomFieldValueRepository.findByOrderIdOrderByIdAsc(order.getId()).stream().map(value -> {
            OrderCustomFieldResponse field = new OrderCustomFieldResponse();
            field.setId(value.getId());
            field.setFieldCode(value.getFieldCode());
            field.setFieldLabel(value.getFieldLabel());
            field.setFieldType(value.getFieldType());
            field.setFieldScope(value.getFieldScope());
            field.setFieldValue(value.getFieldValue());
            return field;
        }).toList());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
