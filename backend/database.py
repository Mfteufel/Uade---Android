import os
import sqlite3
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

RUTA_BASE = Path(os.environ.get("RONDA_DB", Path(__file__).resolve().parent / "ronda.db"))

ESQUEMA = """
CREATE TABLE IF NOT EXISTS usuarios (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    email         TEXT NOT NULL UNIQUE,
    nombre        TEXT NOT NULL,
    password_hash TEXT,
    password_pendiente TEXT,
    zona          TEXT,
    telefono      TEXT,
    creado_en     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS codigos_otp (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    email     TEXT NOT NULL,
    codigo    TEXT NOT NULL,
    expira_en TEXT NOT NULL,
    usado     INTEGER NOT NULL DEFAULT 0,
    creado_en TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS indice_otp_email ON codigos_otp (email);

CREATE TABLE IF NOT EXISTS publicaciones (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo             TEXT NOT NULL,
    descripcion        TEXT NOT NULL,
    precio             REAL NOT NULL,
    categoria          TEXT NOT NULL,
    estado_articulo    TEXT NOT NULL,
    zona               TEXT NOT NULL,
    estado_publicacion TEXT NOT NULL DEFAULT 'ACTIVA',
    vendedor_id        TEXT NOT NULL,
    texto_busqueda     TEXT NOT NULL,
    fecha_publicacion  INTEGER NOT NULL,
    direccion_entrega  TEXT
);

CREATE TABLE IF NOT EXISTS ofertas (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    publicacion_id INTEGER NOT NULL,
    comprador_id   INTEGER NOT NULL,
    vendedor_id    TEXT NOT NULL,
    precio         REAL NOT NULL,
    mensaje        TEXT,
    estado         TEXT NOT NULL DEFAULT 'PENDIENTE',
    turno          TEXT NOT NULL DEFAULT 'VENDEDOR',
    fecha_creacion INTEGER NOT NULL,
    vence_en       INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS favoritos (
    usuario_id     INTEGER NOT NULL,
    publicacion_id INTEGER NOT NULL,
    precio_visto   REAL NOT NULL,
    PRIMARY KEY (usuario_id, publicacion_id)
);

CREATE TABLE IF NOT EXISTS busquedas (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id     INTEGER NOT NULL,
    nombre         TEXT NOT NULL,
    filtro         TEXT NOT NULL,
    visto_hasta    INTEGER NOT NULL,
    fecha_guardado INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS fotos (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    publicacion_id INTEGER NOT NULL,
    archivo        TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS preguntas (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    publicacion_id INTEGER NOT NULL,
    autor_id       INTEGER NOT NULL,
    texto          TEXT NOT NULL,
    creado_en      INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS indice_preguntas_publicacion ON preguntas (publicacion_id);
"""

ORDENES = {
    "RECIENTES": "fecha_publicacion DESC",
    "PRECIO_MENOR": "precio ASC",
    "PRECIO_MAYOR": "precio DESC",
}


def ahora():
    return datetime.now(timezone.utc)


def ahora_en_milisegundos():
    return int(ahora().timestamp() * 1000)


def a_texto(momento):
    return momento.isoformat()


def desde_texto(texto):
    return datetime.fromisoformat(texto)


def normalizar(texto):
    sin_tildes = unicodedata.normalize("NFD", texto or "")
    return "".join(c for c in sin_tildes if unicodedata.category(c) != "Mn").lower()


def conectar():
    conexion = sqlite3.connect(RUTA_BASE)
    conexion.row_factory = sqlite3.Row
    return conexion


def inicializar():
    RUTA_BASE.parent.mkdir(parents=True, exist_ok=True)
    with conectar() as conexion:
        conexion.executescript(ESQUEMA)
        # las bases creadas antes de sumar el telefono no tienen esa columna
        columnas = [fila["name"] for fila in conexion.execute("PRAGMA table_info(usuarios)")]
        if "telefono" not in columnas:
            conexion.execute("ALTER TABLE usuarios ADD COLUMN telefono TEXT")
        if "password_pendiente" not in columnas:
            conexion.execute("ALTER TABLE usuarios ADD COLUMN password_pendiente TEXT")
        columnas = [fila["name"] for fila in conexion.execute("PRAGMA table_info(publicaciones)")]
        if "direccion_entrega" not in columnas:
            conexion.execute("ALTER TABLE publicaciones ADD COLUMN direccion_entrega TEXT")


def buscar_usuario_por_email(email):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT * FROM usuarios WHERE email = ?", (email,)
        ).fetchone()


