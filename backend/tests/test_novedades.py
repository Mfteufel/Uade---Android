import os
import tempfile
import threading
import time
import unittest
from pathlib import Path

# la base y las fotos de prueba van a una carpeta temporal: nunca se toca ronda.db.
# Tiene que estar antes de importar database, que lee RONDA_DB al cargarse.
CARPETA = tempfile.mkdtemp()
os.environ["RONDA_DB"] = str(Path(CARPETA) / "prueba.db")
os.environ["RONDA_FOTOS"] = str(Path(CARPETA) / "fotos")
os.environ["RONDA_OPERACIONES_DE_PRUEBA"] = "1"

from fastapi.testclient import TestClient

import database
from main import app
from routers import novedades
from seguridad import hashear_password

CLAVE = "clave-de-prueba"
TEXTO_PREGUNTA = "¿Sigue disponible?"

cliente = TestClient(app)
usuarios_creados = [0]


# el prefijo "aviso" evita chocar con los usuarios de las otras pruebas, que comparten la base
def nuevo_usuario(nombre):
    usuarios_creados[0] += 1
    email = "aviso" + str(usuarios_creados[0]) + "@prueba.com"
    usuario = database.crear_usuario(email, nombre, hashear_password(CLAVE), "PALERMO")
    respuesta = cliente.post("/auth/login", json={"email": email, "password": CLAVE})
    return usuario["id"], {"Authorization": "Bearer " + respuesta.json()["token"]}


def consultar(sesion, **parametros):
    return cliente.get("/novedades", params=parametros, headers=sesion)


# lo que llega a la app: la lista de avisos, o vacia si el servidor contesto 204
def avisos_de(sesion, **parametros):
    respuesta = consultar(sesion, **parametros)
    if respuesta.status_code == 204:
        return []
    assert respuesta.status_code == 200, respuesta.text
    return respuesta.json()


