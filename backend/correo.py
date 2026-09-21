import json
import os
import smtplib
import urllib.request
from email.message import EmailMessage

REMITENTE = os.environ.get("RONDA_MAIL_FROM", "")

# opcion 1: servicio de envio por HTTPS (funciona aunque el hosting bloquee SMTP)
API_KEY = os.environ.get("RONDA_MAIL_API_KEY", "")
API_URL = os.environ.get("RONDA_MAIL_API_URL", "https://api.brevo.com/v3/smtp/email")

# opcion 2: servidor SMTP clasico
SMTP_HOST = os.environ.get("RONDA_SMTP_HOST", "")
SMTP_PORT = int(os.environ.get("RONDA_SMTP_PORT", "587"))
SMTP_USER = os.environ.get("RONDA_SMTP_USER", "")
SMTP_PASS = os.environ.get("RONDA_SMTP_PASS", "")


def configurado():
    return bool(REMITENTE and (API_KEY or SMTP_HOST))


def enviar_codigo(destinatario, codigo, minutos):
    asunto = "Tu código de Ronda: " + codigo
    texto = (
        "Hola,\n\nTu código para ingresar a Ronda es: " + codigo + "\n\n"
        "Vence en " + str(minutos) + " minutos y sirve una sola vez.\n"
        "Si no lo pediste, ignorá este mensaje.\n"
    )
    try:
        if API_KEY:
            enviar_por_api(destinatario, asunto, texto)
        else:
            enviar_por_smtp(destinatario, asunto, texto)
        print("[MAIL] enviado a " + destinatario, flush=True)
    except Exception as error:
        # si el mail falla el servidor sigue andando; queda registrado para revisarlo
        print("[MAIL] no se pudo enviar a " + destinatario + ": " + repr(error), flush=True)


def enviar_por_api(destinatario, asunto, texto):
    cuerpo = json.dumps({
        "sender": {"name": "Ronda", "email": REMITENTE},
        "to": [{"email": destinatario}],
        "subject": asunto,
        "textContent": texto,
    }).encode("utf-8")
    pedido = urllib.request.Request(
        API_URL,
        data=cuerpo,
        method="POST",
        headers={"api-key": API_KEY, "content-type": "application/json", "accept": "application/json"},
    )
    with urllib.request.urlopen(pedido, timeout=15) as respuesta:
        respuesta.read()


def enviar_por_smtp(destinatario, asunto, texto):
    mensaje = EmailMessage()
    mensaje["From"] = REMITENTE
    mensaje["To"] = destinatario
    mensaje["Subject"] = asunto
    mensaje.set_content(texto)
    with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=15) as servidor:
        if SMTP_USER:
            servidor.starttls()
            servidor.login(SMTP_USER, SMTP_PASS)
        servidor.send_message(mensaje)
