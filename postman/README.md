# Colección Postman — RetailOrder Service

Esta carpeta contiene todo lo necesario para probar la API end-to-end sin tocar código:

- `RetailOrderService.postman_collection.json` — colección con 33 solicitudes y 66 aserciones automatizadas.
- `RetailOrderService.postman_environment.json` — entorno con la variable `baseUrl` (por defecto `http://localhost:8080`).

## Qué cubre

| Carpeta | Casos |
|---|---|
| `01 - Productos` | Alta, SKU duplicado (409), precio inválido (400), stock superior a 10 000 (400), consulta por ID, no encontrado (404), listado con filtros (`category`, `minPrice`), actualización, actualización de inexistente (404), baja lógica, consulta tras baja (404), baja duplicada (404), reactivación, consulta tras reactivar, reactivación duplicada (409), reactivar inexistente (404) |
| `02 - Pedidos` | Alta con descuento de stock, verificación del descuento, stock insuficiente (409), producto inexistente (404), consulta por ID, no encontrado (404), listado por cliente, transición de estado inválida (409), transición válida, cancelación con reversión de stock, verificación de la reversión, cancelación duplicada (409) |
| `03 - Auditoría` | Verifica que las tareas asíncronas (auditoría de stock, alertas de reabastecimiento, notificación de pedido) realmente se ejecutaron, consultando `/api/audit-logs` |
| `04 - Documentación OpenAPI` | `/v3/api-docs` y `/swagger-ui/index.html` responden |

## Por qué el orden importa aquí (y está documentado)

La colección **sí depende del orden de ejecución**, de forma intencional y documentada: cada carpeta construye el estado (producto creado, pedido creado) que usan las solicitudes siguientes dentro de la misma carpeta. Esto es el comportamiento normal de Collection Runner (ejecuta de arriba hacia abajo) y de Newman (ejecuta en el orden del archivo). No se debe reordenar manualmente las solicitudes dentro de una carpeta.

Entre ejecuciones distintas, la colección es repetible: un script a nivel de colección genera un `runId` (timestamp) al inicio de cada corrida y lo usa para construir SKUs (`SKU-001-{{runId}}`) y el `customerId` (`CLI-{{runId}}`), evitando colisiones de SKU único aunque la base H2 en memoria conserve datos de corridas previas (la base se reinicia solo al reiniciar la aplicación).

## Antes de correr la colección: desactiva el retraso artificial de demo

`application.properties` define `app.async.demo-delay-ms=1500`: un retraso artificial pensado solo
para demos manuales en vivo (que se note en consola que la notificación/auditoría/alerta corren en
otro hilo, después de responder al cliente — ver README.md en la raíz). **No lo dejes activo al
correr la colección**: con 1500 ms, la carpeta `03 - Auditoria` puede llegar a consultar
`/api/audit-logs` antes de que el registro `CANCELLED` del pedido termine de escribirse (la
cancelación ocurre muy cerca, en la secuencia, de esa verificación), y la prueba falla de forma
intermitente — ya lo comprobé: con 1500 ms falló 1 de 66 aserciones; con 0 ms, 66/66 pasan siempre.

Arranca la aplicación con el retraso desactivado antes de correr la colección (Collection Runner o
Newman):

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.async.demo-delay-ms=0
```

Deja el valor por defecto (1500 ms) únicamente para una demo manual en vivo, sin correr la
colección completa a continuación.

## Ejecutar con Collection Runner (Postman)

1. Levanta la aplicación con el retraso en 0 (ver arriba), puerto 8080 por defecto.
2. Importa `RetailOrderService.postman_collection.json` y `RetailOrderService.postman_environment.json` en Postman.
3. Selecciona el entorno "RetailOrder Service - Local".
4. Abre la colección → **Run** (Collection Runner).
5. Deja el orden por defecto (carpetas 01 → 04) y ejecuta. Deberían pasar las 33 solicitudes / 66 aserciones.

## Ejecutar con Newman (terminal)

```bash
npx newman run postman/RetailOrderService.postman_collection.json \
  -e postman/RetailOrderService.postman_environment.json
```

(o `newman run ...` si lo tienes instalado globalmente: `npm install -g newman`)

Validado en este proyecto (con el retraso de demo en 0): 33/33 solicitudes y 66/66 aserciones
pasaron.

## Swagger UI vs. Postman Runner/Newman

- **Swagger UI** (`http://localhost:8080/swagger-ui/index.html`) sirve para **explorar y probar manualmente** la API: ver los DTOs, mandar una petición suelta y revisar la respuesta, mientras desarrollas o depuras.
- **Postman Collection Runner / Newman** automatiza la **ejecución completa** de los casos de prueba (éxito y error) de un solo clic o desde línea de comandos/CI, sin intervención manual.
