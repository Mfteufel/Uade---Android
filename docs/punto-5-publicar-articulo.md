# Punto 5 — Publicar un artículo

Documentación de lo implementado para el Punto 5 del TPO "Ronda": el wizard de
publicación de artículos, el borrador persistente y la sección "Mis
publicaciones". Está pensada para que cualquiera del equipo (o la cátedra)
entienda qué se hizo y por qué, sin tener que leer todos los archivos.

## Requerimientos cubiertos

1. **Carga guiada en pasos (wizard)**: fotos → título/descripción →
   categoría/estado → precio/zona → resumen.
2. **Borrador persistente**: si el usuario cierra la app a mitad de carga, al
   volver a abrir el wizard retoma en el paso donde lo dejó, con los datos ya
   cargados.
3. **"Mis publicaciones"**: listado con el estado de cada publicación
   (activa/pausada/vendida) y las acciones de pausar/reactivar según
   corresponda.

## Cómo se llega a la feature

- Un **FAB** (botón flotante circular con un `+`) en la esquina inferior
  derecha del Home abre el wizard.
- Un ítem **"Mis publicaciones"** en el menú (⋮) de la toolbar del Home abre
  esa pantalla.

## Arquitectura general

Se respetó exactamente el mismo patrón que ya usa el Punto 3 (Home): **interfaz
de repositorio + implementación**, para que la UI nunca dependa directamente
de Room o de Retrofit, y **callbacks al estilo Retrofit** (`RepositorioCallback`)
para todo lo asincrónico.

```
UI (Fragments)
   │
   ▼
PublicarArticuloViewModel   (scoped al sub nav graph del wizard)
   │                    │
   ▼                    ▼
BorradorRepository   MisPublicacionesRepository
   │                    │
   ▼                    ▼
Room (borrador local)   Retrofit (API_Rest del TPO)
```

### Wizard: navegación y ViewModel

- `res/navigation/nav_graph_publicar.xml` es un **sub-grafo** incluido con
  `<include>` en `nav_graph.xml`. Al incluirse, comparte el mismo
  `NavController` que el resto de la app — no es una Activity ni un NavHost
  aparte, solo una forma de agrupar los cinco pasos en su propio archivo.
- Los cinco pasos son Fragments independientes que heredan de
  `PublicarPasoFragment` (`ui/publicar/PublicarPasoFragment.java`), cuya única
  responsabilidad es resolver el `PublicarArticuloViewModel` **con scope al
  sub-grafo** (`NavController.getBackStackEntry(R.id.nav_graph_publicar)`).
  Eso hace que el ViewModel sobreviva mientras el usuario esté en cualquiera
  de los cinco pasos, y se destruya recién cuando sale del wizard completo —
  equivalente en Java a lo que en Kotlin sería `by navGraphViewModels(...)`.
- El estado que se va completando vive en un único objeto mutable,
  `BorradorPublicacion` (`model/BorradorPublicacion.java`): fotos, título,
  descripción, categoría, estado del artículo, precio, zona y el número de
  paso en el que quedó. Cada Fragment escribe directamente sobre ese objeto y
  llama a `viewModel.guardarPaso(n)` al tocar "Siguiente", que:
  1. marca `paso = n` en el borrador,
  2. lo persiste en Room,
  3. notifica a los observers de `LiveData<BorradorPublicacion>`.

### Retomar el borrador (requisito 2)

- Al crearse el `PublicarArticuloViewModel` (primera vez que se entra al
  wizard en esa sesión de navegación), busca en Room si hay un borrador
  guardado. Si lo hay, lo carga tal cual quedó; si no, arranca de uno vacío.
- El primer paso del wizard, `PublicarFotosFragment`, es el único que además
  chequea el campo `paso` del borrador cargado: si es mayor a 0, significa que
  el usuario ya había avanzado y cerró la app antes de terminar, así que
  **salta directo** al paso correspondiente (`NavController.navigate(idDelPaso)`)
  en lugar de obligarlo a repasar todo desde cero.
- **Importante — granularidad del autosave**: el borrador se guarda en Room
  recién al tocar "Siguiente" en cada paso, no tecla por tecla. Si el usuario
  cierra la app a mitad de tipear un campo (sin haber avanzado ese paso
  puntual), se pierde lo tecleado en *ese* paso, pero no los pasos ya
  completados antes. Es la interpretación más simple de "no perder el
  progreso"; si se necesita autosave por campo, habría que agregar un
  `TextWatcher` con debounce en cada paso que llame a `guardarPaso()` sin
  cambiar de paso.

