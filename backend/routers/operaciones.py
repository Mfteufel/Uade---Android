import sqlite3
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, status

import database
import seguridad
from models.operacion import CalificacionNueva, Operacion

router = APIRouter(prefix="/operaciones", tags=["Operaciones"])

DIAS_PARA_CALIFICAR = 7
MILISEGUNDOS_POR_DIA = 24 * 3600 * 1000
LARGO_MAXIMO_DE_COMENTARIO = 280
TIPOS = ("COMPRA", "VENTA")


def error(codigo, mensaje):
    return HTTPException(status_code=codigo, detail=mensaje)


def resumen(usuario_id):
    return {"id": str(usuario_id), "nombre": database.nombre_de_vendedor(usuario_id)}


def calificacion_a_json(fila):
    return {
        "id": str(fila["id"]),
        "operacionId": str(fila["oferta_id"]),
        "autor": resumen(fila["autor_id"]),
        "calificadoId": str(fila["calificado_id"]),
        "articulo": fila["titulo"],
        "estrellas": fila["estrellas"],
        "comentario": fila["comentario"],
        "fecha": fila["fecha"],
    }


# la operacion se arma siempre desde el lado de quien pregunta: tipo, su calificacion
# y si todavia puede calificar
def a_json(fila, usuario_id, ahora):
    fecha_entrega = fila["fecha_entrega"]
    calificable_hasta = None
    if fecha_entrega is not None:
        calificable_hasta = fecha_entrega + DIAS_PARA_CALIFICAR * MILISEGUNDOS_POR_DIA
    mia = database.calificacion_de(fila["id"], usuario_id)
    return {
        "id": str(fila["id"]),
        "publicacionId": str(fila["publicacion_id"]),
        "articulo": fila["titulo"],
        "montoFinal": fila["precio"],
        "fechaEntrega": fecha_entrega,
        "estado": "PENDIENTE_ENTREGA" if fecha_entrega is None else "ENTREGADA",
        "tipo": "COMPRA" if fila["comprador_id"] == usuario_id else "VENTA",
        "comprador": resumen(fila["comprador_id"]),
        "vendedor": resumen(fila["vendedor_id"]),
        "miCalificacion": None if mia is None else calificacion_a_json(mia),
        "puedeCalificar": mia is None and calificable_hasta is not None and ahora <= calificable_hasta,
        "calificableHasta": calificable_hasta,
    }


# solo una oferta aceptada es una venta: una pendiente o rechazada no es una operacion
def operacion_propia(operacion_id, usuario):
    fila = database.buscar_oferta(operacion_id)
    if fila is None or fila["estado"] != "ACEPTADA":
        raise error(404, "La operacion no existe")
    if fila["comprador_id"] != usuario["id"] and fila["vendedor_id"] != str(usuario["id"]):
        raise error(403, "No participaste de esta operacion")
    return fila


# historial: solo operaciones concretadas (con la entrega confirmada)
@router.get("", response_model=List[Operacion])
def listar(tipo: Optional[str] = None, desde: Optional[int] = None, hasta: Optional[int] = None,
           usuario: dict = Depends(seguridad.usuario_actual)):
    if tipo is not None and tipo not in TIPOS:
        raise error(400, "El tipo tiene que ser COMPRA o VENTA")
    if desde is not None and hasta is not None and desde > hasta:
        raise error(400, "La fecha de inicio no puede ser posterior a la de fin")
    ahora = database.ahora_en_milisegundos()
    filas = database.operaciones_de(usuario["id"], tipo, desde, hasta)
    return [a_json(fila, usuario["id"], ahora) for fila in filas]


@router.get("/pendientes", response_model=List[Operacion])
def listar_pendientes(usuario: dict = Depends(seguridad.usuario_actual)):
    ahora = database.ahora_en_milisegundos()
    filas = database.pendientes_de_entrega(usuario["id"])
    return [a_json(fila, usuario["id"], ahora) for fila in filas]


# la entrega la confirma solo el comprador: es quien sabe que recibio el articulo
@router.post("/{operacion_id}/entrega", response_model=Operacion)
def confirmar_entrega(operacion_id: int, usuario: dict = Depends(seguridad.usuario_actual)):
    fila = operacion_propia(operacion_id, usuario)
    if fila["comprador_id"] != usuario["id"]:
        raise error(403, "Solo el comprador puede confirmar la entrega")
    ahora = database.ahora_en_milisegundos()
    if not database.confirmar_entrega(fila["id"], ahora):
        raise error(409, "La entrega ya estaba confirmada")
    return a_json(database.buscar_oferta(operacion_id), usuario["id"], ahora)


@router.post("/{operacion_id}/calificacion", response_model=Operacion,
             status_code=status.HTTP_201_CREATED)
def calificar(operacion_id: int, datos: CalificacionNueva,
              usuario: dict = Depends(seguridad.usuario_actual)):
    comentario = (datos.comentario or "").strip() or None
    if datos.estrellas < 1 or datos.estrellas > 5:
        raise error(400, "La calificacion tiene que ser de 1 a 5 estrellas")
    if comentario and len(comentario) > LARGO_MAXIMO_DE_COMENTARIO:
        raise error(400, "El comentario es demasiado largo")

    fila = operacion_propia(operacion_id, usuario)
    if fila["fecha_entrega"] is None:
        raise error(409, "Todavia no se confirmo la entrega")
    if database.calificacion_de(fila["id"], usuario["id"]) is not None:
        raise error(409, "Ya calificaste esta operacion")
    ahora = database.ahora_en_milisegundos()
    if ahora > fila["fecha_entrega"] + DIAS_PARA_CALIFICAR * MILISEGUNDOS_POR_DIA:
        raise error(409, "Vencio el plazo para calificar (7 dias desde la entrega)")

    # el calificado no viaja en el pedido: es siempre la otra parte de la operacion
    if fila["comprador_id"] == usuario["id"]:
        calificado_id = int(fila["vendedor_id"])
    else:
        calificado_id = fila["comprador_id"]
    try:
        database.crear_calificacion(fila["id"], usuario["id"], calificado_id,
                                    datos.estrellas, comentario, ahora)
    except sqlite3.IntegrityError:
        # dos envios casi simultaneos: la restriccion UNIQUE frena el segundo
        raise error(409, "Ya calificaste esta operacion")
    return a_json(database.buscar_oferta(operacion_id), usuario["id"], ahora)
