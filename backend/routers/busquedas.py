import json
from typing import List

from fastapi import APIRouter, Depends, HTTPException, Response, status

import database
import seguridad
from models.perfil import Busqueda, BusquedaNueva

router = APIRouter(prefix="/busquedas", tags=["Busquedas guardadas"])


def a_json(fila):
    filtro = json.loads(fila["filtro"])
    return {
        "id": str(fila["id"]),
        "nombre": fila["nombre"],
        "filtro": filtro,
        "fechaGuardado": fila["fecha_guardado"],
        # publicaciones que cumplen el filtro y aparecieron despues de la ultima visita
        "cantidadNuevas": database.contar_publicaciones_nuevas(filtro, fila["visto_hasta"]),
    }


@router.get("", response_model=List[Busqueda])
def listar(usuario: dict = Depends(seguridad.usuario_actual)):
    return [a_json(fila) for fila in database.busquedas_de(usuario["id"])]


@router.post("", response_model=Busqueda, status_code=status.HTTP_201_CREATED)
def guardar(datos: BusquedaNueva, usuario: dict = Depends(seguridad.usuario_actual)):
    nombre = datos.nombre.strip()
    if not nombre:
        raise HTTPException(status_code=400, detail="La busqueda necesita un nombre")
    nuevo_id = database.crear_busqueda(usuario["id"], nombre, json.dumps(datos.filtro.model_dump()))
    for fila in database.busquedas_de(usuario["id"]):
        if fila["id"] == nuevo_id:
            return a_json(fila)


@router.post("/vistas", status_code=status.HTTP_204_NO_CONTENT)
def marcar_todo_visto(usuario: dict = Depends(seguridad.usuario_actual)):
    database.marcar_busquedas_vistas(usuario["id"])
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.delete("/{busqueda_id}", status_code=status.HTTP_204_NO_CONTENT)
def eliminar(busqueda_id: int, usuario: dict = Depends(seguridad.usuario_actual)):
    if not database.borrar_busqueda(usuario["id"], busqueda_id):
        raise HTTPException(status_code=404, detail="La busqueda no existe")
    return Response(status_code=status.HTTP_204_NO_CONTENT)
