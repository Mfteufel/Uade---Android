# Contrato API — Perfil y Reputación (Punto 2) · Historial y Calificaciones (Punto 9)

Qué necesita la app Android del backend para dejar de usar datos simulados en los
Puntos 2 y 9. Está escrito contra lo que la app **ya tiene implementado**:

| Lado Android | Archivo |
|---|---|
| Interfaces Retrofit | `data/remote/UsuarioApi.java`, `data/remote/OperacionApi.java` |
| DTOs (nombres de campos JSON) | `data/remote/dto/*.java` |
| Implementaciones listas (sin activar) | `data/PerfilRepositoryApi.java`, `data/OperacionRepositoryApi.java` |
| Reglas de negocio de referencia | `data/BaseDeDatosMock.java` + `app/src/test/.../BaseDeDatosMockTest.java` |
| Contrato JSON ejecutable | `app/src/test/.../remote/ContratoJsonTest.java` |

Si algún nombre cambia del lado del backend, se ajusta **solo el DTO** correspondiente
(y el test de contrato avisa cuál); ninguna pantalla se toca.

---

## 1. Convenciones

- **Autenticación:** todos los endpoints de este documento piden
  `Authorization: Bearer <jwt>`. El usuario se obtiene del token
  (`seguridad.usuario_actual`, campo `sub`), **nunca** de un parámetro. Sin token o
  con token vencido: `401`.
- **JSON en snake_case**, igual que el resto del backend (`expira_en_segundos`).
- **IDs:** enteros en la base. La app los guarda como texto, así que acepta `7` o
  `"7"` indistintamente.
- **Fechas:** en el JSON van como **epoch en milisegundos (UTC)**, número entero.
  La app las usa como `long` (minSdk 24, sin `java.time`). En SQLite pueden seguir
  guardándose como ISO, como hace hoy `database.a_texto`.
- **Errores:** `{"detail": "mensaje para el usuario"}`. La app muestra ese texto tal
  cual para `400/403/404/409`. Los `422` de validación de FastAPI (con `detail`
  como lista) se reemplazan por un mensaje genérico de la pantalla.
- **Enums** (como string, en mayúsculas):
  - `zona`: `PALERMO, BELGRANO, NUNEZ, RECOLETA, CABALLITO, ALMAGRO, VILLA_CRESPO, FLORES, BOEDO, BARRACAS, VICENTE_LOPEZ, SAN_ISIDRO, TIGRE, LOMAS_DE_ZAMORA, AVELLANEDA, QUILMES` (los mismos de `model/Zona.java`; `usuarios.zona` ya guarda `"CABALLITO"`).
  - `tipo`: `COMPRA, VENTA` (desde el punto de vista del usuario del token).
  - `estado` de operación: `PENDIENTE_ENTREGA, ENTREGADA`.

## 2. Modelos JSON

### Usuario (perfil propio y público)

```json
{
  "id": 1,
  "nombre": "Walter",
  "email": "walter@uade.edu.ar",
  "telefono": "1145678901",
  "zona": "CABALLITO",
  "fecha_alta": 1726000000000,
  "foto_url": "usuarios/1/foto",
  "reputacion": {
    "promedio": 4.2,
    "cantidad_calificaciones": 5,
    "operaciones_como_comprador": 5,
    "operaciones_como_vendedor": 3
  },
  "calificaciones_pendientes": 3
}
```

| Campo | Tipo | Nulo | Nota |
|---|---|---|---|
| `email`, `telefono` | string | sí | **Solo en el perfil propio.** En `GET /usuarios/{id}` no se mandan. |
| `zona` | string (enum) | sí | Los usuarios creados por OTP no tienen zona: la app lo tolera. |
| `fecha_alta` | int (ms) | no | Sale de `usuarios.creado_en`. Es la "antigüedad". |
| `foto_url` | string | sí | Ruta **relativa a la URL base**, o `null` si no cargó foto. |
| `reputacion` | objeto | no | Calculada, ver §4. |
| `calificaciones_pendientes` | int | sí | Solo perfil propio: operaciones que el usuario todavía puede calificar. |

