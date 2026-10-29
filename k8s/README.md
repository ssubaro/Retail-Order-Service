# RetailOrder Service en Kubernetes (Docker Desktop)

Despliegue del microservicio Spring Boot siguiendo el mismo esquema del ejercicio
`Kubernetes/proyecto_integrador_k8s`: los manifiestos se aplican en orden 01 → 04.

| Archivo | Qué crea | Concepto |
|---|---|---|
| `01-config-and-secret.yaml` | ConfigMap `retail-order-config` + Secret `retail-order-secret` | Configuración fuera de la imagen |
| `02-deployment.yaml` | Deployment `retail-order-deployment` (3 réplicas, H2 en memoria) | Réplicas, probes, rolling update |
| `03-pvc-and-deployment.yaml` | PVC `retail-order-pvc` + Deployment `retail-order-pvc-deployment` (1 réplica, H2 en archivo) | Persistencia |
| `04-service.yaml` | Service `retail-order-service` tipo LoadBalancer → `localhost:8080` | Balanceo de carga |

Diferencias respecto al ejercicio base:
- **Health checks** con Spring Boot Actuator (`/actuator/health/liveness` y `/readiness`) en lugar de un `/health` hecho a mano.
- **ConfigMap/Secret con efecto real**: Spring Boot convierte variables de entorno en propiedades
  (`SPRING_DATASOURCE_PASSWORD` → `spring.datasource.password`).
- **Persistencia real**: el `03` cambia `SPRING_DATASOURCE_URL` a `jdbc:h2:file:/app/data/retaildb`, dentro del volumen.
- `startupProbe` y más memoria (384Mi/768Mi), porque una JVM con Spring tarda más y consume más que la app de ejemplo.
- Si tu Docker Desktop usa el clúster tipo *kind* (nodo `desktop-control-plane`), el StorageClass se llama
  `standard` en vez de `hostpath`. El PVC no fija ninguno, así que funciona igual.

## 0. Preparación
```powershell
kubectl config current-context        # docker-desktop
kubectl get nodes                     # STATUS Ready
# Si el ejercicio anterior sigue arriba, libera el puerto 8080:
kubectl delete -f ..\..\..\..\Kubernetes\proyecto_integrador_k8s\k8s
```

## 1. Construir la imagen
Desde la carpeta del proyecto (donde está el `Dockerfile`):
```powershell
docker build -t retail-order-service:v1 .
```
Docker Desktop comparte las imágenes con su clúster, así que no hace falta `docker push`
(por eso los YAML usan `imagePullPolicy: IfNotPresent`).

## 2. ConfigMap y Secret
```powershell
kubectl apply -f k8s/01-config-and-secret.yaml
kubectl get secret retail-order-secret -o jsonpath='{.data}'   # base64, NO cifrado
```

## 3. Deployment con 3 réplicas
```powershell
kubectl apply -f k8s/02-deployment.yaml
kubectl rollout status deployment/retail-order-deployment
kubectl get pods -l app=retail-order-service -o wide
kubectl exec deploy/retail-order-deployment -- env | Select-String "SPRING|APP_|SERVER_PORT"
```
`APP_ENVIRONMENT` vale `produccion-local` (ConfigMap) aunque el Dockerfile define `desarrollo`: el ConfigMap gana.

## 4. Volumen persistente
```powershell
kubectl apply -f k8s/03-pvc-and-deployment.yaml
kubectl get pvc retail-order-pvc                 # STATUS Bound

# Este Deployment no está detrás del Service: se accede con port-forward (déjalo abierto en otra terminal)
kubectl port-forward deploy/retail-order-pvc-deployment 8081:8080

# Crear un producto
curl.exe -X POST http://localhost:8081/api/products -H "Content-Type: application/json" `
  -d '{\"sku\":\"SKU-K8S-PVC-001\",\"name\":\"Teclado persistente\",\"price\":899.00,\"stock\":15,\"category\":\"Electronica\"}'
kubectl exec deploy/retail-order-pvc-deployment -- ls -la /app/data   # retaildb.mv.db

