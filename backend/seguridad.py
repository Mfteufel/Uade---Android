import hashlib
import hmac
import os
import secrets
from datetime import timedelta

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

import database

CLAVE_SECRETA = os.environ.get("RONDA_JWT_SECRET", "clave-solo-para-desarrollo")
ALGORITMO = "HS256"
HORAS_DE_VALIDEZ = 8
ITERACIONES_PBKDF2 = 120000

esquema_bearer = HTTPBearer(auto_error=False)


def crear_token(usuario_id, email):
    emision = database.ahora()
    carga = {
        "sub": str(usuario_id),
        "email": email,
        "iat": int(emision.timestamp()),
        "exp": int((emision + timedelta(hours=HORAS_DE_VALIDEZ)).timestamp()),
    }
    return jwt.encode(carga, CLAVE_SECRETA, algorithm=ALGORITMO)


def hashear_password(password):
    sal = secrets.token_hex(16)
    resumen = hashlib.pbkdf2_hmac(
        "sha256", password.encode("utf-8"), bytes.fromhex(sal), ITERACIONES_PBKDF2
    ).hex()
    return "pbkdf2_sha256$" + str(ITERACIONES_PBKDF2) + "$" + sal + "$" + resumen


def verificar_password(password, almacenado):
    if not almacenado:
        return False
    partes = almacenado.split("$")
    if len(partes) != 4:
        return False
    iteraciones, sal, resumen = partes[1], partes[2], partes[3]
    calculado = hashlib.pbkdf2_hmac(
        "sha256", password.encode("utf-8"), bytes.fromhex(sal), int(iteraciones)
    ).hex()
    return hmac.compare_digest(calculado, resumen)


def usuario_actual(credenciales: HTTPAuthorizationCredentials = Depends(esquema_bearer)):
    sin_permiso = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Necesitas iniciar sesion",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if credenciales is None:
        raise sin_permiso
    try:
        carga = jwt.decode(credenciales.credentials, CLAVE_SECRETA, algorithms=[ALGORITMO])
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="La sesion vencio, volve a iniciar sesion",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError:
        raise sin_permiso

    usuario = database.buscar_usuario_por_id(int(carga["sub"]))
    if usuario is None:
        raise sin_permiso
    return dict(usuario)
