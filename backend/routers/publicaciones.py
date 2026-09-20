import os
import uuid
from pathlib import Path
from typing import List, Optional

from fastapi import APIRouter, File, Form, HTTPException, Query, Request, Response, UploadFile, status
from PIL import Image, ImageOps, UnidentifiedImageError

import database
from models.publicacion import (
    CambioEstado,
    EdicionPublicacion,
    MiPublicacion,
    PaginaPublicaciones,
    PublicacionCreada,
    PublicacionDetalle,
)

router = APIRouter(prefix="/publicaciones", tags=["Publicaciones"])

CARPETA_FOTOS = Path(os.environ.get("RONDA_FOTOS", Path(__file__).resolve().parent.parent / "fotos"))
TAMANIO_PAGINA = 8
MAXIMO_DE_FOTOS = 6
LADO_MAXIMO_FOTO = 1024

CATEGORIAS = {"TECNOLOGIA", "HOGAR", "INDUMENTARIA", "DEPORTES", "LIBROS", "INSTRUMENTOS", "BEBES", "OTROS"}
ESTADOS_ARTICULO = {"NUEVO", "COMO_NUEVO", "USADO"}
ESTADOS_PUBLICACION = {"ACTIVA", "PAUSADA", "VENDIDA"}


def error(codigo, mensaje):
    return HTTPException(status_code=codigo, detail=mensaje)


# las fotos se achican y se recomprimen antes de guardarlas: una foto de celular
# pesa varios MB y asi queda en unos cientos de KB
def guardar_foto(foto):
    try:
        imagen = Image.open(foto.file)
        imagen = ImageOps.exif_transpose(imagen).convert("RGB")
    except (UnidentifiedImageError, OSError):
        raise error(400, "Una de las fotos no es una imagen valida")
    imagen.thumbnail((LADO_MAXIMO_FOTO, LADO_MAXIMO_FOTO))
    archivo = uuid.uuid4().hex + ".jpg"
    imagen.save(CARPETA_FOTOS / archivo, "JPEG", quality=80)
    return archivo


def urls_de_fotos(request, publicacion_id):
    base = str(request.base_url)
    return [base + "fotos/" + archivo for archivo in database.fotos_de(publicacion_id)]


def a_json(request, fila):
    fotos = urls_de_fotos(request, fila["id"])
    return {
        "id": str(fila["id"]),
        "titulo": fila["titulo"],
        "descripcion": fila["descripcion"],
        "precio": fila["precio"],
        "categoria": fila["categoria"],
        "estadoArticulo": fila["estado_articulo"],
        "zona": fila["zona"],
        "estadoPublicacion": fila["estado_publicacion"],
        "fechaPublicacion": fila["fecha_publicacion"],
        "vendedorId": fila["vendedor_id"],
        "nombreVendedor": database.nombre_de_vendedor(fila["vendedor_id"]),
        "fotoPrincipalUrl": fotos[0] if fotos else None,
        "fotos": fotos,
    }


@router.get("", response_model=PaginaPublicaciones)
def buscar(
    request: Request,
    q: Optional[str] = None,
    categoria: Optional[str] = None,
    estado: List[str] = Query(default=[]),
    zona: List[str] = Query(default=[]),
    precio_min: Optional[float] = None,
    precio_max: Optional[float] = None,
    vendedorId: Optional[str] = None,
    orden: str = "RECIENTES",
    pagina: int = 0,
):
    if orden not in database.ORDENES:
        raise error(400, "Orden invalido")
    if categoria and categoria not in CATEGORIAS:
        raise error(400, "Categoria invalida")
    if pagina < 0:
        raise error(400, "La pagina no puede ser negativa")
    if precio_min is not None and precio_max is not None and precio_min > precio_max:
        raise error(400, "El precio minimo no puede ser mayor que el maximo")

    filas, total = database.buscar_publicaciones(
        q, categoria, estado, zona, precio_min, precio_max, orden, pagina, TAMANIO_PAGINA,
        vendedorId,
    )
    return {
        "publicaciones": [a_json(request, fila) for fila in filas],
        "pagina": pagina,
        "hayMas": (pagina + 1) * TAMANIO_PAGINA < total,
        "totalResultados": total,
    }


@router.post("", response_model=PublicacionCreada, status_code=status.HTTP_201_CREATED)
def crear(
    vendedorId: str = Form(...),
    titulo: str = Form(...),
    descripcion: str = Form(...),
    categoria: str = Form(...),
    estadoArticulo: str = Form(...),
    precio: float = Form(...),
    zona: str = Form(...),
    fotos: List[UploadFile] = File(default=[]),
):
    if not titulo.strip() or not descripcion.strip():
        raise error(400, "El titulo y la descripcion son obligatorios")
    if categoria not in CATEGORIAS:
        raise error(400, "Categoria invalida")
    if estadoArticulo not in ESTADOS_ARTICULO:
        raise error(400, "Estado del articulo invalido")
    if precio <= 0:
        raise error(400, "El precio tiene que ser mayor a cero")
    if len(fotos) > MAXIMO_DE_FOTOS:
        raise error(400, "Se pueden subir hasta " + str(MAXIMO_DE_FOTOS) + " fotos")

    # primero las fotos: si alguna no es valida no se crea la publicacion
    CARPETA_FOTOS.mkdir(parents=True, exist_ok=True)
    archivos = [guardar_foto(foto) for foto in fotos]

    publicacion_id = database.crear_publicacion(
        titulo.strip(), descripcion.strip(), precio, categoria, estadoArticulo, zona, vendedorId
    )

    for archivo in archivos:
        database.agregar_foto(publicacion_id, archivo)

    return {"id": str(publicacion_id)}


# tiene que ir antes que /{publicacion_id}: si no, "mias" se toma como un id
@router.get("/mias", response_model=List[MiPublicacion])
def listar_mias(request: Request, vendedorId: str):
    return [a_json(request, fila) for fila in database.publicaciones_de(vendedorId)]


@router.get("/{publicacion_id}", response_model=PublicacionDetalle)
def ver_detalle(request: Request, publicacion_id: int):
    fila = database.buscar_publicacion(publicacion_id)
    if fila is None:
        raise error(404, "La publicacion no existe")
    return a_json(request, fila)


@router.put("/{publicacion_id}", response_model=PublicacionDetalle)
def editar(request: Request, publicacion_id: int, datos: EdicionPublicacion):
    fila = database.buscar_publicacion(publicacion_id)
    if fila is None:
        raise error(404, "La publicacion no existe")
    titulo = fila["titulo"] if datos.titulo is None else datos.titulo.strip()
    descripcion = fila["descripcion"] if datos.descripcion is None else datos.descripcion.strip()
    precio = fila["precio"] if datos.precio is None else datos.precio
    if not titulo or not descripcion:
        raise error(400, "El titulo y la descripcion son obligatorios")
    if precio <= 0:
        raise error(400, "El precio tiene que ser mayor a cero")
    database.actualizar_publicacion(publicacion_id, titulo, descripcion, precio)
    return a_json(request, database.buscar_publicacion(publicacion_id))


@router.patch("/{publicacion_id}/estado", status_code=status.HTTP_204_NO_CONTENT)
def cambiar_estado(publicacion_id: int, datos: CambioEstado):
    if datos.estadoPublicacion not in ESTADOS_PUBLICACION:
        raise error(400, "Estado de publicacion invalido")
    if not database.cambiar_estado_publicacion(publicacion_id, datos.estadoPublicacion):
        raise error(404, "La publicacion no existe")
    return Response(status_code=status.HTTP_204_NO_CONTENT)
