# Colección Postman — RetailOrder Service

Esta carpeta contiene todo lo necesario para probar la API end-to-end sin tocar código:

- `RetailOrderService.postman_collection.json` — colección con 28 solicitudes y 56 aserciones automatizadas.
- `RetailOrderService.postman_environment.json` — entorno con la variable `baseUrl` (por defecto `http://localhost:8080`).

## Qué cubre

| Carpeta | Casos |
|---|---|
| `01 - Productos` | Alta, SKU duplicado (409), precio inválido (400), consulta por ID, no encontrado (404), listado con filtros (`category`, `minPrice`), actualización, actualización de inexistente (404), baja lógica, consulta tras baja (404), baja duplicada (404), reactivación, consulta tras reactivar, reactivación duplicada (409), reactivar inexistente (404) |
| `02 - Pedidos` | Alta con descuento de stock, verificación del descuento, stock insuficiente (409), producto inexistente (404), consulta por ID, no encontrado (404), listado por cliente, transición de estado inválida (409), transición válida, cancelación con reversión de stock, verificación de la reversión, cancelación duplicada (409) |
| `03 - Auditoría` | Verifica que las tareas asíncronas (auditoría de stock, alertas de reabastecimiento, notificación de pedido) realmente se ejecutaron, consultando `/api/audit-logs` |
| `04 - Documentación OpenAPI` | `/v3/api-docs` y `/swagger-ui/index.html` responden |

## Por qué el orden importa aquí (y está documentado)

La colección **sí depende del orden de ejecución**, de forma intencional y documentada: cada carpeta construye el estado (producto creado, pedido creado) que usan las solicitudes siguientes dentro de la misma carpeta. Esto es el comportamiento normal de Collection Runner (ejecuta de arriba hacia abajo) y de Newman (ejecuta en el orden del archivo). No se debe reordenar manualmente las solicitudes dentro de una carpeta.

Entre ejecuciones distintas, la colección es repetible: un script a nivel de colección genera un `runId` (timestamp) al inicio de cada corrida y lo usa para construir SKUs (`SKU-001-{{runId}}`) y el `customerId` (`CLI-{{runId}}`), evitando colisiones de SKU único aunque la base H2 en memoria conserve datos de corridas previas (la base se reinicia solo al reiniciar la aplicación).

## Ejecutar con Collection Runner (Postman)

1. Levanta la aplicación (`./mvnw spring-boot:run`, puerto 8080 por defecto).
2. Importa `RetailOrderService.postman_collection.json` y `RetailOrderService.postman_environment.json` en Postman.
3. Selecciona el entorno "RetailOrder Service - Local".
4. Abre la colección → **Run** (Collection Runner).
5. Deja el orden por defecto (carpetas 01 → 04) y ejecuta. Deberían pasar las 28 solicitudes / 56 aserciones.

## Ejecutar con Newman (terminal)

```bash
npx newman run postman/RetailOrderService.postman_collection.json \
  -e postman/RetailOrderService.postman_environment.json
```

(o `newman run ...` si lo tienes instalado globalmente: `npm install -g newman`)

Validado en este proyecto: 28/28 solicitudes y 56/56 aserciones pasaron, en dos corridas consecutivas sin reiniciar el servidor.

## Swagger UI vs. Postman Runner/Newman

- **Swagger UI** (`http://localhost:8080/swagger-ui/index.html`) sirve para **explorar y probar manualmente** la API: ver los DTOs, mandar una petición suelta y revisar la respuesta, mientras desarrollas o depuras.
- **Postman Collection Runner / Newman** automatiza la **ejecución completa** de los casos de prueba (éxito y error) de un solo clic o desde línea de comandos/CI, sin intervención manual.
