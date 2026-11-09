# TaskManager API

API REST de TaskManager (Proyecto Unidad 3). Node.js + Express + SQLite, JavaScript normal.

## Requisitos
- Node.js 22 o superior (lo exige `better-sqlite3`)
- No necesitas compiladores: `better-sqlite3` trae su binario y el `.npmrc` del proyecto
  desactiva los scripts de instalación (evita que `npm ci` intente compilar).

## Correr en local
```bash
npm install
cp .env.example .env        # y llena JWT_SECRET (mínimo 32 caracteres)
npm start                   # o: npm run dev (se reinicia al guardar)
```
Comprobar: `curl http://localhost:3000/health` -> `{"status":"ok"}`

Generar un secreto:
```bash
node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
```

## Estructura
```
src/
├── server.js                  # arranca el servidor
├── app.js                     # arma Express: middlewares y rutas
├── config.js                  # lee y valida .env
├── db/                        # conexión SQLite y schema.sql
├── routes/                    # qué URL llama a qué controlador
├── controllers/               # reciben la petición, responden
├── validators/                # validan y normalizan el body
├── repositories/              # único lugar con SQL
├── middleware/                # auth (JWT), errores, log
└── utils/                     # HttpError y firma/verificación del token
```

## Contrato

| Método | Ruta | Auth | Éxito | Errores |
|---|---|---|---|---|
| POST | `/register` | no | 201 `{ token, user }` | 400, 409 (correo repetido) |
| POST | `/login` | no | 200 `{ token, user }` | 400, 401 |
| GET | `/tasks` | Bearer | 200 `[Task]` | 401 |
| GET | `/tasks/:id` | Bearer | 200 `Task` | 401, 404 |
| POST | `/tasks` | Bearer | 201 `Task` | 400, 401 |
| PUT | `/tasks/:id` | Bearer | 200 `Task` | 400, 401, 404 |
| DELETE | `/tasks/:id` | Bearer | 204 sin cuerpo | 401, 404 |
| GET | `/health` | no | 200 `{ status }` | |

**user:** `{ id, name, email }`

**Task:** `{ id, title, description, priority, dueDate, completed, category }`
- `description` y `dueDate` pueden ser `null`.
- `priority` entero 1–5 (default 3). `dueDate` con formato `yyyy-MM-dd`.
- `completed` es boolean (default false). `category` default `"General"`.
- `title` obligatorio, máximo 120 caracteres.
- `PUT` reemplaza la tarea completa: los opcionales que no se envíen vuelven a su default.
- El `userId` NO viaja en el JSON: el servidor lo saca del token.
- `/register` no recibe `confirmPassword`: esa comparación la hace la app.

**Formato de error (siempre igual):**
```json
{ "message": "Hay datos inválidos", "errors": { "email": "El correo no tiene un formato válido" } }
```
`errors` solo aparece en 400 y 409. Un 401 en `/tasks` significa token ausente, inválido,
expirado o de un usuario que ya no existe: la app debe cerrar sesión.

## Notas para la app Android
- Emulador: base URL `http://10.0.2.2:3000/`. Con HTTP, cleartext solo en el build debug.
- `Json { ignoreUnknownKeys = true }` por si el contrato crece.
- `description` y `dueDate` en el DTO: `String?`.
- `deleteTask` puede ser `suspend fun ...(): Unit`; Retrofit acepta el 204.

## Despliegue en EC2 (resumen)
1. Instalar Node 22+, copiar el proyecto y correr `npm ci --omit=dev`.
2. Crear `.env` en el servidor (no en GitHub).
3. Mantenerlo vivo y que arranque con la instancia: `pm2 start src/server.js --name taskmanager && pm2 save && pm2 startup`.
4. Security group: abrir el puerto 3000 (TCP) de entrada.
5. Asignar una Elastic IP para que la URL no cambie al reiniciar.
6. Respaldar `data/taskmanager.db` (es toda la base de datos).
