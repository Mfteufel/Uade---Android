import database
from seguridad import hashear_password

CLAVE = "ronda1234"

USUARIOS = [
    ("walter@uade.edu.ar", "Walter", "CABALLITO"),
    ("ana@ronda.com", "Ana", "PALERMO"),
    ("bruno@ronda.com", "Bruno", "QUILMES"),
    ("carla@ronda.com", "Carla", "SAN_ISIDRO"),
]

# titulo, descripcion, precio, categoria, estado, zona, vendedor (posicion en USUARIOS), hace cuantas horas
PUBLICACIONES = [
    ("Celular Motorola G54", "Con caja y cargador. Pantalla sin marcas.", 280000, "TECNOLOGIA", "COMO_NUEVO", "PALERMO", 1, 2),
    ("Auriculares Bluetooth JBL", "Sin uso, regalo repetido.", 65000, "TECNOLOGIA", "NUEVO", "CABALLITO", 0, 5),
    ("Notebook HP 14 pulgadas", "8GB de RAM y disco sólido. Batería dura 3 horas.", 420000, "TECNOLOGIA", "USADO", "QUILMES", 2, 20),
    ("Tablet Samsung A8", "Poco uso, incluye funda.", 190000, "TECNOLOGIA", "COMO_NUEVO", "BELGRANO", 3, 30),
    ("Silla de escritorio", "Regulable en altura, tapizado gris.", 85000, "HOGAR", "USADO", "ALMAGRO", 0, 8),
    ("Lámpara de pie", "Metal negro, 1,60 de alto.", 40000, "HOGAR", "COMO_NUEVO", "RECOLETA", 1, 50),
    ("Microondas BGH", "Funciona perfecto. Lo vendo por mudanza.", 95000, "HOGAR", "USADO", "AVELLANEDA", 2, 72),
    ("Juego de ollas", "Cinco piezas de acero, sin estrenar.", 78000, "HOGAR", "NUEVO", "SAN_ISIDRO", 3, 96),
    ("Campera de abrigo", "Talle L, azul. Una temporada de uso.", 55000, "INDUMENTARIA", "USADO", "FLORES", 0, 12),
    ("Zapatillas Adidas", "Talle 41, en caja.", 110000, "INDUMENTARIA", "NUEVO", "NUNEZ", 1, 100),
    ("Bicicleta rodado 26", "Cambios Shimano, cubiertas nuevas.", 230000, "DEPORTES", "USADO", "TIGRE", 3, 26),
    ("Pesas rusas 8 y 12 kg", "El par. Casi sin uso.", 60000, "DEPORTES", "COMO_NUEVO", "BOEDO", 2, 120),
    ("Raqueta de tenis Wilson", "Con funda y encordado nuevo.", 90000, "DEPORTES", "COMO_NUEVO", "VICENTE_LOPEZ", 3, 150),
    ("Saga El Señor de los Anillos", "Tres tomos, edición de bolsillo.", 35000, "LIBROS", "USADO", "CABALLITO", 0, 40),
    ("Libro de Cálculo de Stewart", "Séptima edición, sin subrayar.", 48000, "LIBROS", "COMO_NUEVO", "VILLA_CRESPO", 1, 170),
    ("Guitarra eléctrica Squier", "Incluye cable y funda.", 310000, "INSTRUMENTOS", "USADO", "BARRACAS", 2, 60),
    ("Ukelele soprano", "Nuevo, con afinador.", 42000, "INSTRUMENTOS", "NUEVO", "PALERMO", 1, 200),
    ("Cochecito de bebé", "Plegable, con cubre lluvia.", 150000, "BEBES", "USADO", "LOMAS_DE_ZAMORA", 2, 90),
    ("Silla para auto", "Grupo 1-2-3. Nunca tuvo un choque.", 120000, "BEBES", "COMO_NUEVO", "SAN_ISIDRO", 3, 220),
    ("Valija grande", "Rígida, cuatro ruedas.", 70000, "OTROS", "USADO", "BELGRANO", 0, 240),
]


def cargar():
    ids = []
    for email, nombre, zona in USUARIOS:
        usuario = database.buscar_usuario_por_email(email)
        if usuario is None:
            usuario = database.crear_usuario(email, nombre, hashear_password(CLAVE), zona)
        ids.append(usuario["id"])

    if database.contar_publicaciones() > 0:
        return

    ahora = database.ahora_en_milisegundos()
    for titulo, descripcion, precio, categoria, estado, zona, vendedor, horas in PUBLICACIONES:
        database.crear_publicacion(
            titulo, descripcion, precio, categoria, estado, zona,
            ids[vendedor], ahora - horas * 3600 * 1000,
        )
