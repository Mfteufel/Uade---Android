import os
import tempfile
import unittest
from pathlib import Path

# la base y las fotos de prueba van a una carpeta temporal: nunca se toca ronda.db.
# Tiene que estar antes de importar database, que lee RONDA_DB al cargarse.
CARPETA = tempfile.mkdtemp()
os.environ["RONDA_DB"] = str(Path(CARPETA) / "prueba.db")
os.environ["RONDA_FOTOS"] = str(Path(CARPETA) / "fotos")
# las operaciones de la demo se piden explicitamente; DatosDePruebaTest las verifica
os.environ["RONDA_OPERACIONES_DE_PRUEBA"] = "1"

from fastapi.testclient import TestClient

import database
import datos_prueba
from main import app
from seguridad import hashear_password

DIA = 24 * 3600 * 1000
MINUTO = 60 * 1000
CLAVE = "clave-de-prueba"

cliente = TestClient(app)
usuarios_creados = [0]


def nuevo_usuario(nombre):
    usuarios_creados[0] += 1
    email = "usuario" + str(usuarios_creados[0]) + "@prueba.com"
    usuario = database.crear_usuario(email, nombre, hashear_password(CLAVE), "PALERMO")
    return usuario["id"], sesion(email, CLAVE)


def sesion(email, clave):
    respuesta = cliente.post("/auth/login", json={"email": email, "password": clave})
    return {"Authorization": "Bearer " + respuesta.json()["token"]}


def hace_dias(dias):
    return database.ahora_en_milisegundos() - dias * DIA


def reputacion(usuario_id):
    return cliente.get("/usuarios/" + str(usuario_id)).json()["reputacion"]