### Operación (vista por el usuario del token)

```json
{
  "id": 12,
  "publicacion_id": 4,
  "articulo": "Teclado mecánico Redragon",
  "monto_final": 45000,
  "fecha_operacion": 1726000000000,
  "fecha_entrega": 1726200000000,
  "estado": "ENTREGADA",
  "tipo": "COMPRA",
  "comprador": {"id": 1, "nombre": "Walter"},
  "vendedor": {"id": 3, "nombre": "Sofía M."},
  "mi_calificacion": null,
  "puede_calificar": true,
  "calificable_hasta": 1726804800000
}
```

| Campo | Nota |
|---|---|
| `articulo`, `monto_final` | **Copia** tomada al aceptar la oferta: si la publicación se edita, el historial no cambia. |
| `fecha_operacion` | Cuándo se aceptó la oferta. |
| `fecha_entrega` | Cuándo se confirmó la entrega; `null` si todavía no. |
| `tipo` | `COMPRA` si el usuario del token es el comprador, `VENTA` si es el vendedor. |
| `mi_calificacion` | La calificación que **el usuario del token** dejó (objeto Calificación), o `null`. |
| `puede_calificar` | `true` si: entregada, dentro de los 7 días y sin calificación previa del usuario del token. |
| `calificable_hasta` | `fecha_entrega + 7 días` (ms), o `null` si no hubo entrega. |

La app **no** calcula la ventana de 7 días: muestra/oculta el botón con
`puede_calificar` y `calificable_hasta`. El servidor igual vuelve a validar al recibir
la calificación (§5).

### Calificación

```json
{
  "id": 30,
  "operacion_id": 12,
  "autor": {"id": 3, "nombre": "Sofía M."},
  "calificado_id": 1,
  "articulo": "Teclado mecánico Redragon",
  "estrellas": 5,
  "comentario": "Excelente comprador",
  "fecha": 1726300000000
}
```

`comentario` es opcional (`null` si vino vacío o solo con espacios).

## 3. Endpoints

| # | Método y ruta | Request | Response | Quién | Errores |
|---|---|---|---|---|---|
| 1 | `GET /usuarios/me` | — | `200` Usuario (propio) | el usuario del token | `401` |
| 2 | `PATCH /usuarios/me` | `{"nombre", "email", "telefono", "zona"}` | `200` Usuario (propio) actualizado | el usuario del token | `400` datos inválidos, `409` email en uso, `401` |
| 3 | `PUT /usuarios/me/foto` | multipart, campo `foto` (JPEG ≤ 512 px, ya comprimido por la app) | `200` Usuario (propio) con `foto_url` | el usuario del token | `415` no es imagen, `413` > 2 MB, `401` |
| 4 | `GET {foto_url}` (p. ej. `/usuarios/{id}/foto`) | — | `200` bytes `image/jpeg` | cualquier usuario logueado | `404` sin foto |
| 5 | `GET /usuarios/{id}` | — | `200` Usuario (público: sin `email` ni `telefono`) | cualquier usuario logueado | `404` |
| 6 | `GET /usuarios/{id}/calificaciones` | — | `200` `[Calificación]`, recibidas por `{id}`, más recientes primero | cualquier usuario logueado | `404` |
| 7 | `GET /operaciones?tipo=&desde=&hasta=` | query opcionales: `tipo` (`COMPRA`/`VENTA`), `desde`/`hasta` (ms, inclusivos, comparados contra `fecha_entrega`) | `200` `[Operación]`: solo `ENTREGADA` y solo donde el usuario es comprador o vendedor, más recientes primero | el usuario del token | `422` tipo inválido, `400` desde > hasta |
| 8 | `POST /operaciones/{id}/calificacion` | `{"estrellas": 1..5, "comentario": "..." \| null}` | `201` Operación actualizada (con `mi_calificacion`) | comprador o vendedor de esa operación | ver §5 |

Notas de implementación:
- En FastAPI, declarar `/usuarios/me` **antes** que `/usuarios/{id}` (si no, "me" se
  intenta leer como id).