### Persistencia del borrador — Room (`data/local/`)

Un solo borrador activo, sin historial: la tabla `borrador_publicacion` tiene
como mucho una fila, con `id` fijo (`BorradorPublicacionEntity.ID_UNICO`) y
`OnConflictStrategy.REPLACE` en cada guardado.

| Archivo | Rol |
|---|---|
| `BorradorPublicacionEntity.java` | Fila de la tabla. Enums guardados como `String` (`name()`), no con un `TypeConverter` de enum, para que un borrador viejo en disco no rompa la carga si un valor del enum se renombra o elimina. |
| `Converters.java` | `TypeConverter` de `List<Uri>` (las fotos) usando Gson, reutilizando la dependencia que ya trae Retrofit. |
| `BorradorPublicacionDao.java` | `guardar()`, `obtener()`, `borrar()`. |
| `AppDatabase.java` | Singleton de Room, mismo criterio que `PublicacionRepositoryMock`/`SesionUsuario`. |
| `BorradorRepository.java` (interfaz) + `BorradorRepositoryLocal.java` (impl) | Corre todo en un `ExecutorService` de un solo hilo (Room prohíbe el Main Thread) y devuelve los resultados por `Handler` al Main Thread, igual que hace `PublicacionRepositoryMock` con su demora simulada. |

`borrar()` se llama una sola vez: cuando `MisPublicacionesRepository.publicar()`
devuelve éxito.

### Alta y gestión de publicaciones — Retrofit (`data/remote/`)

| Archivo | Rol |
|---|---|
| `ApiService.java` | `POST publicaciones` (multipart, sube fotos + campos), `GET publicaciones/mias`, `PATCH publicaciones/{id}/estado`. |
| `ApiClient.java` | Builder singleton de Retrofit + OkHttp (con logging interceptor) + Gson. |
| `dto/PublicacionCreadaResponse.java`, `dto/MiPublicacionResponse.java`, `dto/CambiarEstadoPublicacionRequest.java` | DTOs del contrato con el servidor, separados a propósito de los modelos de dominio (`MiPublicacion`, etc.) para que un cambio en el JSON de la API no obligue a tocar la UI. |
| `MisPublicacionesRepository.java` (interfaz) + `MisPublicacionesRepositoryApi.java` (impl) | Agrupa `publicar()`, `listar()`, `pausar()`, `reactivar()`: las cuatro operaciones son el mismo ciclo de vida de las publicaciones de un usuario. |

**⚠️ Backend pendiente**: `ApiClient.BASE_URL` apunta a un placeholder
(`https://api.ronda.tpo.uade.edu.ar/`) porque todavía no existe una API real
para este punto. El código compila y corre, pero publicar, listar, pausar y
reactivar van a fallar con un mensaje de error de red hasta que:

1. se reemplace `BASE_URL` por la URL real, y
2. se confirme que el JSON que devuelve/espera el backend coincide con los
   DTOs de `data/remote/dto/` (si no coincide, hay que ajustar esos DTOs, no
   el resto de la app).

Las fotos se leen con `ContentResolver` y se mandan enteras en memoria como
`MultipartBody.Part` (sin streaming): para la cantidad de fotos de un TPO
alcanza sin problema.

### Selección de fotos

`PublicarFotosFragment` usa el **Photo Picker del sistema**
(`ActivityResultContracts.PickMultipleVisualMedia`), no un `Intent` de galería
clásico. Ventaja: no requiere el permiso `READ_MEDIA_IMAGES` en el manifest ni
pedirlo en runtime, porque el propio selector del sistema le da acceso a la
app solo sobre las fotos que el usuario eligió.

### Mis publicaciones (`ui/mispublicaciones/`)

- `MisPublicacionesFragment` sigue la misma estructura de estados que el Home
  (lista / vacío / error / cargando, uno visible a la vez).
- `MiPublicacionAdapter` decide qué botón mostrar según
  `EstadoPublicacion`:
  - `ACTIVA` → botón "Pausar".
  - `PAUSADA` → botón "Reactivar".
  - `VENDIDA` → sin botón de acción.
