from typing import Optional

from pydantic import BaseModel


# los campos son los que lee Novedad.java en la app
class Novedad(BaseModel):
    id: str
    tipo: str
    titulo: str
    cuerpo: str
    publicacionId: Optional[str] = None
    ofertaId: Optional[str] = None
    rol: Optional[str] = None
    creadoEn: int
