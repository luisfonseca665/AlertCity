# AlertCity - Contratos de API y Guía de Integración para Frontend (Kotlin)

Este documento define la especificación técnica oficial de la API REST de **AlertCity** (Etapa 2), las estructuras de datos JSON, los códigos de estado HTTP, la autenticación JWT, los catálogos enumerados y las interfaces Kotlin/Retrofit que el equipo de **Frontend (Android / Kotlin)** debe implementar para consumir el backend.

---

## 1. Lineamientos Generales de Integración

- **URL Base en Entorno Local:** `http://192.168.1.6:8080/api/v1/`
  *(Sustituir `192.168.1.6` por la IP local de la computadora donde corre el backend).*
- **Documentación Interactiva (Swagger UI):** `http://localhost:8080/swagger-ui.html` (esquema `bearerAuth`).
- **Headers Obligatorios:**
  - `Content-Type: application/json` (excepto carga de fotos, que usa `multipart/form-data`)
  - `Accept: application/json`
  - `Authorization: Bearer <accessToken>` (obligatorio en todos los endpoints de `/api/v1/dashboard` y `/api/v1/reports/**`)
- **Formato de Identificadores:** Cadenas en formato **UUID v4** estándar (ej: `11111111-1111-4111-a111-111111111111`).
- **Formato de Fechas:** Estándar **ISO-8601** (`yyyy-MM-ddTHH:mm:ss`, ej: `"2026-11-02T14:30:00"`).
- **Convención de Coordenadas Geográficas:**
  - El frontend envía y recibe dos campos numéricos decimales separados: `"latitude"` y `"longitude"`.
  - Ejemplo: `"latitude": 20.1458`, `"longitude": -101.1923`.
  - El backend proyecta estas coordenadas al sistema geoespacial PostGIS (WGS 84 / SRID 4326).
- **Manejo de Evidencia Fotográfica:**
  - Las imágenes reales (JPEG, PNG o WebP, máximo `10 MB`) se suben mediante petición `multipart/form-data` al endpoint `POST /api/v1/reports/{id}/media`.
  - El backend almacena el archivo y devuelve su URL relativa en `imageUrl` (accesible públicamente bajo `/uploads/reports/<archivo>`).

---

## 2. Catálogos y Enumeraciones (Enums)

Todos los valores enumerados deben enviarse y recibirse en **MAYÚSCULAS ESTRICTAS**.

### A. Categorías de Incidencias (`Category`)
| Valor | Descripción / Ejemplos |
| :--- | :--- |
| `VIALIDAD` | Baches, pavimento quebrado, señalamientos caídos, topes sin pintar. |
| `ALUMBRADO` | Luminarias apagadas, postes caídos o ladeados, fotoceldas dañadas. |
| `AGUA` | Fugas de agua potable, alcantarillas sin tapa, drenaje colapsado. |
| `LIMPIEZA` | Basureros clandestinos, escombros en banqueta, contenedores desbordados. |
| `SEGURIDAD` | Barandales de puentes dañados, zanjas de obras sin señalización preventiva. |
| `AREAS_VERDES` | Árboles o ramas caídas sobre cables, maleza alta en canchas o parques. |
| `ANIMALES` | Jaurías agresivas en vía pública, ganado/caballos sueltos en carretera. |
| `OTRO` | Graffiti en monumentos históricos, afectaciones no clasificadas. |

### B. Estados del Reporte (`ReportState`)
| Valor | Descripción |
| :--- | :--- |
| `ENVIADO` | Reporte registrado por el ciudadano, en espera de asignación de cuadrilla. |
| `EN_PROCESO` | Cuadrilla de trabajadores asignada y laborando en la solución del reporte. |
| `RESUELTO` | Problema atendido, solucionado y cerrado formalmente por la autoridad. |

### C. Roles de Usuario (`Rol`)
| Valor | Descripción |
| :--- | :--- |
| `CIUDADANO` | Habitante de la localidad que genera y consulta reportes ciudadanos. |
| `ADMIN` | Administrador general de la plataforma AlertCity (puede cambiar estado de cualquier reporte). |
| `VIALIDAD`, `ALUMBRADO`, `AGUA_DRENAJE`, `LIMPIEZA`, `SEGURIDAD`, `AREAS_VERDES`, `ANIMALES` | Personal operativo asignado a dependencias específicas (puede cambiar estado de reportes de su categoría). |

---

## 3. Especificación de Endpoints

### 3.0. Autenticación y Tokens JWT (Endpoints Públicos)

> **Nota para pruebas con usuarios semilla (`V1` y `V2`):** Todos los usuarios precargados (ej. `laura_ciudadana`, `carlos_vialidad`) tienen la contraseña `"password"`.

#### A. Registrar Nuevo Ciudadano
- **Método:** `POST`
- **Ruta:** `/api/v1/auth/register`
- **Códigos:** `201 Created`, `400 Bad Request`, `409 Conflict` (username o email duplicado).

