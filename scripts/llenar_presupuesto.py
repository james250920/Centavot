"""Escribe montos en "Arma tu presupuesto" de la copia de prueba, buscando cada fila por su nombre.

Uso: python3 -I scripts/llenar_presupuesto.py 'Sueldo=2000' 'Gastos menores (por semana)=40' ...
Hay que estar ya en la pantalla "Arma tu presupuesto". Hace scroll hasta encontrar la fila, toca el
campo del monto (a la derecha), borra lo que hubiera, escribe y cierra el teclado.

Ejemplo personal del documento 04 §10 (cuadra en "Cada sol tiene destino" si luego se agrega
"Regalos y otros=240" en prescindibles):
  Sueldo=2000 Comisiones=350 "Clases o asesorías=500" "Ganancia de inversiones=150"
  "Ahorro del mes=200" "Fondo de emergencia=300" Inversión=100
  "Alquiler o hipoteca=650" "Luz, agua, internet=120" "Pago de deudas=180" Seguros=80
  "Comida y mercado=280" Transporte=110 Salud=40 Educación=40
  "Salidas y restaurantes=300" "Streaming y suscripciones=50" Ropa=150 "Gastos menores (por semana)=40"
Negocio: Ventas=7000 "Colchón del negocio=400" "Alquiler del local=500" "Luz, agua, internet=110"
  "Cuota del Nuevo RUS=50" "Mercadería e insumos=2600" Transporte=80   (sobran S/ 3,260)
"""
import os, subprocess, sys, time

AQUI = os.path.dirname(os.path.abspath(__file__))
UI = ["python3", "-I", os.path.join(AQUI, "ui.py")]
ADB = [os.environ.get("ADB", os.path.expanduser("~/Android/Sdk/platform-tools/adb")), "shell"]


def filas():
    salida = subprocess.run(UI + ["dump"], capture_output=True, text=True).stdout
    for linea in salida.splitlines():
        xy, texto, _ = linea.split(" | ")
        x, y = map(int, xy.split(","))
        yield texto.strip("'"), x, y


for par in sys.argv[1:]:
    nombre, monto = par.split("=")
    for _ in range(8):
        y = next((y for t, x, y in filas() if t == nombre and x < 700), None)
        if y is not None and 280 < y < 1800:
            break
        abajo = y is None or y >= 1800
        subprocess.run(UI + ["swipe", "1500" if abajo else "700", "900" if abajo else "1400"], capture_output=True)
    else:
        sys.exit(f"no encontré {nombre}")
    subprocess.run(UI + ["tapxy", "880", str(y)], capture_output=True)
    subprocess.run(ADB + ["input", "keyevent", "123"])  # al final del texto
    for _ in range(8):
        subprocess.run(ADB + ["input", "keyevent", "67"])  # borrar
    subprocess.run(UI + ["texto", monto], capture_output=True)
    subprocess.run(ADB + ["input", "keyevent", "4"])  # cerrar el teclado
    time.sleep(0.6)
    print("ok", nombre, monto)