def buscar_usuario_por_id(usuario_id):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT * FROM usuarios WHERE id = ?", (usuario_id,)
        ).fetchone()


def crear_usuario(email, nombre, password_hash=None, zona=None):
    with conectar() as conexion:
        cursor = conexion.execute(
            "INSERT INTO usuarios (email, nombre, password_hash, zona, creado_en)"
            " VALUES (?, ?, ?, ?, ?)",
            (email, nombre, password_hash, zona, a_texto(ahora())),
        )
        nuevo_id = cursor.lastrowid
    return buscar_usuario_por_id(nuevo_id)


def guardar_registro_pendiente(usuario_id, nombre, password_hash):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE usuarios SET nombre = ?, password_pendiente = ? WHERE id = ?",
            (nombre, password_hash, usuario_id),
        )


def activar_password_pendiente(usuario_id):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE usuarios SET password_hash = password_pendiente, password_pendiente = NULL"
            " WHERE id = ? AND password_pendiente IS NOT NULL",
            (usuario_id,),
        )


def invalidar_codigos(email):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE codigos_otp SET usado = 1 WHERE email = ? AND usado = 0",
            (email,),
        )


def guardar_codigo(email, codigo, expira_en):
    with conectar() as conexion:
        conexion.execute(
            "INSERT INTO codigos_otp (email, codigo, expira_en, usado, creado_en)"
            " VALUES (?, ?, ?, 0, ?)",
            (email, codigo, a_texto(expira_en), a_texto(ahora())),
        )


def ultimo_codigo(email):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT * FROM codigos_otp WHERE email = ? ORDER BY id DESC LIMIT 1",
            (email,),
        ).fetchone()


def marcar_codigo_usado(codigo_id):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE codigos_otp SET usado = 1 WHERE id = ?", (codigo_id,)
        )


def contar_publicaciones():
    with conectar() as conexion:
        return conexion.execute("SELECT COUNT(*) FROM publicaciones").fetchone()[0]


def crear_publicacion(titulo, descripcion, precio, categoria, estado_articulo, zona,
                      vendedor_id, fecha_publicacion=None, direccion_entrega=None):
    if fecha_publicacion is None:
        fecha_publicacion = ahora_en_milisegundos()
    with conectar() as conexion:
        cursor = conexion.execute(
            "INSERT INTO publicaciones (titulo, descripcion, precio, categoria,"
            " estado_articulo, zona, vendedor_id, texto_busqueda, fecha_publicacion,"
            " direccion_entrega)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (titulo, descripcion, precio, categoria, estado_articulo, zona,
             str(vendedor_id), normalizar(titulo + " " + descripcion), fecha_publicacion,
             direccion_entrega),
        )
        return cursor.lastrowid


def completar_direccion(vendedor_id, direccion):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE publicaciones SET direccion_entrega = ?"
            " WHERE vendedor_id = ? AND direccion_entrega IS NULL",
            (direccion, str(vendedor_id)),
        )


def buscar_publicacion(publicacion_id):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT * FROM publicaciones WHERE id = ?", (publicacion_id,)
        ).fetchone()


def publicaciones_de(vendedor_id):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT * FROM publicaciones WHERE vendedor_id = ? ORDER BY fecha_publicacion DESC",
            (str(vendedor_id),),
        ).fetchall()


def cambiar_estado_publicacion(publicacion_id, estado):
    with conectar() as conexion:
        cursor = conexion.execute(
            "UPDATE publicaciones SET estado_publicacion = ? WHERE id = ?",
            (estado, publicacion_id),
        )
        return cursor.rowcount > 0


def armar_condiciones(texto, categoria, estados, zonas, precio_min, precio_max,
                      vendedor_id=None, desde=None):
    condiciones = ["estado_publicacion = 'ACTIVA'"]
    valores = []
    if texto:
        condiciones.append("texto_busqueda LIKE ?")
        valores.append("%" + normalizar(texto) + "%")
    if categoria:
        condiciones.append("categoria = ?")
        valores.append(categoria)
    if estados:
        condiciones.append("estado_articulo IN (" + ",".join("?" * len(estados)) + ")")
        valores.extend(estados)
    if zonas:
        condiciones.append("zona IN (" + ",".join("?" * len(zonas)) + ")")
        valores.extend(zonas)
    if precio_min is not None:
        condiciones.append("precio >= ?")
        valores.append(precio_min)
    if precio_max is not None:
        condiciones.append("precio <= ?")
        valores.append(precio_max)
    if vendedor_id:
        condiciones.append("vendedor_id = ?")
        valores.append(str(vendedor_id))
    if desde is not None:
        condiciones.append("fecha_publicacion > ?")
        valores.append(desde)
    return " AND ".join(condiciones), valores


