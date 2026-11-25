import asyncio
import time
from typing import List, Optional

from fastapi import APIRouter, Depends, Request, Response, status
from starlette.concurrency import run_in_threadpool

import database
import seguridad
from models.novedad import Novedad

router = APIRouter(prefix="/novedades", tags=["Novedades"])

# La app espera hasta 40 s (readTimeout de PollingService): el servidor tiene que
# cortar antes, o la app lo toma como un error de red.
SEGUNDOS_DE_ESPERA = 25
# cada cuanto se mira si llego algo mientras la conexion esta retenida
INTERVALO_DE_CONSULTA = 1


def a_json(fila):
    return {
        "id": str(fila["id"]),
        "tipo": fila["tipo"],
        "titulo": fila["titulo"],
        "cuerpo": fila["cuerpo"],
        "publicacionId": str(fila["publicacion_id"]) if fila["publicacion_id"] is not None else None,
        "ofertaId": str(fila["oferta_id"]) if fila["oferta_id"] is not None else None,
        "rol": fila["rol"],
        "creadoEn": fila["creado_en"],
    }


# Long polling: si hay novedades responde ya con 200; si no, retiene la conexion y
# sigue mirando. Si se cumple el tiempo sin nada responde 204 y la app vuelve a preguntar.
# Es async para que una conexion esperando no ocupe un hilo; las consultas a la base,
# que son sincronicas, van al pool de hilos.
@router.get("", response_model=List[Novedad], responses={204: {"description": "Sin novedades"}})
async def esperar_novedades(request: Request, desde: Optional[int] = None,
                            usuario: dict = Depends(seguridad.usuario_actual)):
    limite = time.monotonic() + SEGUNDOS_DE_ESPERA
    while True:
        filas = await run_in_threadpool(database.novedades_sin_entregar, usuario["id"], desde)
        # si la app corto (cerro sesion, mataron el servicio) no se da nada por entregado
        if await request.is_disconnected():
            return Response(status_code=status.HTTP_204_NO_CONTENT)
        if filas:
            await run_in_threadpool(
                database.marcar_novedades_entregadas, [fila["id"] for fila in filas])
            return [a_json(fila) for fila in filas]
        if time.monotonic() >= limite:
            return Response(status_code=status.HTTP_204_NO_CONTENT)
        await asyncio.sleep(INTERVALO_DE_CONSULTA)
