# Anotado — documentación técnica y explicación de la arquitectura

**Fecha de revisión:** 9 de noviembre de 2026.  
**Proyecto:** aplicación Android nativa para tareas personales.  
**Ubicación revisada:** `/home/yharim/AndroidStudioProjects/Anotado`.  
**Fuente principal:** código y configuración presentes en el repositorio, contrastados con el contrato de API entregado por el propietario.

Este documento explica qué hace el proyecto, cómo se distribuyen sus responsabilidades y cómo se conecta al servidor. Los ejemplos de código corresponden a archivos existentes; cuando se abrevia un fragmento se indica expresamente. Una función implementada no se considera automáticamente probada contra el servidor.

**Terminología:** «repo» es una abreviatura de **Repository**, no un patrón diferente. Por el contexto del proyecto, «ODT» se interpreta aquí como **DTO**, *Data Transfer Object*. ODT también es el nombre de un formato de documento, pero no es el patrón de transporte de datos empleado en esta aplicación. Las tres partes principales son:

1. [MVVM: presentación, estado y eventos](#parte-1-mvvm).
2. [Repository: acceso a datos, dependencias y conexión con AWS](#parte-2-repository).
3. [DTO y Mapper: transporte, conversiones y verificación](#parte-3-dto).

<a id="parte-1-mvvm"></a>

## Parte 1. MVVM: presentación, estado y eventos

### 1.1. Qué es Anotado y qué está implementado

Anotado permite que un usuario se registre, inicie sesión y gestione tareas personales mediante una API REST. Su diseño utiliza una identidad verde y blanca, controles de Material 3 y pantallas construidas con Jetpack Compose.

| Función | Situación del código revisado |
|---|---|
| Registro | Formulario, validaciones, petición POST y persistencia de sesión implementados. |
| Login | Formulario, mensajes de error, petición POST y sesión implementados. |
| Home | Listado, filtros, progreso, estados de carga/error/vacío y reintento implementados. |
| Crear tarea | Formulario, validaciones, POST y actualización posterior de Home implementados. |
| Completar o reabrir tarea | Implementado con lectura de la tarea actual y PUT completo. |
| Consultar atributos | Home muestra un diálogo con los datos de la tarea ya cargada. |
| Cerrar sesión | Implementado mediante ViewModel, repositorio y almacenamiento de sesión. |
| Editar título y demás campos de una tarea existente | Pendiente de interfaz y flujo completos. |
| Eliminar tarea con confirmación | Pendiente en el cliente Android. |
| Pantalla independiente de detalle | `TaskDetailScreen.kt` existe, pero está vacío. |
| Integración completa con AWS | URL configurada; no verificada de extremo a extremo desde este entorno. |

Por tanto, **el CRUD completo es un objetivo del proyecto, no una condición ya alcanzada**. Las cuatro pantallas principales solicitadas —login, registro, Home y agregar tarea— sí tienen implementación y navegación.

### 1.2. Qué significa MVVM

**MVVM** significa *Model–View–ViewModel*. Es una forma de organizar la aplicación para separar la interfaz de las decisiones que gestionan sus datos y operaciones.

| Elemento | Responsabilidad | Ejemplo en Anotado |
|---|---|---|
| Model | Representa los datos y el acceso a operaciones del sistema; no se limita a una única clase. | `Task`, `TaskDraft` y los contratos de repositorio. |
| View | Representa el estado y comunica acciones del usuario. | `LoginScreen`, `RegisterScreen`, `HomeScreen`, `TaskFormScreen`. |
| ViewModel | Mantiene estado de presentación, valida entradas y coordina operaciones mediante contratos de dominio. | `LoginViewModel`, `RegisterViewModel`, `HomeViewModel`, `TaskViewModel`. |

En esta implementación, Repository, DTO y Mapper complementan MVVM. **MVVM no significa que un archivo de pantalla deba carecer de cualquier `if`**: decidir cómo representar un estado pertenece a la interfaz. Validar una fecha, filtrar las tareas o enviar HTTP pertenece a otras responsabilidades.

Por ejemplo, Home puede elegir el color de un filtro seleccionado. Sin embargo, la selección de qué tareas corresponden a ese filtro se calcula en `HomeViewModel`, no en `HomeScreen`.

### 1.3. Organización general de carpetas

Las rutas Kotlin de esta documentación son relativas a:

```text
app/src/main/java/com/yharim/anotado/
```

```text
Anotado/
├── app/src/main/
│   ├── java/com/yharim/anotado/
│   │   ├── MainActivity.kt
│   │   ├── TaskManagerApplication.kt
│   │   ├── ui/        # Pantallas, estado, ViewModels, navegación y tema
│   │   ├── domain/    # Modelos y contratos de repositorios
│   │   ├── data/      # Implementaciones, servicios, DTO y mapper
│   │   ├── di/        # Ensamblado manual de dependencias
│   │   ├── network/   # Retrofit, OkHttp e interceptor
│   │   └── session/   # Contrato y persistencia de la sesión
│   ├── AndroidManifest.xml
│   ├── res/
│   └── keepRules/
├── app/src/debug/     # Configuración específica de debug
├── app/src/test/      # Pruebas JVM
├── app/src/androidTest/ # Pruebas en dispositivo
├── gradle/           # Catálogo, wrapper y toolchain
└── docs/             # Documentación y referencias visuales
```

**Discrepancia de nombres existente:** el directorio, `namespace` y `applicationId` usan `com.yharim.anotado`. Login, navegación y sesión conservan declaraciones `package com.example.taskmanager...`. Kotlin resuelve clases por sus paquetes e imports; la ubicación física por sí sola no determina el paquete. Esta mezcla dificulta el mantenimiento, pero no prueba una violación de MVVM. La unificación está pendiente de una decisión del propietario y no se realizó durante la documentación.

### 1.4. Carpetas y archivos de presentación

| Carpeta | Archivo | Qué contiene y para qué sirve |
|---|---|---|
| `ui/auth/login/` | `LoginScreen.kt` | Vista del login, contenido visual, foco/teclado y previews. Observa el ViewModel y envía eventos. |
| `ui/auth/login/` | `LoginViewModel.kt` | `LoginUiState`, validación del correo y contraseña no vacía, envío, errores y visibilidad de contraseña. |
| `ui/auth/register/` | `RegisterScreen.kt` | Campos de registro, indicadores de carga y errores suministrados por el estado. |
| `ui/auth/register/` | `RegisterViewModel.kt` | `RegisterUiState`, validación del nombre, correo, contraseña y confirmación; coordina el registro. |
| `ui/home/` | `HomeScreen.kt` | Cabecera, lista con `LazyColumn`, filtros, progreso y diálogo de atributos. Recibe el evento para abrir creación. |
| `ui/home/` | `HomeViewModel.kt` | `HomeUiState`, `TaskItemUiState`, `TaskFilter`; carga, filtrado, conteos, formato de fechas, completar tareas, refresco y logout. |
| `ui/task/` | `TaskFormScreen.kt` | Formulario para agregar tareas. Muestra título, descripción, prioridad, categoría, fecha y completada. |
| `ui/task/` | `TaskViewModel.kt` | `TaskFormUiState`, validaciones, preparación de `TaskDraft`, envío y resultado del guardado. |
| `ui/task/` | `TaskDetailScreen.kt` | Composable vacío reservado para una pantalla independiente. El diálogo actual pertenece a Home. |
| `ui/navigation/` | `AppViewModel.kt` | `AppUiState` y observación global de la sesión mediante `AuthRepository`. |
| `ui/navigation/` | `AppNavigation.kt` | `NavHost`, destinos y callbacks entre pantallas. Observa `AppViewModel`; no accede al repositorio. |
| `ui/navigation/` | `Routes.kt` | Constantes `login`, `register`, `home` y `tasks/new`. |
| `ui/components/` | `TaskCard.kt` | Tarjeta de tarea reutilizable; representa prioridad y estado, y comunica pulsaciones. |
| `ui/components/` | `LoadingIndicator.kt` | Archivo todavía vacío. Las pantallas usan directamente los indicadores Material 3. |
| `ui/theme/` | `Theme.kt` | `AnotadoTheme`, esquema claro verde/blanco y aplicación de tipografía. |
| `ui/theme/` | `Type.kt` | Configuración de `Typography`, incluyendo `bodyLarge`. |
| `ui/theme/` | `Color.kt` | Conserva colores morados de la plantilla; el esquema activo se define en `Theme.kt`. |

Los archivos `Screen` no deberían importar `data`, Retrofit, OkHttp o SessionManager. Los ViewModels importan **interfaces** de `domain/repository`; la Factory les proporciona las implementaciones sin que las construyan.

### 1.5. Estado inmutable, StateFlow y observación del ciclo de vida

Un `UiState` agrupa los valores necesarios para dibujar una pantalla. Por ejemplo, `TaskFormUiState` contiene el título, la prioridad, los errores, `isSaving` y `saveSuccess`.

Fragmento real de [TaskViewModel.kt](../app/src/main/java/com/yharim/anotado/ui/task/TaskViewModel.kt):

```kotlin
private val _uiState = MutableStateFlow(TaskFormUiState())
val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()
```

- `MutableStateFlow` permite cambiar el estado y es privado.
- `StateFlow` ofrece a los consumidores una vista de lectura del estado observable.
- Los `UiState` usan propiedades `val`. Se actualizan mediante `copy`, creando nuevos valores en vez de modificar sus campos.
- Las colecciones expuestas usan interfaces de lectura; los ViewModels generan nuevas listas o conjuntos al actualizar.

La vista observa ese flujo:

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

Esta observación está vinculada al ciclo de vida de Android. Cuando llega un estado nuevo, Compose puede recomponer los elementos que dependen de él. La pantalla no tiene que consultar periódicamente el servidor para conocer el estado.

**Límite de persistencia:** un ViewModel conserva su estado ante recreaciones de configuración mientras se mantiene su propietario. Esto no equivale a persistencia del formulario después de que Android termine el proceso. Los formularios actuales no implementan restauración completa mediante `SavedStateHandle` o almacenamiento local. El JWT sí se persiste por separado.

### 1.6. Eventos y flujo unidireccional

El usuario modifica un campo. La vista comunica el valor al ViewModel. El ViewModel actualiza su estado. La vista representa el nuevo valor.

```text
Usuario escribe → Screen.onValueChange → ViewModel.onTitleChange
                → nuevo TaskFormUiState → Screen vuelve a representar el campo
```

Fragmento real:

```kotlin
fun onTitleChange(value: String) = edit {
    copy(title = value, titleCounter = "${value.length}/120", titleError = null)
}
```

El contador y la limpieza del error se gestionan junto con el estado del formulario. La vista recibe `state.titleCounter` y lo muestra.

Los callbacks de navegación, como `onBack` y `onNavigateToCreate`, son eventos de presentación. No son solicitudes HTTP ni reglas de dominio.

### 1.7. Validaciones de cada ViewModel

| ViewModel | Reglas y decisiones implementadas |
|---|---|
| `LoginViewModel` | Correo no vacío con comprobación básica de formato; contraseña no vacía; trim del correo; evita envíos repetidos; traduce excepciones de dominio a estado visible. |
| `RegisterViewModel` | Nombre no vacío; correo con formato básico; contraseña no blanca y de al menos 8 caracteres; confirmación obligatoria e idéntica. Solo envía nombre, correo y contraseña. |
| `HomeViewModel` | Filtra en memoria por estado, calcula pendientes y progreso global, transforma tareas a filas de UI, coordina completar/reabrir y mantiene datos anteriores ante fallo de refresco. |
| `TaskViewModel` | Título normalizado obligatorio, máximo 120; prioridad 1–5; fecha opcional con formato y calendario válidos; transforma opcionales vacíos a `null` y categoría vacía a `General`. |
| `AppViewModel` | Presenta el estado de autenticación inicial y sus cambios; no expone el token. |

La validación del cliente mejora la experiencia. El servidor debe validar también: la interfaz de Android no constituye la barrera de seguridad del backend.

### 1.8. Corrutinas y operaciones asíncronas

Una corrutina permite estructurar trabajo que puede suspenderse. Los ViewModels lanzan operaciones mediante `viewModelScope.launch` y llaman a métodos `suspend` de los repositorios.

**`suspend` no significa por sí solo «ejecutar todo en otro hilo».** En este proyecto, Retrofit proporciona el soporte suspend para la espera de la respuesta HTTP. Las vistas no hacen llamadas bloqueantes de red.

Esquema abreviado del guardado:

```kotlin
viewModelScope.launch {
    try {
        taskRepository.createTask(draft)
        _uiState.update { it.copy(saveSuccess = true) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: TaskException) {
        // El ViewModel coloca errores generales o de campo en UiState.
    } finally {
        _uiState.update { it.copy(isSaving = false) }
    }
}
```

`CancellationException` se vuelve a lanzar porque la cancelación no debe convertirse en un error de conexión ni en un éxito ficticio. Los bloques `finally` restauran indicadores de carga. El cierre local de sesión es una operación síncrona pequeña y no necesita una corrutina solo por pertenecer a un ViewModel.

### 1.9. Home: filtros, progreso y cambios de estado

`HomeViewModel` mantiene las tareas de dominio en una lista privada. `renderTasks()` construye las filas visibles y el resumen:

- `completed` cuenta las tareas completadas.
- Pendientes es el total menos las completadas.
- El progreso es completadas dividido entre total; para cero tareas se usa cero.
- El filtro cambia las filas visibles, pero el progreso sigue referido a la lista completa.
- `TaskItemUiState` contiene los textos preparados para la tarjeta, incluida fecha y prioridad.

El título «Progreso de tareas» es deliberado. La API no documenta fecha de finalización y, por tanto, no permite calcular honestamente progreso diario.

Al completar una tarea se bloquean nuevas pulsaciones sobre ese id. La tarjeta cambia **después** de recibir éxito del servidor. Ante fallo, conserva su estado anterior y se presenta un error. `onTaskCreated()` espera operaciones anteriores, selecciona Todas y vuelve a cargar para mostrar la nueva tarea.

### 1.10. Navegación y sesión

[AppNavigation.kt](../app/src/main/java/com/yharim/anotado/ui/navigation/AppNavigation.kt) observa `AppViewModel.uiState` y configura el destino inicial:

```kotlin
val state by appViewModel.uiState.collectAsStateWithLifecycle()
val authenticated = state.isAuthenticated

key(authenticated) {
    val navController = rememberNavController()
    // NavHost y destinos definidos dentro de este bloque.
}
```

La clave dependiente de la sesión recrea el grupo de navegación cuando cambia la autenticación. El objetivo es que el cierre de sesión descarte el historial protegido. El comportamiento del botón Atrás debe comprobarse en dispositivo; las pruebas JVM de estado no prueban el back stack de Android.

`AppViewModel` se obtiene con una Factory y se conserva en el ámbito de `MainActivity`. Los ViewModels de las pantallas se crean en sus entradas de navegación. Para refrescar Home tras crear una tarea se recupera su ViewModel usando la entrada existente de Home, evitando crear una segunda instancia de esa pantalla.

El formulario observa `saveSuccess` mediante `LaunchedEffect` y emite `onSaved`. La navegación comunica `onTaskCreated()` a Home y vuelve atrás. La vista del formulario no consulta repositorios.

### 1.11. Presentación permitida y decisiones separadas

Permanecen en las vistas los colores, tamaños, scroll, foco, teclado, selección visual de controles y condiciones que eligen qué dibujar. Algunas combinan estados, por ejemplo:

```kotlin
state.hasLoaded && state.tasks.isEmpty() && !state.isLoading
```

Esta condición decide mostrar el mensaje de lista vacía. Se acordó conservarla de momento. Una variante más estricta podría exponer `showEmptyState`, pero la condición actual no filtra datos, no valida entradas y no llama al backend.

No es exacto describir las vistas como «cero lógica». La afirmación comprobable es: **no contienen validaciones de negocio, persistencia, llamadas HTTP ni accesos directos a repositorios**.

### 1.12. Tecnologías de interfaz y compilación

Las siguientes son **versiones declaradas en el repositorio**, no una afirmación de que sean las últimas disponibles ni de que toda la combinación haya sido verificada por Gradle en este entorno.

| Tecnología | Versión/configuración declarada | Uso |
|---|---|---|
| Kotlin | Plugins Compose/Serialization 2.2.10 | Lenguaje de la app, clases, funciones, corrutinas y estado. |
| Android Gradle Plugin | 9.4.1 | Construcción del módulo Android. |
| Gradle Wrapper | 9.6.0 | Versión de Gradle fijada para el proyecto. |
| Jetpack Compose | BOM 2026.02.01 | UI declarativa. La BOM coordina versiones de artefactos Compose. |
| Material 3 | Gestionado por la BOM | Botones, campos, tarjetas, diálogos y tema. |
| Activity Compose | 1.13.0 | Integración de Compose con la Activity y gestión de Atrás. |
| Lifecycle | 2.11.0 | ViewModel y observación de estado vinculada al ciclo de vida. |
| Navigation Compose | 2.10.2 | Destinos y back stack. |
| Core KTX | 1.19.1 | Utilidades Android y compatibilidad. |
| Kotlin Coroutines | 1.9.0 | Corrutinas, StateFlow y herramientas de prueba. |
| SDK | min 26, compile 37, target 37 | Requisitos y nivel objetivo declarados. |
| Compatibilidad Java del módulo | 11 | Configuración source/target de compilación Java. |
| JVM del daemon | Toolchain 25 | Configuración separada del proceso Gradle; no confundir con minSdk ni Java sourceCompatibility. |

Las dependencias se centralizan en `gradle/libs.versions.toml`. Los scripts Gradle están escritos en Kotlin DSL (`.gradle.kts`).

<a id="parte-2-repository"></a>

## Parte 2. Repository: acceso a datos, dependencias y conexión con AWS

### 2.1. Qué es el patrón Repository

Un repositorio presenta operaciones de datos mediante un contrato y oculta los detalles de su ejecución. El ViewModel puede pedir «crear una tarea» sin conocer URLs, JSON, cabeceras HTTP o clases Retrofit.

En Anotado se separan dos elementos:

- **Interfaz en `domain/repository`:** expresa qué operaciones necesita la aplicación utilizando tipos de dominio.
- **Implementación en `data/repository`:** realiza las llamadas al servicio, convierte DTOs, coordina la sesión cuando corresponde y traduce errores técnicos.

Esto permite probar un ViewModel con un repositorio falso que implementa el mismo contrato. El repositorio falso pertenece a las pruebas; no sustituye al backend en las pantallas reales.

Un Repository no exige una base de datos local. En este proyecto, las tareas se obtienen de una fuente remota. No se ha implementado Room, caché persistente de tareas ni sincronización offline.

### 2.2. Archivos del patrón y de la infraestructura que lo sostiene

| Carpeta | Archivo | Responsabilidad |
|---|---|---|
| `domain/repository/` | `AuthRepository.kt` | Contrato de login, registro, nombre de usuario, estado autenticado y logout. También declara `AuthException`. |
| `domain/repository/` | `TaskRepository.kt` | Contrato de creación, listado y cambio de completada. También declara `TaskException`, con errores de campo opcionales. |
| `data/repository/` | `AuthRepositoryImpl.kt` | Implementa autenticación con AuthApiService, procesa respuestas/errores y guarda nombre/token mediante SessionManager. |
| `data/repository/` | `TaskRepositoryImpl.kt` | Implementa operaciones de tareas con TaskApiService y TaskMapper. No dibuja pantallas ni decide rutas. |
| `data/remote/` | `AuthApiService.kt` | Declara POST `/login` y POST `/register`, ambos suspend. |
| `data/remote/` | `TaskApiService.kt` | Declara GET/POST `/tasks` y GET/PUT `/tasks/{id}`. |
| `network/` | `RetrofitClient.kt` | Construye Retrofit con OkHttp, convertidor JSON, timeouts y política de redirecciones. |
| `network/` | `AuthInterceptor.kt` | Añade Bearer a solicitudes de tareas y reacciona ante respuestas 401 protegidas. |
| `session/` | `SessionManager.kt` | Contiene la interfaz SessionManager y la clase SharedPrefsSessionManager. |
| `di/` | `AppContainer.kt` | Crea las dependencias compartidas y conecta implementaciones con interfaces. |
| `di/` | `ViewModelFactory.kt` | Construye cada ViewModel con sus repositorios. |
| Raíz Kotlin | `TaskManagerApplication.kt` | Mantiene el AppContainer compartido durante la vida del proceso de la aplicación. |
| Raíz Kotlin | `MainActivity.kt` | Punto de entrada visual: configura barras del sistema, Factory, AppViewModel, tema y navegación. |

**El servicio Retrofit y el repositorio no son lo mismo.** El servicio declara el protocolo HTTP; el repositorio ofrece una operación útil para la aplicación y transforma el resultado.

### 2.3. Contrato de autenticación

Código de [AuthRepository.kt](../app/src/main/java/com/yharim/anotado/domain/repository/AuthRepository.kt), omitiendo imports y excepciones:

```kotlin
interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    fun getUserName(): String?
    fun logout()
}
```

`login` y `register` no devuelven `AuthResponseDto` al ViewModel. La implementación consume esa respuesta dentro de `data`, persiste la sesión y deja que el estado autenticado notifique el cambio.

`getUserName()` devuelve un texto de dominio, no un DTO ni una referencia al almacenamiento. `logout()` limpia la sesión local; la API entregada no documenta `/logout`.

### 2.4. Cómo autentica AuthRepositoryImpl

Fragmentos reales de [AuthRepositoryImpl.kt](../app/src/main/java/com/yharim/anotado/data/repository/AuthRepositoryImpl.kt):

```kotlin
override suspend fun login(email: String, password: String) = authenticate {
    it.login(LoginRequestDto(email.trim(), password))
}

override suspend fun register(name: String, email: String, password: String) = authenticate {
    it.register(RegisterRequestDto(name.trim(), email.trim(), password))
}
```

Ambas operaciones reutilizan `authenticate`, un método privado que:

1. Comprueba que exista un servicio configurado.
2. Ejecuta la solicitud que recibió.
3. Rechaza un token vacío o que contenga espacios en blanco.
4. Guarda el nombre y luego el token usando SessionManager.
5. Convierte fallos HTTP, de JSON o de conexión en `AuthException`.

La comprobación local del texto del token **no valida su firma criptográfica ni su vigencia**. Esa verificación corresponde al backend cuando recibe solicitudes protegidas.

Las contraseñas se usan para la solicitud. No se guardan en SharedPreferences. Los ViewModels limpian sus campos sensibles al completar con éxito login o registro.

### 2.5. Contrato de tareas y separación del formulario

[TaskRepository.kt](../app/src/main/java/com/yharim/anotado/domain/repository/TaskRepository.kt):

```kotlin
interface TaskRepository {
    suspend fun createTask(draft: TaskDraft): Task
    suspend fun getTasks(): List<Task>
    suspend fun setCompleted(id: Long, completed: Boolean): Task
}
```

`TaskDraft` representa una tarea nueva sin id de servidor. `Task` representa una tarea que ya tiene identificador. Ningún método del contrato necesita `TaskDto` o `TaskRequestDto`.

Fragmento real de creación:

```kotlin
override suspend fun createTask(draft: TaskDraft): Task = request { service ->
    TaskMapper.toDomain(service.createTask(TaskMapper.toRequest(draft)))
}
```

Se lee de dentro hacia fuera:

1. `toRequest(draft)` transforma el modelo de dominio en DTO de escritura.
2. `service.createTask(...)` envía POST y recibe el DTO de respuesta.
3. `toDomain(...)` convierte la respuesta en `Task`.
4. El ViewModel recibe un resultado de dominio.

`request` es un método privado de `TaskRepositoryImpl` que concentra la traducción de errores repetida por sus operaciones. No es un cliente HTTP global ni una clase que controle toda la app.

### 2.6. Por qué completar una tarea usa un PUT completo

El contrato del backend establece que PUT reemplaza los campos de la tarea. Omitir campos opcionales puede restaurar valores predeterminados. Por eso no se envía solamente `completed`.

Código real:

```kotlin
override suspend fun setCompleted(id: Long, completed: Boolean): Task = request { service ->
    val current = TaskMapper.toDomain(service.getTask(id))
    TaskMapper.toDomain(
        service.updateTask(id, TaskMapper.toRequest(current.copy(completed = completed)))
    )
}
```

Primero se consulta la tarea actual; después se cambia `completed` y se envían todos los campos editables. Esto reduce el riesgo de sobrescribir datos usando una tarjeta antigua. **No es un mecanismo de concurrencia atómica:** si otro cliente edita entre GET y PUT, el contrato actual no aporta ETag o versión para detectar ese conflicto.

### 2.7. Dónde está exactamente la conexión

No hay una única función que reúna toda la conexión. Cada archivo cumple una parte:

| Pregunta | Archivo y elemento que la responde |
|---|---|
| ¿Cuál es el servidor? | `gradle.properties`, propiedad `ANOTADO_API_BASE_URL`. |
| ¿Cómo llega la URL al código Kotlin? | `app/build.gradle.kts`, campo generado `BuildConfig.API_BASE_URL`. |
| ¿Dónde se construye el cliente HTTP? | `network/RetrofitClient.kt`, método `create`. |
| ¿Dónde se añade el token? | `network/AuthInterceptor.kt`, método `intercept`. |
| ¿Dónde se definen las rutas? | `data/remote/AuthApiService.kt` y `TaskApiService.kt`. |
| ¿Quién llama a esos servicios? | Implementaciones de `data/repository`. |
| ¿Quién crea y conecta esas instancias? | `di/AppContainer.kt`. |
| ¿Quién solicita operaciones desde presentación? | Los ViewModels mediante interfaces de `domain/repository`. |

La URL proporcionada por el propietario es:

```properties
ANOTADO_API_BASE_URL=https://54.152.139.22.nip.io/
```

La dirección de diagnóstico es `https://54.152.139.22.nip.io/health`. **No se incluye `/health` en la URL base**, porque Retrofit debe construir rutas como `/login` y `/tasks`, no `/health/login`.

Fragmentos de [app/build.gradle.kts](../app/build.gradle.kts):

```kotlin
val apiBaseUrl = providers.gradleProperty("ANOTADO_API_BASE_URL").orElse("")
val configuredApiUrl = apiBaseUrl.get().trim()
```

Después de validar la URL y escapar el literal para el código generado:

```kotlin
buildConfigField("String", "API_BASE_URL", apiUrlLiteral)
```

La validación exige esquema HTTP o HTTPS, host, ausencia de credenciales/query/fragment y barra final. La configuración admite un valor vacío para abrir la interfaz sin backend: el repositorio informa indisponibilidad al intentar operar.

**La URL se incorpora durante la compilación.** Cambiar `gradle.properties` no modifica una APK instalada. Es necesario sincronizar, compilar y reinstalar o ejecutar la nueva versión. La URL pública no es una contraseña; ningún secreto AWS se requiere en el cliente.

### 2.8. Retrofit, OkHttp y JSON

| Tecnología | Declarada | Función concreta |
|---|---|---|
| Retrofit | 3.0.0 | Implementa interfaces HTTP anotadas y adapta respuestas a tipos Kotlin. |
| Convertidor Kotlinx Serialization para Retrofit | 3.0.0 | Integra la codificación/decodificación JSON con Retrofit. |
| OkHttp | 4.12.0 | Transporte HTTP, TLS, interceptor y timeouts. |
| Kotlinx Serialization JSON | 1.9.0 | Serialización de DTOs `@Serializable`. |

Configuración relevante de [RetrofitClient.kt](../app/src/main/java/com/yharim/anotado/network/RetrofitClient.kt):

```kotlin
val json = Json { ignoreUnknownKeys = true }
```

Permite ignorar claves adicionales del JSON que no estén representadas en el DTO. No convierte automáticamente una respuesta mal formada en una respuesta válida.

```kotlin
OkHttpClient.Builder()
    .addInterceptor(interceptor)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS)
    .followRedirects(false)
    .followSslRedirects(false)
    .build()
```

Los límites se refieren a conexión, lectura y duración de la llamada. Las redirecciones automáticas están desactivadas. No se instaló un verificador TLS que acepte cualquier certificado.

El convertidor se registra para `application/json`:

```kotlin
.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
```

### 2.9. Interfaces suspend: no se utiliza Call<T>

Ejemplo real de [TaskApiService.kt](../app/src/main/java/com/yharim/anotado/data/remote/TaskApiService.kt):

```kotlin
@POST("tasks")
suspend fun createTask(@Body request: TaskRequestDto): TaskDto

@GET("tasks/{id}")
suspend fun getTask(@Path("id") id: Long): TaskDto
```

- `@POST` y `@GET` indican el método HTTP y la ruta relativa.
- `@Body` identifica el cuerpo que serializa el convertidor.
- `@Path` sustituye el parámetro de la ruta.
- `suspend` permite esperar el resultado dentro de una corrutina.
- El retorno es el DTO directamente; no `Call<TaskDto>`.

No se usa Retrofit `Call<T>`, `Call.enqueue()` ni `Call.execute()` en la app. En los tests aparece `server.enqueue(...)`: pertenece a **MockWebServer** y prepara una respuesta simulada. No es el mecanismo de callbacks de Retrofit.

### 2.10. Rutas documentadas frente a rutas utilizadas

| Método y ruta | Autenticación | Respuesta documentada | Uso en el Android actual |
|---|---|---|---|
| POST `/register` | Pública | 201 con token y usuario | Implementado. |
| POST `/login` | Pública | 200 con token y usuario | Implementado. |
| GET `/tasks` | Bearer | 200 con lista | Implementado en Home. |
| POST `/tasks` | Bearer | 201 con tarea | Implementado en creación. |
| GET `/tasks/{id}` | Bearer | 200 con tarea | Usado antes de cambiar completada. No existe aún destino independiente de detalle. |
| PUT `/tasks/{id}` | Bearer | 200 con tarea | Usado para completar/reabrir preservando campos. Edición general pendiente. |
| DELETE `/tasks/{id}` | Bearer | 204 sin cuerpo | Documentado por backend, todavía no declarado en TaskApiService. |
| GET `/health` | Pública | 200 con estado | Diagnóstico externo; no declarado en las interfaces Retrofit de la app. |

Cuando se implemente DELETE, el contrato indica cuerpo vacío; el retorno suspend apropiado puede ser `Unit`. Esto describe el trabajo pendiente, no una función existente.

### 2.11. Sesión persistente e interceptor Bearer

**JWT** significa *JSON Web Token*. En este proyecto la API entrega un token al autenticar y el cliente lo reenvía en peticiones protegidas. El servidor determina el usuario y la autorización; Android no envía `userId` para elegir al propietario.

`SessionManager.kt` define la abstracción y su implementación SharedPreferences. Guarda:

| Archivo de preferencias | Clave | Contenido |
|---|---|---|
| `anotado_session` | `jwt_token` | Token recibido del backend. |
| `anotado_session` | `user_name` | Nombre para el saludo. |

Se usa el contexto de la aplicación y `Context.MODE_PRIVATE`. Esta privacidad de archivo **no significa cifrado explícito**: el proyecto no implementa EncryptedSharedPreferences. Las reglas de backup excluyen `anotado_session.xml`. No se almacena la contraseña.

Al guardar el token, el flujo `isAuthenticated` cambia a `true`. Al limpiar la sesión cambia a `false`. El estado inicial deriva de la presencia de un token no vacío, no de una comprobación previa de su vigencia contra AWS.

Fragmento real de [AuthInterceptor.kt](../app/src/main/java/com/yharim/anotado/network/AuthInterceptor.kt):

```kotlin
val protected = "tasks" in original.url.pathSegments
val token = if (protected) session.getToken() else null
val request = if (!token.isNullOrBlank()) {
    original.newBuilder().header("Authorization", "Bearer $token").build()
} else original
```

La regla actual protege rutas que contienen el segmento `tasks`, incluyendo `/tasks/{id}`. Los endpoints públicos login/register no reciben automáticamente Bearer. Si se añaden otras familias de rutas protegidas, habrá que ampliar esta regla.

Cuando una respuesta protegida tiene código 401, el interceptor llama a `clearIfTokenMatches(token)`. La comparación impide que una respuesta tardía de una sesión anterior borre una sesión nueva con otro token.

El flujo de invalidación queda así:

```text
401 de tareas → AuthInterceptor → SessionManager.clearIfTokenMatches
             → AuthRepository.isAuthenticated = false
             → AppViewModel actualiza AppUiState
             → AppNavigation representa el flujo de login
```

Un 401 de login se interpreta como credenciales incorrectas. No se trata como una invalidación automática desde el interceptor.

### 2.12. Inyección de dependencias manual

**Inyección de dependencias** significa proporcionar a una clase los objetos que necesita, en lugar de que ella los construya internamente. Ejemplo real:

```kotlin
class TaskViewModel(private val taskRepository: TaskRepository) : ViewModel()
```

El constructor depende de una interfaz. La implementación se decide en otro lugar.

`AppContainer` construye y comparte SessionManager, Retrofit y los repositorios. Los servicios se obtienen con `retrofit.create(...)`. No hace login, no carga tareas por iniciativa propia y no navega.

`ViewModelFactory` reconoce cada clase solicitada y entrega:

| ViewModel | Dependencia inyectada |
|---|---|
| LoginViewModel | AuthRepository |
| RegisterViewModel | AuthRepository |
| AppViewModel | AuthRepository |
| HomeViewModel | AuthRepository y TaskRepository |
| TaskViewModel | TaskRepository |

No se usa Hilt, Dagger ni Koin. `ViewModelProvider.Factory` permite combinar los constructores personalizados con la gestión de ciclo de vida de Android.

[TaskManagerApplication.kt](../app/src/main/java/com/yharim/anotado/TaskManagerApplication.kt) mantiene el contenedor:

```kotlin
class TaskManagerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
```

`by lazy` lo crea la primera vez que se necesita y reutiliza esa instancia dentro del proceso. No significa una instancia universal entre diferentes procesos Android.

Su tamaño pequeño es intencional. Es una clase de infraestructura con una responsabilidad concreta, no un modelo de dominio defectuoso por ser «anémico».

### 2.13. Tratamiento de errores

| Situación | Tratamiento actual |
|---|---|
| Falta de URL/servicio | Excepción de disponibilidad; no se simula éxito. |
| 401 en autenticación | `AuthException.Credentials`. |
| 400/409 en autenticación | `AuthException.Validation`, con errores por campo si el backend los devuelve. El cliente también contempla 422 aunque no está en el contrato principal. |
| 409 sin mapa de errores | Mensaje de correo ya registrado asociado a email. |
| 400 en tareas | `TaskException` con mensaje y `fieldErrors`. |
| 401 en tareas | Invalidación por interceptor y mensaje de sesión vencida. |
| 404 en tareas | Mensaje que indica que la tarea no está disponible. |
| Fallo de conexión | IOException traducida a un error de dominio legible. |
| JSON incompatible | SerializationException traducida a error de respuesta. |
| Cancelación | Se propaga CancellationException. |

Los ViewModels convierten esas excepciones en campos de UiState. No muestran directamente un stack trace al usuario. El servidor continúa siendo la fuente de validación definitiva.

### 2.14. Configuración Android y archivos de construcción

| Ruta desde la raíz | Función |
|---|---|
| `settings.gradle.kts` | Declara `:app`, nombre del proyecto, repositorios Google/Maven Central y resolución de plugins/toolchains. |
| `build.gradle.kts` | Declara plugins raíz con `apply false`. |
| `app/build.gradle.kts` | Configura SDK, identificadores, BuildConfig, Compose, dependencias y variante release. |
| `gradle/libs.versions.toml` | Version Catalog con versiones, alias de librerías y plugins. |
| `gradle.properties` | Propiedades de Gradle y URL pública de la API. |
| `local.properties` | Ubicación local del SDK; no es configuración del backend ni un archivo portable entre equipos. |
| `gradlew`, `gradlew.bat` | Lanzadores del wrapper para sistemas Unix y Windows. |
| `gradle/wrapper/gradle-wrapper.properties` | URL, versión y checksum de la distribución Gradle. |
| `gradle/wrapper/gradle-wrapper.jar` | Componente ejecutable del wrapper. |
| `gradle/gradle-daemon-jvm.properties` | Criterios del toolchain del daemon. |
| `app/src/main/AndroidManifest.xml` | Permiso INTERNET, Application, Activity de lanzamiento y políticas de backup/tráfico. |
| `app/src/debug/AndroidManifest.xml` | Permite tráfico HTTP sin cifrar solo para debug, útil con servidor local. |
| `app/src/main/keepRules/rules.keep` | Archivo para reglas R8; actualmente vacío, sin reglas personalizadas. |
| `.gitignore`, `app/.gitignore` | Exclusiones de archivos generados/locales para Git. |
| `.idea/` | Metadatos del IDE, no lógica de la aplicación. |
| `.gradle/`, `app/build/` | Caché y resultados generados; no son capas de la arquitectura. |

El manifiesto principal declara `android:usesCleartextTraffic="false"`; debug lo sobrescribe a `true`. La URL AWS actual usa HTTPS. No se necesita permitir HTTP para acceder a ese endpoint HTTPS.

### 2.15. Backend documentado y frontera con Android

Según [API-README.md](API-README.md), el backend existente usa Node.js 22 o superior, Express, SQLite mediante better-sqlite3 y JWT. El propietario informó que fue desplegado en AWS EC2 y proporcionó la URL pública.

- **Node.js:** entorno de ejecución del servidor.
- **Express:** organiza rutas y manejo HTTP del backend.
- **SQLite:** persistencia del servidor.
- **EC2:** infraestructura donde se aloja el servicio.
- **HTTPS:** conexión cifrada entre Android y el endpoint público.

Estos son datos del contrato entregado y del reporte del propietario. No se inspeccionó el código backend ni la configuración interna de EC2 desde este repositorio. No se debe afirmar una configuración concreta de proxy, PM2, certificado o Security Group como comprobada.

Android no abre directamente SQLite del servidor, no contiene credenciales AWS y no necesita JWT_SECRET. Se comunica exclusivamente mediante las rutas de la API.

### 2.16. Ejecutar y diagnosticar la conexión

Desde la raíz del proyecto:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Para usar explícitamente otra URL durante una compilación:

```bash
./gradlew :app:assembleDebug -PANOTADO_API_BASE_URL=https://54.152.139.22.nip.io/
```

En Android Studio: abrir Anotado, sincronizar Gradle, seleccionar el dispositivo y ejecutar el módulo `app`. Para probar los flujos reales es necesario crear una cuenta válida en el servidor mediante Registro o usar una cuenta existente.

Diagnóstico básico de disponibilidad:

```bash
curl --connect-timeout 10 --max-time 20 https://54.152.139.22.nip.io/health
```

El contrato espera `{"status":"ok"}`, pero el último intento desde este entorno falló antes de obtener HTTP por resolución del dominio. No se presenta ese JSON como una respuesta efectivamente observada aquí.

En un emulador, `10.0.2.2` sirve para acceder al host local cuando hay un backend local escuchando. En el celular físico no representa AWS. La configuración actual utiliza la dirección pública, adecuada para intentar acceso remoto desde ambos tipos de dispositivo.

<a id="parte-3-dto"></a>

## Parte 3. DTO y Mapper: transporte, conversiones y verificación

### 3.1. Qué es un DTO

**DTO** significa *Data Transfer Object*: objeto para transportar datos entre componentes o sistemas. En Anotado, sus propiedades representan el formato JSON que intercambian Android y la API.

Un DTO no representa una pantalla y no debe decidir navegación, validar formularios o almacenar el token por su cuenta. Describe datos de una solicitud o respuesta. Las operaciones sobre esos datos pertenecen al repositorio, al mapper o a la presentación según su responsabilidad.

En este proyecto los DTO son `data class` de Kotlin anotadas con `@Serializable` y están dentro de `data/remote/dto`. El plugin de Kotlin Serialization y su biblioteca JSON permiten generar/utilizar los serializadores. La anotación por sí sola no sustituye la configuración de dependencias y del plugin.

### 3.2. Archivos de DTO y sus responsabilidades

| Archivo en `data/remote/dto/` | Clases | Uso |
|---|---|---|
| `LoginRequestDto.kt` | `LoginRequestDto` | Cuerpo de login: email y password. |
| `RegisterRequestDto.kt` | `RegisterRequestDto` | Cuerpo de registro: name, email y password. No lleva confirmación. |
| `AuthResponseDto.kt` | `AuthResponseDto`, `UserDto` | Respuesta de autenticación con token y usuario. UserDto contiene id, name y email. |
| `TaskDto.kt` | `TaskDto` | Respuesta de tarea con id y sus atributos. |
| `TaskRequestDto.kt` | `TaskRequestDto` | Cuerpo de escritura de tarea, sin id ni userId. |
| `ErrorResponseDto.kt` | `ErrorResponseDto` | Error HTTP con message y mapa errors, si existe. |

Carpetas relacionadas:

| Carpeta | Archivos | Papel respecto al DTO |
|---|---|---|
| `data/remote/` | AuthApiService.kt, TaskApiService.kt | Declaran DTOs de entrada y salida del transporte. |
| `data/repository/` | AuthRepositoryImpl.kt, TaskRepositoryImpl.kt | Consumen DTOs dentro de data; no los entregan a la vista. |
| `data/mapper/` | TaskMapper.kt | Conversión de tareas entre transporte y dominio. |
| `domain/model/` | Task.kt, TaskDraft.kt | Modelos sin anotaciones de serialización que usa la aplicación. |
| `ui/home/` | HomeViewModel.kt | Convierte Task de dominio en TaskItemUiState de presentación; no recibe TaskDto. |
| `ui/task/` | TaskViewModel.kt | Construye TaskDraft a partir de entradas validadas; no construye TaskRequestDto. |

### 3.3. DTO, modelo de dominio y UiState no son equivalentes

| Tipo | Pregunta que responde | Ejemplo de datos |
|---|---|---|
| DTO | ¿Qué estructura acepta o devuelve la API? | `TaskDto.dueDate`, `TaskRequestDto.completed`. |
| Modelo de dominio | ¿Qué información maneja la aplicación independientemente del formato HTTP? | `Task`, `TaskDraft`. |
| UiState | ¿Qué necesita la pantalla para representar su estado actual? | `titleError`, `isSaving`, `progressLabel`, `dateLabel`. |

Aunque `TaskDto` y `Task` tienen campos similares, se mantienen separados para que las anotaciones y cambios de transporte no se propaguen directamente a la interfaz. Un cambio de nombre o estructura JSON podría resolverse en data y el mapper.

`TaskItemUiState` contiene textos preparados para mostrar. Su fecha puede ser «9 nov 2026», mientras el dato transportado sigue siendo `2026-11-09`. Esa transformación de presentación ocurre en el ViewModel, no en el DTO.

### 3.4. Modelo de tareas y campos opcionales

| Campo | Tipo | Regla de esta implementación |
|---|---|---|
| id | Long | Identificador recibido del servidor; solo en Task/TaskDto. |
| title | String | Obligatorio; el formulario normaliza espacios exteriores y limita a 120 caracteres. |
| description | String? | Opcional. El formulario convierte un texto vacío a null. |
| priority | Int | De 1 a 5; predeterminado 3. |
| dueDate | String? | Opcional; el cliente valida formato yyyy-MM-dd y fecha real antes de crear. |
| completed | Boolean | Predeterminado false; puede marcarse durante creación. |
| category | String | Predeterminado General cuando el campo del formulario está vacío. |

El signo `?` indica que Kotlin permite `null`. `null` expresa ausencia del valor; no es la cadena literal `"null"`.

`TaskDraft` evita inventar un id antes de crear la tarea. La respuesta del POST aporta el id definitivo y el mapper construye un `Task` con ese valor.

### 3.5. JSON de autenticación: qué se envía y qué se queda local

Ejemplo ilustrativo de registro, sin credenciales reales:

```json
{
  "name": "Usuario de ejemplo",
  "email": "ejemplo@example.com",
  "password": "CLAVE_DE_EJEMPLO"
}
```

La pantalla también solicita confirmar contraseña, pero `RegisterRequestDto` solo declara:

```kotlin
@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)
```

La confirmación se compara en `RegisterViewModel` y no viaja al servidor. La validación local no requiere añadir un campo al contrato de red.

Respuesta ilustrativa:

```json
{
  "token": "TOKEN_DE_EJEMPLO_NO_FUNCIONAL",
  "user": {
    "id": 1,
    "name": "Usuario de ejemplo",
    "email": "ejemplo@example.com"
  }
}
```

`AuthResponseDto` contiene `UserDto`. `AuthRepositoryImpl` extrae nombre y token y los pasa al administrador de sesión. No existe un `AuthMapper` adicional ni un modelo `User` de dominio en el código actual: la interfaz solo expone estado autenticado y nombre, y no devuelve el DTO. No debe documentarse una clase de mapper de usuario que no existe.

### 3.6. TaskRequestDto: por qué no tiene valores predeterminados

Código de [TaskRequestDto.kt](../app/src/main/java/com/yharim/anotado/data/remote/dto/TaskRequestDto.kt):

```kotlin
@Serializable
data class TaskRequestDto(
    val title: String,
    val description: String?,
    val priority: Int,
    val dueDate: String?,
    val completed: Boolean,
    val category: String
)
```

Estos campos no declaran valores predeterminados. La intención es que una escritura completa incluya también valores como `false` y `null`, que son significativos para el contrato PUT. La configuración JSON actual no desactiva explícitamente la emisión de valores null.

Ejemplo de cuerpo de creación o actualización:

```json
{
  "title": "Repasar Kotlin",
  "description": null,
  "priority": 3,
  "dueDate": "2026-11-15",
  "completed": false,
  "category": "Estudio"
}
```

No contiene `id`: el id existente va en la URL de PUT y el servidor lo asigna en POST. Tampoco contiene `userId`: el backend identifica al propietario a partir del JWT.

`TaskDto`, en cambio, sí incorpora el id recibido y declara algunos defaults. Esos defaults ayudan con campos opcionales ausentes, pero no sustituyen validar respuestas reales. Si el servidor cambia el contrato, deben revisarse DTO y pruebas de red.

### 3.7. Qué hace TaskMapper

Un **Mapper** convierte representaciones distintas de una misma información. No tiene que consultar la API ni observar una pantalla: su trabajo es transformar valores.

[TaskMapper.kt](../app/src/main/java/com/yharim/anotado/data/mapper/TaskMapper.kt) contiene tres conversiones:

```kotlin
fun toDomain(dto: TaskDto) = Task(
    dto.id, dto.title, dto.description, dto.priority,
    dto.dueDate, dto.completed, dto.category
)

fun toRequest(draft: TaskDraft) = TaskRequestDto(
    draft.title, draft.description, draft.priority,
    draft.dueDate, draft.completed, draft.category
)

fun toRequest(task: Task) = TaskRequestDto(
    task.title, task.description, task.priority,
    task.dueDate, task.completed, task.category
)
```

El formato del fragmento se distribuyó en varias líneas para lectura; la conversión corresponde a las funciones existentes.

- **TaskDto → Task:** al leer del servidor.
- **TaskDraft → TaskRequestDto:** al crear.
- **Task → TaskRequestDto:** al actualizar una tarea existente.

La conversión se concentra en `data/mapper`, no se repite en pantallas o ViewModels. Así queda una ubicación identificable para adaptar cambios del transporte.

### 3.8. Ejemplo completo: guardar una tarea desde el formulario

1. El usuario escribe el título y el resto de campos en `TaskFormScreen`.
2. Cada cambio se comunica a un método de `TaskViewModel`, que publica un nuevo `TaskFormUiState`.
3. Al pulsar Guardar, la vista emite `onSaveClick()`; solo limpia el foco visual por su cuenta.
4. El ViewModel valida título, prioridad y fecha. Si hay errores, actualiza el estado y no llama al repositorio.
5. Construye un `TaskDraft` con opcionales normalizados y marca `isSaving`.
6. Llama a `TaskRepository.createTask(draft)` desde `viewModelScope`.
7. `TaskRepositoryImpl` convierte el borrador con TaskMapper y llama a TaskApiService.
8. Retrofit serializa TaskRequestDto; OkHttp realiza HTTPS y AuthInterceptor añade Bearer.
9. El servidor devuelve la tarea, que se deserializa como TaskDto.
10. TaskMapper produce Task de dominio. El ViewModel marca `saveSuccess` solo tras completar la llamada.
11. La pantalla comunica el evento de éxito a navegación. Se vuelve a Home y su ViewModel recarga la lista.

Si la solicitud falla, el paso 10 no marca éxito. Los campos se conservan y se muestran errores. Si el servidor es inaccesible, no se genera una tarea local ficticia para aparentar funcionamiento.

### 3.9. ErrorResponseDto y errores de campo

Formato documentado:

```json
{
  "message": "Hay datos inválidos",
  "errors": {
    "title": "El título es obligatorio"
  }
}
```

`ErrorResponseDto` tiene `message` y un mapa `errors` con valor predeterminado vacío. El repositorio intenta deserializar el cuerpo HTTP y construye una excepción de dominio. Si el cuerpo no se puede leer como ese DTO, usa un mensaje alternativo.

En el formulario, `TaskViewModel` asigna las claves conocidas a `titleError`, `descriptionError`, `priorityError`, `categoryError` y `dueDateError`. La vista solo muestra esos textos junto al campo correspondiente.

No todos los errores se deben mostrar como «lista vacía»: un fallo de red representa una operación fallida. Por eso Home distingue `hasLoaded`, `isLoading` y `error`.

### 3.10. Conceptos de Kotlin presentes en el código

| Concepto | Significado y uso |
|---|---|
| `data class` | Clase orientada a valores, con funciones generadas como igualdad y copy. Se usa en modelos, DTOs y UiStates. |
| `val` / `var` | Referencia de solo lectura / variable reasignable. Los campos de los estados son val; la referencia privada al estado se actualiza por flujo. |
| `copy(...)` | Crea una versión de un valor cambiando solo propiedades indicadas. Ejemplo: cambiar completed preservando título y fecha. |
| `interface` | Contrato que pueden implementar clases reales o falsas de prueba. |
| `override` | Implementa una operación definida en un contrato o clase base. |
| `private` | Restringe acceso a detalles como MutableStateFlow y helpers del repositorio. |
| `object` | Declaración de una instancia única Kotlin. Se usa en TaskMapper, RetrofitClient y Routes con responsabilidades acotadas. |
| `by lazy` | Inicializa una propiedad al primer acceso y conserva su valor. Se usa para el contenedor y la Factory. |
| `suspend` | Función que puede suspenderse en una corrutina; usada en servicios y operaciones remotas de repositorio. |
| `?.`, `?:` | Acceso seguro a un valor nullable y elección de un valor alternativo. |
| `let`, `also` | Funciones de alcance para trabajar con valores o ejecutar acciones relacionadas. No son patrones arquitectónicos. |
| `map`, `filter`, `count` | Transformación, selección y conteo de colecciones. En Home se aplican en el ViewModel. |
| `@Serializable` | Marca DTOs para Kotlinx Serialization. |
| `@Composable` | Función que participa en la composición de interfaz de Compose. |
| `@Preview` | Vista previa para Android Studio; sus datos de muestra no son datos de producción. |

### 3.11. Recursos visuales y archivos de apoyo

| Carpeta/ruta | Archivos | Función |
|---|---|---|
| `app/src/main/res/drawable/` | `ic_check.xml` | Marca de verificación de la identidad y estados. |
| Misma carpeta | `ic_eye.xml`, `ic_eye_off.xml` | Mostrar/ocultar contraseña. |
| Misma carpeta | `ic_arrow_back.xml`, `ic_chevron_right.xml` | Regreso y dirección/selección visual. |
| Misma carpeta | `ic_plus.xml`, `ic_logout.xml`, `ic_calendar.xml` | Crear, salir y fecha. |
| Misma carpeta | `ic_launcher_background.xml`, `ic_launcher_foreground.xml` | Recursos del icono de lanzamiento. |
| `res/mipmap-anydpi/` | `ic_launcher.xml`, `ic_launcher_round.xml` | Definiciones de iconos adaptativos. |
| `res/mipmap-mdpi/`, `hdpi/`, `xhdpi/`, `xxhdpi/`, `xxxhdpi/` | `ic_launcher.webp`, `ic_launcher_round.webp` | Variantes de iconos por densidad. |
| `res/values/` | `strings.xml` | Contiene app_name = Anotado. Muchos textos de pantalla siguen escritos en Kotlin; no se ha centralizado toda la localización. |
| `res/values/` | `themes.xml` | Tema base Android sin ActionBar usado por el manifiesto. Es distinto al tema Compose de Theme.kt. |
| `res/values/` | `colors.xml` | Recursos heredados de la plantilla; no constituyen la paleta activa completa de Compose. |
| `res/xml/` | `backup_rules.xml` | Excluye el archivo de sesión del backup definido por estas reglas. |
| `res/xml/` | `data_extraction_rules.xml` | Excluye ese archivo del backup y transferencia de dispositivo bajo estas reglas. |
| Raíz | `AGENTS.md` | Instrucciones de trabajo y restricciones de arquitectura. |
| Raíz | `README.md` | Entrada operativa, ejecución, configuración y evolución del proyecto. |
| `docs/` | `API-README.md` | Contrato del backend recibido del propietario. |
| `docs/referencias/` | `AGENTS-original.md`, `REGLAS-IMPORTANTES.md` | Instrucciones originales preservadas. Incluyen decisiones históricas que deben contrastarse con el estado actual. |
| `docs/figma/` | `login.jpeg`, `registro.jpeg`, `inicio.png`, `home.png`, `formulario.png` | Referencias visuales; no son pantallas ejecutables ni evidencia de que el flujo ya fue probado. |
| `docs/` | `DOCUMENTACION-TECNICA.md`, `DOCUMENTACION-TECNICA.html` | Este documento editable y su versión para lectura/impresión. |

Las pantallas usan vectores y controles Compose. No se insertó la imagen de Figma como una captura estática para simular una pantalla funcional.

### 3.12. Pruebas existentes y qué demuestran

Las pruebas se encuentran en `app/src/test/java/com/yharim/anotado/`. JUnit 4.13.2 organiza los casos; kotlinx-coroutines-test permite controlar el dispatcher y avanzar el trabajo pendiente. MockWebServer 4.12.0 ofrece un servidor HTTP local de prueba.

| Archivo | Casos | Qué cubre | Evidencia disponible |
|---|---:|---|---|
| `LoginViewModelTest.kt` | 9 | Campos, trim, doble envío, reintento, cancelación y visibilidad. | Aprobados en ejecución JVM directa previa. |
| `RegisterViewModelTest.kt` | 10 | Validación, confirmación, errores, carga, cancelación y limpieza de secretos. | Aprobados en ejecución JVM directa previa. |
| `SessionViewModelsTest.kt` | 5 | Estado inicial, cambios de sesión, invalidación y logout/reintento. | Aprobados en ejecución JVM directa previa. |
| `HomeViewModelTest.kt` | 8 | Carga, filtros, progreso, errores, actualización y selección. | Aprobados en ejecución JVM directa previa. |
| `TaskViewModelTest.kt` | 10 | Límites, fecha real, prioridad, normalización, envío y errores del formulario. | Aprobados en ejecución JVM directa previa. |
| `TaskMapperTest.kt` | 2 | Conversión de respuesta a dominio y conservación de campos para PUT. | Aprobados en ejecución JVM directa previa. |
| `AuthNetworkTest.kt` | 8 | JSON, sesión, login/registro, errores y Bearer con servidor de prueba. | Escritos; ejecución pendiente en el entorno Gradle disponible al usuario. |
| `TaskNetworkTest.kt` | 5 | GET/Bearer, PUT completo, 401, POST y errores 400. | Escritos; ejecución pendiente. |
| `ExampleUnitTest.kt` | 1 | Suma de ejemplo de la plantilla. | No aporta evidencia funcional sobre Anotado. |

En `app/src/androidTest/java/com/yharim/anotado/ExampleInstrumentedTest.kt` existe la prueba de contexto de la plantilla. Están declaradas dependencias de pruebas Compose, AndroidX Test y Espresso, pero **no se ha implementado aquí una batería instrumental completa de los flujos de usuario**.

La última ejecución registrada aprobó **44 casos** de ViewModels y mapper mediante el compilador Kotlin y JUnit de la caché local, fuera de Gradle. Esa comprobación no generó el APK y no valida toda la UI Compose, el manifiesto final ni los serializadores generados. Para compilar los tests del mapper se usó la anotación Serializable de una dependencia disponible en caché, no una prueba completa del transporte de la app.

Las comprobaciones de `git diff --check` y de dependencias entre capas complementan las pruebas. Buscar imports indebidos ayuda a detectar problemas, pero no demuestra por sí solo que toda la arquitectura sea correcta.

### 3.13. Límites de verificación y pendientes reales

El último intento de Gradle falló al iniciar FileLockContentionHandler, antes de compilar, por restricciones de sockets del entorno. No corresponde afirmar que los errores de compilación de toda la aplicación están descartados por ese intento.

El propietario informó que realiza pruebas del APK en su celular. Esa actividad es distinta de una ejecución observada por el agente y no permite dar por aprobados todos los casos de aceptación sin resultados concretos.

La URL AWS ya está configurada. El último acceso a `/health` desde este entorno no obtuvo respuesta HTTP por resolución DNS, y la herramienta web tampoco pudo abrir el endpoint. Esto limita la verificación desde aquí; no demuestra que el servidor esté caído para otros clientes.

Pendientes de funcionalidad y mantenimiento:

- Implementar la edición general de tareas reutilizando el formulario y precargando sus datos.
- Implementar DELETE con confirmación y manejar su respuesta 204 vacía.
- Decidir si se requiere una pantalla independiente de detalle y completar TaskDetailScreen si corresponde.
- Verificar registro, login, tareas, expiración de sesión, logout y back stack contra la API real.
- Completar pruebas instrumentales y comprobación de rotación/restauración según requisitos de entrega.
- Resolver la mezcla de nombres de paquetes sin renombrar arbitrariamente la identidad instalada.
- Revisar clases vacías y recursos heredados cuando se complete su función o se decida retirarlos.

### 3.14. Responsabilidad única, God Class y modelo anémico

Una **God Class** acumula responsabilidades ajenas y controla demasiadas partes del sistema. No se identifica solamente por el número de líneas. La separación actual evita reunir UI, red, persistencia y navegación en una sola clase:

- MainActivity inicia la interfaz y conecta dependencias.
- AppContainer ensambla objetos.
- ViewModelFactory crea ViewModels.
- Cada ViewModel coordina su estado de presentación.
- Los repositorios gestionan sus operaciones de datos.
- Los servicios describen HTTP.
- SessionManager persiste la sesión.
- TaskMapper transforma representaciones.

HomeViewModel reúne varias operaciones relacionadas con Home: listado, filtros, resumen, selección y logout. No implementa transporte HTTP ni almacenamiento. Si crece con funcionalidades ajenas a Home, deberá revisarse su cohesión.

Un **modelo anémico**, en el debate sobre modelado de dominio, se refiere a objetos de dominio reducidos a datos cuando el diseño requeriría encapsular comportamiento e invariantes. No toda `data class` ni toda clase pequeña es un defecto. Los DTO están destinados a transportar datos, los UiStates a describir presentación y TaskManagerApplication a sostener infraestructura. Este proyecto emplea modelos de dominio sencillos y validaciones en ViewModels; no pretende implementar un modelo de dominio rico de DDD.

Los principios aplicados se pueden reconocer en decisiones concretas:

| Principio | Ejemplo verificable |
|---|---|
| Responsabilidad única | AuthApiService y TaskApiService separados; AppContainer no hace operaciones de negocio. |
| Inversión de dependencias | ViewModels reciben interfaces de dominio; las implementaciones se seleccionan en DI. |
| DRY | Traducción común de errores en helpers privados y conversiones centralizadas en TaskMapper. |
| KISS | DI manual y un módulo app, sin framework de inyección adicional. |
| YAGNI | No se introdujeron Room, notificaciones, sincronización offline ni capas UseCase sin requerimiento. |

Estos ejemplos explican decisiones específicas. No constituyen una certificación de cumplimiento absoluto de todos los principios SOLID.

### 3.15. Lectura guiada para explicar el proyecto

Para seguir la arquitectura directamente en el código:

1. Abrir `TaskManagerApplication.kt`, `AppContainer.kt` y `ViewModelFactory.kt`: muestran cómo se ensamblan las dependencias.
2. Abrir `MainActivity.kt`, `AppViewModel.kt` y `AppNavigation.kt`: muestran el arranque y la sesión.
3. Comparar `TaskFormScreen.kt` con `TaskViewModel.kt`: muestra qué representa la vista y dónde se valida.
4. Abrir `domain/repository/TaskRepository.kt`: muestra el contrato sin detalles HTTP.
5. Abrir `data/repository/TaskRepositoryImpl.kt`: muestra la implementación remota.
6. Comparar `TaskDraft`, `TaskRequestDto`, `TaskDto` y `Task`: muestra por qué transporte y dominio se separan.
7. Abrir `TaskMapper.kt`, `TaskApiService.kt`, `RetrofitClient.kt` y `AuthInterceptor.kt`: muestra conversión, rutas y autenticación HTTP.
8. Revisar `TaskViewModelTest.kt` y `TaskNetworkTest.kt`: diferencia pruebas de coordinación de pruebas del contrato de transporte.

Para describir la creación de tareas en una exposición: **la pantalla comunica el evento; el ViewModel valida y prepara un modelo de dominio; el repositorio convierte y solicita datos al servicio; el mapper transforma la respuesta; el nuevo estado actualiza la pantalla**.

