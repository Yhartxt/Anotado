# Anotado

Aplicación Android académica de tareas: registro/login JWT y gestión de tareas,
con CRUD completo como objetivo; edición general y eliminación aún pendientes.
Esta documentación se inicializó a partir de los archivos entregados por el
usuario en `/home/yharim/Escritorio/reglas para codex`.

## Conexión AWS configurada

URL base proporcionada por el propietario el 9 de noviembre de 2026:
`https://54.152.139.22.nip.io/`.
Se configura en `gradle.properties` como `ANOTADO_API_BASE_URL` y se incorpora
al APK mediante `BuildConfig.API_BASE_URL`. `/health` es una ruta de diagnóstico,
no parte de la URL base. Las rutas /register, /login y /tasks conservan su contrato.

Para probar en el celular: sincronizar Gradle, volver a ejecutar/instalar la app,
crear una cuenta desde Registro y después crear una tarea. No hace falta editar
los repositorios ni los DTOs. Una APK ya instalada conserva su URL anterior hasta
recompilar. La validación TLS estándar sigue activa.

Comprobación desde este entorno: la herramienta web no pudo abrir /health ni
/tasks y curl devolvió `Could not resolve host` (HTTP 000). No se obtuvo respuesta
HTTP del servidor; disponibilidad, certificado e integración E2E quedan pendientes.
No se crearon usuarios ni tareas. No había override de la URL en propiedades de
usuario de Gradle ni en ORG_GRADLE_PROJECT_ANOTADO_API_BASE_URL al revisar.
El intento de assembleDebug sigue bloqueado por el error de sockets de Gradle.

## Documentación y diseño

- [Documentación técnica detallada: MVVM, Repository y DTO](docs/DOCUMENTACION-TECNICA.md).
- [Versión HTML para lectura e impresión](docs/DOCUMENTACION-TECNICA.html).
- [Instrucciones del proyecto](AGENTS.md).
- [Especificación completa, arquitectura, etapas y rúbrica](docs/referencias/REGLAS-IMPORTANTES.md).
- [Contrato de la API](docs/API-README.md).
- [Instrucciones originales recibidas](docs/referencias/AGENTS-original.md).
- Referencias visuales: [login](docs/figma/login.jpeg),
  [registro](docs/figma/registro.jpeg), [inicio](docs/figma/inicio.png)
  y [formulario](docs/figma/formulario.png).

Las copias de los documentos originales se conservan sin modificaciones.
Los PDFs del enunciado y la rúbrica mencionados en esos documentos no estaban
en la carpeta entregada; la rúbrica disponible es su transcripción en la especificación.

## Hito actual: Agregar tarea conectado

- Nueva/+ abren `TaskFormScreen` con la referencia `docs/figma/formulario.png`.
  El archivo recibido como `Agregar-tarea.jpeg` contiene la imagen de registro;
  se usó la referencia de formulario correcta ya disponible.
- Cabecera verde, título con contador, descripción, prioridades 1–5, categoría
  editable con sugerencias, fecha opcional yyyy-MM-dd, completada, Cancelar y Guardar.
- `TaskViewModel` posee estado, validación de título (trim, obligatorio y máximo 120),
  prioridad, fecha real y conversión de vacíos a valores opcionales/defaults.
  No hay validaciones ni DTOs en el composable.
- `TaskDraft` es entrada de dominio sin id asignado; TaskRepository.createTask
  recibe ese modelo. TaskMapper lo transforma a TaskRequestDto y el servicio
  suspend hace POST `/tasks`; el resultado vuelve como Task de dominio.
- Guardado duplicado bloqueado, errores de servidor por campo, borrador conservado
  ante fallo y éxito solo después de respuesta real. No se guarda localmente como
  si fuera éxito remoto. Atrás/Cancelar quedan bloqueados mientras se guarda.
- Tras éxito se vuelve a Home, se selecciona Todas y se refresca el listado,
  esperando cualquier carga/actualización previa para evitar perder el refresco.
