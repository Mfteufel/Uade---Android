from typing import Optional

from pydantic import BaseModel

from models.operacion import UsuarioResumen


class CodigoEntrega(BaseModel):
    codigo: str


class Entrega(BaseModel):
    operacionId: str
    articulo: str
    montoFinal: float
    tipo: str
    contraparte: UsuarioResumen
    estado: str
    fechaEntrega: Optional[int] = None
    enlaceQr: Optional[str] = None
