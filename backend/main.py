from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

import database
import datos_prueba
from routers.auth import router as router_auth
from routers.publicaciones import CARPETA_FOTOS, router as router_publicaciones

database.inicializar()
datos_prueba.cargar()
CARPETA_FOTOS.mkdir(parents=True, exist_ok=True)

app = FastAPI(title="Ronda API", version="0.2.0")
app.include_router(router_auth)
app.include_router(router_publicaciones)
app.mount("/fotos", StaticFiles(directory=CARPETA_FOTOS), name="fotos")


@app.get("/salud")
def salud():
    return {"estado": "ok"}
