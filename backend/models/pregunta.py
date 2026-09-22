from pydantic import BaseModel


class PreguntaNueva(BaseModel):
    texto: str


class Pregunta(BaseModel):
    id: str
    publicacionId: str
    autorId: str
    autorNombre: str
    texto: str
    fecha: int
