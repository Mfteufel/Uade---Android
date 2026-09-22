from typing import Optional

from pydantic import BaseModel


class UsuarioResumen(BaseModel):
    id: str
    nombre: str


class Calificacion(BaseModel):
    id: str
    operacionId: str
    autor: UsuarioResumen
    calificadoId: str
    articulo: str
    estrellas: int
    comentario: Optional[str] = None
    fecha: int


class CalificacionNueva(BaseModel):
    estrellas: int
    comentario: Optional[str] = None


class Operacion(BaseModel):
    id: str
    publicacionId: str
    articulo: str
    montoFinal: float
    fechaEntrega: Optional[int] = None
    estado: str
    tipo: str
    comprador: UsuarioResumen
    vendedor: UsuarioResumen
    miCalificacion: Optional[Calificacion] = None
    puedeCalificar: bool
    calificableHasta: Optional[int] = None
