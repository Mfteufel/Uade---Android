from typing import List, Optional

from pydantic import BaseModel


class PublicacionResumen(BaseModel):
    id: str
    titulo: str
    descripcion: str
    precio: float
    categoria: str
    estadoArticulo: str
    zona: str
    estadoPublicacion: str
    fechaPublicacion: int
    vendedorId: str
    nombreVendedor: str
    fotoPrincipalUrl: Optional[str] = None


class PublicacionDetalle(PublicacionResumen):
    fotos: List[str] = []
    # Solo viene completa cuando quien pide el detalle es el dueño de la
    # publicacion (ver seguridad.usuario_actual_opcional en el router). El
    # comprador la ve por otro lado: GET /ofertas/{id} una vez ACEPTADA.
    direccionEntrega: Optional[str] = None


class PaginaPublicaciones(BaseModel):
    publicaciones: List[PublicacionResumen]
    pagina: int
    hayMas: bool
    totalResultados: int


class MiPublicacion(BaseModel):
    id: str
    titulo: str
    precio: float
    fotoPrincipalUrl: Optional[str] = None
    estadoArticulo: str
    estadoPublicacion: str
    fechaPublicacion: int


class PublicacionCreada(BaseModel):
    id: str


class CambioEstado(BaseModel):
    estadoPublicacion: str


class EdicionPublicacion(BaseModel):
    titulo: Optional[str] = None
    descripcion: Optional[str] = None
    precio: Optional[float] = None