class OperacionesTest(unittest.TestCase):

    # cada prueba arma sus propios usuarios: no depende de los datos de prueba ni de otra prueba
    def setUp(self):
        self.comprador_id, self.comprador = nuevo_usuario("Comprador")
        self.vendedor_id, self.vendedor = nuevo_usuario("Vendedor")
        self.tercero_id, self.tercero = nuevo_usuario("Tercero")

    # recorre el camino real del Punto 7: el comprador oferta y el vendedor acepta
    def venta(self, comprador=None, vendedor_id=None, vendedor=None, titulo="Bicicleta"):
        comprador = comprador or self.comprador
        vendedor_id = vendedor_id or self.vendedor_id
        vendedor = vendedor or self.vendedor
        publicacion_id = database.crear_publicacion(
            titulo, "Descripcion", 100000, "DEPORTES", "USADO", "PALERMO", vendedor_id)
        oferta = cliente.post("/ofertas", json={"publicacionId": str(publicacion_id), "precio": 90000},
                              headers=comprador)
        self.assertEqual(201, oferta.status_code)
        aceptada = cliente.patch("/ofertas/" + oferta.json()["id"] + "/estado",
                                 json={"estado": "ACEPTADA"}, headers=vendedor)
        self.assertEqual(200, aceptada.status_code)
        return oferta.json()["id"]

    # venta con la entrega ya confirmada hace unos dias (para no depender del reloj)
    def entregada(self, titulo="Bicicleta", dias=1, **partes):
        operacion_id = self.venta(titulo=titulo, **partes)
        database.confirmar_entrega(int(operacion_id), hace_dias(dias))
        return operacion_id

    def confirmar(self, operacion_id, sesion_usuario):
        return cliente.post("/operaciones/" + operacion_id + "/entrega", headers=sesion_usuario)

    def historial(self, sesion_usuario, **filtros):
        respuesta = cliente.get("/operaciones", params=filtros, headers=sesion_usuario)
        self.assertEqual(200, respuesta.status_code)
        return respuesta.json()

    def pendientes(self, sesion_usuario):
        respuesta = cliente.get("/operaciones/pendientes", headers=sesion_usuario)
        self.assertEqual(200, respuesta.status_code)
        return respuesta.json()

    def calificar(self, operacion_id, sesion_usuario, estrellas=5, comentario=None):
        return cliente.post("/operaciones/" + operacion_id + "/calificacion",
                            json={"estrellas": estrellas, "comentario": comentario},
                            headers=sesion_usuario)

    # ------------------------------------------------------------------ historial

    def test_sin_sesion_no_hay_historial(self):
        self.assertEqual(401, cliente.get("/operaciones").status_code)
        self.assertEqual(401, cliente.get("/operaciones/pendientes").status_code)

    def test_cada_parte_ve_la_operacion_desde_su_lado(self):
        operacion_id = self.entregada(titulo="Bicicleta rodado 29")

        compra = self.historial(self.comprador)
        venta = self.historial(self.vendedor)

        self.assertEqual([operacion_id], [o["id"] for o in compra])
        self.assertEqual("COMPRA", compra[0]["tipo"])
        self.assertEqual("VENTA", venta[0]["tipo"])
        self.assertEqual("ENTREGADA", compra[0]["estado"])
        self.assertIsNotNone(compra[0]["fechaEntrega"])
        self.assertEqual("Bicicleta rodado 29", compra[0]["articulo"])
        self.assertEqual(90000, compra[0]["montoFinal"])
        self.assertEqual(str(self.vendedor_id), compra[0]["vendedor"]["id"])
        self.assertEqual("Vendedor", compra[0]["vendedor"]["nombre"])
        self.assertEqual("Comprador", venta[0]["comprador"]["nombre"])

    def test_nadie_ve_operaciones_ajenas(self):
        self.venta(titulo="Pendiente")
        self.entregada(titulo="Entregada")
        self.assertEqual([], self.historial(self.tercero))
        self.assertEqual([], self.pendientes(self.tercero))

    def test_una_aceptada_sin_entrega_no_esta_en_el_historial(self):
        operacion_id = self.venta()

        self.assertEqual([], self.historial(self.comprador))
        self.assertEqual([], self.historial(self.vendedor))
        # queda aparte, como pendiente de entrega, para las dos partes
        del_comprador = self.pendientes(self.comprador)
        del_vendedor = self.pendientes(self.vendedor)
        self.assertEqual([operacion_id], [o["id"] for o in del_comprador])
        self.assertEqual("PENDIENTE_ENTREGA", del_comprador[0]["estado"])
        self.assertEqual("COMPRA", del_comprador[0]["tipo"])
        self.assertEqual("VENTA", del_vendedor[0]["tipo"])
        self.assertIsNone(del_comprador[0]["fechaEntrega"])
        self.assertIsNone(del_comprador[0]["calificableHasta"])
        self.assertFalse(del_comprador[0]["puedeCalificar"])

    def test_al_confirmar_la_entrega_pasa_al_historial(self):
        operacion_id = self.venta()

        self.assertEqual(200, self.confirmar(operacion_id, self.comprador).status_code)

        self.assertEqual([operacion_id], [o["id"] for o in self.historial(self.comprador)])
        self.assertEqual([operacion_id], [o["id"] for o in self.historial(self.vendedor)])
        self.assertEqual("ENTREGADA", self.historial(self.vendedor)[0]["estado"])
        self.assertEqual([], self.pendientes(self.comprador))
        self.assertEqual([], self.pendientes(self.vendedor))

    def test_una_oferta_sin_aceptar_no_es_una_operacion(self):
        publicacion_id = database.crear_publicacion(
            "Mesa", "Descripcion", 50000, "HOGAR", "USADO", "PALERMO", self.vendedor_id)
        oferta = cliente.post("/ofertas", json={"publicacionId": str(publicacion_id), "precio": 40000},
                              headers=self.comprador).json()

        self.assertEqual([], self.historial(self.comprador))
        self.assertEqual([], self.pendientes(self.comprador))
        self.assertEqual(404, self.confirmar(oferta["id"], self.comprador).status_code)

    def test_filtro_por_tipo(self):
        compra_id = self.entregada(titulo="Compra")
        venta_id = self.entregada(titulo="Venta", comprador=self.tercero,
                                  vendedor_id=self.comprador_id, vendedor=self.comprador)

        self.assertEqual([compra_id], [o["id"] for o in self.historial(self.comprador, tipo="COMPRA")])
        self.assertEqual([venta_id], [o["id"] for o in self.historial(self.comprador, tipo="VENTA")])
        self.assertEqual(2, len(self.historial(self.comprador)))
        invalido = cliente.get("/operaciones", params={"tipo": "OTRO"}, headers=self.comprador)
        self.assertEqual(400, invalido.status_code)

    def test_filtro_por_fechas_contra_la_entrega(self):
        vieja = self.entregada(titulo="Vieja", dias=10)
        nueva = self.entregada(titulo="Nueva", dias=2)
        self.venta(titulo="Sin entregar")

        self.assertEqual([nueva], [o["id"] for o in self.historial(self.comprador, desde=hace_dias(5))])
        self.assertEqual([vieja], [o["id"] for o in self.historial(self.comprador, hasta=hace_dias(5))])
        # sin filtros tampoco aparece la que no se entrego
        self.assertEqual([nueva, vieja], [o["id"] for o in self.historial(self.comprador)])

    def test_rango_de_fechas_invertido(self):
        respuesta = cliente.get("/operaciones", params={"desde": hace_dias(1), "hasta": hace_dias(5)},
                                headers=self.comprador)
        self.assertEqual(400, respuesta.status_code)

    # ------------------------------------------------------------------ entrega

    def test_el_comprador_confirma_la_entrega(self):
        operacion_id = self.venta()

        respuesta = self.confirmar(operacion_id, self.comprador)

        self.assertEqual(200, respuesta.status_code)
        operacion = respuesta.json()
        self.assertEqual("ENTREGADA", operacion["estado"])
        self.assertIsNotNone(operacion["fechaEntrega"])
        self.assertTrue(operacion["puedeCalificar"])
        # el vendedor tambien puede calificar desde ese momento
        self.assertTrue(self.historial(self.vendedor)[0]["puedeCalificar"])

    def test_el_vendedor_no_puede_confirmar_la_entrega(self):
        operacion_id = self.venta()

        self.assertEqual(403, self.confirmar(operacion_id, self.vendedor).status_code)

        self.assertEqual([], self.historial(self.vendedor))
        self.assertEqual([operacion_id], [o["id"] for o in self.pendientes(self.vendedor)])

    def test_un_tercero_no_puede_confirmar_la_entrega(self):
        operacion_id = self.venta()
        self.assertEqual(403, self.confirmar(operacion_id, self.tercero).status_code)

    def test_la_entrega_se_confirma_una_sola_vez(self):
        operacion_id = self.venta()
        self.confirmar(operacion_id, self.comprador)

        self.assertEqual(409, self.confirmar(operacion_id, self.comprador).status_code)

    def test_operacion_inexistente(self):
        self.assertEqual(404, self.confirmar("999999", self.comprador).status_code)
        self.assertEqual(404, self.calificar("999999", self.comprador).status_code)

    # ------------------------------------------------------------------ calificaciones

    def test_ambas_partes_califican_despues_de_la_entrega(self):
        operacion_id = self.entregada()

        del_comprador = self.calificar(operacion_id, self.comprador, 5, "  Todo perfecto  ")
        del_vendedor = self.calificar(operacion_id, self.vendedor, 4)

        self.assertEqual(201, del_comprador.status_code)
        self.assertEqual(201, del_vendedor.status_code)
        mia = del_comprador.json()["miCalificacion"]
        self.assertEqual(5, mia["estrellas"])
        self.assertEqual("Todo perfecto", mia["comentario"])
        self.assertEqual(str(self.comprador_id), mia["autor"]["id"])
        self.assertEqual(str(self.vendedor_id), mia["calificadoId"])
        self.assertFalse(del_comprador.json()["puedeCalificar"])
        self.assertEqual(str(self.comprador_id), del_vendedor.json()["miCalificacion"]["calificadoId"])

    def test_estrellas_fuera_de_rango(self):
        operacion_id = self.entregada()
        self.assertEqual(400, self.calificar(operacion_id, self.comprador, 0).status_code)
        self.assertEqual(400, self.calificar(operacion_id, self.comprador, 6).status_code)
        self.assertTrue(self.historial(self.comprador)[0]["puedeCalificar"])

    def test_comentario_opcional_y_breve(self):
        operacion_id = self.entregada()

        largo = self.calificar(operacion_id, self.comprador, 5, "x" * 281)
        vacio = self.calificar(operacion_id, self.comprador, 5, "   ")

        self.assertEqual(400, largo.status_code)
        self.assertEqual(201, vacio.status_code)
        self.assertIsNone(vacio.json()["miCalificacion"]["comentario"])

    def test_no_se_califica_antes_de_la_entrega(self):
        operacion_id = self.venta()
        self.assertEqual(409, self.calificar(operacion_id, self.comprador).status_code)
        self.assertEqual(409, self.calificar(operacion_id, self.vendedor).status_code)

    def test_no_se_califica_dos_veces(self):
        operacion_id = self.entregada()
        self.calificar(operacion_id, self.comprador, 5)

        otra_vez = self.calificar(operacion_id, self.comprador, 1)

        self.assertEqual(409, otra_vez.status_code)
        self.assertEqual(5, self.historial(self.comprador)[0]["miCalificacion"]["estrellas"])

    def test_no_se_califica_una_operacion_ajena(self):
        operacion_id = self.entregada()
        self.assertEqual(403, self.calificar(operacion_id, self.tercero).status_code)

    def test_plazo_de_siete_dias_desde_la_entrega(self):
        dentro = self.entregada(titulo="Dentro", dias=6)
        vencida = self.entregada(titulo="Vencida", dias=8)

        self.assertEqual(409, self.calificar(vencida, self.comprador).status_code)
        self.assertEqual(201, self.calificar(dentro, self.comprador).status_code)
        por_id = {o["id"]: o for o in self.historial(self.comprador)}
        self.assertFalse(por_id[vencida]["puedeCalificar"])

    def test_los_siete_dias_parten_de_la_fecha_de_entrega(self):
        # oferta hecha y aceptada hace un mes, pero recibida recien hoy: el plazo corre desde hoy
        operacion_id = self.venta(titulo="Aceptada hace un mes")
        with database.conectar() as conexion:
            conexion.execute("UPDATE ofertas SET fecha_creacion = ?, vence_en = ? WHERE id = ?",
                             (hace_dias(30), hace_dias(30), int(operacion_id)))

        confirmada = self.confirmar(operacion_id, self.comprador).json()

        self.assertEqual(confirmada["fechaEntrega"] + 7 * DIA, confirmada["calificableHasta"])
        self.assertEqual(201, self.calificar(operacion_id, self.comprador).status_code)

        # y se corta a los 7 dias justos de la entrega
        ahora = database.ahora_en_milisegundos()
        casi = self.venta(titulo="Casi siete dias")
        pasada = self.venta(titulo="Siete dias y un minuto")
        database.confirmar_entrega(int(casi), ahora - 7 * DIA + MINUTO)
        database.confirmar_entrega(int(pasada), ahora - 7 * DIA - MINUTO)
        self.assertEqual(201, self.calificar(casi, self.vendedor).status_code)
        self.assertEqual(409, self.calificar(pasada, self.vendedor).status_code)

    # ------------------------------------------------------------------ reputacion

    def test_reputacion_sin_actividad(self):
        self.assertEqual({"promedioEstrellas": 0.0, "cantidadCalificaciones": 0,
                          "operacionesComoComprador": 0, "operacionesComoVendedor": 0},
                         reputacion(self.tercero_id))

    def test_una_aceptada_sin_entrega_no_suma_a_la_reputacion(self):
        self.venta()

        del_comprador = reputacion(self.comprador_id)
        del_vendedor = reputacion(self.vendedor_id)

        self.assertEqual(0, del_comprador["operacionesComoComprador"])
        self.assertEqual(0, del_vendedor["operacionesComoVendedor"])
        self.assertEqual(0, del_vendedor["cantidadCalificaciones"])
        self.assertEqual(0.0, del_vendedor["promedioEstrellas"])

    def test_al_confirmar_la_entrega_suma_al_contador_de_cada_parte(self):
        operacion_id = self.venta()

        self.confirmar(operacion_id, self.comprador)

        del_comprador = reputacion(self.comprador_id)
        del_vendedor = reputacion(self.vendedor_id)
        self.assertEqual(1, del_comprador["operacionesComoComprador"])
        self.assertEqual(0, del_comprador["operacionesComoVendedor"])
        self.assertEqual(1, del_vendedor["operacionesComoVendedor"])
        self.assertEqual(0, del_vendedor["operacionesComoComprador"])
        # confirmar la entrega no inventa calificaciones
        self.assertEqual(0, del_vendedor["cantidadCalificaciones"])

    def test_la_reputacion_sale_de_las_calificaciones_y_las_entregas(self):
        primera = self.entregada(titulo="Primera")
        segunda = self.entregada(titulo="Segunda")
        self.venta(titulo="Sin entregar")
        self.calificar(primera, self.comprador, 5)
        self.calificar(segunda, self.comprador, 2)
        self.calificar(primera, self.vendedor, 4)

        del_vendedor = reputacion(self.vendedor_id)
        propia = cliente.get("/usuarios/yo", headers=self.comprador).json()["reputacion"]

        self.assertEqual(3.5, del_vendedor["promedioEstrellas"])
        self.assertEqual(2, del_vendedor["cantidadCalificaciones"])
        self.assertEqual(2, del_vendedor["operacionesComoVendedor"])
        self.assertEqual(0, del_vendedor["operacionesComoComprador"])
        self.assertEqual(4.0, propia["promedioEstrellas"])
        self.assertEqual(2, propia["operacionesComoComprador"])

    def test_el_promedio_sale_solo_de_las_calificaciones_recibidas(self):
        operacion_id = self.entregada()
        self.calificar(operacion_id, self.comprador, 1)
        self.calificar(operacion_id, self.vendedor, 5)

        # la estrella que dio el comprador no baja su propio promedio
        self.assertEqual(5.0, reputacion(self.comprador_id)["promedioEstrellas"])
        self.assertEqual(1, reputacion(self.comprador_id)["cantidadCalificaciones"])
        self.assertEqual(1.0, reputacion(self.vendedor_id)["promedioEstrellas"])
        self.assertEqual(1, reputacion(self.vendedor_id)["cantidadCalificaciones"])

    # ------------------------------------------------------------------ perfil publico

    def test_calificaciones_visibles_en_el_perfil_publico(self):
        operacion_id = self.entregada(titulo="Guitarra")
        self.calificar(operacion_id, self.comprador, 5, "Excelente")

        respuesta = cliente.get("/usuarios/" + str(self.vendedor_id) + "/calificaciones")

        self.assertEqual(200, respuesta.status_code)
        calificacion = respuesta.json()[0]
        self.assertEqual("Comprador", calificacion["autor"]["nombre"])
        self.assertEqual("Guitarra", calificacion["articulo"])
        self.assertEqual("Excelente", calificacion["comentario"])
        self.assertNotIn("email", calificacion["autor"])

    def test_perfil_sin_calificaciones(self):
        respuesta = cliente.get("/usuarios/" + str(self.tercero_id) + "/calificaciones")
        self.assertEqual(200, respuesta.status_code)
        self.assertEqual([], respuesta.json())

    def test_calificaciones_de_un_usuario_inexistente(self):
        self.assertEqual(404, cliente.get("/usuarios/999999/calificaciones").status_code)