```json
{
  "username": "carlos_vecino",
  "email": "carlos@ejemplo.com",
  "password": "Password123!",
  "firstName": "Carlos",
  "lastName": "Ramírez"
}
```

#### B. Iniciar Sesión
- **Método:** `POST`
- **Ruta:** `/api/v1/auth/login`
- **Códigos:** `200 OK`, `401 Unauthorized`.

```json
{
  "usernameOrEmail": "laura_ciudadana",
  "password": "password"
}
```

#### C. Renovar Access Token
- **Método:** `POST`
- **Ruta:** `/api/v1/auth/refresh`
- **Códigos:** `200 OK`, `401 Unauthorized`.

```json
{
  "refreshToken": "<refresh_token_jwt>"
}
```

#### Respuesta Estándar de Autenticación (`AuthResponse`):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "userId": "11111111-1111-4111-a111-111111111111",
  "username": "laura_ciudadana",
  "email": "laura.sanchez@gmail.com",
  "rol": "CIUDADANO",
  "firstName": "Laura",
  "lastName": "Sánchez"
}
```

---

### 3.1. Obtener Datos del Dashboard (BFF - Pantalla Principal)
Carga métricas personales, reportes propios y el feed comunitario en un solo viaje (*round-trip*).

- **Método:** `GET`
- **Ruta:** `/api/v1/dashboard`
- **Auth:** Requiere `Authorization: Bearer <accessToken>`
- **Query Parameters:**
  - `userId` *(UUID, Opcional)*: Si se omite, el backend extrae automáticamente el `userId` del token JWT autenticado.

#### Ejemplo de Respuesta (`200 OK`):
```json
{
  "myActiveReportsCount": 2,
  "mySolvedReportsCount": 0,
  "myReports": [
    {
      "reportId": "c0000001-0000-4000-8000-000000000005",
      "userId": "11111111-1111-4111-a111-111111111111",
      "title": "Barandal de protección fracturado tras choque",
      "description": "El muro de contención y barandal metálico del puente fue derribado...",
      "category": "SEGURIDAD",
      "urgent": true,
      "dateTime": "2026-10-31T20:14:48",
      "latitude": 20.1448,
      "longitude": -101.1905,
      "addressReference": "Puente del Río Huacapa, carril norte-sur",
      "state": "EN_PROCESO",
      "imageUrl": null
    }
  ],
  "allReports": []
}
```

---

### 3.2. Crear un Nuevo Reporte
- **Método:** `POST`
- **Ruta:** `/api/v1/reports`
- **Auth:** Requiere `Authorization: Bearer <accessToken>`

#### Estructura del Request Body (`CreateReportRequest`):
| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `userId` | String (UUID) | No | Si se omite, se toma del usuario autenticado en el JWT. |
| `title` | String | **Sí** | Título breve de la problemática (máx. 150 caracteres). |
| `description` | String | **Sí** | Descripción detallada del problema. |
| `category` | String | **Sí** | Valor exacto del catálogo `Category` (ej: `"VIALIDAD"`). |
| `urgent` | Boolean | **Sí** | `true` si representa peligro inminente; `false` si es ordinario. |
| `latitude` | Double | No (Recomendado) | Latitud obtenida por el GPS del móvil (ej: `20.1458`). |
| `longitude` | Double | No (Recomendado) | Longitud obtenida por el GPS del móvil (ej: `-101.1923`). |
| `addressReference` | String | No | Calle, número o referencia visual (máx. 255 caracteres). |

#### Códigos de Respuesta:
- `201 CREATED`: Reporte registrado exitosamente.
- `400 BAD REQUEST`: Categoría no válida o datos de validación incorrectos.
- `401 UNAUTHORIZED`: Token ausente o inválido.
- `404 NOT FOUND`: El `userId` enviado no existe.

---

### 3.3. Listar y Filtrar Reportes (Lista y Paginación)

#### A. Listado Filtrado
- **Método:** `GET`
- **Ruta:** `/api/v1/reports`
- **Auth:** Requiere `Authorization: Bearer <accessToken>`
- **Query Parameters (todos opcionales):**
  - `userId` *(UUID)*
  - `category` *(`Category`)*
  - `state` *(`ReportState`)*
  - `urgent` *(Boolean)*
  - `fromDate` *(ISO-8601 DateTime, ej: `2026-11-01T00:00:00`)*
  - `toDate` *(ISO-8601 DateTime)*

#### B. Listado Paginado
- **Método:** `GET`
- **Ruta:** `/api/v1/reports/page`
- **Auth:** Requiere `Authorization: Bearer <accessToken>`
- **Query Parameters:** Los mismos filtros anteriores + parámetros estándar de Spring `Pageable` (`page`, `size`, `sort`, por defecto `size=10&sort=dateTime,desc`).

---

### 3.4. Buscar Reportes Cercanos (PostGIS)
Encuentra incidencias dentro de un radio en kilómetros ordenadas de la más cercana a la más lejana usando `ST_DWithin` sobre el índice GiST.

- **Método:** `GET`
- **Ruta:** `/api/v1/reports/nearby`
- **Auth:** Requiere `Authorization: Bearer <accessToken>`
- **Query Parameters:**
  - `lat` *(Double, Obligatorio)*: Latitud GPS de referencia.
  - `lng` *(Double, Obligatorio)*: Longitud GPS de referencia.
  - `radiusKm` *(Double, Opcional, default `3.0`)*: Radio máximo en kilómetros (`(0, 100]`).

---

### 3.5. Obtener Reporte por ID
- **Método:** `GET`
- **Ruta:** `/api/v1/reports/{id}`
- **Auth:** Requiere `Authorization: Bearer <accessToken>`
- **Códigos:** `200 OK`, `404 Not Found`.

---

### 3.6. Actualizar Estado de Reporte (RBAC)
Permite a un `ADMIN` o al operador del departamento correspondiente a la categoría del reporte cambiar su estado (`ENVIADO`, `EN_PROCESO`, `RESUELTO`). Un `CIUDADANO` recibirá `403 Forbidden`.

- **Método:** `PATCH`
- **Ruta:** `/api/v1/reports/{id}/state`
- **Auth:** Requiere `Authorization: Bearer <accessToken>` con rol `ADMIN` u operador de área.

```json
{
  "state": "EN_PROCESO"
}
```

---

### 3.7. Subir Evidencia Fotográfica (Multipart)
- **Método:** `POST`
- **Ruta:** `/api/v1/reports/{id}/media`
- **Content-Type:** `multipart/form-data`
- **Form Part:** `file` (JPEG, PNG o WebP, máximo `10 MB`).
- **Respuesta (`200 OK`):** Retorna el `ReportResponse` actualizado con la ruta en `imageUrl` (ej. `"/uploads/reports/<id>_<uuid>.jpg"`).

---

### 3.8. Formato Estándar de Errores (RFC 7807 ProblemDetail)
Todas las respuestas `400`, `401`, `403`, `404`, `409`, `413` y `500` devuelven un cuerpo estandarizado RFC 7807:

```json
{
  "type": "https://alertcity.app/errors/validation-error",
  "title": "Error de validación",
  "status": 400,
  "detail": "Uno o más campos de la petición no son válidos",
  "instance": "/api/v1/auth/register",
  "timestamp": "2026-11-22T18:30:00Z",
  "errors": {
    "email": "El formato del correo electrónico no es válido"
  }
}
```

---

## 4. Implementación de Referencia en Kotlin

### Data Classes de Kotlin:
```kotlin
package com.alertcity.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("expiresIn") val expiresIn: Long,
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String
)