def buscar_publicaciones(texto, categoria, estados, zonas, precio_min, precio_max,
                         orden, pagina, tamanio, vendedor_id=None):
    donde, valores = armar_condiciones(
        texto, categoria, estados, zonas, precio_min, precio_max, vendedor_id
    )
    with conectar() as conexion:
        total = conexion.execute(
            "SELECT COUNT(*) FROM publicaciones WHERE " + donde, valores
        ).fetchone()[0]
        filas = conexion.execute(
            "SELECT * FROM publicaciones WHERE " + donde
            + " ORDER BY " + ORDENES[orden] + " LIMIT ? OFFSET ?",
            valores + [tamanio, pagina * tamanio],
        ).fetchall()
    return filas, total


def contar_publicaciones_nuevas(filtro, desde):
    donde, valores = armar_condiciones(
        filtro.get("texto"), filtro.get("categoria"), filtro.get("estados") or [],
        filtro.get("zonas") or [], filtro.get("precioMinimo"), filtro.get("precioMaximo"),
        None, desde,
    )
    with conectar() as conexion:
        return conexion.execute(
            "SELECT COUNT(*) FROM publicaciones WHERE " + donde, valores
        ).fetchone()[0]


def actualizar_publicacion(publicacion_id, titulo, descripcion, precio):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE publicaciones SET titulo = ?, descripcion = ?, precio = ?, texto_busqueda = ?"
            " WHERE id = ?",
            (titulo, descripcion, precio, normalizar(titulo + " " + descripcion), publicacion_id),
        )


def agregar_foto(publicacion_id, archivo):
    with conectar() as conexion:
        conexion.execute(
            "INSERT INTO fotos (publicacion_id, archivo) VALUES (?, ?)",
            (publicacion_id, archivo),
        )


def fotos_de(publicacion_id):
    with conectar() as conexion:
        filas = conexion.execute(
            "SELECT archivo FROM fotos WHERE publicacion_id = ? ORDER BY id",
            (publicacion_id,),
        ).fetchall()
    return [fila["archivo"] for fila in filas]


def crear_pregunta(publicacion_id, autor_id, texto):
    creado_en = ahora_en_milisegundos()
    with conectar() as conexion:
        cursor = conexion.execute(
            "INSERT INTO preguntas (publicacion_id, autor_id, texto, creado_en)"
            " VALUES (?, ?, ?, ?)",
            (publicacion_id, autor_id, texto, creado_en),
        )
        return cursor.lastrowid


def preguntas_de(publicacion_id):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT * FROM preguntas WHERE publicacion_id = ? ORDER BY id",
            (publicacion_id,),
        ).fetchall()


def nombre_de_vendedor(vendedor_id):
    if str(vendedor_id).isdigit():
        usuario = buscar_usuario_por_id(int(vendedor_id))
        if usuario is not None:
            return usuario["nombre"]
    return "Usuario"


def actualizar_usuario(usuario_id, nombre, email, telefono, zona):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE usuarios SET nombre = ?, email = ?, telefono = ?, zona = ? WHERE id = ?",
            (nombre, email, telefono, zona, usuario_id),
        )
    return buscar_usuario_por_id(usuario_id)


def agregar_favorito(usuario_id, publicacion_id, precio):
    with conectar() as conexion:
        conexion.execute(
            "INSERT OR IGNORE INTO favoritos (usuario_id, publicacion_id, precio_visto)"
            " VALUES (?, ?, ?)",
            (usuario_id, publicacion_id, precio),
        )


def quitar_favorito(usuario_id, publicacion_id):
    with conectar() as conexion:
        conexion.execute(
            "DELETE FROM favoritos WHERE usuario_id = ? AND publicacion_id = ?",
            (usuario_id, publicacion_id),
        )


def favoritos_de(usuario_id):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT p.*, f.precio_visto FROM favoritos f"
            " JOIN publicaciones p ON p.id = f.publicacion_id"
            " WHERE f.usuario_id = ? ORDER BY p.fecha_publicacion DESC",
            (usuario_id,),
        ).fetchall()


def marcar_favoritos_vistos(usuario_id):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE favoritos SET precio_visto = (SELECT precio FROM publicaciones"
            " WHERE publicaciones.id = favoritos.publicacion_id) WHERE usuario_id = ?",
            (usuario_id,),
        )


