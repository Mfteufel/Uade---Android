# API de ofertas (Punto 7)

Contrato entre la app y el backend. La app y el backend lo siguen campo por campo.

- Base: `https://ronda-api-production.up.railway.app`
- Todos los endpoints piden `Authorization: Bearer <token>`.
- El usuario que opera sale siempre del token, nunca del body.
- Ids como string, fechas en milisegundos (igual que `fechaPublicacion`).

## Objeto Oferta

Es el mismo en todas las respuestas.

```json
{
  "id": "7",
  "publicacionId": "12",
  "tituloPublicacion": "Bicicleta rodado 29",
  "fotoPrincipalUrl": "https://ronda-api-production.up.railway.app/fotos/ab12.jpg",
  "compradorId": "3",
  "nombreComprador": "Bruno",
  "vendedorId": "1",
  "nombreVendedor": "Walter",
  "precio": 42000.0,
  "mensaje": "Te la retiro hoy",
  "estado": "PENDIENTE",
  "turno": "VENDEDOR",
  "fechaCreacion": 1726000000000,
  "venceEn": 1726172800000,
  "direccionEntrega": null
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `id`, `publicacionId`, `compradorId`, `vendedorId` | string | |
| `tituloPublicacion` | string | |
| `fotoPrincipalUrl` | string o null | null si la publicación no tiene fotos |
| `nombreComprador`, `nombreVendedor` | string | vienen los dos: la app muestra el de la contraparte |
| `precio` | number | último precio propuesto |
| `mensaje` | string o null | opcional, hasta 200 caracteres |
| `estado` | string | `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `VENCIDA` |
| `turno` | string | `VENDEDOR` o `COMPRADOR`: a quién le toca responder. Solo importa si está `PENDIENTE` |
| `fechaCreacion` | int | milisegundos |
| `venceEn` | int | milisegundos |
| `direccionEntrega` | string o null | solo trae valor si la oferta está `ACEPTADA` |

## Endpoints

| # | Método y ruta | Body | Respuesta |
|---|---|---|---|
| 1 | `POST /ofertas` | `{ "publicacionId": "12", "precio": 42000, "mensaje": "..." }` (`mensaje` opcional) | `201` Oferta |
| 2 | `GET /ofertas/enviadas` | | `200` lista de Oferta: las que hice como comprador, más recientes primero |
| 3 | `GET /ofertas/recibidas` | | `200` lista de Oferta: las que me hicieron como vendedor, más recientes primero |
| 4 | `GET /ofertas/{id}` | | `200` Oferta |
| 5 | `PATCH /ofertas/{id}/estado` | `{ "estado": "ACEPTADA" }` o `{ "estado": "RECHAZADA" }` | `200` Oferta actualizada |
| 6 | `PATCH /ofertas/{id}/precio` | `{ "precio": 40000 }` | `200` Oferta actualizada (contraoferta) |

## Reglas

- El comprador crea la oferta y el turno queda en `VENDEDOR`.
- Solo quien tiene el turno puede aceptar, rechazar o contraofertar.
- Contraofertar cambia el precio, pasa el turno al otro y renueva `venceEn`.
- Vigencia: 6 horas desde que se crea o desde la última contraoferta.
- Vencimiento: cada vez que se lee o se responde una oferta, si `venceEn` ya
  pasó y sigue `PENDIENTE`, queda `VENCIDA`.
- Un comprador puede tener una sola oferta `PENDIENTE` por publicación.
- No se puede ofertar sobre una publicación propia ni sobre una que no esté `ACTIVA`.
- Al aceptar: la publicación pasa a `VENDIDA` y las demás ofertas `PENDIENTE`
  de esa publicación quedan `RECHAZADA`.
- Solo el comprador y el vendedor de una oferta pueden verla o responderla.
- `direccionEntrega` es la que cargó el vendedor al publicar (campo
  `direccionEntrega` de `POST /publicaciones`). Si la publicación no tiene
  dirección, viene null aunque la oferta esté aceptada.
- Una oferta `ACEPTADA` es el registro de la venta: quién compró, a quién, qué
  y a cuánto.

## Errores

El cuerpo es siempre `{ "detail": "mensaje para mostrar" }`.

| Código | Cuándo |
|---|---|
| `400` | precio menor o igual a 0; mensaje de más de 200 caracteres; `estado` distinto de `ACEPTADA` o `RECHAZADA` |
| `401` | sin token, token inválido o sesión vencida |
| `403` | ofertar sobre una publicación propia; ver o responder una oferta ajena; responder cuando no es tu turno |
| `404` | la publicación o la oferta no existe |
| `409` | la publicación no está `ACTIVA`; ya tenés una oferta `PENDIENTE` en esa publicación; la oferta ya no está `PENDIENTE` (incluye vencida) |
| `422` | falta un campo o el tipo no corresponde (lo responde el framework) |
