package com.newproject.order.controller;

import com.newproject.order.dto.OrderItemRequest;
import com.newproject.order.dto.OrderItemResponse;
import com.newproject.order.service.OrderItemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderItemController {
    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @GetMapping("/{orderId}/items")
    public List<OrderItemResponse> list(@PathVariable Long orderId) {
        return orderItemService.listItems(orderId);
    }

    @PostMapping("/{orderId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponse add(@PathVariable Long orderId, @Valid @RequestBody OrderItemRequest request) {
        return orderItemService.addItem(orderId, request);
    }

    @PutMapping("/items/{itemId}")
    public OrderItemResponse update(@PathVariable Long itemId, @Valid @RequestBody OrderItemRequest request) {
        return orderItemService.updateItem(itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long itemId) {
        orderItemService.removeItem(itemId);
    }
}
