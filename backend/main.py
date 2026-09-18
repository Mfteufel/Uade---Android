from fastapi import FastAPI

import database
from routers.auth import router as router_auth

database.inicializar()

app = FastAPI(title="Ronda API", version="0.1.0")
app.include_router(router_auth)


@app.get("/salud")
def salud():
    return {"estado": "ok"}
