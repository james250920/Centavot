"""Llena la base de la copia de prueba con la bodega de demostración de Rosa (mayo a hoy).

Uso: python3 scripts/demo.py centavot.db [AAAA-MM-DD]

La base tiene que venir de la app después de la bienvenida (aviso aceptado, perfil y régimen), para
que tenga el esquema y el consentimiento. Se reemplazan el perfil, el régimen y todos los movimientos.
Los montos salen de una semilla fija, así que cada corrida da los mismos datos.
"""
import datetime as dt
import random
import sqlite3
import sys

db = sqlite3.connect(sys.argv[1])
hoy = dt.date.fromisoformat(sys.argv[2]) if len(sys.argv) > 2 else dt.date.today()
azar = random.Random(29733)
inicio = hoy.replace(day=1)
for _ in range(5):
    inicio = (inicio - dt.timedelta(days=1)).replace(day=1)

PRODUCTOS = [("Gaseosa", 350), ("Pan", 50), ("Aceite", 990), ("Arroz", 420), ("Leche", 450),
             ("Azúcar", 380), ("Huevos", 60), ("Abarrotes", 4500), ("Detergente", 650), ("Fideos", 280)]
# Lo que se vende en un día, por mes, para que el gráfico de mes a mes suba y baje.
VENTA_DIARIA = {0: 17000, 1: 19000, 2: 16000, 3: 20500, 4: 23500, 5: 51900}

for tabla in ("ingresos", "gastos", "cobros", "contactos", "actividades", "notificaciones", "montos_presupuesto", "partidas", "abonos_meta", "metas"):
    db.execute(f"delete from {tabla}")
db.execute("delete from eventos_uso where tipo != 'CONSENTIMIENTO_PRIVACIDAD'")
db.execute("update perfil set nombre='Rosa', rubro='COMERCIO', ingresoMensualCentimos=150000, "
           "tasaAhorroDecimas=100, modo='NEGOCIO'")
db.execute("delete from regimen")
db.execute("insert into regimen values (0, 'RUS', 'RUS · Categoría 2', 800000, 'MENSUAL')")

ingresos, gastos, eventos = [], [], []
n = 0


def iid(prefijo):
    global n
    n += 1
    return f"{prefijo}{n:05d}"


dia = inicio
while dia <= hoy:
    mes = (dia.year - inicio.year) * 12 + dia.month - inicio.month
    meta = VENTA_DIARIA[min(mes, 5)] * azar.uniform(0.8, 1.2)
    vendido = 0
    while vendido < meta:
        nombre, monto = azar.choice(PRODUCTOS)
        ingresos.append((iid("v"), monto, dia, "NEGOCIO", nombre, 0))
        vendido += monto
    eventos.append(("APERTURA", f"{dia}T08:{azar.randint(0, 59):02d}:00", None))
    eventos.append(("REGISTRO", f"{dia}T19:{azar.randint(0, 59):02d}:00", None))
    if dia.weekday() == 0:
        gastos.append((iid("g"), azar.randint(60000, 90000), dia, "NEGOCIO", "MERCADERIA", "Makro", "Mercadería de la semana"))
    if dia.day == 1:
        gastos.append((iid("g"), 50000, dia, "NEGOCIO", "ALQUILER", None, "Alquiler del local"))
        gastos.append((iid("g"), 9000, dia, "PERSONAL", "EDUCACION", None, "Pensión del colegio"))
    if dia.day == 5:
        gastos.append((iid("g"), azar.randint(8000, 12000), dia, "NEGOCIO", "SERVICIOS", None, "Luz del local"))
    if dia.weekday() == 3:
        gastos.append((iid("g"), 1800, dia, "NEGOCIO", "TRANSPORTE", None, "Mototaxi al mercado"))
    if dia.weekday() == 5:
        ingresos.append((iid("r"), 15000, dia, "PERSONAL", "Para la semana", 1))
        gastos.append((iid("g"), azar.randint(9000, 13000), dia, "PERSONAL", "ALIMENTACION", None, "Mercado"))
    dia += dt.timedelta(days=1)

# Hoy: unas pocas ventas y un gasto, para que Inicio y Movimientos se vean con el día empezado.
ingresos = [i for i in ingresos if i[2] != hoy]
for nombre, monto in [("Gaseosa", 350), ("Abarrotes", 4500), ("Aceite", 990), ("Aceite", 990), ("Aceite", 990),
                      ("Pan", 50), ("Pan", 50), ("Leche", 450), ("Gaseosa", 350)]:
    ingresos.append((iid("v"), monto, hoy, "NEGOCIO", nombre, 0))
