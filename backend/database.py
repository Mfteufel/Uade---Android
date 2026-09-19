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
    zona          TEXT,
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
    fecha_publicacion  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS fotos (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    publicacion_id INTEGER NOT NULL,
    archivo        TEXT NOT NULL
);
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
                      vendedor_id, fecha_publicacion=None):
    if fecha_publicacion is None:
        fecha_publicacion = ahora_en_milisegundos()
    with conectar() as conexion:
        cursor = conexion.execute(
            "INSERT INTO publicaciones (titulo, descripcion, precio, categoria,"
            " estado_articulo, zona, vendedor_id, texto_busqueda, fecha_publicacion)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (titulo, descripcion, precio, categoria, estado_articulo, zona,
             str(vendedor_id), normalizar(titulo + " " + descripcion), fecha_publicacion),
        )
        return cursor.lastrowid


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


def buscar_publicaciones(texto, categoria, estados, zonas, precio_min, precio_max,
                         orden, pagina, tamanio):
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

    donde = " AND ".join(condiciones)
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


def nombre_de_vendedor(vendedor_id):
    if str(vendedor_id).isdigit():
        usuario = buscar_usuario_por_id(int(vendedor_id))
        if usuario is not None:
            return usuario["nombre"]
    return "Usuario"