- Cancelar vuelve a Home sin enviar solicitud. La fecha se introduce como texto;
  no se ha añadido calendario seleccionable.
- Todavía faltan edición completa, eliminación y verificación E2E con API real.
  Las cuatro vistas solicitadas están conectadas; no equivale a app terminada.

Verificación actual: **44 pruebas JVM aprobadas**, incluidas 10 de TaskViewModel
más las anteriores; inspección de MVVM/Repository/DTO y `git diff --check` correctos.
Se añadieron pruebas de red para POST y errores 400 (5 TaskNetworkTest, 8 AuthNetworkTest),
pendientes de ejecución. `./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain`
sigue bloqueado antes de compilar por FileLockContentionHandler/sockets. El APK
no se ha verificado desde este entorno.

## Home conectado a tareas

Referencia actual recibida: [home.png](docs/figma/home.png). Se implementaron
cabecera, saludo con nombre real, listado LazyColumn, tarjetas, filtros
Todas/Pendientes/Completadas, progreso, botones Nueva/+ y logout.

- `HomeViewModel` calcula todos los filtros, conteos, porcentaje, etiquetas,
  formato de fecha y estados loading/empty/error/retry. La vista solo renderiza.
- `TaskRepository` expone modelos de dominio. `TaskRepositoryImpl` consume servicios
  suspend y convierte mediante `TaskMapper`; DTOs permanecen exclusivamente en data.
- GET `/tasks` carga datos reales. Marcar pendiente/completada consulta la tarea
  actual y hace PUT con todos sus campos, incluidos null y false, sin userId.
  Se espera éxito del servidor antes de modificar la tarjeta o el progreso.
- Las respuestas protegidas 401 siguen invalidando sesión por interceptor.
- El nombre recibido por login/register se persiste junto a la sesión y se lee
  por AuthRepository. Sesiones antiguas sin nombre muestran «Hola»; volver a
  iniciar sesión obtiene el nombre real, sin inventar «María».
- El título es «Progreso de tareas», porque no hay fecha de finalización en API
  para calcular progreso diario. Los filtros y este resumen simple se incluyeron
  siguiendo la petición expresa de reproducir `home.png`.
- Pulsar tarjeta muestra un resumen con los datos cargados. Nueva/+ abren el formulario de creación. Editar otros
  campos y eliminar aún no están implementados; no se afirma CRUD completo.
- Las tareas de ejemplo están exclusivamente en la preview y pruebas.

Validación: **34 pruebas JVM aprobadas** (login 9, registro 10, sesión 5, Home 8,
mapper 2), compiladas con herramientas de la caché, fuera de Gradle. Para las
pruebas de mapper se usó la anotación Serializable de la caché local; esto no
verifica generación de serializers ni transporte JSON. `TaskNetworkTest` añade
3 pruebas de integración para GET/Bearer, PUT completo y 401; pendientes de
Gradle, al igual que las 8 pruebas de AuthNetworkTest. La compilación del APK
sigue bloqueada aquí por FileLockContentionHandler/sockets. El propietario
realiza las comprobaciones del APK en su celular; no se han verificado desde
esta sesión. La URL de AWS ya fue proporcionada; todavía no se afirma integración E2E.

## Login y registro

- MainActivity abre la navegación real. El login reproduce la referencia verde/blanca,
  con decoración circular, tarjeta redondeada, icono de contraseña, teclado y scroll.
- Validación de campos, correo sin espacios exteriores, envíos duplicados bloqueados,
  loading y errores de credenciales, validación, red y servicio.
- POST `/login` vía Retrofit suspend y Kotlinx Serialization; JWT persistente detrás
  de SessionManager, excluido de backups/transferencias. No se guarda la contraseña.
- Interceptor Bearer para tareas; 401 protegido invalida la sesión. Logout recrea el
  grafo de navegación y elimina el historial protegido.
- DI manual conectada en Application. La URL no configurada produce un error de
  disponibilidad al enviar, sin simular autenticación.