gastos.append((iid("g"), 1800, hoy, "NEGOCIO", "TRANSPORTE", None, "Mototaxi al mercado"))
ingresos.append((iid("r"), 3000, hoy, "PERSONAL", "Menú del almuerzo", 1))
gastos.append((iid("g"), 1000, hoy, "PERSONAL", "ALIMENTACION", None, "Pasaje del colegio"))

db.executemany("insert into ingresos (id, montoCentimos, fecha, categoria, descripcion, retiroDelNegocio) values (?,?,?,?,?,?)", [(a, b, str(c), d, e, f) for a, b, c, d, e, f in ingresos])
db.executemany(
    "insert into gastos (id, montoCentimos, fecha, origen, estado, categoria, subcategoria, proveedor, descripcion, corregidoManualmente) "
    "values (?,?,?,'TEXTO','CONFIRMADO',?,?,?,?,0)",
    [(a, b, str(c), d, e, f, g) for a, b, c, d, e, f, g in gastos],
)
db.executemany("insert into eventos_uso (tipo, fechaHora, valor) values (?,?,?)", eventos)

ayer, hace = hoy - dt.timedelta(days=1), lambda d: str(hoy - dt.timedelta(days=d))
db.executemany("insert into contactos values (?,?,?)", [
    ("c1", "Don Lucho", "987654321"), ("c2", "Señora Juana", "912345678"),
    ("c3", "Carmen", "998877665"), ("c4", "Mi prima Ana", "955443322"),
])
db.executemany(
    "insert into cobros (id, contactoId, motivo, montoCentimos, fecha, estado, fechaCobrado, tipo, "
    "adelantoCentimos, abonadoCentimos, categoria) values (?,?,?,?,?,?,?,?,?,?,?)",
    [
        ("k1", "c1", "20 menús para la obra", 30000, str(ayer), "PENDIENTE", None, "PEDIDO", 10000, 0, "NEGOCIO"),
        ("k2", "c2", "Abarrotes de la semana", 8500, hace(9), "PENDIENTE", None, "FIADO", 0, 3000, "NEGOCIO"),
        ("k3", "c3", "Gaseosas para el cumpleaños", 4200, hace(12), "COBRADO", hace(6), "FIADO", 0, 0, "NEGOCIO"),
        ("k4", "c4", "Préstamo para el pasaje", 15000, hace(5), "PENDIENTE", None, "FIADO", 0, 0, "PERSONAL"),
        # Más de un mes sin pagar: deja el aviso de cobro antiguo (P7).
        ("k5", "c3", "Fiado de la quincena", 6000, hace(38), "PENDIENTE", None, "FIADO", 0, 0, "NEGOCIO"),
    ],
)
db.executemany("insert into actividades (descripcion, fechaHora) values (?,?)", [
    ("Registraste un cobro a Señora Juana por S/ 85.00 (\"Abarrotes de la semana\").", f"{hace(9)}T18:20:00"),
    ("Señora Juana abonó S/ 30.00 a \"Abarrotes de la semana\". Le falta S/ 55.00.", f"{hace(6)}T19:05:00"),
    ("Registraste un pedido a Don Lucho por S/ 300.00 con adelanto de S/ 100.00 (\"20 menús para la obra\").",
     f"{ayer}T12:30:00"),
    ("Registraste una venta \"Gaseosa\" de S/ 3.50.", f"{hoy}T08:10:00"),
    ("Registraste una venta \"Abarrotes\" de S/ 45.00.", f"{hoy}T10:15:00"),
])
db.commit()
ventas_mes = db.execute("select sum(montoCentimos) from ingresos where categoria='NEGOCIO' and fecha >= ?",
                        (str(hoy.replace(day=1)),)).fetchone()[0]
compras_mes = db.execute("select sum(montoCentimos) from gastos where categoria='NEGOCIO' and fecha >= ?",
                         (str(hoy.replace(day=1)),)).fetchone()[0]
print(f"{len(ingresos)} ingresos, {len(gastos)} gastos · este mes: ventas S/ {ventas_mes / 100:,.2f} "
      f"({ventas_mes / 8000:.0f} % del tope), compras S/ {compras_mes / 100:,.2f}")
