"""Punto 11: avisos para el usuario cuando pasa algo que lo involucra.

Cada funcion arma el texto y deja la novedad guardada; la app las recibe por
long polling en GET /novedades. "rol" es el rol que tiene el DESTINATARIO en la
oferta (COMPRADOR o VENDEDOR): con eso la app sabe si abrir las ofertas
enviadas o las recibidas.
"""

import database

PREGUNTA_NUEVA = "PREGUNTA_NUEVA"
PREGUNTA_RESPONDIDA = "PREGUNTA_RESPONDIDA"
OFERTA_RECIBIDA = "OFERTA_RECIBIDA"
OFERTA_ACEPTADA = "OFERTA_ACEPTADA"
OFERTA_RECHAZADA = "OFERTA_RECHAZADA"
CONTRAOFERTA = "CONTRAOFERTA"


def precio_en_pesos(precio):
    # 85500.0 -> "$ 85.500"
    return "$ " + "{:,.0f}".format(precio).replace(",", ".")


# quien recibe el aviso de una oferta es siempre la otra parte de quien actuo
def _otra_parte(oferta, usuario):
    if oferta["comprador_id"] == usuario["id"]:
        return oferta["vendedor_id"], "VENDEDOR"
    return str(oferta["comprador_id"]), "COMPRADOR"


def pregunta_nueva(publicacion, autor):
    database.crear_novedad(
        publicacion["vendedor_id"], PREGUNTA_NUEVA,
        "Nueva pregunta",
        autor["nombre"] + " preguntó en " + publicacion["titulo"],
        publicacion_id=publicacion["id"], rol="VENDEDOR",
    )


def pregunta_respondida(publicacion, pregunta):
    database.crear_novedad(
        pregunta["autor_id"], PREGUNTA_RESPONDIDA,
        "Respondieron tu pregunta",
        "El vendedor respondió tu pregunta en " + publicacion["titulo"],
        publicacion_id=publicacion["id"], rol="COMPRADOR",
    )


def oferta_recibida(oferta):
    database.crear_novedad(
        oferta["vendedor_id"], OFERTA_RECIBIDA,
        "Recibiste una oferta",
        database.nombre_de_vendedor(oferta["comprador_id"]) + " ofreció "
        + precio_en_pesos(oferta["precio"]) + " por " + oferta["titulo"],
        publicacion_id=oferta["publicacion_id"], oferta_id=oferta["id"], rol="VENDEDOR",
    )


def oferta_aceptada(oferta, usuario):
    destinatario, rol = _otra_parte(oferta, usuario)
    database.crear_novedad(
        destinatario, OFERTA_ACEPTADA,
        "Oferta aceptada",
        usuario["nombre"] + " aceptó la oferta de " + precio_en_pesos(oferta["precio"])
        + " por " + oferta["titulo"],
        publicacion_id=oferta["publicacion_id"], oferta_id=oferta["id"], rol=rol,
    )


def oferta_rechazada(oferta, usuario):
    destinatario, rol = _otra_parte(oferta, usuario)
    database.crear_novedad(
        destinatario, OFERTA_RECHAZADA,
        "Oferta rechazada",
        usuario["nombre"] + " rechazó la oferta de " + precio_en_pesos(oferta["precio"])
        + " por " + oferta["titulo"],
        publicacion_id=oferta["publicacion_id"], oferta_id=oferta["id"], rol=rol,
    )


# el vendedor acepto la oferta de otro: las que estaban pendientes se rechazan solas
def oferta_rechazada_por_otra_venta(oferta):
    database.crear_novedad(
        oferta["comprador_id"], OFERTA_RECHAZADA,
        "Oferta rechazada",
        "El vendedor aceptó otra oferta por " + oferta["titulo"],
        publicacion_id=oferta["publicacion_id"], oferta_id=oferta["id"], rol="COMPRADOR",
    )


# "oferta" tiene que ser la fila ya actualizada: trae el precio nuevo
def contraoferta(oferta, usuario):
    destinatario, rol = _otra_parte(oferta, usuario)
    database.crear_novedad(
        destinatario, CONTRAOFERTA,
        "Recibiste una contraoferta",
        usuario["nombre"] + " propone " + precio_en_pesos(oferta["precio"])
        + " por " + oferta["titulo"],
        publicacion_id=oferta["publicacion_id"], oferta_id=oferta["id"], rol=rol,
    )
