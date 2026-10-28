# Diagramas — RetailOrder Service

## Diagrama de clases (modelo de dominio)

```mermaid
classDiagram
    class Product {
        -Long id
        -String sku
        -String name
        -String description
        -BigDecimal price
        -Integer stock
        -String category
        -boolean active
        -Long version
    }

    class Order {
        -Long id
        -String customerId
        -OrderStatus status
        -BigDecimal total
        -LocalDateTime createdAt
        -boolean stockReverted
        -List~OrderItem~ items
        +addItem(OrderItem)
        +removeItem(OrderItem)
    }

    class OrderItem {
        -Long id
        -Integer quantity
        -BigDecimal unitPrice
    }

    class OrderStatus {
        <<enumeration>>
        PENDIENTE
        EN_PROCESO
        ENVIADO
        CANCELADO
    }

    class AuditLog {
        -Long id
        -String entityType
        -Long entityId
        -String action
        -String message
        -LocalDateTime createdAt
    }

    Order "1" o-- "many" OrderItem : items
    OrderItem "many" --> "1" Product : product
    Order --> OrderStatus : status
```

`AuditLog` es independiente del modelo transaccional: no tiene relación JPA directa con `Order`/`Product`, se enlaza por `entityType` + `entityId` (referencia débil), ya que solo registra evidencia de las tareas asíncronas (ver `CLAUDE.md`, sección de procesamiento asíncrono).

## Diagrama entidad-relación (DER)

```mermaid
erDiagram
    PRODUCTS ||--o{ ORDER_ITEMS : "es referenciado por"
    ORDERS ||--o{ ORDER_ITEMS : contiene

    PRODUCTS {
        bigint id PK
        varchar sku UK
        varchar name
        varchar description
        numeric price
        int stock
        varchar category
        boolean active
        bigint version
    }

    ORDERS {
        bigint id PK
        varchar customer_id
        varchar status
        numeric total
        timestamp created_at
        boolean stock_reverted
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        numeric unit_price
    }

    AUDIT_LOGS {
        bigint id PK
        varchar entity_type
        bigint entity_id
        varchar action
        varchar message
        timestamp created_at
    }
```

`audit_logs` no tiene clave foránea hacia `products`/`orders`: se escribe de forma asíncrona y desacoplada (ver justificación en `CLAUDE.md`), por lo que modelarla con FK obligaría a acoplar el escritor asíncrono a la existencia síncrona del registro padre en el instante de guardado.
