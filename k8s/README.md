# RetailOrder Service en Kubernetes (Docker Desktop)

Despliegue del microservicio Spring Boot siguiendo el mismo esquema del ejercicio
`Kubernetes/proyecto_integrador_k8s`: los manifiestos se aplican en orden 01 → 04.

```
                 localhost:8080
                       │
          Service LoadBalancer (04)
           ┌───────────┼───────────┐
        pod app     pod app     pod app      Deployment, 3 réplicas stateless (02)
           └───────────┼───────────┘
                       │  jdbc:postgresql://postgres:5432/retaildb
             Service ClusterIP "postgres" (03)
                       │
                  pod postgres ── PVC postgres-pvc (1Gi)   (03)
```

| Archivo | Qué crea | Concepto |
|---|---|---|
| `01-config-and-secret.yaml` | ConfigMap `retail-order-config` + Secret `retail-order-secret` | Configuración fuera de la imagen; credenciales de la BD |
| `02-deployment.yaml` | Deployment `retail-order-deployment` (3 réplicas + initContainer) | Réplicas stateless, probes, rolling update |
| `03-postgres.yaml` | ConfigMap `postgres-init` (init.sql) + PVC `postgres-pvc` + Deployment y Service `postgres` | Persistencia con PVC, datos precargados, DNS interno |
| `04-service.yaml` | Service `retail-order-service` tipo LoadBalancer → `localhost:8080` | Balanceo de carga |

Puntos clave:
- **Health checks** con Spring Boot Actuator (`/actuator/health/liveness` y `/readiness`).
- **ConfigMap/Secret con efecto real**: Spring Boot convierte variables de entorno en propiedades
  (`SPRING_DATASOURCE_URL` → `spring.datasource.url`). La misma imagen corre con H2 en local y con PostgreSQL en el clúster.
- **Secret como única fuente de verdad**: `SPRING_DATASOURCE_USERNAME/PASSWORD` los usa la app (`envFrom`) y Postgres
  para crear su usuario (`secretKeyRef` → `POSTGRES_USER/PASSWORD`).
- **Datos precargados**: `init.sql` (ConfigMap montado como archivo en `/docker-entrypoint-initdb.d`) crea las tablas,
  8 productos y 4 pedidos **solo la primera vez**, cuando el PVC está vacío.
- **`ddl-auto=validate`**: Hibernate no crea tablas, solo verifica que el esquema de `init.sql` coincida con las entidades.
  Si no coincide, la app no arranca y el motivo aparece en `kubectl logs`.
- **initContainer**: el pod de la app espera (`Init:0/1`) a que Postgres acepte conexiones antes de arrancar Spring.
- **`GET /`**: página de bienvenida con versión, entorno y el pod que respondió.
- Si tu Docker Desktop usa el clúster tipo *kind* (nodo `desktop-control-plane`), el StorageClass se llama
  `standard` en vez de `hostpath`. El PVC no fija ninguno, así que funciona igual.

## 0. Preparación
```powershell
kubectl config current-context        # docker-desktop
kubectl get nodes                     # STATUS Ready
```

## 1. Construir la imagen
Desde la carpeta del proyecto (donde está el `Dockerfile`). **Usa una etiqueta nueva por cada cambio de código**:
con `imagePullPolicy: IfNotPresent`, si reutilizas una etiqueta el nodo sigue usando la imagen vieja que tiene en caché.
```powershell
docker build --build-arg APP_VERSION=v5 -t retail-order-service:v5 .
```
Docker Desktop comparte las imágenes con su clúster, así que no hace falta `docker push`.

## 2. Aplicar los manifiestos
```powershell
kubectl apply -f k8s/01-config-and-secret.yaml
kubectl apply -f k8s/02-deployment.yaml
kubectl get pods -l app=retail-order-service       # Init:0/1 -> esperan a Postgres
kubectl apply -f k8s/03-postgres.yaml
kubectl rollout status deployment/postgres
kubectl rollout status deployment/retail-order-deployment
kubectl apply -f k8s/04-service.yaml
```
O todo junto: `kubectl apply -f k8s/` (el initContainer resuelve el orden de arranque).

## 3. ConfigMap y Secret
```powershell
kubectl get configmap retail-order-config -o yaml
kubectl exec deploy/retail-order-deployment -- env | Select-String "SPRING|APP_"

# base64 NO es cifrado
$b64 = kubectl get secret retail-order-secret -o jsonpath='{.data.SPRING_DATASOURCE_PASSWORD}'
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))

# Postgres recibe las mismas credenciales del Secret
kubectl exec deploy/postgres -- env | Select-String "POSTGRES_"
```