class DatosDePruebaTest(unittest.TestCase):

    # lo que se muestra en la demo local con ana@ronda.com
    def test_ana_tiene_compras_ventas_calificables_vencidas_y_una_pendiente(self):
        ana = sesion("ana@ronda.com", "ronda1234")

        historial = {o["articulo"]: o for o in cliente.get("/operaciones", headers=ana).json()}
        pendientes = [o["articulo"] for o in cliente.get("/operaciones/pendientes", headers=ana).json()]
        perfil = cliente.get("/usuarios/yo", headers=ana).json()

        self.assertEqual(["Juego de ollas"], pendientes)
        self.assertNotIn("Juego de ollas", historial)
        self.assertIsNotNone(historial["Microondas BGH"]["miCalificacion"])
        self.assertEqual("VENTA", historial["Lámpara de pie"]["tipo"])
        self.assertTrue(historial["Lámpara de pie"]["puedeCalificar"])
        self.assertFalse(historial["Campera de abrigo"]["puedeCalificar"])
        self.assertIsNone(historial["Campera de abrigo"]["miCalificacion"])
        self.assertEqual(5.0, perfil["reputacion"]["promedioEstrellas"])
        self.assertEqual(1, perfil["reputacion"]["cantidadCalificaciones"])
        # la compra pendiente de entrega no cuenta
        self.assertEqual(2, perfil["reputacion"]["operacionesComoComprador"])
        self.assertEqual(1, perfil["reputacion"]["operacionesComoVendedor"])

    # sin RONDA_OPERACIONES_DE_PRUEBA (como en Railway) no se vende ninguna publicacion
    def test_sin_la_variable_no_se_cargan_operaciones(self):
        ruta_original = database.RUTA_BASE
        database.RUTA_BASE = Path(CARPETA) / "sin_operaciones.db"
        del os.environ["RONDA_OPERACIONES_DE_PRUEBA"]
        try:
            database.inicializar()
            datos_prueba.cargar()

            self.assertEqual(0, database.contar_entregas())
            self.assertEqual(0, database.contar_calificaciones())
            titulos = [titulo for _, _, titulo, _, _, _ in datos_prueba.OPERACIONES]
            with database.conectar() as conexion:
                estados = [fila["estado_publicacion"] for fila in conexion.execute(
                    "SELECT estado_publicacion FROM publicaciones WHERE titulo IN (?, ?, ?, ?)",
                    titulos)]
            self.assertEqual(["ACTIVA"] * 4, estados)
        finally:
            database.RUTA_BASE = ruta_original
            os.environ["RONDA_OPERACIONES_DE_PRUEBA"] = "1"


if __name__ == "__main__":
    unittest.main()
