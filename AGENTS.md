# Instrucciones para trabajar en Anotado

## Contexto obligatorio

Antes de implementar, leer `README.md`, `docs/referencias/REGLAS-IMPORTANTES.md`
y `docs/API-README.md`. Las referencias visuales están en `docs/figma/`.
Se conserva el documento de instrucciones recibido en
`docs/referencias/AGENTS-original.md`.

## Identidad y discrepancia pendiente

- Proyecto real indicado por el usuario: `/home/yharim/AndroidStudioProjects/Anotado`.
- Nombre visible: **Anotado**.
- Gradle declara namespace/applicationId `com.yharim.anotado`; las fuentes están
  en `app/src/main/java/com/yharim/anotado`, pero algunas declaran `com.example.taskmanager`.
- Los documentos recibidos exigen `TaskManager/` y `com.example.taskmanager` y
  prohíben renombrar sin autorización. Esta discrepancia no está resuelta:
  no asumir que la importación de documentación autoriza una migración.
  Resolver con el propietario antes de unificar la identidad; continuar con
  tareas independientes mientras tanto. No crear un segundo proyecto.

## Reglas de implementación

- Kotlin, Compose Material 3, Navigation Compose, Retrofit 3, OkHttp,
  kotlinx.serialization, Gradle Kotlin DSL y Version Catalog.
- Conservar paquetes hermanos `data`, `domain`, `ui`, `di`, `session`, `network`
  y los nombres de clases del árbol documentado. No añadir capas innecesarias.
- MVVM: UiState inmutable, MutableStateFlow privado, StateFlow público,
  viewModelScope y collectAsStateWithLifecycle. Compose presenta estado y eventos.
- Repositorios: interfaces en domain, implementaciones en data. DTO @Serializable
  exclusivamente en data; conversiones en mapper. Sin Retrofit ni DTO en UI/VM.
- DI manual: AppContainer único en TaskManagerApplication, inyección por
  constructor y ViewModelProvider.Factory. AppContainer solo ensambla.
- Separar auth y tareas en servicios/repositorios. Prohibidas God Classes,
  Hilt, Dagger, Koin y Retrofit Call<T>, enqueue() o execute(). Servicios suspend
  con retornos directos. Preservar CancellationException.
- Backend existente Node/Express/SQLite/JWT; no recrearlo, sustituirlo ni
  desplegarlo sin petición expresa. No Firebase, json-server, RDS ni MySQL.
- Contrato: POST /register y /login; GET/POST /tasks;
  GET/PUT/DELETE /tasks/{id}; GET /health. PUT completo; DELETE 204 como Unit.
  Nunca enviar userId ni confirmPassword.
- Task: id numérico según API, title obligatorio máximo 120, description nullable,
  priority 1–5 (default 3), dueDate nullable yyyy-MM-dd, completed false por defecto,
  category "General" por defecto. No inventar campos.
- Registro: nombre, email válido, contraseña de 8 o más y confirmación local.
- JWT persistente detrás de SessionManager y Bearer por interceptor.
  401 login: credenciales incorrectas. 401 tareas: borrar sesión y volver a Login
  limpiando historial protegido. Logout local, sin endpoint inventado.
- Base URL configurable, sin inventar EC2. 10.0.2.2:3000 es solo para backend
  local desde emulador. HTTP cleartext solo en debug; no registrar secretos/tokens.
- Home con LazyColumn, loading/empty/error/retry y crear/cerrar sesión.
  Detalle completo; formulario compartido crear/editar; confirmar eliminación;
  actualizar lista y detalle tras cambios.
- Prioridad: compilar → red/domain/DI → auth real → CRUD real → QA → pulido visual.
  Sin datos ficticios presentados como integración real ni funciones extras.

## Separación estricta confirmada por el propietario

- Composables (incluida navegación) no acceden a repositorios, SessionManager,
  servicios HTTP ni DTO. Observan UiState del ViewModel y envían eventos.
- AppViewModel en ui/navigation presenta la sesión global; HomeViewModel coordina
  logout; LoginViewModel y RegisterViewModel poseen su estado de formulario.
- MainActivity/Factory son puntos de ensamblado. AppNavigation recibe ViewModels
  y ViewModelProvider.Factory, no el contenedor de repositorios.
- Toda nueva funcionalidad debe mantener UI → ViewModel → interfaz de repositorio
  en domain → implementación en data → servicio/DTO. Convertir DTO de tareas a
  dominio en TaskMapper antes de exponer datos a UI. No usar el estado incompleto
  del CRUD como evidencia de arquitectura o funcionalidad terminadas.

## Trabajo y verificación

- Inspeccionar git status, fuentes y configuración antes de editar; preservar
  cambios del usuario, incluidos staged y untracked. No resets destructivos.
- Trabajar en este repositorio; no modificar backend ni sistema innecesariamente.
- Al implementar, avanzar por hitos y probar sin pedir confirmación por archivo.
  Una petición de análisis o inicialización de contexto no equivale a implementar
  automáticamente todos los hitos.
- Ejecutar `./gradlew assembleDebug` y `./gradlew testDebugUnitTest` cuando
  corresponda al hito. Para verificar arquitectura, inspeccionar código y buscar
  `Call<`, `.enqueue(`, `.execute(`, Hilt/Dagger/Koin y DTO fuera de data.
- No afirmar compilación, emulador o integración E2E exitosos sin evidencia real.
- Hacer commits incrementales solo de cambios propios, sin incluir el trabajo
  staged ajeno. No sobrescribir cambios de otros agentes.
- Reportar archivos cambiados, comandos, resultados reales y bloqueos.

