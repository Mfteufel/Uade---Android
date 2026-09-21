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

# el logo del mail se sirve desde este mismo backend; Railway informa el dominio publico
DOMINIO_PUBLICO = os.environ.get("RAILWAY_PUBLIC_DOMAIN", "")

SALTO = chr(10)
FRASE_DE_CIERRE = "Que tengas una linda experiencia 🙂"


def configurado():
    return bool(REMITENTE and (API_KEY or SMTP_HOST))


def enviar_codigo(destinatario, codigo, minutos):
    asunto = "Tu código de Ronda: " + codigo
    texto = SALTO.join([
        "¡Hola!",
        "",
        "Tu código para ingresar a Ronda es: " + codigo,
        "",
        "Vence en " + str(minutos) + " minutos y sirve una sola vez.",
        "",
        FRASE_DE_CIERRE,
        "",
    ])
    html = armar_html(codigo, minutos)
    try:
        if API_KEY:
            enviar_por_api(destinatario, asunto, texto, html)
        else:
            enviar_por_smtp(destinatario, asunto, texto, html)
        print("[MAIL] enviado a " + destinatario, flush=True)
    except Exception as error:
        # si el mail falla el servidor sigue andando; queda registrado para revisarlo
        print("[MAIL] no se pudo enviar a " + destinatario + ": " + repr(error), flush=True)


def armar_html(codigo, minutos):
    logo = ""
    if DOMINIO_PUBLICO:
        logo = (
            '<img src="https://' + DOMINIO_PUBLICO + '/estatico/logo.png" width="120" height="120"'
            ' alt="Ronda" style="display:block;margin:0 auto 16px auto">'
        )
    return (
        '<div style="font-family:Arial,sans-serif;max-width:420px;margin:0 auto;padding:24px;'
        'text-align:center;color:#1F2D3A">'
        + logo +
        '<h2 style="color:#235F8B;margin:0 0 12px 0">¡Hola!</h2>'
        '<p style="margin:0 0 8px 0">Tu código para ingresar a Ronda es:</p>'
        '<p style="font-size:32px;font-weight:bold;letter-spacing:6px;color:#F68342;margin:8px 0 16px 0">'
        + codigo + '</p>'
        '<p style="margin:0 0 20px 0">Vence en ' + str(minutos) + ' minutos y sirve una sola vez.</p>'
        '<p style="color:#49A7AD;font-style:italic;margin:0">' + FRASE_DE_CIERRE + '</p>'
        '</div>'
    )


def enviar_por_api(destinatario, asunto, texto, html):
    cuerpo = json.dumps({
        "sender": {"name": "Ronda", "email": REMITENTE},
        "to": [{"email": destinatario}],
        "subject": asunto,
        "textContent": texto,
        "htmlContent": html,
    }).encode("utf-8")
    pedido = urllib.request.Request(
        API_URL,
        data=cuerpo,
        method="POST",
        headers={"api-key": API_KEY, "content-type": "application/json", "accept": "application/json"},
    )
    with urllib.request.urlopen(pedido, timeout=15) as respuesta:
        respuesta.read()


def enviar_por_smtp(destinatario, asunto, texto, html):
    mensaje = EmailMessage()
    mensaje["From"] = REMITENTE
    mensaje["To"] = destinatario
    mensaje["Subject"] = asunto
    mensaje.set_content(texto)
    mensaje.add_alternative(html, subtype="html")
    with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=15) as servidor:
        if SMTP_USER:
            servidor.starttls()
            servidor.login(SMTP_USER, SMTP_PASS)
        servidor.send_message(mensaje)
