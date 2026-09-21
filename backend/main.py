from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

import database
import datos_prueba
from routers.auth import router as router_auth
from routers.busquedas import router as router_busquedas
from routers.favoritos import router as router_favoritos
from routers.publicaciones import CARPETA_FOTOS, router as router_publicaciones
from routers.usuarios import router as router_usuarios

database.inicializar()
datos_prueba.cargar()
CARPETA_FOTOS.mkdir(parents=True, exist_ok=True)

app = FastAPI(title="Ronda API", version="0.3.0")
app.include_router(router_auth)
app.include_router(router_publicaciones)
app.include_router(router_usuarios)
app.include_router(router_favoritos)
app.include_router(router_busquedas)
app.mount("/fotos", StaticFiles(directory=CARPETA_FOTOS), name="fotos")


@app.get("/salud")
def salud():
    return {"estado": "ok"}