data class DashboardResponse(
    @SerializedName("myActiveReportsCount") val myActiveReportsCount: Int,
    @SerializedName("mySolvedReportsCount") val mySolvedReportsCount: Int,
    @SerializedName("myReports") val myReports: List<ReportResponse>,
    @SerializedName("allReports") val allReports: List<ReportResponse>
)

data class ReportResponse(
    @SerializedName("reportId") val reportId: String,
    @SerializedName("userId") val userId: String?,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("urgent") val urgent: Boolean,
    @SerializedName("dateTime") val dateTime: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("addressReference") val addressReference: String?,
    @SerializedName("state") val state: String,
    @SerializedName("imageUrl") val imageUrl: String?
)

data class CreateReportRequest(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("urgent") val urgent: Boolean,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("addressReference") val addressReference: String?
)

data class UpdateReportStateRequest(
    @SerializedName("state") val state: String
)
```

### Interfaz de Retrofit (`AlertCityApi.kt`):
```kotlin
package com.alertcity.data.network

import com.alertcity.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AlertCityApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @GET("api/v1/dashboard")
    suspend fun getDashboard(
        @Query("userId") userId: String? = null
    ): Response<DashboardResponse>

    @GET("api/v1/reports")
    suspend fun getReports(
        @Query("userId") userId: String? = null,
        @Query("category") category: String? = null,
        @Query("state") state: String? = null,
        @Query("urgent") urgent: Boolean? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null
    ): Response<List<ReportResponse>>

    @GET("api/v1/reports/nearby")
    suspend fun getNearbyReports(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double = 3.0
    ): Response<List<ReportResponse>>

    @POST("api/v1/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): Response<ReportResponse>

    @PATCH("api/v1/reports/{id}/state")
    suspend fun updateReportState(
        @Path("id") reportId: String,
        @Body request: UpdateReportStateRequest
    ): Response<ReportResponse>

    @Multipart
    @POST("api/v1/reports/{id}/media")
    suspend fun uploadReportMedia(
        @Path("id") reportId: String,
        @Part file: MultipartBody.Part
    ): Response<ReportResponse>
}
```