- No hay forma, dentro de esta feature, de que una publicación pase a
  `VENDIDA`: eso depende de otra funcionalidad (una compra) fuera del alcance
  del Punto 5. La UI ya está preparada para ese estado cuando exista.

### Modelo nuevo (`model/`)

| Clase | Para qué |
|---|---|
| `EstadoPublicacion` | Activa/Pausada/Vendida — **no confundir** con `EstadoArticulo` (nuevo/como nuevo/usado), que describe la conservación del artículo. |
| `BorradorPublicacion` | Objeto mutable que viaja por el wizard y se persiste en Room. |
| `MiPublicacion` | Ítem de "Mis publicaciones". No reutiliza `Publicacion` (la del Home) porque esa modela publicaciones ajenas (con zona, vendedor) y esta necesita el estado de la publicación, que no tiene sentido en el Home. |

También se le agregó un campo `id` a `SesionUsuario` (placeholder hasta que
exista el Punto 1 — Autenticación): "Mis publicaciones" lo necesita para
pedirle a la API solo las publicaciones del usuario logueado.

## Archivos nuevos, por carpeta

```
model/
  EstadoPublicacion.java
  BorradorPublicacion.java
  MiPublicacion.java

data/
  BorradorRepository.java
  BorradorRepositoryLocal.java
  MisPublicacionesRepository.java
  MisPublicacionesRepositoryApi.java
  local/
    AppDatabase.java
    BorradorPublicacionEntity.java
    BorradorPublicacionDao.java
    Converters.java
  remote/
    ApiService.java
    ApiClient.java
    dto/
      PublicacionCreadaResponse.java
      MiPublicacionResponse.java
      CambiarEstadoPublicacionRequest.java

ui/publicar/
  PublicarPasoFragment.java
  PublicarArticuloViewModel.java
  PublicarFotosFragment.java
  PublicarTituloDescripcionFragment.java
  PublicarCategoriaEstadoFragment.java
  PublicarPrecioZonaFragment.java
  PublicarResumenFragment.java
  FotoSeleccionadaAdapter.java

ui/mispublicaciones/
  MisPublicacionesFragment.java
  MiPublicacionAdapter.java

res/navigation/
  nav_graph_publicar.xml

res/layout/
  fragment_publicar_fotos.xml
  fragment_publicar_titulo_descripcion.xml
  fragment_publicar_categoria_estado.xml
  fragment_publicar_precio_zona.xml
  fragment_publicar_resumen.xml
  fragment_mis_publicaciones.xml
  item_foto_seleccionada.xml
  item_mi_publicacion.xml

res/menu/
  menu_home.xml

res/drawable/
  ic_cerrar.xml
  ic_atras.xml
  ic_agregar.xml
  bg_boton_quitar_foto.xml
```

## Archivos existentes que se modificaron

- `gradle/libs.versions.toml` y `app/build.gradle.kts` — dependencias de Room,
  Retrofit/OkHttp/Gson y Lifecycle (ViewModel + LiveData).
- `app/src/main/AndroidManifest.xml` — permiso `INTERNET`.
- `res/navigation/nav_graph.xml` — `<include>` del sub-grafo del wizard,
  destino `misPublicacionesFragment` y las dos acciones desde `homeFragment`.
- `res/layout/fragment_home.xml` — FAB de "Publicar".
- `ui/home/HomeFragment.java` — wiring del FAB y del menú de la toolbar.
- `data/SesionUsuario.java` — campo `id`.
- `res/values/strings.xml` — strings del wizard, de "Mis publicaciones" y del
  menú del Home.

## Pendiente / decisiones abiertas

- **URL real del backend** (ver sección de Retrofit arriba).
- **Autosave más fino** (campo por campo) si se necesita, en vez de solo al
  avanzar de paso.
- **Tests**: no se agregaron tests automatizados para este punto, en línea con
  que el resto del proyecto tampoco los tiene todavía (los únicos archivos de
  test son los placeholders default de Android Studio).
- **No se probó en un emulador real** dentro de este entorno: solo se validó
  que compila y linkea (`./gradlew assembleDebug`). Falta correrlo a mano para
  chequear la UX del wizard (selector de fotos, dropdown de zona, etc.).
- Todo este trabajo está en la branch `feature/publicar-articulo`, sin
  commitear todavía. Antes de mergear a `main` corresponde abrir un Pull
  Request, según las reglas de la cátedra.
