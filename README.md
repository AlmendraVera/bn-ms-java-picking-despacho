# bn-ms-java-picking-despacho

Microservicio de **picking, despacho y seguimiento** en bodega (Capa 3). El operario prepara la orden ítem por ítem; al despachar, descuenta el stock en *inventario* y marca la orden como DESPACHADA en *ordenes*.

> Proyecto de la **Evaluación Parcial 2 (JVY0101)**. Forma parte de la arquitectura de 4 microservicios: usuarios, inventario, órdenes y picking-despacho.

## Modelo de datos

```
Despacho 1 ───── N ItemPicking        (@OneToMany en Despacho / @ManyToOne en ItemPicking)
```

- **Despacho**: `id`, `ordenId` (único), `operario`, `estado`, `fechaCreacion`, `fechaDespacho`, `codigoSeguimiento`
- **ItemPicking**: `id`, `sku`, `cantidad`, `recogido`

## Flujo de bodega

```
1. POST /api/despachos                      crea el despacho de una orden CREADA (un ítem por línea de la orden)
2. POST /api/despachos/{id}/iniciar-picking el operario toma el trabajo → despacho EN_PICKING, orden EN_PICKING
3. PUT  /api/despachos/{id}/items/{itemId}/recoger   marca cada ítem como recogido
4. POST /api/despachos/{id}/despachar       verifica stock, lo descuenta en inventario,
                                            orden → DESPACHADA, genera código de seguimiento (DSP-000001)
```

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/despachos` | Crea el despacho (`{"ordenId": 1}`) | 201, 400, 404, 409, 503 |
| GET | `/api/despachos` | Lista despachos (`?estado=EN_PICKING` para filtrar) | 200 |
| GET | `/api/despachos/{id}` | Busca por id | 200, 404 |
| GET | `/api/despachos/orden/{ordenId}` | Busca por orden | 200, 404 |
| POST | `/api/despachos/{id}/iniciar-picking` | Inicia el picking (`{"operario":"operario@bn.cl"}`) | 200, 400, 404, 409, 503 |
| PUT | `/api/despachos/{id}/items/{itemId}/recoger` | Marca un ítem como recogido | 200, 404, 409 |
| POST | `/api/despachos/{id}/despachar` | Despacha: descuenta stock y cierra la orden | 200, 404, 409, 503 |
| DELETE | `/api/despachos/{id}` | Elimina (solo si está PENDIENTE) | 204, 404, 409 |

### Ejemplo

```bash
curl -X POST localhost:8084/api/despachos -H "Content-Type: application/json" -d '{"ordenId":1}'
curl -X POST localhost:8084/api/despachos/1/iniciar-picking -H "Content-Type: application/json" -d '{"operario":"operario@bn.cl"}'
curl -X PUT  localhost:8084/api/despachos/1/items/1/recoger
curl -X PUT  localhost:8084/api/despachos/1/items/2/recoger
curl -X POST localhost:8084/api/despachos/1/despachar
```

## Requisitos

- JDK 17 o superior (`java -version`)
- Git
- No hace falta instalar Maven: el proyecto incluye el *Maven Wrapper* (`mvnw` / `mvnw.cmd`)

## Cómo clonar, instalar dependencias y ejecutar

```bash
git clone https://github.com/AlmendraVera/bn-ms-java-picking-despacho.git
cd bn-ms-java-picking-despacho

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Maven descarga las dependencias automáticamente la primera vez. Cuando termine verás
`Started Application` y el servicio quedará escuchando en **http://localhost:8084**.

**Depende de `bn-ms-java-ordenes` (8083) y `bn-ms-java-inventario` (8082)**: levantar primero esos dos. Si alguno no responde, la API devuelve **503**. Las URLs están en `application.properties` (`servicios.ordenes.url` y `servicios.inventario.url`).

## Base de datos

H2 en memoria (se crea al iniciar y se borra al detener). Consola web: **http://localhost:8084/h2-console**

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:mem:pickingdb;DB_CLOSE_DELAY=-1` |
| User Name | `sa` |
| Password | *(vacía)* |

La configuración está en `src/main/resources/application.properties`.

## Compilar y empaquetar con Maven

```bash
./mvnw clean      # borra la carpeta target/
./mvnw install    # compila, ejecuta los tests e instala el jar en el repositorio local
./mvnw package    # genera target/bn-ms-java-picking-despacho-0.0.1-SNAPSHOT.jar

java -jar target/bn-ms-java-picking-despacho-0.0.1-SNAPSHOT.jar
```

## Estructura (arquitectura en capas)

```
src/main/java/cl/duoc/bn/picking/
├── controller/   endpoints REST (reciben la petición y devuelven la respuesta HTTP)
├── service/      lógica de negocio y validaciones
├── repository/   acceso a datos (Spring Data JPA)
├── model/        entidades JPA
├── exception/    ApiException + ManejadorExcepciones (respuestas de error en JSON)
├── client/       OrdenesClient e InventarioClient: llamadas HTTP a los otros microservicios
└── Application.java
```

## Formato de los errores

Todos los errores devuelven el mismo JSON:

```json
{
  "timestamp": "2026-10-15T21:03:08",
  "status": 404,
  "error": "Not Found",
  "mensaje": "No existe un recurso con id 99",
  "ruta": "/api/..."
}
```

| Código | Cuándo ocurre |
|---|---|
| 400 | Datos faltantes o inválidos, JSON mal formado, parámetro con tipo incorrecto |
| 404 | El recurso no existe |
| 409 | Conflicto: dato duplicado o acción no permitida en el estado actual |
| 503 | Ordenes o inventario no responden |

## Integrantes

- *(Nombre y apellido del integrante 1)*
- *(Nombre y apellido del integrante 2)*
