package com.axity.course.retailorderservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.axity.course.retailorderservice.model.Order;
import com.axity.course.retailorderservice.model.Product;
import com.axity.course.retailorderservice.repository.OrderRepository;
import com.axity.course.retailorderservice.repository.ProductRepository;
import com.axity.course.retailorderservice.service.AuditService;

/**
 * Escucha eventos de dominio una vez confirmada la transacción (AFTER_COMMIT)
 * y ejecuta en segundo plano (hilo separado vía @Async) las tareas que no deben
 * bloquear la respuesta HTTP: notificación simulada al cliente, auditoría de
 * movimientos de almacén y alertas de reabastecimiento.
 *
 * No se integra un proveedor de notificaciones ni un broker externo: para el
 * alcance de este proyecto educativo basta con el pool de hilos de Spring
 * (@Async) y con persistir la evidencia en la tabla audit_logs.
 */
@Component
public class OrderDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderDomainEventListener.class);
    private static final int RESTOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    /**
     * Retraso artificial (ms) antes de procesar cada evento. No tiene ningun proposito funcional:
     * en una base H2 local todo esto ocurre tan rapido (unos pocos milisegundos) que en una demo
     * en vivo parece sincrono aunque corra en otro hilo. Con este retraso se nota claramente que
     * el log aparece DESPUES de que la respuesta HTTP ya se envio. Se desactiva con
     * app.async.demo-delay-ms=0 (recomendado para pruebas automatizadas, ver postman/README.md).
     */
    @Value("${app.async.demo-delay-ms:0}")
    private long demoDelayMs;

    public OrderDomainEventListener(OrderRepository orderRepository, ProductRepository productRepository,
            AuditService auditService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        simulateDemoLatency();
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            logAsyncStart("pedido " + order.getId() + " creado");
            notifyCustomer(order, "Pedido recibido");
            auditService.record("ORDER", order.getId(), "CREATED",
                    "Pedido creado para el cliente " + order.getCustomerId() + " con total " + order.getTotal());
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        simulateDemoLatency();
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            logAsyncStart("pedido " + order.getId() + " cancelado");
            notifyCustomer(order, "Pedido cancelado");
            auditService.record("ORDER", order.getId(), "CANCELLED",
                    "Pedido cancelado y existencias revertidas para el cliente " + order.getCustomerId());
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockChanged(StockChangedEvent event) {
        simulateDemoLatency();
        productRepository.findById(event.productId()).ifPresent(product -> {
            logAsyncStart("stock de producto " + product.getId() + " cambio");
            auditService.record("PRODUCT", product.getId(), "STOCK_CHANGED",
                    "Nuevo stock de '" + product.getName() + "': " + product.getStock());
            checkRestockAlert(product);
        });
    }

    private void notifyCustomer(Order order, String subject) {
        log.info("[ASYNC] [Notificación simulada] {} -> cliente {} (pedido {})",
                subject, order.getCustomerId(), order.getId());
    }

    private void checkRestockAlert(Product product) {
        if (product.getStock() != null && product.getStock() <= RESTOCK_THRESHOLD) {
            log.warn("[ASYNC] [Alerta de reabastecimiento] Producto {} ({}) con stock bajo: {}",
                    product.getId(), product.getSku(), product.getStock());
            auditService.record("PRODUCT", product.getId(), "RESTOCK_ALERT",
                    "Stock en o por debajo del umbral mínimo (" + RESTOCK_THRESHOLD + "): quedan "
                            + product.getStock() + " unidades");
        }
    }

    private void logAsyncStart(String what) {
        log.info("[ASYNC] (hilo: {}) procesando en segundo plano -> {} (ya se respondio al cliente)",
                Thread.currentThread().getName(), what);
    }

    private void simulateDemoLatency() {
        if (demoDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(demoDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