- Validaciones de `PATCH /usuarios/me` (las mismas que hoy aplica
  `PerfilRepositoryMock.validar`): nombre no vacío; email con formato válido (reusar
  `models/usuario.es_email_valido`), normalizado a minúsculas y **único**; zona
  dentro del enum; teléfono vacío o con al menos 8 dígitos. Ignorar cualquier campo
  que no sea uno de esos cuatro (id, reputación, fecha de alta no se editan).
- Foto: guardar el archivo (p. ej. `backend/fotos_perfil/{id}.jpg`, en `.gitignore`)
  y su nombre en una columna nueva `usuarios.foto_archivo`. Hace falta
  `python-multipart` en `requirements.txt` para recibir `UploadFile`.
- Hace falta agregar `usuarios.telefono`. Como cada integrante ya tiene su
  `ronda.db`, conviene un `ALTER TABLE ... ADD COLUMN` si la columna no existe
  (`PRAGMA table_info`), porque `CREATE TABLE IF NOT EXISTS` no agrega columnas.

### Endpoints que dependen de otros puntos (no los consume esta app todavía)

| Para | Qué | Por qué me importa |
|---|---|---|
| Punto 7 (Ofertas) | Al **aceptar** una oferta, crear la operación en la **misma transacción**: `INSERT INTO operaciones (oferta_id, publicacion_id, articulo, comprador_id, vendedor_id, monto_final, estado='PENDIENTE_ENTREGA', fecha_operacion=ahora)`. Comprador = autor de la oferta; vendedor = dueño de la publicación; monto = el de la oferta/contraoferta aceptada. `oferta_id UNIQUE`. | Sin esto no hay operaciones reales: el historial queda vacío. |
| Punto 8 (Entrega) | `POST /operaciones/{id}/entrega` (comprador o vendedor): `estado='ENTREGADA'`, `fecha_entrega=ahora`. `409` si ya estaba entregada. | Es el inicio de la ventana de 7 días. Sin fecha de entrega no se puede calificar. |
| Puntos 3/5 (Publicaciones) | Listar publicaciones **activas** de un usuario (p. ej. `GET /publicaciones?vendedor_id={id}`), devolviendo `[]` si no tiene. | La pestaña "Publicaciones" del perfil público. |
| Punto 4 (Detalle) | Que el vendedor dentro de una publicación traiga la misma `reputacion` de §2. | Hoy el Detalle muestra una reputación fija del mock y no coincide con la del perfil público. |

## 4. Reputación: se calcula, no se guarda

Con cuatro consultas agregadas en cada lectura (en SQLite es instantáneo a esta
escala) y nunca contadores guardados que se puedan desincronizar:

```sql
SELECT
  (SELECT AVG(estrellas) FROM calificaciones WHERE calificado_id = :id)          AS promedio,
  (SELECT COUNT(*)       FROM calificaciones WHERE calificado_id = :id)          AS cantidad_calificaciones,
  (SELECT COUNT(*) FROM operaciones WHERE comprador_id = :id AND estado = 'ENTREGADA') AS operaciones_como_comprador,
  (SELECT COUNT(*) FROM operaciones WHERE vendedor_id  = :id AND estado = 'ENTREGADA') AS operaciones_como_vendedor;
```

`promedio` = `0` cuando no hay calificaciones (la app usa `cantidad_calificaciones`
para mostrar "sin calificaciones" en vez de "0,0").

Tablas sugeridas (mismo estilo que `database.py`):