# Borrar el pod: el Deployment crea otro que monta el MISMO volumen
kubectl delete pod -l app=retail-order-service-pvc
# Reinicia el port-forward (el pod anterior ya no existe) y consulta:
curl.exe http://localhost:8081/api/products      # el producto sigue ahí
```
El `03` usa `strategy: Recreate` porque H2 bloquea el archivo: un pod nuevo no puede abrirlo mientras viva el anterior.

## 5. Service LoadBalancer
```powershell
kubectl apply -f k8s/04-service.yaml
kubectl get svc retail-order-service
kubectl get endpointslices -l kubernetes.io/service-name=retail-order-service   # las 3 IPs de los pods
curl.exe http://localhost:8080/actuator/health
```
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Postman: importa la colección de `postman/` con `baseUrl = http://localhost:8080`.

**Demostrar el balanceo:** `/actuator/info` responde con el pod que atendió la petición (Downward API, ver `02`).
`curl.exe` abre una conexión nueva por llamada, y kube-proxy elige un pod al azar por conexión:
```powershell
curl.exe -s http://localhost:8080/actuator/info     # {"pod":{"name":"retail-order-deployment-...","ip":"10.244.0.x"}}

# 30 peticiones, agrupadas por pod
1..30 | ForEach-Object { (curl.exe -s http://localhost:8080/actuator/info | ConvertFrom-Json).pod.name } |
  Group-Object | Select-Object Count, Name

# En vivo, una línea por petición (Ctrl+C para parar). En otra terminal: kubectl delete pod <uno-de-los-pods>
while ($true) { $r = curl.exe -s -m 2 http://localhost:8080/actuator/info; if ($r) { "$(Get-Date -f HH:mm:ss)  $(($r | ConvertFrom-Json).pod.name)" } else { "$(Get-Date -f HH:mm:ss)  ERROR" }; Start-Sleep -m 300 }
```
Al borrar un pod no aparecen errores: el Service lo saca de rotación, y el pod de reemplazo (`0/1`) no recibe tráfico
hasta que pasa su readinessProbe.

**Lección stateless vs stateful:** crea un producto por `localhost:8080` y consulta cada réplica por separado:
```powershell
kubectl get pods -l app=retail-order-service -o name | ForEach-Object { "$_ -> " + (kubectl exec $_ -- wget -qO- http://localhost:8080/api/products) }
```
Solo un pod lo tiene, porque cada réplica tiene su propia H2 en memoria. En producción las réplicas comparten una base
de datos externa. Por lo mismo, la colección Postman puede fallar contra las 3 réplicas: un pedido se crea en un
pod y la siguiente petición puede caer en otro.

## 6. Rolling update y rollback
```powershell
kubectl annotate deployment/retail-order-deployment kubernetes.io/change-cause="v1 inicial" --overwrite

# v2 (retag de la misma imagen; basta con que cambie el nombre de la imagen para disparar el rollout)
docker tag retail-order-service:v1 retail-order-service:v2
kubectl set image deployment/retail-order-deployment retail-order-app=retail-order-service:v2
kubectl annotate deployment/retail-order-deployment kubernetes.io/change-cause="actualizacion a v2" --overwrite
kubectl rollout status deployment/retail-order-deployment
kubectl get rs -l app=retail-order-service       # el ReplicaSet viejo queda en 0: es la "foto" para el rollback

# Despliegue roto: imagen inexistente
kubectl set image deployment/retail-order-deployment retail-order-app=retail-order-service:v3-rota
kubectl get pods -l app=retail-order-service      # 1 pod ErrImagePull, los 3 v2 siguen Running (maxUnavailable: 0)

# Rollback
kubectl rollout undo deployment/retail-order-deployment
kubectl rollout history deployment/retail-order-deployment
kubectl rollout undo deployment/retail-order-deployment --to-revision=1   # volver a una revisión específica
```
`rollout undo` no actualiza el YAML. La forma declarativa es corregir `02-deployment.yaml` y volver a hacer `kubectl apply`.

## 7. Observabilidad
```powershell
kubectl describe pod <pod>                               # eventos, probes, montajes, variables
kubectl logs -l app=retail-order-service --tail=50       # incluye los logs [ASYNC] de los eventos de dominio
kubectl exec -it deploy/retail-order-deployment -- sh
```

## Limpieza
```powershell
kubectl delete -f k8s/          # borra todo, incluido el PVC (y sus datos)
```