- Registro conectado desde login con nombre, email, contraseña y confirmación,
  siguiendo `docs/figma/registro.jpeg`. `RegisterScreen` solo muestra estado y eventos;
  validación, carga, bloqueo de envíos y visibilidad están en `RegisterViewModel`.
- `AuthRepository.register()` envía POST `/register`; `RegisterRequestDto` contiene
  únicamente name/email/password. La confirmación nunca sale del ViewModel.
  Errores 400/409 se muestran por campo; un registro correcto guarda JWT y cambia
  al grafo autenticado. El destino autenticado ahora muestra Home.
- Editar/eliminar pendientes. Volver desde registro usa Atrás o «Ya tengo una cuenta».
  No se requiere URL para abrir las pantallas; sí para autenticar/registrar realmente.
- Se mantienen los paquetes actuales de login/navegación/sesión y los imports se
  conectan a las clases reales. La unificación de identidad sigue pendiente de la
  respuesta del propietario; no se renombró namespace, applicationId ni carpeta.

### Ejecutar y configurar API

Abrir esta carpeta en Android Studio, sincronizar Gradle y ejecutar `app` en el
emulador. `LoginScreen.kt` y `RegisterScreen.kt` incluyen previews de 393×852 y 320×640 con texto ampliado.
No es necesario configurar API para ver la pantalla.

Con el backend real corriendo en la computadora anfitriona:

```sh
./gradlew :app:assembleDebug -PANOTADO_API_BASE_URL=http://10.0.2.2:3000/
```

Las ejecuciones desde Android Studio usan por defecto la URL AWS de
`gradle.properties`. Para otro servidor, sobrescribir ANOTADO_API_BASE_URL con
`-P` o en las propiedades de usuario de Gradle. La URL debe terminar en `/`.
En teléfono físico, 10.0.2.2 no sirve: la URL pública de AWS sí está configurada
para ese uso. HTTP solo está habilitado en debug; la URL actual utiliza HTTPS.

### Validación del hito

- `LoginViewModelTest` (9), `RegisterViewModelTest` (10) y `SessionViewModelsTest` (5):
  **24 pruebas aprobadas**, compilando las fuentes reales de
  dominio/ViewModel y el test con Kotlin 2.2.10, Lifecycle 2.11.0 y JUnit 4.13.2
  de la caché local. Se ejecutaron con `org.junit.runner.JUnitCore`, fuera de Gradle.
- `AuthNetworkTest`: 8 pruebas escritas para MockWebServer, **pendientes de ejecución**.
  Cubren JSON/login/token, 401 público/protegido, errores de campos/respuesta, URL ausente,
  cuerpo de registro sin confirmación y 409 sin crear sesión.
