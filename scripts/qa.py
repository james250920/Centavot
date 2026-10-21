"""Mini motor de pruebas para la copia de prueba: acciones con scripts/ui.py y comprobaciones en pantalla.

Uso, desde un script de Python:  sys.path.insert(0, "scripts"); from qa import *
  caso("Registrar una venta", lambda: (tap("Venta", True), espera("¿Cuánto vendiste?")))
Cada caso queda en qa_resultados.jsonl (o en la ruta de QA_RESULTADOS) con ok/nota, y falla también si
logcat registra un cierre inesperado. El recorrido completo del 20 de octubre de 2026 está en reporte.md.
"""
import json, os, subprocess, sys, time

APP = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UI = ["python3", "-I", f"{APP}/scripts/ui.py"]
ADB = [os.path.expanduser("~/Android/Sdk/platform-tools/adb"), "-s", "R58X70ZMQQN"]
REG = os.environ.get("QA_RESULTADOS", "qa_resultados.jsonl")


def ui(*a):
    r = subprocess.run(UI + list(a), capture_output=True, text=True)
    if "ALTO" in r.stdout + r.stderr:
        sys.exit("ALTO: la copia de prueba no está al frente")
    return r.stdout + r.stderr


def adb(*a):
    return subprocess.run(ADB + list(a), capture_output=True, text=True).stdout


def textos():
    out = ui("dump")
    res = []
    for l in out.splitlines():
        p = l.split(" | ")
        if len(p) == 3:
            res += [p[1].strip("'"), p[2].strip("'")]
    return [t for t in res if t]


def hay(t):
    return any(t.lower() in x.lower() for x in textos())


def tap(t, exacto=False):
    out = ui("tap", t, "=") if exacto else ui("tap", t)
    if "NO ENCONTRADO" in out:
        raise AssertionError(f"no encontré el botón «{t}»")


def escribir(t): ui("texto", t)
def atras(): ui("back")
def ocultar_teclado(): adb("shell", "input", "keyevent", "4"); time.sleep(0.6)
def desliza(a, b): ui("swipe", str(a), str(b))


def buscar(t, max_swipes=6, abajo=True):
    for _ in range(max_swipes):
        if hay(t):
            return True
        desliza(1500, 800) if abajo else desliza(800, 1500)
    return hay(t)


def caso(nombre, f):
    adb("logcat", "-c")
    try:
        f()
        ok, nota = True, ""
    except AssertionError as e:
        ok, nota = False, str(e)
    except SystemExit as e:
        ok, nota = False, str(e)
    fatal = "FATAL EXCEPTION" in adb("logcat", "-d")
    if fatal:
        ok, nota = False, (nota + " · CIERRE INESPERADO en logcat").strip(" ·")
    with open(REG, "a") as fh:
        fh.write(json.dumps({"caso": nombre, "ok": ok, "nota": nota}, ensure_ascii=False) + "\n")
    print(("✔" if ok else "✘"), nombre, nota)


def espera(t, s=6):
    for _ in range(int(s / 1.5) + 1):
        if hay(t):
            return
        time.sleep(1.5)
    raise AssertionError(f"no apareció «{t}»")
