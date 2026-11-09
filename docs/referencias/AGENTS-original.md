# AGENTS.md — instrucciones persistentes para Codex (raíz de TaskManager)

## Objetivo

Eres el programador principal Android de **Anotado**, una aplicación académica de tareas con registro/login JWT y CRUD completo. **Antes de trabajar, lee el `README.md` del proyecto**: contiene arquitectura, contrato API, estructura de carpetas, aceptación y plan de ejecución. Prioridad: APK que compile + auth/CRUD reales + rúbrica; no pulido visual ni extras.

## Invariantes que NO debes romper

1. App visible: **Anotado**. Mantén carpeta raíz `TaskManager/` y package **`com.example.taskmanager`**. No renombres ambos sin autorización.
2. Estructura fija: paquetes hermanos `data/`, `domain/`, `ui/`, `di/`, `session/`, `network/` directamente bajo `com.example.taskmanager`. Conserva nombres/rutas del árbol del README.
3. **PROHIBIDO Retrofit `Call<T>`, `.enqueue()` y `.execute()`**. Solo servicios `suspend` con retornos directos.
4. **PROHIBIDAS God Classes**. AuthApiService y TaskApiService separados; AuthRepository y TaskRepository separados; AppContainer **solo ensambla** dependencias.
5. **NO** Hilt, Dagger, Koin. DI manual: `AppContainer` único en `TaskManagerApplication`, constructor injection y `ViewModelProvider.Factory`.
6. Interfaces de repositorio en `domain/repository`; implementaciones y DTO `@Serializable` en `data`; mapper exclusivo en `data/mapper`. **Ningún DTO entra en ViewModel o Compose**.
7. Composables son presentación y eventos. ViewModels poseen `MutableStateFlow` privado + `StateFlow` público, `UiState` inmutable y `viewModelScope`; observar en UI con `collectAsStateWithLifecycle()`.
8. Backend **EXISTENTE**: Node.js + Express + SQLite JWT, alojado/por alojar en EC2. **No** json-server, RDS, MySQL ni Firebase. No construir backend desde Android.
9. API exacta: POST `/register`, POST `/login`; GET/POST `/tasks`; GET/PUT/DELETE `/tasks/{id}`; GET `/health`. Protected tasks: Bearer JWT por interceptor. `PUT` completo, `DELETE` 204 sin cuerpo. **No** incluir `userId` en JSON ni `confirmPassword` en registro.
10. `Task`: id, title (obligatorio, máximo 120), description (`String?`), priority (`Int` 1–5, default 3), dueDate (`String?` yyyy-MM-dd), completed (`Boolean`, default false), category (`String`, default "General"). No inventar otros campos.
11. `401` de `/login` = error de credenciales. `401` de tasks = limpiar JWT, navegar a Login sin back stack protegido. Logout solo local; no endpoint `/logout` documentado.
12. Base URL configurable; para emulador con servidor local `http://10.0.2.2:3000/`, para EC2 usar URL **real** que dé Gemini. **No inventar URL/IP**. HTTP cleartext únicamente en debug; preferir HTTPS fuera de debug.
13. No guardar contraseñas, tokens en logs ni secretos AWS en repositorio. No borrar trabajo existente ni ejecutar comandos destructivos sin autorización.
14. Todo el código debe ser Kotlin + Compose Material 3, Navigation Compose, Retrofit 3, kotlinx.serialization, OkHttp, Version Catalog y Gradle Kotlin DSL. No agregar funciones no requeridas.

## Definición de completado

- Registro con validaciones: nombre, email, password >= 8, confirmación; login real con errores.
- Sesión JWT persistente detrás de SessionManager, interceptor Bearer y logout con backstack limpio.
- Home `LazyColumn` con loading/empty/error/retry, botón `+` para crear y logout.
- Detalle muestra atributos; formulario único sirve create/edit con validación; delete pide confirmación; lista actualiza después de cambios.
- MVVM, DTO/Mapper/Repository y DI manual visibles para la rúbrica.
- `./gradlew assembleDebug` ejecutado y resultado comprobado. Si API inaccesible, no afirmar E2E probado.

## Forma de trabajar

1. Inspecciona archivos, Gradle, SDK y `git status` antes de cambiar nada; preserva cambios preexistentes.
2. Implementa el plan en etapas: proyecto compilable → red/domain/DI → auth → CRUD → pruebas.
3. **No pares después de planificar**: edita y prueba dentro de permisos disponibles; compila después de cada hito, corrige errores y continúa.
4. No pidas confirmación por cada archivo; pregunta solo si hay bloqueo real o cambio de contrato. No uses permisos sin supervisión ni cambios de sistema innecesarios.
5. Realiza commits incrementales descriptivos si Git ya está configurado y no sobrescribes trabajo ajeno.
6. Reporta siempre **qué archivos cambiaste, comandos ejecutados, resultados reales y bloqueos**. No inventes éxito de build ni despliegue.
7. Figma define diseño visual aparte; pantalla simple funcional es mejor que pixel-perfect incompleto. Marca visible **Anotado**; no mantener logos morados antiguos como contrato.

**Documento de detalle:** [`README.md`](README.md). En caso de dudas, compara con el README backend y los PDFs oficiales del profesor disponibles en el contexto del proyecto.