## 4. PostgreSQL y datos precargados
```powershell
kubectl get pvc postgres-pvc                                           # STATUS Bound
kubectl logs deploy/postgres | Select-String "init.sql|CREATE TABLE|INSERT"
kubectl exec deploy/postgres -- psql -U retail_admin -d retaildb -c "SELECT sku, name, stock FROM products;"
kubectl exec deploy/postgres -- psql -U retail_admin -d retaildb -c "SELECT customer_id, status, total FROM orders;"
```

## 5. Service LoadBalancer
```powershell
kubectl get svc retail-order-service
kubectl get endpointslices -l kubernetes.io/service-name=retail-order-service   # las 3 IPs de los pods
curl.exe -s http://localhost:8080/
curl.exe -s http://localhost:8080/actuator/health
```
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Postman: importa la colección de `postman/` con `baseUrl = http://localhost:8080`.

**Demostrar el balanceo:** `GET /` y `/actuator/info` dicen qué pod atendió. `curl.exe` abre una conexión nueva por
llamada y kube-proxy elige un pod al azar por conexión (`Invoke-WebRequest` reutiliza la conexión y no sirve para esto):
```powershell
1..30 | ForEach-Object { (curl.exe -s http://localhost:8080/ | ConvertFrom-Json).pod } |
  Group-Object | Select-Object Count, Name

# En vivo, una línea por petición (Ctrl+C para parar). En otra terminal: kubectl delete pod <uno-de-los-pods>
while ($true) { $r = curl.exe -s -m 2 http://localhost:8080/; if ($r) { "$(Get-Date -f HH:mm:ss)  $(($r | ConvertFrom-Json).pod)" } else { "$(Get-Date -f HH:mm:ss)  ERROR" }; Start-Sleep -m 300 }
```
Al borrar un pod no aparecen errores: el Service lo saca de rotación, y el pod de reemplazo (`0/1`) no recibe tráfico
hasta que pasa su readinessProbe.

## 6. Réplicas stateless: todas ven los mismos datos
```powershell
$prod = @{ sku="SONY-WH1000XM5-NEG"; name="Audifonos Sony WH-1000XM5"; description="Inalambricos con cancelacion de ruido"; price=6999.00; stock=20; category="Audio" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/products -ContentType "application/json" -Body $prod

# Preguntar a CADA pod directamente: todos devuelven el mismo número de productos
kubectl get pods -l app=retail-order-service -o name | ForEach-Object { "$_ -> " + ((kubectl exec $_ -c retail-order-app -- wget -qO- http://localhost:8080/api/products | ConvertFrom-Json).Count) + " productos" }
```
El POST lo atendió un solo pod, pero lo guardó en Postgres, así que las 3 réplicas lo ven.

## 7. Persistencia: borrar el pod de Postgres
```powershell
kubectl delete pod -l app=postgres
kubectl wait --for=condition=Ready pod -l app=postgres --timeout=180s
kubectl logs deploy/postgres | Select-String "Skipping initialization"   # init.sql NO se repite
(Invoke-RestMethod http://localhost:8080/api/products) | Format-Table id, sku, name
```
El pod de Postgres es nuevo, pero monta el mismo PVC: los datos (incluidos los creados por la API) siguen ahí.

## 8. Rolling update y rollback
```powershell
kubectl annotate deployment/retail-order-deployment kubernetes.io/change-cause="v5 con PostgreSQL" --overwrite

# Despliegue roto: imagen inexistente
kubectl set image deployment/retail-order-deployment retail-order-app=retail-order-service:v99-rota
kubectl get pods -l app=retail-order-service      # 1 pod ErrImagePull, los 3 anteriores siguen Running (maxUnavailable: 0)

kubectl rollout undo deployment/retail-order-deployment
kubectl rollout history deployment/retail-order-deployment
```
`rollout undo` no actualiza el YAML. La forma declarativa es corregir `02-deployment.yaml` y volver a hacer `kubectl apply`.

## 9. Observabilidad
```powershell
kubectl describe pod -l app=postgres                     # eventos, probes, montajes (PVC y ConfigMap)
kubectl logs -l app=retail-order-service --tail=20 --prefix -c retail-order-app
kubectl logs <pod-app> -c esperar-postgres               # salida del initContainer
kubectl exec -it deploy/postgres -- psql -U retail_admin -d retaildb
```

## Limpieza
```powershell
kubectl delete -f k8s/          # borra todo, incluido el PVC (y los datos de Postgres)
```
Para volver a cargar los datos iniciales hay que borrar el PVC: `init.sql` solo corre con el volumen vacío.
