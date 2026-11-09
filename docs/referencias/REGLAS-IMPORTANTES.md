# ANOTADO — README MAESTRO DE IMPLEMENTACIÓN PARA CODEX

> **Leer completo antes de modificar archivos.**  
> **Tipo de proyecto:** Android nativo, proyecto integrador de Unidad 3 (CRUD + autenticación).  
> **Nombre visible:** **Anotado**.  
> **Carpeta raíz acordada:** `TaskManager/`.  
> **Package Kotlin acordado:** `com.example.taskmanager`.  
> **Estado conocido al redactar:** Codex CLI instalado y funcionando en CachyOS; el proyecto Android estaba vacío en el último reporte. Codex debe **inspeccionar el árbol real** al comenzar, ya que puede haber cambiado.  
> **Prioridad absoluta:** una app real que **compile, se conecte con la API, autentique, haga CRUD y cumpla la rúbrica**. No invertir tiempo en características secundarias.

---

## ÍNDICE

1. [Misión y prioridades](#1-misión-y-prioridades)
2. [Hechos, decisiones y asuntos pendientes](#2-hechos-decisiones-y-asuntos-pendientes)
3. [Reglas innegociables](#3-reglas-innegociables)
4. [Tecnologías y arquitectura](#4-tecnologías-y-arquitectura)
5. [Estructura exacta acordada](#5-estructura-exacta-acordada)
6. [Contrato REST vigente](#6-contrato-rest-vigente)
7. [Modelo de datos y validaciones](#7-modelo-de-datos-y-validaciones)
8. [Pantallas y navegación](#8-pantallas-y-navegación)
9. [Diseño interno de responsabilidades](#9-diseño-interno-de-responsabilidades)
10. [Inyección manual, sesión y red](#10-inyección-manual-sesión-y-red)
11. [Estados, errores y seguridad](#11-estados-errores-y-seguridad)
12. [Plan de ejecución para Codex](#12-plan-de-ejecución-para-codex)
13. [Pruebas, calidad y rúbrica](#13-pruebas-calidad-y-rúbrica)
14. [Entorno CachyOS, Gradle y EC2](#14-entorno-cachyos-gradle-y-ec2)
15. [Comunicación con otros agentes](#15-comunicación-con-otros-agentes)
16. [Criterios de terminado y reporte final](#16-criterios-de-terminado-y-reporte-final)
17. [Prompt de inicio rápido](#17-prompt-de-inicio-rápido)

---

## 1. MISIÓN Y PRIORIDADES

Construir **Anotado**, una aplicación Android para gestionar tareas personales, con:

- **Registro real de usuario** contra la API.
- **Inicio de sesión real** contra la API.
- **JWT persistente** mediante abstracción de sesión.
- **Home directamente con el listado de tareas**, no un dashboard.
- **CRUD completo:** listar, ver detalle, crear, editar y eliminar.
- **Validaciones** y estados de carga, vacío, error y reintento.
- **Navegación segura:** logout borra el token y limpia el historial de pantallas protegidas.
- **Arquitectura evaluable:** MVVM, Repository, DTO, Mapper e inyección de dependencias manual.

**Orden de prioridades:**

1. Compilar y arrancar.
2. Conectar la infraestructura y la autenticación real.
3. Entregar CRUD completo real.
4. Cubrir estados, validaciones y pruebas de la rúbrica.
5. Ajustar diseño Figma solo cuando no comprometa los puntos anteriores.

**Fuera de alcance:** dashboard, estadísticas, sincronización offline, notificaciones, recordatorios push, búsqueda compleja, filtros avanzados, categorías como entidad propia, multi-módulo Gradle, Hilt, Firebase o migraciones de backend.

---

## 2. HECHOS, DECISIONES Y ASUNTOS PENDIENTES

Utiliza estas etiquetas mentalmente:

- **[OFICIAL]**: exigido por enunciado/rúbrica del profesor.
- **[ACORDADO]**: decisión explícita con el propietario del proyecto.
- **[API DOCUMENTADA]**: aparece en README del backend, pero puede requerir confirmación con una petición HTTP real.
- **[PENDIENTE]**: NO confirmado; verificar, no inventar.

| Tema | Estado vigente |
|---|---|
| Nombre de producto | **Anotado** [ACORDADO] |
| Directorio raíz del proyecto | `TaskManager/` [ACORDADO] |
| Package Kotlin | `com.example.taskmanager` [ACORDADO] |
| Lenguaje/plataforma | Kotlin para Android nativo [OFICIAL] |
| Interfaz | Jetpack Compose + Material 3 [OFICIAL] |
| Backend | Node.js 22+, Express y SQLite con JWT [API DOCUMENTADA] |
| Hosting | AWS EC2; Gemini se encarga de publicar/verificar [ACORDADO] |
| Backend GitHub | Repositorio privado, según propietario [ACORDADO] |
| URL EC2 definitiva | [PENDIENTE]; no inventar IP, dominio ni esquema HTTPS |
| Figma | Agente separado produce diseño. Referencia visual reciente: identidad verde, blanca, simple. No hay diseño final obligatorio de todas las pantallas. |
| Estado del código local | [PENDIENTE] comprobar archivos existentes antes de crear o reemplazar nada |
| Aprobación de dominio por docente | [PENDIENTE] confirmar con el propietario, no bloquear trabajo técnico |

**Decisiones descartadas:** el plan inicial de `json-server`, `json-server-auth`, Amazon RDS y MySQL quedó sustituido por la API **Node + Express + SQLite**. Los SQL de RDS anteriores **NO** son el backend actual. Android **nunca** accede a SQLite directamente.

### Cambio de nombre importante

La aplicación debe llamarse **Anotado** en la UI, `app_name`, launcher y textos visibles. **NO** renombrar el `package`, `namespace`, `applicationId` ni la carpeta del proyecto sin autorización expresa; preservar `com.example.taskmanager` y `TaskManager/`.

---

## 3. REGLAS INNEGOCIABLES

### 3.1 Prohibiciones académicas expresas

**PROHIBIDO `Call<T>` DE RETROFIT.** No usar:

```kotlin
fun getTasks(): Call<List<TaskDto>> // PROHIBIDO
// tampoco Call.enqueue() ni Call.execute()
```

**OBLIGATORIO**:

```kotlin
@GET("tasks")
suspend fun getTasks(): List<TaskDto>
```

Las interfaces Retrofit deben usar métodos `suspend` y retornos directos. Usar corrutinas (`viewModelScope`) y manejo de excepciones o resultados de dominio, nunca callbacks `Call`.

**PROHIBIDAS las God Classes:**

- Nunca concentrar auth, CRUD, almacenamiento, Retrofit, validaciones, navegación y UI en una sola clase.
- `AuthApiService` y `TaskApiService` están separados.
- `AuthRepository` y `TaskRepository` están separados.
- `AppContainer` **solo ensambla dependencias**: no hace llamadas de negocio ni decide navegación.
- `MainActivity` **no** es repositorio, controlador API ni contenedor de lógica de negocio.
- No crear un `EverythingManager`, `ApiManager` global ni un único ViewModel para toda la app.

### 3.2 Otras reglas obligatorias

- **NO** Hilt, Dagger ni Koin: inyección **manual** mediante `AppContainer` en `Application`, constructor injection y `ViewModelProvider.Factory`.
- **NO** invocar servicios Retrofit desde ViewModels ni Composables.
- **NO** exponer DTO en `domain`, `ui`, `ViewModel` o Composables; convertirlos en un **Mapper** dedicado.
- Las **interfaces** de repositorios viven en `domain/repository`; sus implementaciones en `data/repository`.
- Usar `MutableStateFlow` **privado** y exponer `StateFlow`/`UiState` **inmutable**.
- Los Composables muestran `UiState` con `collectAsStateWithLifecycle()` y envían eventos; sin persistencia ni validaciones de negocio ocultas en Compose.
- **NO** abrir una sesión ficticia, datos falsos presentados como backend real, ni saltarse el login para afirmar éxito.
- **NO** cambiar a otra tecnología, reorganizar la estructura oficial ni agregar paquetes de arquitectura por preferencia personal.
- Aplicar **SOLID, KISS, DRY y YAGNI**, priorizando simplicidad y responsabilidad única.
- Usar el **Version Catalog** `gradle/libs.versions.toml` para dependencias.
- Mantener Git y realizar commits **frecuentes y descriptivos**, sin sobrescribir trabajo no confirmado.

---

## 4. TECNOLOGÍAS Y ARQUITECTURA

| Área | Requisito |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3; sin pantallas XML |
| Navegación | Navigation Compose |
| Estado | ViewModel, StateFlow, UiState inmutable, `collectAsStateWithLifecycle()` |
| Asincronía | Corrutinas, `viewModelScope.launch`, `suspend` |
| HTTP | **Retrofit 3** + OkHttp + interceptor Bearer |
| JSON | `kotlinx.serialization`, DTOs `@Serializable` |
| Sesión | SharedPreferences o DataStore **tras abstracción**; escoger SharedPreferences si acelera la implementación |
| DI | `AppContainer` en `TaskManagerApplication`, constructor injection, `ViewModelProvider.Factory` |
| Builds | Gradle Kotlin DSL + `libs.versions.toml` |
| Dev | Android Studio y emulador Android, máquina CachyOS (Arch Linux) |

**Arquitectura:**

```text
UI (Compose) → ViewModel → domain.Repository (interfaz)
                                   ↑
                data.RepositoryImpl (implementación)
                         ↓                  ↓
                   ApiService         DTO ↔ Mapper ↔ Task
                         ↓
               Retrofit / OkHttp / JWT
```

`domain` no conoce Android, Retrofit, DTOs ni `Compose`. `data` es propietaria de DTOs/conversión. `ui` trabaja exclusivamente con modelos/contratos de dominio y UiState.

### Patrones de estado

- Estado UI: `data class LoginUiState(...)`, `RegisterUiState`, `HomeUiState`, `TaskUiState`, declaradas donde tenga sentido cerca de su ViewModel si no se acordaron archivos nuevos.
- `private val _uiState = MutableStateFlow(...)` + `val uiState: StateFlow<...> = _uiState.asStateFlow()`.
- Evitar flujos mal modelados que muestren éxito y error simultáneamente.
- Al girar el emulador, ViewModel debe conservar el estado de forma acorde al lifecycle.
- No introducir capas UseCase adicionales solo por estética; la rúbrica no las exige.

---

## 5. ESTRUCTURA EXACTA ACORDADA

**Este árbol es la fuente de verdad.** Todos los paquetes listados son hermanos bajo `com.example.taskmanager`, incluido **`domain/` fuera de `data/`**.

```text
TaskManager/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/taskmanager/
│   │       │   ├── data/
│   │       │   │   ├── remote/
│   │       │   │   │   ├── AuthApiService.kt
│   │       │   │   │   ├── TaskApiService.kt
│   │       │   │   │   └── dto/
│   │       │   │   │       ├── LoginRequestDto.kt
│   │       │   │   │       ├── RegisterRequestDto.kt
│   │       │   │   │       ├── AuthResponseDto.kt
│   │       │   │   │       └── TaskDto.kt
│   │       │   │   ├── repository/
│   │       │   │   │   ├── AuthRepositoryImpl.kt
│   │       │   │   │   └── TaskRepositoryImpl.kt
│   │       │   │   └── mapper/
│   │       │   │       └── TaskMapper.kt
│   │       │   ├── domain/
│   │       │   │   ├── model/
│   │       │   │   │   └── Task.kt
│   │       │   │   └── repository/
│   │       │   │       ├── AuthRepository.kt
│   │       │   │       └── TaskRepository.kt
│   │       │   ├── ui/
│   │       │   │   ├── auth/
│   │       │   │   │   ├── login/
│   │       │   │   │   │   ├── LoginScreen.kt
│   │       │   │   │   │   └── LoginViewModel.kt
│   │       │   │   │   └── register/
│   │       │   │   │       ├── RegisterScreen.kt
│   │       │   │   │       └── RegisterViewModel.kt
│   │       │   │   ├── home/
│   │       │   │   │   ├── HomeScreen.kt
│   │       │   │   │   └── HomeViewModel.kt
│   │       │   │   ├── task/
│   │       │   │   │   ├── TaskFormScreen.kt
│   │       │   │   │   ├── TaskDetailScreen.kt
│   │       │   │   │   └── TaskViewModel.kt
│   │       │   │   ├── navigation/
│   │       │   │   │   ├── AppNavigation.kt
│   │       │   │   │   └── Routes.kt
│   │       │   │   └── components/
│   │       │   │       ├── TaskCard.kt
│   │       │   │       └── LoadingIndicator.kt
│   │       │   ├── di/
│   │       │   │   ├── AppContainer.kt
│   │       │   │   └── ViewModelFactory.kt
│   │       │   ├── session/
│   │       │   │   └── SessionManager.kt
│   │       │   ├── network/
│   │       │   │   ├── AuthInterceptor.kt
│   │       │   │   └── RetrofitClient.kt
│   │       │   ├── TaskManagerApplication.kt
│   │       │   └── MainActivity.kt
│   │       └── res/
│   │           ├── drawable/
│   │           ├── mipmap/
│   │           └── values/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

**Excepciones operativas, sin alterar arquitectura:** Android Studio agregará archivos Gradle wrapper, AndroidManifest, temas Compose, tests, recursos y manifiestos de variante debug. Si se necesitan `UserDto`, `TaskRequestDto` o `ErrorResponseDto`, preferentemente añadir **archivos pequeños exclusivamente en `data/remote/dto/`**, justificando por qué; no renombrar ni mover los archivos ya enumerados. La implementación de `SessionManager` puede ir en el mismo archivo que su interfaz o en un pequeño archivo compañero en `session/` si fuera indispensable; no crear paquetes alternativos.

**La estructura es un contrato:** inspeccionar lo creado por Android Studio y armonizarlo sin destruir elementos generados que sean necesarios para compilar (temas, `res`, `AndroidManifest`, gradle wrapper, etc.).

---

## 6. CONTRATO REST VIGENTE

**Origen:** README de la API entregado por el usuario (`TaskManager API`). No se trata de un contrato inventado. El backend está construido en Node.js + Express, usa SQLite (`better-sqlite3`) y JWT. Su repositorio es privado. **Este README describe comportamiento; Codex debe contrastarlo con respuestas reales cuando la API sea accesible.**

### 6.1 Endpoints

| Método | Ruta | Bearer | HTTP éxito | Respuesta |
|---|---|---:|---:|---|
| `POST` | `/register` | No | `201` | `{ "token": "...", "user": { ... } }` |
| `POST` | `/login` | No | `200` | `{ "token": "...", "user": { ... } }` |
| `GET` | `/tasks` | Sí | `200` | `[{...Task}, ...]` |
| `GET` | `/tasks/{id}` | Sí | `200` | `{...Task}` |
| `POST` | `/tasks` | Sí | `201` | `{...Task}` |
| `PUT` | `/tasks/{id}` | Sí | `200` | `{...Task}` |
| `DELETE` | `/tasks/{id}` | Sí | `204` | Sin cuerpo |
| `GET` | `/health` | No | `200` | `{ "status": "ok" }` |

Errores documentados: `/register` 400 o 409 (duplicado), `/login` 400 o 401, `/tasks` 401, detalle 401/404, crear 400/401, modificar 400/401/404, eliminar 401/404.

### 6.2 Auth JSON exacto

Registro: UI pide nombre, correo, contraseña y confirmación, pero **`confirmPassword` nunca viaja al backend**:

```json
{
  "name": "María",
  "email": "maria@example.com",
  "password": "contraseña-de-ejemplo"
}
```

Login:

```json
{
  "email": "maria@example.com",
  "password": "contraseña-de-ejemplo"
}
```

Respuesta documentada:

```json
{
  "token": "JWT_DEVUELTO_POR_LA_API",
  "user": {
    "id": 1,
    "name": "María",
    "email": "maria@example.com"
  }
}
```

Los ejemplos son **ilustrativos**, no credenciales funcionales ni salidas verificadas. El cliente debe manejar errores HTTP, red caída y timeouts.

### 6.3 Modelo `Task`

Respuesta:

```json
{
  "id": 1,
  "title": "Estudiar Kotlin",
  "description": "Repasar corrutinas y Retrofit",
  "priority": 3,
  "dueDate": "2026-11-15",
  "completed": false,
  "category": "Estudio"
}
```

| Campo | Tipo REST | Restricción |
|---|---|---|
| `id` | Identificador numérico | Lo asigna servidor; usar tipo Android según respuesta real |
| `title` | `String` | Obligatorio y longitud **máx. 120** |
| `description` | `String?` | Puede ser `null` |
| `priority` | `Int` | **1..5**, predeterminado **3** |
| `dueDate` | `String?` | `yyyy-MM-dd` o `null` |
| `completed` | `Boolean` | Predeterminado `false` |
| `category` | `String` | Predeterminado `"General"` |

**No incluir `userId` en el JSON público**: la API obtiene propietario desde JWT. En DB puede existir `user_id`, pero el modelo REST no lo expone ni lo requiere en create/update. No agregarlo al DTO público por el diseño original del TaskManager antiguo.

**`PUT`, NO `PATCH`:** `PUT /tasks/{id}` reemplaza una tarea completa; los opcionales omitidos vuelven al predeterminado. El formulario de edición debe enviar el objeto completo y preservarlo correctamente.

**`DELETE` devuelve 204 vacío:** usar retorno `Unit` con Retrofit suspend, no esperar cuerpo JSON.

### 6.4 Errores

Estructura documentada:

```json
{
  "message": "Hay datos inválidos",
  "errors": { "email": "El correo no tiene un formato válido" }
}
```

`errors` se documenta solamente para HTTP `400` y `409`; puede no venir en otras respuestas. En pantalla mostrar `message` o una explicación clara sin filtrar excepciones técnicas. **No interpretar un error de red como lista vacía.**

**Diferenciar 401 según contexto:**

- `401` durante `/login`: credenciales inválidas -> mostrar error en Login, **no** cerrar un token por accidente en el proceso.
- `401` en endpoint protegido `/tasks`: token ausente, inválido, expirado o usuario eliminado -> **limpiar sesión y navegar a Login** sin que Atrás regrese a Home.

### 6.5 Ejemplo de servicio correcto

```kotlin
interface TaskApiService {
    @GET("tasks")
    suspend fun getTasks(): List<TaskDto>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: Int): TaskDto

    @POST("tasks")
    suspend fun createTask(@Body body: TaskRequestDto): TaskDto

    @PUT("tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: Int,
        @Body body: TaskRequestDto
    ): TaskDto

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Unit
}
```

`TaskRequestDto` es un ejemplo de **DTO de escritura** permitido dentro de `data/remote/dto`. No usar `Call<T>`, no pasar DTO al ViewModel. Configurar Kotlinx serialization incluyendo `Json { ignoreUnknownKeys = true }` y Retrofit compatible con la versión 3 instalada. Verificar compatibilidad real en Gradle; no fijar a ciegas versiones incompatibles.

---

## 7. MODELO DE DATOS Y VALIDACIONES

### Registro (ViewModel)

- `name`: obligatorio (no espacios vacíos).
- `email`: obligatorio, formato válido.
- `password`: obligatoria, **mínimo 8 caracteres**.
- `confirmPassword`: obligatoria, debe coincidir con password; **validación local**, no enviar al backend.
- Deshabilitar envíos duplicados mientras se procesa.

### Login (ViewModel)

- Correo y contraseña no vacíos.
- Error claro para formato inválido si se valida, credenciales erróneas y falta de conexión.
- Nunca guardar la contraseña en SharedPreferences ni logs.

### Task (formulario único crear/editar)

- `title`: obligatorio, trim y longitud máxima 120.
- `priority`: número entero en rango 1..5, por defecto 3.
- `dueDate`: opcional; si se proporciona, validar `yyyy-MM-dd` y fecha real, usando fecha seleccionable si es rápido.
- `description`: opcional (`null` o texto según contrato).
- `completed`: booleano, por defecto false en creación.
- `category`: texto, usar `"General"` cuando proceda.
- Mantener nombres JSON camelCase (`dueDate`, `completed`) idénticos al backend.
- Mostrar retroalimentación visual cuando guardar o eliminar tenga éxito/fracase.

**Prioridades:** el backend almacena **1–5**. Las etiquetas de Figma «Alta/Media/Baja» no son campos de DB; son solo posibles representaciones visuales. No transformar el valor del backend sin un mapeo explícito y coherente. Priorizar un control numérico simple si Figma aún no define escalas.

---

## 8. PANTALLAS Y NAVEGACIÓN

| Vista | Archivos | Mínimo funcional |
|---|---|---|
| Login | `ui/auth/login/*` | Correo, contraseña, mostrar/ocultar, entrar, ir a registro, errores y loading |
| Register | `ui/auth/register/*` | Nombre, email, contraseña, confirmación, validaciones y navegación |
| Home | `ui/home/*` | `LazyColumn`, loading, empty, error, retry, botón `+`, logout |
| Detalle | `ui/task/TaskDetailScreen.kt` | Mostrar todos los atributos, editar y eliminar |
| Formulario | `ui/task/TaskFormScreen.kt` | **Mismo formulario** para crear/editar, precarga al editar |
| Navegación | `ui/navigation/*` | Destino inicial según JWT; limpiar back stack login/logout |

```text
APP START
   ├── token guardado → HOME → cargar /tasks (si 401: borrar token y Login)
   └── sin token   → LOGIN
                         ├── REGISTRO → /register → guardar JWT → HOME
                         └── LOGIN    → /login    → guardar JWT → HOME

HOME (LazyColumn, botón +, logout)
   ├── + → FORM (CREAR) → POST → refrescar HOME
   ├── pulsar tarea → DETALLE
   │                   ├── EDITAR → FORM (EDITAR) → PUT → refrescar HOME/DETALLE
   │                   └── ELIMINAR → confirmar → DELETE → refrescar HOME
   └── LOGOUT → borrar token + limpiar back stack → LOGIN
```

**Persistencia:** al reabrir la app con token guardado, saltar Login; comprobar vigencia al consultar API. Evitar pantalla Login fugaz durante resolución inicial del estado, cuando sea práctico. No navegar automáticamente a Home si registro/login real falló.

**Rúbrica UI:** todas las operaciones deben mostrar resultado. `Snackbar`, texto de error o navegación son aceptables. La lista se actualiza tras create/update/delete. En creación/edición se puede navegar de regreso y cargar de nuevo de forma explícita y confiable.

**Visual:** nombre **Anotado**, estética limpia Material 3. El diseño de Figma se trabaja en paralelo; la referencia reciente de Login usa blanco y verde oscuro, pero no bloquear el código funcional por fidelidad pixel a pixel. No agregar filtros/buscador solo por antiguos mockups.

---

## 9. DISEÑO INTERNO DE RESPONSABILIDADES

### `domain`

- `model/Task.kt`: modelo limpio de tarea, no `@Serializable` por obligación si se separa de DTO.
- `repository/AuthRepository.kt`: operaciones de registro, login y sesión mediante conceptos de dominio, sin `AuthResponseDto`.
- `repository/TaskRepository.kt`: `list/get/create/update/delete` usando `Task` u otros tipos de dominio. Los detalles de red no se filtran a presentación.

### `data/remote`

- `AuthApiService.kt`: solo rutas `/register` y `/login`, ambas `suspend`.
- `TaskApiService.kt`: solo rutas `/tasks`, `suspend`.
- DTO: modelos de transporte `@Serializable`, `@SerialName` cuando sea necesario; `description` y `dueDate` nulables.

### `data/mapper`

- `TaskMapper.kt`: conversión **DTO ↔ Task** y/o DTO de escritura; NO replicar mapeos en ViewModels ni pantallas.

### `data/repository`

- `AuthRepositoryImpl.kt`: coordina API auth, traducción de errores y SessionManager.
- `TaskRepositoryImpl.kt`: coordina API tasks y mapper; traduce errores de red a excepciones/resultado de aplicación manejables.

### `ui`

- `LoginViewModel`, `RegisterViewModel`: validación y operaciones de auth; `UiState`, estado de carga y mensajes.
- `HomeViewModel`: consulta de tareas, loading/empty/error/retry; no REST directo.
- `TaskViewModel`: estado del detalle, formulario y eliminación según lo requiera la pantalla. Mantener responsabilidades manejables y usar funciones pequeñas.
- Composables sin lógica de negocio, consumiendo estado y disparando eventos.
- `AppNavigation.kt` y `Routes.kt`: rutas y flujo login/logout, no lógica de red.

### `di`, `session`, `network`

- `AppContainer`: composición/instanciación de infraestructura y repositorios compartidos.
- `ViewModelFactory`: creación de ViewModels con interfaces inyectadas por constructor.
- `SessionManager`: contrato + implementación de persistir/leer/borrar JWT (SharedPreferences como elección sencilla).
- `AuthInterceptor`: adjunta Bearer al existir sesión; se debe evitar añadirlo en endpoints públicos si puede causar confusión.
- `RetrofitClient`: OkHttp, Retrofit y convertidor JSON; configuración **sin** dobles instancias por pantalla.
- `TaskManagerApplication`: mantiene una sola instancia de contenedor.
- `MainActivity`: solo punto de entrada UI, lifecycle y tema.

---

## 10. INYECCIÓN MANUAL, SESIÓN Y RED

### Flujo de dependencias

```text
TaskManagerApplication
       └── AppContainer [una instancia compartida]
             ├── SessionManager (SharedPreferences)
             ├── AuthInterceptor (depende de SessionManager)
             ├── OkHttpClient
             ├── Retrofit (URL configurable, Kotlinx serialization)
             ├── AuthApiService / TaskApiService
             ├── AuthRepositoryImpl : AuthRepository
             └── TaskRepositoryImpl : TaskRepository

ViewModelFactory + interfaces domain
       ├── LoginViewModel(AuthRepository)
       ├── RegisterViewModel(AuthRepository)
       ├── HomeViewModel(TaskRepository, AuthRepository si logout)
       └── TaskViewModel(TaskRepository)
```

**No crear Retrofit ni repositorios ad hoc dentro de Screens/ViewModels.** Si la sesión persistida se lee síncronamente para un interceptor, la abstracción debe permitirlo sin bloquear red o disparar I/O costoso en Compose; evitar utilizar `runBlocking` en el hilo principal. Manejar expiración de sesión sin generar bucles de navegación o múltiples mensajes.

### Base URL

- **Emulador + backend local:** `http://10.0.2.2:3000/`.
- **Emulador + AWS EC2:** URL pública **real** informada y verificada por Gemini; `10.0.2.2` NO apunta a EC2.
- La URL debe poder configurarse por variante (`BuildConfig`/propiedad Gradle/local override), **una vez**. Retrofit requiere barra final.
- No dejar una IP EC2 inventada ni un secreto en el código fuente.
- Si se utiliza HTTP, habilitar **cleartext solo en debug**, mediante configuración de variante/manifiesto o `network_security_config` de debug; release debe preferir HTTPS y no permitir cleartext de forma global.
- Agregar permiso `android.permission.INTERNET` en manifiesto.
- Un build que compile sin URL EC2 definitiva no equivale a integración probada. El backend local o EC2 debe estar alcanzable para probar real.

### Seguridad

- No incluir `.env`, JWT_SECRET, contraseñas, API keys, secretos AWS, claves privadas ni tokens reales en repo, logs ni capturas.
- JWT solo en almacenamiento de sesión; evitar imprimir header Authorization en logs.
- El logout definido para la app es **local**; el README de API **no documenta endpoint `/logout`**.
- No usar base de datos SQLite directamente desde Android.
- Para la entrega académica, privilegiar funcionamiento con condiciones reales sin introducir almacenamiento de contraseñas.

---

## 11. ESTADOS, ERRORES Y SEGURIDAD

| Acción | Estado/error necesario | Comportamiento |
|---|---|---|
| Iniciar sesión | Idle, Loading, Error, Success | No duplicar envíos; si 401 login mostrar credenciales incorrectas |
| Registrarse | Idle, Loading, errores de campo, Success | Validar en ViewModel antes de API; 409 correo duplicado |
| Home | Loading, Empty, Success, Error | `LazyColumn` con datos reales; botón Reintentar |
| Detalle | Loading, Success, Error/404 | Mostrar todos los campos o error claro |
| Crear/editar | Valores del formulario, errores, Saving | Validar title, prioridad, fecha; deshabilitar botón guardando |
| Eliminar | Confirmación, Eliminando, resultado | No borrar sin confirmación; refrescar lista |
| Sesión vencida | 401 protegido | Borrar token y limpiar back stack; evitar bucles |
| Offline/red | Error de conexión | Mensaje legible y reintento; nunca confundir con empty |

**Manejo de HTTP y corrutinas:** `suspend`, `try/catch` o `Result` de dominio; **propagar `CancellationException`** cuando corresponda para no romper cancelación estructurada. No usar `catch(Throwable)` indiscriminado. `HttpException` permite distinguir códigos de respuesta; errores de parseo y red deben producir feedback claro.

---

## 12. PLAN DE EJECUCIÓN PARA CODEX

**OBJETIVO:** desarrollar el proyecto Android **completo**, sin necesidad de un mensaje nuevo por cada archivo, pero **compilando por hitos** y reportando bloqueos reales. Empezar por inspección; actuar solo dentro del repositorio del proyecto; pedir autorización normal si una acción rebasa permisos.

### Etapa 0 — Reconocimiento y protección de trabajo existente

1. Ejecutar `pwd`, inspeccionar `ls`, `find` de rutas relevantes, `git status`, archivos Gradle y SDK disponible.
2. Verificar que se está en `~/AndroidStudioProjects/TaskManager` **o la ruta real indicada por el usuario**, no asumir que ya existe.
3. Si no hay proyecto Android, usar plantilla **Empty Activity Compose**, Kotlin, package `com.example.taskmanager`, mínimo SDK **26** como elección propuesta; adaptar a SDK instalado si necesario sin romper rúbrica.
4. No sobrescribir archivos ni borrar cambios existentes. Si `git status` está sucio, preservar trabajo y evitar resets/destructivos.
5. Registrar lo que existe y las carencias reales; no afirmar que Gradle/SDK está listo sin comprobarlo.

**Salida:** proyecto inicial identificado y estructura de trabajo segura.

### Etapa 1 — Proyecto compilable y dependencias

- Configurar `settings.gradle.kts`, `build.gradle.kts` raíz, `app/build.gradle.kts`, `libs.versions.toml` y configuración Android/Compose.
- Verificar versiones compatibles de AGP, Kotlin, Compose, Navigation, lifecycle, Retrofit 3, kotlinx serialization y OkHttp **según catálogo existente y disponibilidad real**, no inventar números de versión.
- Configurar Java/JDK exigido por AGP instalado (probablemente JDK 17 o posterior; **verificar**).
- Crear los paquetes/archivos del árbol oficial con código real o scaffolding mínimo, no cientos de clases vacías sin propósito.
- Configurar `AndroidManifest` con `TaskManagerApplication` y permiso INTERNET.
- `app_name` debe ser **Anotado**.
- Ejecutar `./gradlew assembleDebug` (conservar salida real). Si falla por SDK/JDK/red, diagnosticar e informar; no falsificar éxito.

**Criterio:** build `assembleDebug` correcto y app inicia en emulador si hay AVD.

### Etapa 2 — Infraestructura de red y dominio

- Definir `Task`, `AuthRepository`, `TaskRepository`.
- Crear DTOs `@Serializable`, request de escritura, `TaskMapper`.
- Implementar API services `suspend`; **0** ocurrencias de Retrofit `Call<`.
- Implementar SessionManager y AuthInterceptor; Retrofit con Kotlinx serialization.
- Repositorios con traducción coherente de errores y valores de dominio.
- Instanciar una sola vez con AppContainer y enlazar ViewModelFactory.
- Compilar y corregir antes de continuar.

**Criterio:** separación arquitectónica correcta, dependencias resueltas, build correcto.

### Etapa 3 — Registro y login operativos

- RegisterScreen/ViewModel con 4 campos y validaciones.
- LoginScreen/ViewModel con 2 campos, toggle contraseña si práctico, loading/error.
- `AppNavigation` y `Routes`; inicialización por sesión, post-login, logout sin backstack.
- Llamadas reales a `/register` y `/login`; guardar JWT, interceptor en tasks.
- **No afirmar éxito real** si API inaccesible; permitir terminar el build y marcar integración pendiente.
- Compilar.

**Criterio:** flujo auth completo y comprobado con servidor real cuando esté accesible.

### Etapa 4 — CRUD de tareas completo

- HomeScreen/ViewModel: `LazyColumn`, task cards, botón **+**, loading/empty/error/retry, logout.
- TaskDetailScreen: todos los atributos; opciones editar/eliminar.
- TaskFormScreen **única** reutilizada para create/edit; precarga de datos en modo editar y validaciones.
- TaskViewModel para acciones de tarea; confirmación mediante AlertDialog antes de DELETE.
- Respetar `PUT` completo y `DELETE 204` con `Unit`.
- Actualizar lista y mensajes al crear, editar y borrar.
- Manejar sesión vencida (401 protegido) con cierre limpio.
- Compilar.

**Criterio:** flujo E2E listo para conectarse y probar en el servidor real.

### Etapa 5 — QA y entrega

- Ejecutar `./gradlew assembleDebug`, y `./gradlew testDebugUnitTest` si hay pruebas instaladas.
- Corregir errores reales en lugar de ocultarlos.
- Si hay emulador, ejecutar navegación/flujo real y documentar resultados; si no, explicar por qué.
- Inspeccionar búsquedas `Call<`, `enqueue(`, `execute(`, uso accidental de Hilt, DTO fuera de data, Retrofit en VM/Compose.
- Verificar permisos debug HTTP, `INTERNET`, startup y backstack.
- Probar API `/health` y registrar URL real **sin secretos** si Gemini la entregó.
- Realizar commits descriptivos para fases completadas (no editar proyecto backend/EC2).
- Entregar resumen por archivos, comando probado, salida real, pendientes y próxima acción útil.

**Criterio:** app demostrable según rúbrica; si AWS no está disponible, compile + integración preparada pero **no** decir que E2E está probado.

### Registro de progreso mínimo (en respuesta de Codex)

Por cada etapa indicar:

```text
ETAPA:
CAMBIOS: rutas exactas + propósito
PRUEBAS EJECUTADAS: comando y resultado (no suponer)
ADVERTENCIAS: solo problemas reales
SIGUIENTE PASO: acción concreta
```

No solicitar aprobación por **cada archivo**. Proceder con implementaciones razonables dentro de esta especificación; detenerse solo ante decisiones que rompan una restricción o permisos del sistema.

---

## 13. PRUEBAS, CALIDAD Y RÚBRICA

### 13.1 Rúbrica — total 40 puntos

| Ítem | Porcentaje | Puntos de 40 | Evidencia para logrado |
|---|---:|---:|---|
| Autenticación | 20% | **8** | API real, validaciones, persistencia detrás abstracción, interceptor, logout y backstack correcto |
| Home CRUD | 25% | **10** | 5 operaciones, LazyColumn, formulario compartido validado, confirmación DELETE, refresco, loading/empty/error/retry |
| MVVM | 20% | **8** | ViewModels con DI, MutableStateFlow privado y UiState, UI observa con collectAsStateWithLifecycle y sobrevive rotación |
| Repository + DTO + Mapper | 20% | **8** | Contratos en domain; implementación data; DTO `@Serializable` solo data; Mapper dedicado |
| DI manual | 15% | **6** | AppContainer singleton en Application, instancias compartidas, constructor injection, ViewModelProvider.Factory |
| **TOTAL** | **100%** | **40** | |

### 13.2 Lista de aceptación funcional

- [ ] El proyecto Android compila y abre correctamente.
- [ ] La app muestra **Anotado** como nombre visible.
- [ ] Registro permite ingresar nombre, correo, contraseña y confirmación.
- [ ] Valida vacíos, formato de email, contraseña de 8+ y coincidencia.
- [ ] `/register` crea usuario real o muestra error real; el backend retorna token.
- [ ] `/login` autentica con correo y contraseña o explica error de credenciales/red.
- [ ] El JWT se guarda detrás de `SessionManager` y persiste al reiniciar.
- [ ] Si existe JWT se entra a Home (y 401 protegido limpia sesión).
- [ ] Interceptor añade Bearer sin repetir header manualmente en endpoints.
- [ ] Home muestra LazyColumn de tareas reales del usuario.
- [ ] Home tiene Loading / Empty / Error con Reintentar.
- [ ] Hay botón **+** visible para crear.
- [ ] Detalle muestra todos los atributos del modelo.
- [ ] Crear valida y actualiza lista.
- [ ] Editar reutiliza el formulario, precarga y envía PUT completo.
- [ ] Eliminar exige confirmación, respeta DELETE 204 y actualiza lista.
- [ ] Feedback legible para guardar/eliminar/errores.
- [ ] Logout limpia JWT y backstack; Atrás no regresa a Home.
- [ ] Rotación no pierde estado indebidamente.
- [ ] UI no ejecuta Retrofit ni lógica de negocio.
- [ ] DTO se queda en data y mapper hace conversiones.
- [ ] El AppContainer es único y los ViewModels se crean por Factory.
- [ ] No existe `Call<T>`, callbacks Call ni God Classes.
- [ ] Se realizaron commits descriptivos.

### 13.3 Pruebas manuales con API real

1. `GET /health` responde `200` `{"status":"ok"}`.
2. Register válido obtiene `201 {token,user}`; duplicado `409`.
3. Login correcto obtiene `200 {token,user}`; inválido `401`.
4. GET tasks sin token devuelve `401`; con token `200` lista.
5. Crear tarea con título, prioridad y defaults devuelve `201 Task`.
6. Detalle del nuevo id devuelve esa tarea.
7. Editar mediante PUT con cuerpo completo devuelve Task actualizado.
8. Eliminar devuelve `204`; detalle posterior `404`.
9. Usuario B no puede acceder a tareas privadas del usuario A (verificar backend con Gemini/Claude; Android no debe asumir seguridad no probada).
10. Probar cierre sesión, reabrir aplicación, rotar, red apagada, errores 400/401/404 y reintento.

### 13.4 Comprobaciones automáticas útiles

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest       # si está configurado
rg -n 'Call<|\.enqueue\(|\.execute\(' app/src/main/java || true
rg -n 'Hilt|Dagger|Koin' app/src/main/java gradle || true
git status --short
```

**No considerar `rg` como prueba exhaustiva de arquitectura:** además inspeccionar manualmente firmas Retrofit, importaciones, inyección y uso de DTOs. Un comando que no se puede ejecutar por bloqueo debe reportarse como **NO EJECUTADO**, nunca como aprobado.

---

## 14. ENTORNO CACHYOS, GRADLE Y EC2

### Desarrollo local

El usuario usa **CachyOS**, basado en Arch Linux. Ya instaló **Codex CLI** y confirmó que funciona. El agente debe operar sobre el directorio del proyecto, no sobre todo `$HOME`.

Secuencia típica:

```bash
cd ~/AndroidStudioProjects/TaskManager
pwd
ls -la
codex
```

No instalar dependencias del sistema como root sin justificación y autorización. No asumir que Android SDK, `adb`, JDK, `ANDROID_HOME` y emulador están configurados: comprobarlos si el build falla.

### Despliegue de API

Gemini Pro tiene la responsabilidad principal de subir la API ya escrita a AWS EC2. Las expectativas del README de backend:

- Node.js 22 o posterior + Express + better-sqlite3.
- SQLite persiste en `data/taskmanager.db` en el servidor.
- `.env` privado con `JWT_SECRET` de al menos 32 caracteres.
- PM2 para mantener servidor activo.
- Security Groups y red de EC2 configurados para acceso necesario; tratar puerto 3000 con precaución.
- URL/Elastic IP si disponible y apropiada para el laboratorio.
- Respaldo de SQLite; no borrar base si ya contiene usuarios.

**Codex NO debe reemplazar, recrear ni desplegar el backend por su cuenta salvo petición explícita.** Debe preparar un cliente Android configurable y probar cuando se disponga de URL real. Nunca registrar secretos AWS en repositorio Android.

---

## 15. COMUNICACIÓN CON OTROS AGENTES

| Agente | Área principal | Integración con Codex |
|---|---|---|
| Codex (este agente) | **Implementación principal de Android Anotado** | Propietario de archivos Android en el momento de programación |
| Gemini Pro | API y AWS EC2 | Entrega URL base, estado `/health`, configuración TLS y cualquier discrepancia de contrato |
| Claude Sonnet 5.5 | Revisión/diagnóstico/refactor puntual | Puede revisar diffs, ayudar con errores sin reescribir archivos de Codex al mismo tiempo |
| Agente Android Studio | Compilación, pruebas en emulador, diagnóstico | No modificar simultáneamente los mismos archivos si Codex tiene una fase activa |
| Figma | Diseño visual e interacción | Proporciona pantallas y tokens visuales; no bloquea MVP |
| ChatGPT | Coordinación técnica y revisión de rúbrica | Recibe reportes y mantiene decisiones coherentes |

**Un agente por archivo a la vez.** Si recibes cambios ajenos de forma simultánea, inspecciona Git diff, preserva trabajo y evita restaurar o sobrescribir sin permiso.

---

## 16. CRITERIOS DE TERMINADO Y REPORTE FINAL

**No anunciar "Anotado está terminada" si falta alguno de estos pilares:**

1. Compilación exitosa real (`assembleDebug`).
2. Autenticación real probada con API accesible.
3. CRUD real funcional en la app.
4. Rúbrica respetada (MVVM, Repository/DTO/Mapper, DI manual).
5. Logout correcto y manejo de errores.

Si el servidor sigue en despliegue, describir con precisión: **"Android compila; integración EC2 pendiente de prueba"**. Nunca simular autenticación exitosa ni afirmar pruebas no realizadas.

El reporte al usuario debe incluir:

- Resumen compacto de funcionalidades implementadas.
- Lista de rutas editadas y motivo (agrupadas).
- Comandos de build/tests ejecutados + resultado real.
- Bloqueos/errores y cómo reproducirlos.
- Instrucciones para abrir en Android Studio y correr en emulador.
- Configuración de base URL sin compartir secretos.
- Commits generados, si se crearon y fueron autorizados.

---

## 17. PROMPT DE INICIO RÁPIDO

Copia esto al iniciar Codex en la raíz del proyecto:

> Lee **`AGENTS.md` y `README.md` completos**. Actúa como **programador principal Android** de **Anotado**. Inspecciona el estado real del repositorio antes de cambiar nada. **Implementa toda la aplicación académica** con autenticación JWT real y CRUD completo de tareas según el contrato REST y la estructura estricta de este README. No uses `Call<T>`, callbacks Retrofit, God Classes, Hilt, Dagger ni Koin. Usa Retrofit 3 `suspend`, Compose Material 3, MVVM/StateFlow, repositorios con interfaces en domain, DTO/Mapper en data e inyección manual con AppContainer + Factory. Trabaja por etapas y compila después de cada una con `./gradlew assembleDebug`; corrige fallos, protege cambios existentes y no inventes URL EC2. Si no tienes conectividad backend, deja la URL configurable, entrega un build verificable y reporta la prueba pendiente, sin falsificar éxito. **Comienza ya con inspección y ejecución; no te quedes solo en un plan.**

---

## FUENTES Y PRECEDENCIA

1. Últimas instrucciones expresas del propietario (nombre Anotado, estructura obligatoria, urgencia y arquitectura).
2. `Proyecto_Unidad3_App_CRUD.pdf` (enunciado académico).
3. `Rubrica_Proyecto_Unidad3.pdf` (rúbrica académica de 40 puntos).
4. README real entregado por el usuario para API Node + Express + SQLite + JWT.
5. Este documento operacional y `AGENTS.md` como condensación de reglas.
6. Diseños y mockups de Figma como referencia visual cuando estén confirmados.

**La fuente de verdad para datos de API en tiempo de ejecución es el backend real.** Si una llamada contradice lo documentado, registrar discrepancia y resolverla sin introducir supuestos silenciosos.
