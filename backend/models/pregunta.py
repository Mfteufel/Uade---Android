from typing import Optional

from pydantic import BaseModel


class PreguntaNueva(BaseModel):
    texto: str


class RespuestaNueva(BaseModel):
    texto: str


class Pregunta(BaseModel):
    id: str
    publicacionId: str
    autorId: str
    autorNombre: str
    texto: str
    fecha: int
    respuesta: Optional[str] = None
    respuestaFecha: Optional[int] = None
