package com.newproject.order.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentEventListener(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${order.payment-events.topic:payment.events}", groupId = "${spring.application.name}-payment-events")
    public void onPaymentEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("eventType").asText("");
            if (!"PAYMENT_CREATED".equals(eventType) && !"PAYMENT_UPDATED".equals(eventType)) {
                return;
            }

            JsonNode paymentPayload = root.path("payload");
            Long orderId = asLong(paymentPayload.path("orderId"));
            String paymentStatus = paymentPayload.path("status").asText(null);
            String mappedOrderStatus = mapOrderStatus(paymentStatus);

            if (orderId == null || mappedOrderStatus == null) {
                logger.warn("Skipping payment event with invalid payload: {}", payload);
                return;
            }

            orderService.syncStatusFromPayment(orderId, mappedOrderStatus);
        } catch (Exception ex) {
            logger.warn("Unable to process payment event: {}", ex.getMessage());
        }
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String mapOrderStatus(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return null;
        }
        return switch (paymentStatus.toUpperCase(java.util.Locale.ROOT)) {
            case "CAPTURED", "AUTHORIZED", "SETTLED" -> "PAID";
            case "REFUNDED", "PARTIALLY_REFUNDED" -> "REFUNDED";
            case "FAILED", "CANCELLED" -> "PAYMENT_FAILED";
            case "PENDING_OFFLINE", "CREATED", "REDIRECT_REQUIRED", "APPROVED", "CAPTURE_PENDING", "PENDING_PAYMENT" -> "PENDING_PAYMENT";
            default -> null;
        };
    }
}