- `git diff --check`, parseo de TOML y XML: aprobados.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain`: bloqueado
  antes de compilar por `SocketException: Operation not permitted` al consultar
  interfaces de red (FileLockContentionHandler). ADB también falla al abrir su socket.
- No se generó un APK verificado ni se probó el diseño en emulador desde este entorno.
  No se probaron login ni registro contra backend local/EC2. Compilar y probar desde Android Studio.

Dependencias añadidas al catálogo según documentación de
[Navigation](https://developer.android.com/jetpack/androidx/releases/navigation),
[Retrofit](https://square.github.io/retrofit/) y
[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0).

## Corrección de arquitectura antes del CRUD

- `AppViewModel` (nuevo archivo en `ui/navigation`) encapsula el estado global de
  sesión y se conserva en el scope de MainActivity. Se añadió para eliminar el
  acceso al repositorio desde la navegación, por petición expresa del propietario.
- `AppNavigation` recibe AppViewModel y la interfaz ViewModelProvider.Factory;
  observa UiState y representa rutas, sin acceder a AppContainer ni repositorios.
- `HomeScreen` recibe estado/eventos de HomeViewModel. Logout pasa por
  `HomeViewModel.onLogoutClick()` → AuthRepository → SessionManager.
- Login y registro mantienen visibilidad de contraseñas en sus ViewModels.
  Se eliminó el callback redundante de éxito del login: AppViewModel es la fuente
  de estado de sesión que observa la navegación.
- Factory ensambla los cuatro ViewModels con interfaces de dominio. MainActivity
  conecta las dependencias y el tema, sin operaciones de autenticación.
- Inspección y comprobación estática: sin DTO, Retrofit, OkHttp o SessionManager
  en UI/domain; sin repositorios ni MutableStateFlow en composables. Las vistas
  conservan únicamente presentación, foco, teclado y eventos de navegación.
- AuthRepository no expone DTOs. AuthRepositoryImpl consume DTOs dentro de data;
  TaskRepository/TaskDto/TaskMapper ya soportan creación, listado y cambio de completada;
  el resto del CRUD sigue pendiente.
- Pruebas de sesión cubren estado inicial persistido, cambios de autenticación,
  invalidación, cierre mediante ViewModel y recuperación de errores. No prueban
  el back stack del emulador: esa validación sigue pendiente junto al APK.

## Estado observado al inicializar

- Carpeta real: `Anotado`; namespace/applicationId: `com.yharim.anotado`.
- La especificación menciona `TaskManager/com.example.taskmanager`.
  Hay fuentes de ambos paquetes; la identidad definitiva queda por resolver
  antes de migrar paquetes o renombrar el proyecto.
- MainActivity aún muestra el saludo de la plantilla.
- Login dispone de UI y ViewModel parciales, con referencias sin resolver.
- Repositorios, red, DI, registro y CRUD están mayormente sin implementar.
- Faltan dependencias usadas por las fuentes y configuración del manifiesto.
- Solo existen las pruebas de ejemplo de Android Studio.
- Hay cambios del usuario staged, unstaged y archivos untracked; preservarlos.

## Diseño y alcance

Identidad verde/blanca, tarjetas y campos redondeados, marca Anotado con check.
Las imágenes son referencias visuales, no evidencia de funciones implementadas.
El formulario recibido está recortado en la parte inferior. No se recibió
referencia específica para detalle o confirmación de eliminación.

El mockup de inicio incluye progreso y filtros. La especificación prioriza
listado/CRUD y excluye dashboard/estadísticas/filtros avanzados: no ampliar el
alcance automáticamente por elementos decorativos o adicionales del mockup.
Prioridad sigue siendo app funcional y rúbrica; después fidelidad visual.

## API y pendientes de integración

Backend documentado: Node.js + Express + SQLite con JWT, gestionado aparte.
El propietario proporcionó `https://54.152.139.22.nip.io/`; ya está configurada.
`http://10.0.2.2:3000/` es únicamente la dirección del host desde el emulador
si existe un backend local escuchando en ese puerto; no es una URL de EC2.
El propietario reporta el despliegue. La integración real no se ha verificado desde este entorno.

## Compilación y validación

Desde la raíz de este proyecto:

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Intento previo de diagnóstico:
`./gradlew :app:assembleDebug :app:testDebugUnitTest --offline --console=plain`.
Falló al iniciar Gradle con `Could not determine a usable wildcard IP for this machine`
en FileLockContentionHandler. No llegó a compilar ni ejecutar pruebas; no es un
diagnóstico del compilador Kotlin. No se repitió por este cambio documental.

Gradle declara compile/target SDK 37 y min SDK 26. Se observaron directorios
SDK `android-35`, `android-37.0` y build-tools `36.0.0`; su compatibilidad y el
toolchain completo aún deben verificarse durante la implementación.

## Secuencia de entrega

1. Resolver identidad y conseguir compilación/arranque.
2. Implementar dominio, DTO/mapper, red, sesión y DI manual.
3. Conectar registro/login reales y navegación.
4. Implementar listado, detalle, formulario compartido y eliminación confirmada.
5. Verificar errores, sesión vencida, refresco, pruebas y APK.

No marcar la app terminada sin compilación exitosa y autenticación/CRUD reales
probados, siguiendo la lista de aceptación de la especificación completa.
