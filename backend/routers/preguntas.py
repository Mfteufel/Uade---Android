from typing import List

from fastapi import APIRouter, Depends, HTTPException, status

import database
import seguridad
from models.pregunta import Pregunta, PreguntaNueva

router = APIRouter(prefix="/publicaciones", tags=["Preguntas"])

# igual que LARGO_MINIMO_PREGUNTA del lado de la app: una pregunta muy corta
# no aporta contexto ("hola", "?") y el cliente ya la bloquea antes de mandarla,
# pero el servidor no puede confiar en eso.
LARGO_MINIMO_PREGUNTA = 10


def error(codigo, mensaje):
    return HTTPException(status_code=codigo, detail=mensaje)


def a_json(fila):
    return {
        "id": str(fila["id"]),
        "publicacionId": str(fila["publicacion_id"]),
        "autorId": str(fila["autor_id"]),
        "autorNombre": database.nombre_de_vendedor(fila["autor_id"]),
        "texto": fila["texto"],
        "fecha": fila["creado_en"],
    }


@router.get("/{publicacion_id}/preguntas", response_model=List[Pregunta])
def listar(publicacion_id: int):
    if database.buscar_publicacion(publicacion_id) is None:
        raise error(404, "La publicacion no existe")
    return [a_json(fila) for fila in database.preguntas_de(publicacion_id)]


@router.post("/{publicacion_id}/preguntas", response_model=Pregunta, status_code=status.HTTP_201_CREATED)
def preguntar(publicacion_id: int, datos: PreguntaNueva,
             usuario: dict = Depends(seguridad.usuario_actual)):
    texto = datos.texto.strip()
    if len(texto) < LARGO_MINIMO_PREGUNTA:
        raise error(400, "La pregunta es demasiado corta")

    publicacion = database.buscar_publicacion(publicacion_id)
    if publicacion is None:
        raise error(404, "La publicacion no existe")
    if publicacion["vendedor_id"] == str(usuario["id"]):
        raise error(403, "No podes preguntarle a tu propia publicacion")
    if publicacion["estado_publicacion"] != "ACTIVA":
        raise error(409, "La publicacion ya no esta activa")

    pregunta_id = database.crear_pregunta(publicacion_id, usuario["id"], texto)
    for fila in database.preguntas_de(publicacion_id):
        if fila["id"] == pregunta_id:
            return a_json(fila)
