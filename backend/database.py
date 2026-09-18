import os
import sqlite3
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
"""


def ahora():
    return datetime.now(timezone.utc)


def a_texto(momento):
    return momento.isoformat()


def desde_texto(texto):
    return datetime.fromisoformat(texto)


def conectar():
    conexion = sqlite3.connect(RUTA_BASE)
    conexion.row_factory = sqlite3.Row
    return conexion


def inicializar():
    RUTA_BASE.parent.mkdir(parents=True, exist_ok=True)
    with conectar() as conexion:
        conexion.executescript(ESQUEMA)
    crear_usuario_demo()


def crear_usuario_demo():
    from seguridad import hashear_password

    if buscar_usuario_por_email("walter@uade.edu.ar") is None:
        crear_usuario(
            email="walter@uade.edu.ar",
            nombre="Walter",
            password_hash=hashear_password("ronda1234"),
            zona="CABALLITO",
        )


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