class NovedadesTest(unittest.TestCase):

    # con la espera real de 25 s cada prueba sin novedades tardaria lo mismo
    @classmethod
    def setUpClass(cls):
        cls.espera = novedades.SEGUNDOS_DE_ESPERA
        cls.intervalo = novedades.INTERVALO_DE_CONSULTA
        novedades.SEGUNDOS_DE_ESPERA = 0.3
        novedades.INTERVALO_DE_CONSULTA = 0.05

    @classmethod
    def tearDownClass(cls):
        novedades.SEGUNDOS_DE_ESPERA = cls.espera
        novedades.INTERVALO_DE_CONSULTA = cls.intervalo

    def setUp(self):
        self.comprador_id, self.comprador = nuevo_usuario("Comprador")
        self.vendedor_id, self.vendedor = nuevo_usuario("Vendedor")
        self.tercero_id, self.tercero = nuevo_usuario("Tercero")
        self.publicacion_id = database.crear_publicacion(
            "Bicicleta", "Descripcion", 100000, "DEPORTES", "USADO", "PALERMO", self.vendedor_id)

    def preguntar(self, sesion=None):
        respuesta = cliente.post("/publicaciones/" + str(self.publicacion_id) + "/preguntas",
                                 json={"texto": TEXTO_PREGUNTA}, headers=sesion or self.comprador)
        self.assertEqual(201, respuesta.status_code)
        return respuesta.json()

    def ofertar(self, sesion=None, precio=90000):
        respuesta = cliente.post("/ofertas", json={"publicacionId": str(self.publicacion_id),
                                                   "precio": precio}, headers=sesion or self.comprador)
        self.assertEqual(201, respuesta.status_code)
        return respuesta.json()

    def responder(self, oferta_id, estado, sesion):
        respuesta = cliente.patch("/ofertas/" + oferta_id + "/estado",
                                  json={"estado": estado}, headers=sesion)
        self.assertEqual(200, respuesta.status_code)

    def contraofertar(self, oferta_id, precio, sesion):
        respuesta = cliente.patch("/ofertas/" + oferta_id + "/precio",
                                  json={"precio": precio}, headers=sesion)
        self.assertEqual(200, respuesta.status_code)

    # ---- acceso

    def test_sin_token_da_401(self):
        self.assertEqual(401, cliente.get("/novedades").status_code)

    def test_sin_novedades_da_204(self):
        respuesta = consultar(self.vendedor)
        self.assertEqual(204, respuesta.status_code)
        self.assertEqual(b"", respuesta.content)

    def test_desde_que_no_es_un_numero_da_422(self):
        self.assertEqual(422, consultar(self.vendedor, desde="abc").status_code)

    # ---- preguntas

    def test_una_pregunta_avisa_al_vendedor(self):
        self.preguntar()

        recibidos = avisos_de(self.vendedor)

        self.assertEqual(1, len(recibidos))
        aviso = recibidos[0]
        self.assertEqual("PREGUNTA_NUEVA", aviso["tipo"])
        self.assertEqual(str(self.publicacion_id), aviso["publicacionId"])
        self.assertIsNone(aviso["ofertaId"])
        self.assertEqual("VENDEDOR", aviso["rol"])
        self.assertIn("Comprador", aviso["cuerpo"])
        self.assertIn("Bicicleta", aviso["cuerpo"])
        self.assertIsInstance(aviso["id"], str)
        self.assertIsInstance(aviso["creadoEn"], int)

    def test_quien_pregunta_y_un_tercero_no_reciben_el_aviso(self):
        self.preguntar()

        self.assertEqual([], avisos_de(self.comprador))
        self.assertEqual([], avisos_de(self.tercero))

    def test_responder_avisa_a_quien_pregunto(self):
        pregunta = self.preguntar()
        avisos_de(self.vendedor)

        respuesta = cliente.post(
            "/publicaciones/" + str(self.publicacion_id) + "/preguntas/" + pregunta["id"] + "/respuesta",
            json={"texto": "Sí, está disponible"}, headers=self.vendedor)
        self.assertEqual(200, respuesta.status_code)

        recibidos = avisos_de(self.comprador)
        self.assertEqual(1, len(recibidos))
        self.assertEqual("PREGUNTA_RESPONDIDA", recibidos[0]["tipo"])
        self.assertEqual(str(self.publicacion_id), recibidos[0]["publicacionId"])
        self.assertEqual("COMPRADOR", recibidos[0]["rol"])
        self.assertEqual([], avisos_de(self.vendedor))

    def test_una_pregunta_rechazada_no_genera_aviso(self):
        propia = cliente.post("/publicaciones/" + str(self.publicacion_id) + "/preguntas",
                              json={"texto": TEXTO_PREGUNTA}, headers=self.vendedor)
        corta = cliente.post("/publicaciones/" + str(self.publicacion_id) + "/preguntas",
                             json={"texto": "hola"}, headers=self.comprador)

        self.assertEqual(403, propia.status_code)
        self.assertEqual(400, corta.status_code)
        self.assertEqual([], avisos_de(self.vendedor))

    # ---- ofertas

    def test_una_oferta_avisa_al_vendedor(self):
        oferta = self.ofertar()

        recibidos = avisos_de(self.vendedor)

        self.assertEqual(1, len(recibidos))
        aviso = recibidos[0]
        self.assertEqual("OFERTA_RECIBIDA", aviso["tipo"])
        self.assertEqual(oferta["id"], aviso["ofertaId"])
        self.assertEqual(str(self.publicacion_id), aviso["publicacionId"])
        self.assertEqual("VENDEDOR", aviso["rol"])
        self.assertIn("$ 90.000", aviso["cuerpo"])
        self.assertEqual([], avisos_de(self.comprador))

    def test_aceptar_avisa_al_comprador(self):
        oferta = self.ofertar()
        avisos_de(self.vendedor)

        self.responder(oferta["id"], "ACEPTADA", self.vendedor)

        recibidos = avisos_de(self.comprador)
        self.assertEqual(1, len(recibidos))
        self.assertEqual("OFERTA_ACEPTADA", recibidos[0]["tipo"])
        self.assertEqual(oferta["id"], recibidos[0]["ofertaId"])
        self.assertEqual("COMPRADOR", recibidos[0]["rol"])
        # el vendedor es quien acepto: no se avisa a si mismo
        self.assertEqual([], avisos_de(self.vendedor))

    def test_rechazar_avisa_al_comprador(self):
        oferta = self.ofertar()

        self.responder(oferta["id"], "RECHAZADA", self.vendedor)

        recibidos = avisos_de(self.comprador)
        self.assertEqual(1, len(recibidos))
        self.assertEqual("OFERTA_RECHAZADA", recibidos[0]["tipo"])
        self.assertEqual(oferta["id"], recibidos[0]["ofertaId"])
        self.assertEqual("COMPRADOR", recibidos[0]["rol"])

    def test_una_contraoferta_avisa_a_la_otra_parte(self):
        oferta = self.ofertar()
        avisos_de(self.vendedor)

        self.contraofertar(oferta["id"], 95000, self.vendedor)
        al_comprador = avisos_de(self.comprador)
        self.contraofertar(oferta["id"], 92000, self.comprador)
        al_vendedor = avisos_de(self.vendedor)

        self.assertEqual(1, len(al_comprador))
        self.assertEqual("CONTRAOFERTA", al_comprador[0]["tipo"])
        self.assertEqual("COMPRADOR", al_comprador[0]["rol"])
        self.assertIn("$ 95.000", al_comprador[0]["cuerpo"])
        self.assertEqual(1, len(al_vendedor))
        self.assertEqual("CONTRAOFERTA", al_vendedor[0]["tipo"])
        self.assertEqual("VENDEDOR", al_vendedor[0]["rol"])
        self.assertIn("$ 92.000", al_vendedor[0]["cuerpo"])
        self.assertEqual(oferta["id"], al_vendedor[0]["ofertaId"])

    def test_si_el_comprador_acepta_la_contraoferta_se_avisa_al_vendedor(self):
        oferta = self.ofertar()
        self.contraofertar(oferta["id"], 95000, self.vendedor)
        avisos_de(self.vendedor)

        self.responder(oferta["id"], "ACEPTADA", self.comprador)

        recibidos = avisos_de(self.vendedor)
        self.assertEqual(1, len(recibidos))
        self.assertEqual("OFERTA_ACEPTADA", recibidos[0]["tipo"])
        self.assertEqual("VENDEDOR", recibidos[0]["rol"])
        self.assertIn("$ 95.000", recibidos[0]["cuerpo"])

    def test_aceptar_una_oferta_avisa_a_los_otros_compradores(self):
        otro_id, otro = nuevo_usuario("Otro comprador")
        elegida = self.ofertar()
        perdedora = self.ofertar(sesion=otro, precio=80000)
        avisos_de(self.comprador)
        avisos_de(otro)

        self.responder(elegida["id"], "ACEPTADA", self.vendedor)

        ganador = avisos_de(self.comprador)
        perdedor = avisos_de(otro)
        self.assertEqual(["OFERTA_ACEPTADA"], [aviso["tipo"] for aviso in ganador])
        self.assertEqual(1, len(perdedor))
        self.assertEqual("OFERTA_RECHAZADA", perdedor[0]["tipo"])
        self.assertEqual(perdedora["id"], perdedor[0]["ofertaId"])
        self.assertEqual("COMPRADOR", perdedor[0]["rol"])
        self.assertEqual([], avisos_de(self.tercero))

    def test_una_accion_que_falla_no_genera_aviso(self):
        propia = cliente.post("/ofertas", json={"publicacionId": str(self.publicacion_id),
                                                "precio": 90000}, headers=self.vendedor)
        sin_precio = cliente.post("/ofertas", json={"publicacionId": str(self.publicacion_id),
                                                    "precio": 0}, headers=self.comprador)

        self.assertEqual(403, propia.status_code)
        self.assertEqual(400, sin_precio.status_code)
        self.assertEqual([], avisos_de(self.vendedor))

    # ---- entrega y "desde"

    def test_lo_que_ya_se_entrego_no_vuelve(self):
        self.ofertar()

        primera = consultar(self.vendedor)
        segunda = consultar(self.vendedor)

        self.assertEqual(200, primera.status_code)
        self.assertEqual(204, segunda.status_code)

    def test_un_servicio_reiniciado_recibe_solo_lo_que_no_vio(self):
        # la app guarda el ultimo id en memoria: al reiniciarse pregunta sin "desde"
        self.preguntar()
        vistas = avisos_de(self.vendedor)
        self.ofertar()

        despues = avisos_de(self.vendedor)

        self.assertEqual(["PREGUNTA_NUEVA"], [aviso["tipo"] for aviso in vistas])
        self.assertEqual(["OFERTA_RECIBIDA"], [aviso["tipo"] for aviso in despues])

    def test_desde_deja_afuera_lo_que_la_app_ya_mostro(self):
        primera = database.crear_novedad(self.vendedor_id, "PREGUNTA_NUEVA", "A", "a")
        segunda = database.crear_novedad(self.vendedor_id, "PREGUNTA_NUEVA", "B", "b")

        recibidos = avisos_de(self.vendedor, desde=primera)

        self.assertEqual([str(segunda)], [aviso["id"] for aviso in recibidos])

    def test_los_avisos_llegan_en_orden(self):
        self.preguntar()
        self.ofertar()

        recibidos = avisos_de(self.vendedor)

        self.assertEqual(["PREGUNTA_NUEVA", "OFERTA_RECIBIDA"], [aviso["tipo"] for aviso in recibidos])
        self.assertLess(int(recibidos[0]["id"]), int(recibidos[1]["id"]))

    # ---- long polling

    # la conexion queda retenida y se libera apenas pasa algo, sin esperar a que venza el tiempo
    def test_la_espera_termina_cuando_llega_un_aviso(self):
        novedades.SEGUNDOS_DE_ESPERA = 5
        hilo = threading.Timer(0.5, lambda: self.preguntar())
        hilo.start()

        inicio = time.monotonic()
        respuesta = consultar(self.vendedor)
        duracion = time.monotonic() - inicio
        hilo.join()

        self.assertEqual(200, respuesta.status_code)
        self.assertEqual("PREGUNTA_NUEVA", respuesta.json()[0]["tipo"])
        self.assertGreaterEqual(duracion, 0.4)
        self.assertLess(duracion, 3)

    def test_sin_avisos_la_espera_dura_hasta_el_limite(self):
        novedades.SEGUNDOS_DE_ESPERA = 0.6

        inicio = time.monotonic()
        respuesta = consultar(self.vendedor)
        duracion = time.monotonic() - inicio

        self.assertEqual(204, respuesta.status_code)
        self.assertGreaterEqual(duracion, 0.5)

    def tearDown(self):
        novedades.SEGUNDOS_DE_ESPERA = 0.3


if __name__ == "__main__":
    unittest.main()
