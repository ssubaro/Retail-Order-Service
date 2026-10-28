package com.axity.course.retailorderservice.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.axity.course.retailorderservice.dto.OrderRequestDto;
import com.axity.course.retailorderservice.dto.OrderResponseDto;
import com.axity.course.retailorderservice.dto.OrderStatusUpdateDto;
import com.axity.course.retailorderservice.model.OrderStatus;
import com.axity.course.retailorderservice.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponseDto> findAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerId) {
        return orderService.findAll(status, customerId);
    }

    @GetMapping("/{id}")
    public OrderResponseDto findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto created = orderService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/status")
    public OrderResponseDto updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateDto request) {
        return orderService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponseDto cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
