from fastapi import APIRouter, Depends, HTTPException

import database
import seguridad
from models.perfil import Perfil, PerfilEdicion
from models.usuario import es_email_valido

router = APIRouter(prefix="/usuarios", tags=["Usuarios"])


def a_json(usuario, publico):
    alta = database.desde_texto(usuario["creado_en"])
    return {
        "id": str(usuario["id"]),
        "nombre": usuario["nombre"],
        # el perfil publico no muestra los datos de contacto
        "email": None if publico else usuario["email"],
        "telefono": None if publico else usuario["telefono"],
        "zona": usuario["zona"],
        "fechaAlta": int(alta.timestamp() * 1000),
        "reputacion": {
            "promedioEstrellas": 0.0,
            "operacionesComoComprador": 0,
            "operacionesComoVendedor": 0,
        },
    }


@router.get("/yo", response_model=Perfil)
def ver_mi_perfil(usuario: dict = Depends(seguridad.usuario_actual)):
    return a_json(usuario, publico=False)


@router.put("/yo", response_model=Perfil)
def editar_mi_perfil(datos: PerfilEdicion, usuario: dict = Depends(seguridad.usuario_actual)):
    nombre = datos.nombre.strip()
    email = datos.email.strip().lower()
    if not nombre:
        raise HTTPException(status_code=400, detail="El nombre es obligatorio")
    if not es_email_valido(email):
        raise HTTPException(status_code=400, detail="El email no tiene un formato valido")

    otro = database.buscar_usuario_por_email(email)
    if otro is not None and otro["id"] != usuario["id"]:
        raise HTTPException(status_code=409, detail="Ese email ya lo usa otra cuenta")

    actualizado = database.actualizar_usuario(
        usuario["id"], nombre, email, (datos.telefono or "").strip() or None, datos.zona
    )
    return a_json(actualizado, publico=False)


@router.get("/{usuario_id}", response_model=Perfil)
def ver_perfil_publico(usuario_id: int):
    usuario = database.buscar_usuario_por_id(usuario_id)
    if usuario is None:
        raise HTTPException(status_code=404, detail="El usuario no existe")
    return a_json(usuario, publico=True)
