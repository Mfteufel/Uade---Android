from typing import List, Optional

from pydantic import BaseModel

from models.publicacion import PublicacionResumen


class Reputacion(BaseModel):
    promedioEstrellas: float
    cantidadCalificaciones: int = 0
    operacionesComoComprador: int
    operacionesComoVendedor: int


class Perfil(BaseModel):
    id: str
    nombre: str
    email: Optional[str] = None
    telefono: Optional[str] = None
    zona: Optional[str] = None
    fechaAlta: int
    reputacion: Reputacion


class PerfilEdicion(BaseModel):
    nombre: str
    email: str
    telefono: Optional[str] = None
    zona: Optional[str] = None


class FavoritoNuevo(BaseModel):
    publicacionId: str


class Favorito(PublicacionResumen):
    precioAlGuardar: float
    tieneNovedad: bool


class FiltroGuardado(BaseModel):
    texto: Optional[str] = None
    categoria: Optional[str] = None
    estados: List[str] = []
    zonas: List[str] = []
    precioMinimo: Optional[float] = None
    precioMaximo: Optional[float] = None
    orden: str = "RECIENTES"


class BusquedaNueva(BaseModel):
    nombre: str
    filtro: FiltroGuardado


class Busqueda(BaseModel):
    id: str
    nombre: str
    filtro: FiltroGuardado
    fechaGuardado: int
    cantidadNuevas: int
