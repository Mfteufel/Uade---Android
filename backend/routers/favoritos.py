from typing import List

from fastapi import APIRouter, Depends, HTTPException, Request, Response, status

import database
import seguridad
from models.perfil import Favorito, FavoritoNuevo
from routers.publicaciones import a_json

router = APIRouter(prefix="/favoritos", tags=["Favoritos"])


@router.get("", response_model=List[Favorito])
def listar(request: Request, usuario: dict = Depends(seguridad.usuario_actual)):
    respuesta = []
    for fila in database.favoritos_de(usuario["id"]):
        item = a_json(request, fila)
        item["precioAlGuardar"] = fila["precio_visto"]
        # hay novedad si el precio cambio desde la ultima vez que el usuario lo vio
        item["tieneNovedad"] = fila["precio"] != fila["precio_visto"]
        respuesta.append(item)
    return respuesta


@router.post("", status_code=status.HTTP_201_CREATED)
def marcar(datos: FavoritoNuevo, usuario: dict = Depends(seguridad.usuario_actual)):
    if not datos.publicacionId.isdigit():
        raise HTTPException(status_code=404, detail="La publicacion no existe")
    publicacion = database.buscar_publicacion(int(datos.publicacionId))
    if publicacion is None:
        raise HTTPException(status_code=404, detail="La publicacion no existe")
    database.agregar_favorito(usuario["id"], publicacion["id"], publicacion["precio"])
    return {"publicacionId": str(publicacion["id"])}


@router.post("/vistos", status_code=status.HTTP_204_NO_CONTENT)
def marcar_todo_visto(usuario: dict = Depends(seguridad.usuario_actual)):
    database.marcar_favoritos_vistos(usuario["id"])
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.delete("/{publicacion_id}", status_code=status.HTTP_204_NO_CONTENT)
def desmarcar(publicacion_id: int, usuario: dict = Depends(seguridad.usuario_actual)):
    database.quitar_favorito(usuario["id"], publicacion_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
