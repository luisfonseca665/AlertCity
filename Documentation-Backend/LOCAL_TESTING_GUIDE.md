# Guía Rápida: Estado del Proyecto y Pasos para Pruebas Locales con Kotlin

Este documento resume los componentes que han sido implementados hasta el momento en **AlertCity-Backend** y detalla los pasos exactos que se requieren para realizar la prueba de extremo a extremo conectando la aplicación móvil en **Kotlin** a través de la red local.

---

## 1. Resumen de lo que se ha construido

| Componente | Estado | Detalle de lo Implementado |
| :--- | :---: | :--- |
| **Base de Datos & GIS** | ✅ Listo | PostgreSQL 17 contenerizado con la extensión **PostGIS 3.5** (`postgis/postgis:17-3.5-alpine`). Volumen de datos persistente y healthcheck automático. |
| **Migraciones Flyway** | ✅ Listo | - `V1__init.sql`: Creación de tablas `users` y `reports`, índices espaciales GiST y extensiones `pgcrypto` y `postgis`.<br>- `V2__seed_test_data.sql`: Dataset enriquecido con **27 reportes urbanos** (todas las categorías, estados y coordenadas reales) y **14 usuarios** (incluyendo el rol `CIUDADANO`). |
| **Modelos de Dominio (JPA)** | ✅ Listo | `User` y `Report` sincronizados con Hibernate Spatial y tipos `Point` de JTS (SRID 4326 WGS 84). Enums `Category`, `Rol` (con `CIUDADANO` y `ADMIN`) y `ReportState` con `@Enumerated(EnumType.STRING)`. |
| **Repositorios** | ✅ Listo | `ReportRepository` con consultas derivadas ordenadas cronológicamente (`findByUser_IdUserOrderByDateTimeDesc` y `findAllByOrderByDateTimeDesc`). |
| **Servicios de Negocio** | ✅ Listo | - `ReportService`: Conversor geoespacial (Lat/Lng $\leftrightarrow$ PostGIS Point), persistencia y mapeo a DTOs.<br>- `DashboardService`: Cálculo dinámico de contadores (activos vs. resueltos) y agregación de listas. |
| **Controladores REST** | ✅ Listo | - `ReportController` (`POST /api/v1/reports`, `GET /api/v1/reports`).<br>- `DashboardController` (`GET /api/v1/dashboard` con patrón BFF para cargar toda la pantalla en un solo llamado). |
| **Contratos de Datos (DTOs)** | ✅ Listo | Records de Java 21 (`CreateReportRequest`, `ReportResponse`, `DashboardResponse`) listos para serialización JSON limpia. |
| **Contenerización** | ✅ Listo | `Dockerfile` multi-stage ligero con Java 21 y `docker-compose.yml` con servicios `db`, `backend` y `pgadmin`. |

---

## 2. Qué hace falta para probar de manera local con el Frontend (Kotlin)

Para que tu aplicación móvil en Kotlin pueda comunicarse exitosamente con tu computadora mediante Wi-Fi local y entregar la Demo, debes completar los siguientes **4 pasos**:

---

### Paso A (Backend): Habilitar Seguridad Abierta para la Demo (`SecurityConfig.java`)
Actualmente, la dependencia `spring-boot-starter-security` bloquea los endpoints con **`HTTP 401 Unauthorized`** (para GET) y **`HTTP 403 Forbidden`** (por protección CSRF en POST) porque no hay una clase de configuración.

**Acción a realizar:**
Crea el archivo `src/main/java/com/AlertCity_Backend/config/SecurityConfig.java`:
```java
package com.AlertCity_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

---

### Paso B (Backend): Reiniciar el Proceso en IntelliJ
Si tienes una instancia de Spring Boot corriendo en IntelliJ desde hace horas, estará ocupando el puerto 8080 con una versión anterior en memoria.
1. En IntelliJ IDEA, presiona el botón rojo **Stop** (Detener).
2. Vuelve a dar clic en **Run** o **Debug** para arrancar con las últimas clases compiladas.
3. Verifica que la consola indique: `Tomcat started on port 8080 (http)`.

---

### Paso C (Red Local): Verificar Conectividad y Firewall
1. Conecta tu teléfono celular (o el emulador) a la **misma red Wi-Fi** a la que está conectada tu computadora.
2. Abre una terminal en tu computadora y confirma tu IP local:
   ```powershell
   ipconfig
   ```
   *(Busca el adaptador Wi-Fi y confirma que tu IP siga siendo `192.168.1.6`).*
3. **Firewall de Windows:** Asegúrate de que el Firewall permita conexiones entrantes a Java/IntelliJ en el puerto `8080`.
   *(Prueba rápida: abre el navegador de tu celular e ingresa a `http://192.168.1.6:8080/actuator/health`. Debe responder `{"status":"UP"}`).*

---

### Paso D (Frontend Kotlin): Ajustar la Aplicación Android
En el proyecto de Android Studio:

1. **Permitir tráfico HTTP en [`AndroidManifest.xml`](file:///AndroidManifest.xml):**
   Como la demo local usa `http://` y no `https://`, Android bloqueará la conexión a menos que agregues `android:usesCleartextTraffic="true"` en la etiqueta `<application>`:
   ```xml
   <manifest xmlns:android="http://schemas.android.com/apk/res/android">
       <uses-permission android:name="android.permission.INTERNET" />
       <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
       <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

       <application
           android:usesCleartextTraffic="true"
           android:allowBackup="true"
           ... >
           ...
       </application>
   </manifest>
   ```

2. **Configurar el Cliente Retrofit:**
   Asegúrate de que la URL base apunte a la IP de tu PC:
   ```kotlin
   val retrofit = Retrofit.Builder()
       .baseUrl("http://192.168.1.6:8080/")
       .addConverterFactory(GsonConverterFactory.create())
       .build()

   val api = retrofit.create(AlertCityApi::class.java)
   ```

3. **Copiar Modelos e Interfaz del Contrato:**
   Copia las Data Classes e interfaz Retrofit definidas en **[API_CONTRACTS.md](file:///c:/Users/garci/Documentos/ITSUR/7%20Semestre/Programacion%20Movil/AlertCity-Backend/Documentation/API_CONTRACTS.md)**.

4. **Flujo de Pantallas para la Demo:**
   - **Al iniciar la App:** Ejecutar `api.getDashboard()`.
     - Mostrar los contadores `myActiveReportsCount` y `mySolvedReportsCount`.
     - Pintar la lista de "Mis Reportes" (`myReports`) y la lista de "Comunidad" (`allReports`).
   - **En el formulario de Nuevo Reporte:**
     - Enviar `api.createReport(request)` con el `userId: "11111111-1111-4111-a111-111111111111"` (para la demo).
     - La foto se maneja simulada o con un placeholder estático.
     - Al recibir `201 Created`, volver a la pantalla principal y refrescar el dashboard.
