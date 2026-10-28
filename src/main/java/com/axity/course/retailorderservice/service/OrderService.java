package com.axity.course.retailorderservice.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axity.course.retailorderservice.dto.OrderItemRequestDto;
import com.axity.course.retailorderservice.dto.OrderItemResponseDto;
import com.axity.course.retailorderservice.dto.OrderRequestDto;
import com.axity.course.retailorderservice.dto.OrderResponseDto;
import com.axity.course.retailorderservice.event.OrderCancelledEvent;
import com.axity.course.retailorderservice.event.OrderCreatedEvent;
import com.axity.course.retailorderservice.event.StockChangedEvent;
import com.axity.course.retailorderservice.exception.EstadoInvalidoException;
import com.axity.course.retailorderservice.exception.PrecioInvalidoException;
import com.axity.course.retailorderservice.exception.RecursoNoEncontradoException;
import com.axity.course.retailorderservice.exception.StockInsuficienteException;
import com.axity.course.retailorderservice.model.Order;
import com.axity.course.retailorderservice.model.OrderItem;
import com.axity.course.retailorderservice.model.OrderStatus;
import com.axity.course.retailorderservice.model.Product;
import com.axity.course.retailorderservice.repository.OrderRepository;
import com.axity.course.retailorderservice.repository.ProductRepository;

@Service
@Transactional
public class OrderService {

    /**
     * Transiciones de avance permitidas vía PATCH /{id}/status. La cancelación
     * tiene su propio endpoint (POST /{id}/cancel) y no se puede alcanzar desde aquí,
     * y un pedido ENVIADO o CANCELADO ya no admite más cambios de estado por esta vía.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDIENTE, Set.of(OrderStatus.EN_PROCESO));
        ALLOWED_TRANSITIONS.put(OrderStatus.EN_PROCESO, Set.of(OrderStatus.ENVIADO));
        ALLOWED_TRANSITIONS.put(OrderStatus.ENVIADO, Set.of());
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELADO, Set.of());
    }

    /** Estados de pedido desde los que todavía se admite cancelación formal. */
    private static final Set<OrderStatus> CANCELABLE_STATUSES = Set.of(OrderStatus.PENDIENTE, OrderStatus.EN_PROCESO);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> findAll(OrderStatus status, String customerId) {
        Specification<Order> spec = buildFilterSpecification(status, customerId);
        return orderRepository.findAll(spec).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDto findById(Long id) {
        return toResponse(getOrder(id));
    }

    /**
     * Alta síncrona: valida existencia/estado de cada producto, comprueba stock,
     * calcula el total y descuenta el inventario dentro de la misma transacción.
     * El control de concurrencia se apoya en el @Version de Product (bloqueo
     * optimista): si dos pedidos compiten por el mismo stock, la segunda
     * escritura falla con un conflicto 409 y el cliente puede reintentar.
     */
    public OrderResponseDto create(OrderRequestDto request) {
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDIENTE);

        BigDecimal total = BigDecimal.ZERO;
        List<Long> affectedProductIds = new ArrayList<>();

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new RecursoNoEncontradoException("producto", itemRequest.productId()));

            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PrecioInvalidoException("El producto " + product.getId() + " tiene un precio inválido");
            }
            if (product.getStock() < itemRequest.quantity()) {
                throw new StockInsuficienteException(product.getId(), itemRequest.quantity(), product.getStock());
            }

            product.setStock(product.getStock() - itemRequest.quantity());
            productRepository.save(product);
            affectedProductIds.add(product.getId());

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            total = total.add(lineTotal);

            order.addItem(new OrderItem(product, itemRequest.quantity(), product.getPrice()));
        }

        order.setTotal(total);
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCreatedEvent(saved.getId()));
        affectedProductIds.forEach(productId -> eventPublisher.publishEvent(new StockChangedEvent(productId)));

        return toResponse(saved);
    }

    public OrderResponseDto updateStatus(Long id, OrderStatus targetStatus) {
        Order order = getOrder(id);

        if (targetStatus == OrderStatus.CANCELADO) {
            throw new EstadoInvalidoException(
                    "Use el endpoint de cancelación (POST /api/orders/" + id + "/cancel) para cancelar un pedido");
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new EstadoInvalidoException(
                    "Transición de estado no permitida: " + order.getStatus() + " -> " + targetStatus);
        }

        order.setStatus(targetStatus);
        return toResponse(orderRepository.save(order));
    }

    /**
     * Cancelación formal: revierte el stock de cada ítem solo si aún no se había
     * revertido (idempotencia explícita vía stockReverted), además de impedir la
     * doble cancelación mediante el propio estado del pedido.
     */
    public OrderResponseDto cancel(Long id) {
        Order order = getOrder(id);

        if (order.getStatus() == OrderStatus.CANCELADO) {
            throw new EstadoInvalidoException("El pedido " + id + " ya se encuentra cancelado");
        }
        if (!CANCELABLE_STATUSES.contains(order.getStatus())) {
            throw new EstadoInvalidoException(
                    "No se puede cancelar un pedido en estado " + order.getStatus());
        }

        List<Long> affectedProductIds = new ArrayList<>();
        if (!order.isStockReverted()) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
                affectedProductIds.add(product.getId());
            }
            order.setStockReverted(true);
        }

        order.setStatus(OrderStatus.CANCELADO);
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCancelledEvent(saved.getId()));
        affectedProductIds.forEach(productId -> eventPublisher.publishEvent(new StockChangedEvent(productId)));

        return toResponse(saved);
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("pedido", id));
    }

    private Specification<Order> buildFilterSpecification(OrderStatus status, String customerId) {
        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (customerId != null && !customerId.isBlank()) {
                predicates.add(builder.equal(root.get("customerId"), customerId));
            }
            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private OrderResponseDto toResponse(Order order) {
        List<OrderItemResponseDto> items = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();
        return new OrderResponseDto(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                items);
    }
}