```sql
CREATE TABLE IF NOT EXISTS operaciones (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  oferta_id       INTEGER UNIQUE,
  publicacion_id  INTEGER,
  articulo        TEXT    NOT NULL,
  comprador_id    INTEGER NOT NULL REFERENCES usuarios(id),
  vendedor_id     INTEGER NOT NULL REFERENCES usuarios(id),
  monto_final     REAL    NOT NULL CHECK (monto_final > 0),
  estado          TEXT    NOT NULL CHECK (estado IN ('PENDIENTE_ENTREGA', 'ENTREGADA')),
  fecha_operacion TEXT    NOT NULL,
  fecha_entrega   TEXT,
  CHECK (comprador_id <> vendedor_id)
);

CREATE TABLE IF NOT EXISTS calificaciones (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  operacion_id  INTEGER NOT NULL REFERENCES operaciones(id),
  autor_id      INTEGER NOT NULL REFERENCES usuarios(id),
  calificado_id INTEGER NOT NULL REFERENCES usuarios(id),
  estrellas     INTEGER NOT NULL CHECK (estrellas BETWEEN 1 AND 5),
  comentario    TEXT,
  fecha         TEXT    NOT NULL,
  UNIQUE (operacion_id, autor_id),
  CHECK (autor_id <> calificado_id)
);
```

Ojo: SQLite no aplica `REFERENCES` salvo que cada conexión haga
`PRAGMA foreign_keys = ON` (hoy `database.conectar()` no lo hace).

## 5. Reglas para calificar (las valida el servidor)

La app solo pone límites de forma (mínimo 1 estrella, 280 caracteres) y oculta el
botón según `puede_calificar`. **La regla de negocio vive en el backend**, en este
orden (mismos mensajes que usa hoy `BaseDeDatosMock`, que la app muestra tal cual):

| # | Regla | Código | `detail` |
|---|---|---|---|
| 1 | La operación existe | `404` | La operación no existe |
| 2 | El usuario del token es su comprador o su vendedor (no se puede calificar una operación ajena) | `403` | No participaste de esta operación |
| 3 | `estrellas` entre 1 y 5 (`Field(ge=1, le=5)`) | `422` | — |
| 4 | `comentario` opcional, ≤ 280 caracteres después de `strip()`; vacío → `NULL` | `422` | — |
| 5 | La operación está `ENTREGADA` y tiene `fecha_entrega` | `409` | Todavía no se registró la entrega |
| 6 | No existe ya una calificación de este autor para esta operación (`UNIQUE`) | `409` | Ya calificaste esta operación |
| 7 | `ahora ≤ fecha_entrega + 7 días` (reloj del servidor, UTC) | `409` | Venció el plazo para calificar (7 días desde la entrega) |

`calificado_id` **no viaja en el request**: el servidor lo deduce (la otra parte de
la operación). Así nadie puede calificarse a sí mismo ni calificar a un tercero; el
`CHECK (autor_id <> calificado_id)` queda como red de seguridad.

## 6. Qué pedirle a Walter (checklist)

- [ ] URL base del servidor (y si es `https`).
- [ ] Endpoints 1 a 8 de §3 con los JSON de §2 (o avisar qué nombres cambian).
- [ ] Columnas `usuarios.telefono` y `usuarios.foto_archivo`; tablas de §4.
- [ ] Reglas de §5 con esos códigos y `detail`.
- [ ] Datos de prueba: algunas operaciones `ENTREGADA` con `fecha_entrega` dentro y
      fuera de los 7 días, y algunas calificaciones, para poder mostrar la demo
      aunque Ofertas (Punto 7) todavía no cree operaciones reales.
- [ ] Acordar con Punto 7 y Punto 8 los dos hooks de §3 ("Endpoints que dependen de otros puntos").

## 7. Cómo se conecta la app cuando el backend esté arriba

1. `AndroidManifest.xml`: agregar `<uses-permission android:name="android.permission.INTERNET" />`
   (hoy no está). Si el servidor es `http` (no `https`), habilitar tráfico en claro
   para ese host con un `network_security_config`.
2. `login/NetworkModule.java`: poner la URL base del servidor en `BASE_URL`.
3. Login (Punto 1): que guarde el token con `TokenManager.saveToken(...)` y cargue en
   `SesionUsuario` el `id` real del usuario (hoy queda `"u0"`). Sin token, todos
   estos endpoints responden `401`.
4. `di/RepositoryModule.java`: `USAR_API = true`.
5. Correr `ContratoJsonTest` si cambió algún nombre de campo, y probar en el emulador:
   perfil → editar → foto → mis operaciones → calificar → perfil público de la contraparte.

No hace falta tocar ningún Fragment ni layout.