def crear_busqueda(usuario_id, nombre, filtro_json):
    momento = ahora_en_milisegundos()
    with conectar() as conexion:
        cursor = conexion.execute(
            "INSERT INTO busquedas (usuario_id, nombre, filtro, visto_hasta, fecha_guardado)"
            " VALUES (?, ?, ?, ?, ?)",
            (usuario_id, nombre, filtro_json, momento, momento),
        )
        return cursor.lastrowid


def busquedas_de(usuario_id):
    with conectar() as conexion:
        return conexion.execute(
            "SELECT * FROM busquedas WHERE usuario_id = ? ORDER BY id DESC", (usuario_id,)
        ).fetchall()


def borrar_busqueda(usuario_id, busqueda_id):
    with conectar() as conexion:
        cursor = conexion.execute(
            "DELETE FROM busquedas WHERE id = ? AND usuario_id = ?", (busqueda_id, usuario_id)
        )
        return cursor.rowcount > 0


def marcar_busquedas_vistas(usuario_id):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE busquedas SET visto_hasta = ? WHERE usuario_id = ?",
            (ahora_en_milisegundos(), usuario_id),
        )


OFERTA_CON_PUBLICACION = (
    "SELECT o.*, p.titulo, p.direccion_entrega FROM ofertas o"
    " JOIN publicaciones p ON p.id = o.publicacion_id"
)


def contar_ofertas():
    with conectar() as conexion:
        return conexion.execute("SELECT COUNT(*) FROM ofertas").fetchone()[0]


def crear_oferta(publicacion_id, comprador_id, vendedor_id, precio, mensaje, vence_en,
                 estado="PENDIENTE", turno="VENDEDOR", fecha_creacion=None):
    if fecha_creacion is None:
        fecha_creacion = ahora_en_milisegundos()
    with conectar() as conexion:
        cursor = conexion.execute(
            "INSERT INTO ofertas (publicacion_id, comprador_id, vendedor_id, precio, mensaje,"
            " estado, turno, fecha_creacion, vence_en)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (publicacion_id, comprador_id, str(vendedor_id), precio, mensaje,
             estado, turno, fecha_creacion, vence_en),
        )
        return cursor.lastrowid


def buscar_oferta(oferta_id):
    with conectar() as conexion:
        return conexion.execute(
            OFERTA_CON_PUBLICACION + " WHERE o.id = ?", (oferta_id,)
        ).fetchone()


def ofertas_enviadas(comprador_id):
    with conectar() as conexion:
        return conexion.execute(
            OFERTA_CON_PUBLICACION + " WHERE o.comprador_id = ? ORDER BY o.id DESC",
            (comprador_id,),
        ).fetchall()


def ofertas_recibidas(vendedor_id):
    with conectar() as conexion:
        return conexion.execute(
            OFERTA_CON_PUBLICACION + " WHERE o.vendedor_id = ? ORDER BY o.id DESC",
            (str(vendedor_id),),
        ).fetchall()


def tiene_oferta_pendiente(publicacion_id, comprador_id):
    with conectar() as conexion:
        fila = conexion.execute(
            "SELECT id FROM ofertas WHERE publicacion_id = ? AND comprador_id = ?"
            " AND estado = 'PENDIENTE'",
            (publicacion_id, comprador_id),
        ).fetchone()
    return fila is not None


def vencer_ofertas():
    with conectar() as conexion:
        conexion.execute(
            "UPDATE ofertas SET estado = 'VENCIDA' WHERE estado = 'PENDIENTE' AND vence_en < ?",
            (ahora_en_milisegundos(),),
        )


def rechazar_oferta(oferta_id):
    with conectar() as conexion:
        conexion.execute("UPDATE ofertas SET estado = 'RECHAZADA' WHERE id = ?", (oferta_id,))


# los tres cambios van en la misma transaccion: o se hacen todos o ninguno
def aceptar_oferta(oferta_id, publicacion_id):
    with conectar() as conexion:
        conexion.execute("UPDATE ofertas SET estado = 'ACEPTADA' WHERE id = ?", (oferta_id,))
        conexion.execute(
            "UPDATE ofertas SET estado = 'RECHAZADA'"
            " WHERE publicacion_id = ? AND estado = 'PENDIENTE'",
            (publicacion_id,),
        )
        conexion.execute(
            "UPDATE publicaciones SET estado_publicacion = 'VENDIDA' WHERE id = ?",
            (publicacion_id,),
        )


def contraofertar(oferta_id, precio, turno, vence_en):
    with conectar() as conexion:
        conexion.execute(
            "UPDATE ofertas SET precio = ?, turno = ?, vence_en = ? WHERE id = ?",
            (precio, turno, vence_en, oferta_id),
        )
