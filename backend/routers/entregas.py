import html
import io
from datetime import datetime, timedelta, timezone
from string import Template

import qrcode
from fastapi import APIRouter, HTTPException
from fastapi.responses import HTMLResponse, Response

import database

router = APIRouter(prefix="/entregas", tags=["Entregas"])

HORA_ARGENTINA = timezone(timedelta(hours=-3))

# La hoja que imprime el vendedor. Los $nombres los completa armar_pagina()
PAGINA = Template("""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Código de entrega · Ronda</title>
<style>
  * { box-sizing: border-box; margin: 0; }
  body {
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
    background: #F7FBF6; color: #191C1A; padding: 32px 16px;
    -webkit-print-color-adjust: exact; print-color-adjust: exact;
  }
  .hoja {
    max-width: 440px; margin: 0 auto; background: #FFFFFF;
    border: 1px solid #C0C9C0; border-radius: 20px; padding: 28px;
    box-shadow: 0 8px 24px rgba(25, 28, 26, .08);
  }
  header { display: flex; align-items: center; gap: 14px; }
  .logo { width: 56px; height: 56px; border-radius: 50%; }
  .marca { font-size: 13px; letter-spacing: .12em; text-transform: uppercase; color: #414942; }
  h1 { font-size: 22px; color: #2E6B4F; }
  .venta { margin-top: 24px; padding-top: 20px; border-top: 1px solid #DCE5DC; }
  .etiqueta { font-size: 12px; letter-spacing: .06em; text-transform: uppercase; color: #717972; }
  .titulo { font-size: 20px; font-weight: 600; margin-top: 4px; }
  .datos { display: flex; gap: 24px; margin-top: 16px; }
  .valor { font-size: 16px; font-weight: 600; margin-top: 2px; }
  .qr { margin-top: 24px; padding: 20px; text-align: center; background: #F1F5F0; border-radius: 16px; }
  .qr img { width: 100%; max-width: 260px; height: auto; background: #FFFFFF; padding: 10px; border-radius: 12px; }
  .estado {
    display: inline-block; margin-top: 14px; padding: 6px 14px; border-radius: 999px;
    background: #B3F0CE; color: #00210F; font-size: 14px; font-weight: 600;
  }
  .pasos { margin-top: 22px; padding-left: 20px; color: #414942; font-size: 15px; line-height: 1.5; }
  .aviso { margin-top: 16px; font-size: 13px; color: #717972; }
  button {
    display: block; width: 100%; margin-top: 24px; padding: 14px; border: 0; border-radius: 999px;
    background: #2E6B4F; color: #FFFFFF; font-size: 16px; font-weight: 600; cursor: pointer;
  }
  button:hover { background: #24573F; }
  .confirmada { margin-top: 24px; padding: 28px 20px; text-align: center; background: #B3F0CE; border-radius: 16px; color: #00210F; }
  .tilde { width: 56px; height: 56px; line-height: 56px; margin: 0 auto 12px; border-radius: 50%; background: #2E6B4F; color: #FFFFFF; font-size: 30px; }
  .confirmada h2 { font-size: 20px; }
  .confirmada p { margin-top: 6px; font-size: 15px; }
  @media print {
    body { background: #FFFFFF; padding: 0; }
    .hoja { box-shadow: none; }
    .no-imprimir { display: none; }
  }
</style>
</head>
<body>
<main class="hoja">
  <header>
    <img class="logo" src="/estatico/logo.png" alt="">
    <div>
      <p class="marca">Ronda</p>
      <h1>Código de entrega</h1>
    </div>
  </header>
  <section class="venta">
    <p class="etiqueta">Artículo</p>
    <p class="titulo">$titulo</p>
    <div class="datos">
      <div><p class="etiqueta">Precio acordado</p><p class="valor">$precio</p></div>
      <div><p class="etiqueta">Comprador</p><p class="valor">$comprador</p></div>
    </div>
  </section>
$cuerpo
</main>
</body>
</html>
""")

PENDIENTE = Template("""  <section class="qr">
    <img src="/entregas/$codigo/qr.png" alt="Código QR de esta entrega">
    <br><span class="estado">Pendiente - esperando entrega</span>
  </section>
  <ol class="pasos">
    <li>Imprimí esta hoja y llevala al punto de entrega.</li>
    <li>Entregá el artículo y mostrale el código al comprador.</li>
    <li>El comprador lo escanea desde la app y la venta queda confirmada.</li>
  </ol>
  <p class="aviso">Este código es solo de esta venta. No lo compartas antes de entregar el artículo.</p>
  <button class="no-imprimir" onclick="window.print()">Imprimir</button>""")

CONFIRMADA = Template("""  <section class="confirmada">
    <div class="tilde">&#10003;</div>
    <h2>Entrega confirmada</h2>
    <p>El comprador confirmó que recibió el artículo el $fecha.</p>
  </section>""")


# el codigo es secreto y solo lo recibe el vendedor: por eso estas dos rutas no piden token
def venta_del_codigo(codigo):
    fila = database.buscar_oferta_por_codigo(codigo)
    if fila is None or fila["estado"] != "ACEPTADA":
        raise HTTPException(status_code=404, detail="El codigo de entrega no existe")
    return fila


def precio_en_pesos(precio):
    # 85500.0 -> "$ 85.500"
    return "$ " + "{:,.0f}".format(precio).replace(",", ".")


def armar_pagina(fila):
    if fila["fecha_entrega"] is None:
        cuerpo = PENDIENTE.substitute(codigo=fila["codigo_entrega"])
    else:
        momento = datetime.fromtimestamp(fila["fecha_entrega"] / 1000, HORA_ARGENTINA)
        cuerpo = CONFIRMADA.substitute(fecha=momento.strftime("%d/%m/%Y a las %H:%M"))
    # titulo y nombre los escribieron usuarios: se escapan para que no puedan meter HTML
    return PAGINA.substitute(
        titulo=html.escape(fila["titulo"]),
        precio=precio_en_pesos(fila["precio"]),
        comprador=html.escape(database.nombre_de_vendedor(fila["comprador_id"])),
        cuerpo=cuerpo,
    )


@router.get("/{codigo}", response_class=HTMLResponse)
def ver_pagina(codigo: str):
    fila = venta_del_codigo(codigo)
    # sin cache: al recargar despues del escaneo tiene que verse la entrega confirmada
    return HTMLResponse(armar_pagina(fila), headers={"Cache-Control": "no-store"})


@router.get("/{codigo}/qr.png")
def ver_imagen(codigo: str):
    venta_del_codigo(codigo)
    qr = qrcode.QRCode(box_size=12, border=2)
    qr.add_data(codigo)
    qr.make(fit=True)
    salida = io.BytesIO()
    qr.make_image().save(salida, format="PNG")
    return Response(content=salida.getvalue(), media_type="image/png")
