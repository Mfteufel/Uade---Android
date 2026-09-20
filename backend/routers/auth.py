import hmac
import secrets
from datetime import timedelta

from fastapi import APIRouter, Depends, HTTPException, status

import database
import seguridad
from models.usuario import (
    RespuestaOtp,
    RespuestaToken,
    SolicitudLogin,
    SolicitudOtp,
    SolicitudRegistro,
    UsuarioPublico,
    VerificacionOtp,
    es_email_valido,
)

router = APIRouter(prefix="/auth", tags=["Autenticacion"])

MINUTOS_VALIDEZ_OTP = 5
SEGUNDOS_ENTRE_REENVIOS = 30
LARGO_MINIMO_DE_CLAVE = 6
# en desarrollo el codigo vuelve en la respuesta; en produccion va por mail
DEVOLVER_OTP_EN_RESPUESTA = True


def normalizar_email(email):
    email = (email or "").strip().lower()
    if not es_email_valido(email):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="El email no tiene un formato valido",
        )
    return email


def emitir_codigo(email):
    database.invalidar_codigos(email)
    codigo = "%06d" % secrets.randbelow(1000000)
    database.guardar_codigo(
        email, codigo, database.ahora() + timedelta(minutes=MINUTOS_VALIDEZ_OTP)
    )
    print("[OTP] " + codigo + " para " + email, flush=True)
    return RespuestaOtp(
        mensaje="Te enviamos un codigo a " + email,
        expira_en_segundos=MINUTOS_VALIDEZ_OTP * 60,
        codigo=codigo if DEVOLVER_OTP_EN_RESPUESTA else None,
    )


def respuesta_con_token(usuario):
    return RespuestaToken(
        token=seguridad.crear_token(usuario["id"], usuario["email"]),
        usuario=UsuarioPublico(
            id=usuario["id"],
            email=usuario["email"],
            nombre=usuario["nombre"],
            zona=usuario["zona"],
        ),
    )


@router.post("/otp", response_model=RespuestaOtp)
def solicitar_codigo(datos: SolicitudOtp):
    email = normalizar_email(datos.email)
    if database.buscar_usuario_por_email(email) is None:
        database.crear_usuario(email=email, nombre=email.split("@")[0])
    return emitir_codigo(email)


@router.post("/otp/reenviar", response_model=RespuestaOtp)
def reenviar_codigo(datos: SolicitudOtp):
    email = normalizar_email(datos.email)
    if database.buscar_usuario_por_email(email) is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No hay ningun pedido de codigo para ese email",
        )
    ultimo = database.ultimo_codigo(email)
    if ultimo is not None:
        pasaron = (database.ahora() - database.desde_texto(ultimo["creado_en"])).total_seconds()
        if pasaron < SEGUNDOS_ENTRE_REENVIOS:
            faltan = int(SEGUNDOS_ENTRE_REENVIOS - pasaron)
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="Espera " + str(faltan) + " segundos para pedir otro codigo",
                headers={"Retry-After": str(faltan)},
            )
    return emitir_codigo(email)


@router.post("/otp/verificar", response_model=RespuestaToken)
def verificar_codigo(datos: VerificacionOtp):
    email = normalizar_email(datos.email)
    registro = database.ultimo_codigo(email)
    # mismo mensaje para todos los casos, asi no se revela cual era el codigo
    rechazo = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="El codigo es incorrecto o ya vencio",
    )
    if registro is None or registro["usado"]:
        raise rechazo
    if database.desde_texto(registro["expira_en"]) < database.ahora():
        raise rechazo
    if not hmac.compare_digest(registro["codigo"], (datos.codigo or "").strip()):
        raise rechazo
    # un codigo se usa una sola vez
    database.marcar_codigo_usado(registro["id"])
    return respuesta_con_token(database.buscar_usuario_por_email(email))


@router.post("/registro", response_model=RespuestaToken, status_code=status.HTTP_201_CREATED)
def registrar(datos: SolicitudRegistro):
    email = normalizar_email(datos.email)
    nombre = (datos.nombre or "").strip()
    if not nombre:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="El nombre es obligatorio")
    if len(datos.password or "") < LARGO_MINIMO_DE_CLAVE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="La contrasena tiene que tener al menos " + str(LARGO_MINIMO_DE_CLAVE) + " caracteres",
        )
    # si el email ya existe nunca se le pisa la clave: cualquiera podria robar una cuenta
    if database.buscar_usuario_por_email(email) is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Ese email ya tiene una cuenta")
    usuario = database.crear_usuario(email, nombre, seguridad.hashear_password(datos.password))
    return respuesta_con_token(usuario)


@router.post("/login", response_model=RespuestaToken)
def iniciar_sesion(datos: SolicitudLogin):
    email = normalizar_email(datos.email)
    usuario = database.buscar_usuario_por_email(email)
    # mismo mensaje si el usuario no existe o la clave esta mal: no revela que emails hay
    if usuario is None or not seguridad.verificar_password(datos.password, usuario["password_hash"]):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Email o contrasena incorrectos",
        )
    return respuesta_con_token(usuario)


@router.get("/sesion", response_model=UsuarioPublico)
def ver_sesion(usuario: dict = Depends(seguridad.usuario_actual)):
    return UsuarioPublico(
        id=usuario["id"],
        email=usuario["email"],
        nombre=usuario["nombre"],
        zona=usuario["zona"],
    )
