from typing import List

from fastapi import APIRouter, Depends, HTTPException, Request, status

import database
import seguridad
from models.oferta import CambioEstadoOferta, Contraoferta, Oferta, OfertaNueva
from routers.publicaciones import urls_de_fotos

router = APIRouter(prefix="/ofertas", tags=["Ofertas"])

HORAS_DE_VIGENCIA = 6
LARGO_MAXIMO_DE_MENSAJE = 200


def error(codigo, mensaje):
    return HTTPException(status_code=codigo, detail=mensaje)


def nuevo_vencimiento():
    return database.ahora_en_milisegundos() + HORAS_DE_VIGENCIA * 3600 * 1000


def a_json(request, fila):
    fotos = urls_de_fotos(request, fila["publicacion_id"])
    return {
        "id": str(fila["id"]),
        "publicacionId": str(fila["publicacion_id"]),
        "tituloPublicacion": fila["titulo"],
        "fotoPrincipalUrl": fotos[0] if fotos else None,
        "compradorId": str(fila["comprador_id"]),
        "nombreComprador": database.nombre_de_vendedor(fila["comprador_id"]),
        "vendedorId": fila["vendedor_id"],
        "nombreVendedor": database.nombre_de_vendedor(fila["vendedor_id"]),
        "precio": fila["precio"],
        "mensaje": fila["mensaje"],
        "estado": fila["estado"],
        "turno": fila["turno"],
        "fechaCreacion": fila["fecha_creacion"],
        "venceEn": fila["vence_en"],
        # la direccion solo se entrega cuando la oferta quedo aceptada
        "direccionEntrega": fila["direccion_entrega"] if fila["estado"] == "ACEPTADA" else None,
    }


def rol_de(usuario, fila):
    if fila["comprador_id"] == usuario["id"]:
        return "COMPRADOR"
    if fila["vendedor_id"] == str(usuario["id"]):
        return "VENDEDOR"
    return None


def oferta_para_responder(oferta_id, usuario):
    fila = database.buscar_oferta(oferta_id)
    if fila is None:
        raise error(404, "La oferta no existe")
    rol = rol_de(usuario, fila)
    if rol is None:
        raise error(403, "Esta oferta no es tuya")
    if fila["estado"] != "PENDIENTE":
        raise error(409, "La oferta ya no esta pendiente")
    if fila["turno"] != rol:
        raise error(403, "Todavia no es tu turno de responder")
    return fila


@router.post("", response_model=Oferta, status_code=status.HTTP_201_CREATED)
def ofertar(request: Request, datos: OfertaNueva,
            usuario: dict = Depends(seguridad.usuario_actual)):
    mensaje = (datos.mensaje or "").strip() or None
    if datos.precio <= 0:
        raise error(400, "El precio tiene que ser mayor a cero")
    if mensaje and len(mensaje) > LARGO_MAXIMO_DE_MENSAJE:
        raise error(400, "El mensaje es demasiado largo")
    if not datos.publicacionId.isdigit():
        raise error(404, "La publicacion no existe")
    publicacion = database.buscar_publicacion(int(datos.publicacionId))
    if publicacion is None:
        raise error(404, "La publicacion no existe")
    if publicacion["vendedor_id"] == str(usuario["id"]):
        raise error(403, "No podes ofertar sobre tu propia publicacion")
    if publicacion["estado_publicacion"] != "ACTIVA":
        raise error(409, "La publicacion ya no esta activa")

    database.vencer_ofertas()
    if database.tiene_oferta_pendiente(publicacion["id"], usuario["id"]):
        raise error(409, "Ya tenes una oferta pendiente en esta publicacion")

    oferta_id = database.crear_oferta(
        publicacion["id"], usuario["id"], publicacion["vendedor_id"],
        datos.precio, mensaje, nuevo_vencimiento(),
    )
    return a_json(request, database.buscar_oferta(oferta_id))


@router.get("/enviadas", response_model=List[Oferta])
def listar_enviadas(request: Request, usuario: dict = Depends(seguridad.usuario_actual)):
    database.vencer_ofertas()
    return [a_json(request, fila) for fila in database.ofertas_enviadas(usuario["id"])]


@router.get("/recibidas", response_model=List[Oferta])
def listar_recibidas(request: Request, usuario: dict = Depends(seguridad.usuario_actual)):
    database.vencer_ofertas()
    return [a_json(request, fila) for fila in database.ofertas_recibidas(usuario["id"])]


@router.get("/{oferta_id}", response_model=Oferta)
def ver_oferta(request: Request, oferta_id: int,
               usuario: dict = Depends(seguridad.usuario_actual)):
    database.vencer_ofertas()
    fila = database.buscar_oferta(oferta_id)
    if fila is None:
        raise error(404, "La oferta no existe")
    if rol_de(usuario, fila) is None:
        raise error(403, "Esta oferta no es tuya")
    return a_json(request, fila)


@router.patch("/{oferta_id}/estado", response_model=Oferta)
def responder(request: Request, oferta_id: int, datos: CambioEstadoOferta,
              usuario: dict = Depends(seguridad.usuario_actual)):
    if datos.estado not in ("ACEPTADA", "RECHAZADA"):
        raise error(400, "El estado tiene que ser ACEPTADA o RECHAZADA")
    database.vencer_ofertas()
    fila = oferta_para_responder(oferta_id, usuario)
    if datos.estado == "ACEPTADA":
        database.aceptar_oferta(fila["id"], fila["publicacion_id"])
    else:
        database.rechazar_oferta(fila["id"])
    return a_json(request, database.buscar_oferta(oferta_id))


@router.patch("/{oferta_id}/precio", response_model=Oferta)
def contraofertar(request: Request, oferta_id: int, datos: Contraoferta,
                  usuario: dict = Depends(seguridad.usuario_actual)):
    if datos.precio <= 0:
        raise error(400, "El precio tiene que ser mayor a cero")
    database.vencer_ofertas()
    fila = oferta_para_responder(oferta_id, usuario)
    turno = "COMPRADOR" if fila["turno"] == "VENDEDOR" else "VENDEDOR"
    database.contraofertar(fila["id"], datos.precio, turno, nuevo_vencimiento())
    return a_json(request, database.buscar_oferta(oferta_id))
