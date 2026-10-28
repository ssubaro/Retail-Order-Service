# RetailOrder Service — Instrucciones para Claude Code

## Propósito

Este repositorio contiene un proyecto educativo para un microservicio de retail con Java y Spring Boot. El servicio gestiona productos, inventario y pedidos. Además de implementar la API, el proyecto incluye documentación OpenAPI/Swagger UI y una colección de Postman con pruebas automatizadas.

## Antes de hacer cambios

- Inspecciona la estructura del repositorio y sus archivos de configuración antes de proponer cambios.
- Revisa el estado de Git para detectar cambios locales. No sobrescribas cambios no relacionados.
- Examina especialmente:
  - `model/Order.java`
  - `model/OrderItem.java`
  - `model/Product.java`
- Conserva las clases existentes que sean válidas. No dupliques entidades ni cambies nombres o paquetes sin una razón documentada.
- Usa las versiones de Java, Spring Boot y las herramientas de construcción existentes. No actualices versiones sin justificarlo.

## Convenciones de implementación

- Mantén una separación clara entre API REST, lógica de negocio, persistencia y manejo de errores.
- Usa DTOs para peticiones y respuestas; no expongas entidades JPA directamente desde la API.
- Mantén nombres y convenciones coherentes con el proyecto existente.
- Centraliza el manejo de errores y usa códigos HTTP semánticos.
- Asegura que las operaciones de pedido e inventario que deban ser atómicas estén dentro de transacciones.
- Evita añadir dependencias, capas o infraestructura externa que no sean necesarias.
- No incluyas secretos ni credenciales reales en el código o la documentación.

## Reglas de negocio que deben preservarse

- La creación de un pedido debe validar existencias y actualizar el inventario de forma consistente.
- La cancelación debe evitar devolver existencias más de una vez.
- Las operaciones críticas —validación de stock, cálculo del total y confirmación del pedido— deben permanecer síncronas.
- Notificaciones, auditoría y alertas de reabastecimiento pueden ejecutarse de forma asíncrona cuando la solución implementada lo permita y esté documentada.
- Las decisiones de negocio que no estén definidas en el proyecto deben documentarse como supuestos, no como requisitos confirmados.

## Swagger UI y pruebas de API

- Mantén la documentación OpenAPI y Swagger UI compatibles con las dependencias y versiones del proyecto.
- Asegúrate de que las rutas, parámetros, DTOs y códigos de error documentados coincidan con la implementación.
- La colección Postman debe ser importable y probar los endpoints reales.
- Diseña la colección para que pueda ejecutarse desde Collection Runner, con variables claramente documentadas y sin depender de datos externos.
- Incluye pruebas de éxito y error, especialmente para stock insuficiente, recursos inexistentes y cancelación duplicada.
- Swagger UI se usa para explorar y probar endpoints manualmente; Postman Runner o Newman se usa para automatizar la colección.
- No afirmes que una solicitud o prueba fue verificada si no se ejecutó realmente.

## Verificación

Antes de dar por terminada una modificación:

1. Ejecuta las pruebas o la compilación disponibles para el cambio.
2. Verifica que los artefactos de Postman coincidan con las rutas reales.
3. Informa los comandos ejecutados y sus resultados.
4. Si una verificación no puede ejecutarse, explica qué falta y cómo ejecutarla localmente.

## Comunicación

- Explica las decisiones técnicas en español claro, especialmente cuando afecten al modelo, las transacciones o el comportamiento de la API.
- Al terminar una tarea, resume los archivos modificados, las pruebas ejecutadas, los resultados y los pendientes.
- Distingue claramente entre requisitos del proyecto, decisiones asumidas y recomendaciones.

## Estado actual (referencia rápida)

- Stack: Java 21, Spring Boot 4.1.2, H2 en memoria, `springdoc-openapi-starter-webmvc-ui:3.1.0` (compatible con Spring Boot 4.1.x; la línea 3.0.x de springdoc es para Spring Boot 4.0.x).
- Paquete base: `com.axity.course.retailorderservice` (la clase principal vive en `com.axity.course.retail_order_service` por diseño previo del repo; `scanBasePackages` lo cubre — no es necesario unificarlos).
- Entidades: `Product` (sku único, baja lógica vía `active`, `@Version` para bloqueo optimista), `Order` (`customerId`, `status` como enum `OrderStatus`, `stockReverted` para evitar doble reversión), `OrderItem`, `AuditLog` (evidencia de tareas asíncronas, sin FK hacia `orders`/`products` — ver `docs/DIAGRAMS.md`).
- Excepciones de dominio: `RecursoNoEncontradoException` (404; `ProductNotFoundException` la extiende), `StockInsuficienteException` (409), `PrecioInvalidoException` (400), `EstadoInvalidoException` (409 — genérica de estado: transición de pedido inválida, cancelación duplicada y reactivación duplicada; reutilizada entre `Order` y `Product`, antes se llamaba `EstadoPedidoInvalidoException`). `GlobalExceptionHandler` también cubre JSON malformado, parámetros con tipo inválido, violación de integridad (p. ej. SKU duplicado) y conflictos de bloqueo optimista.
- Endpoints: `GET/POST /api/products`, `GET/PUT/DELETE /api/products/{id}`, `POST /api/products/{id}/reactivate` (revierte la baja lógica; 409 si ya estaba activo) (filtros `category`, `minPrice`, `maxPrice` en el listado); `GET/POST /api/orders`, `GET /api/orders/{id}`, `PATCH /api/orders/{id}/status`, `POST /api/orders/{id}/cancel`; `GET /api/audit-logs` (filtros `entityType`, `entityId`).
- Reglas de estado de pedido: `PENDIENTE → EN_PROCESO → ENVIADO` solo avanza (vía `PATCH .../status`); la cancelación es un endpoint aparte y solo se permite desde `PENDIENTE` o `EN_PROCESO` (no desde `ENVIADO` ni dos veces) — es un supuesto documentado en `OrderService`, no un requisito explícito del PDF del curso; confirmar con el docente si el negocio real permite cancelar pedidos ya enviados.
- Asíncrono: sin broker externo. `OrderService`/`ProductService` publican eventos de dominio (`OrderCreatedEvent`, `OrderCancelledEvent`, `StockChangedEvent`) y `OrderDomainEventListener` los procesa con `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)` (pool propio en `AsyncConfig`). Ahí se registra la notificación simulada (solo log, no hay proveedor de correo/SMS real) y la auditoría en `audit_logs`; la alerta de reabastecimiento usa un umbral fijo de 5 unidades (constante `RESTOCK_THRESHOLD` en `OrderDomainEventListener`) — ajustar si el curso define otro valor.
- Documentación y pruebas: Swagger UI en `/swagger-ui/index.html`, OpenAPI en `/v3/api-docs`; colección Postman en `postman/` (ver `postman/README.md`), verificada con Newman (32/32 solicitudes, 64/64 aserciones).
- Diagramas Mermaid (clases y DER) en `docs/DIAGRAMS.md`.

### Comandos útiles

```bash
./mvnw compile          # compilar
./mvnw test             # ejecutar pruebas JUnit (solo contextLoads por ahora)
./mvnw spring-boot:run  # levantar la app en :8080
npx newman run postman/RetailOrderService.postman_collection.json -e postman/RetailOrderService.postman_environment.json
```