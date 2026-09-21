from typing import Optional

from pydantic import BaseModel


class OfertaNueva(BaseModel):
    publicacionId: str
    precio: float
    mensaje: Optional[str] = None


class CambioEstadoOferta(BaseModel):
    estado: str


class Contraoferta(BaseModel):
    precio: float


class Oferta(BaseModel):
    id: str
    publicacionId: str
    tituloPublicacion: str
    fotoPrincipalUrl: Optional[str] = None
    compradorId: str
    nombreComprador: str
    vendedorId: str
    nombreVendedor: str
    precio: float
    mensaje: Optional[str] = None
    estado: str
    turno: str
    fechaCreacion: int
    venceEn: int
    direccionEntrega: Optional[str] = None
