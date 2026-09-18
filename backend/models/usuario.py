from typing import Optional

from pydantic import BaseModel


def es_email_valido(valor):
    valor = (valor or "").strip()
    if valor.count("@") != 1 or " " in valor:
        return False
    parte_usuario, dominio = valor.split("@")
    if not parte_usuario or not dominio or "." not in dominio:
        return False
    return not dominio.startswith(".") and not dominio.endswith(".")


class SolicitudOtp(BaseModel):
    email: str


class VerificacionOtp(BaseModel):
    email: str
    codigo: str


class SolicitudLogin(BaseModel):
    email: str
    password: str


class UsuarioPublico(BaseModel):
    id: int
    email: str
    nombre: str
    zona: Optional[str] = None


class RespuestaToken(BaseModel):
    token: str
    tipo: str = "Bearer"
    usuario: UsuarioPublico


class RespuestaOtp(BaseModel):
    mensaje: str
    expira_en_segundos: int
    codigo: Optional[str] = None
