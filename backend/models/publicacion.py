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
