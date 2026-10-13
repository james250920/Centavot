"""Maneja la copia de prueba en el teléfono con adb.

Uso: ui.py dump | tap TEXTO [=] | tapxy X Y | texto TEXTO | back | swipe Y1 Y2 | has TEXTO | shot ARCHIVO.png
Antes de cada acción comprueba que la copia de prueba esté al frente, para no tocar la app real.
"""
import os, re, subprocess, sys, time, xml.etree.ElementTree as ET

ADB = [os.environ.get("ADB", os.path.expanduser("~/Android/Sdk/platform-tools/adb"))]
PRUEBA = "com.app.centavot.prueba/"


def sh(*a):
    return subprocess.run(ADB + list(a), capture_output=True, text=True).stdout


def al_frente():
    linea = next((l for l in sh("shell", "dumpsys", "activity", "activities").splitlines() if "topResumedActivity" in l), "")
    if PRUEBA not in linea:
        sys.exit(f"ALTO: la copia de prueba no está al frente ({linea.strip()})")


def nodos():
    xml = ""
    for _ in range(3):
        sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = sh("exec-out", "cat", "/sdcard/ui.xml")
        if "<?xml" in xml:
            break
        time.sleep(1)
    for n in ET.fromstring(xml[xml.index("<"):]).iter("node"):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        yield n.get("text") or "", n.get("content-desc") or "", (x1 + x2) // 2, (y1 + y2) // 2


a = sys.argv
if a[1] == "dump":
    for t, d, x, y in nodos():
        if t or d:
            print(f"{x},{y} | {t!r} | {d!r}")
elif a[1] == "has":
    r = [v for t, d, x, y in nodos() for v in (t, d) if v and a[2].lower() in v.lower()]
    print("SÍ" if r else "NO", a[2], r[:5])
elif a[1] == "shot":
    with open(a[2], "wb") as f:
        f.write(subprocess.run(ADB + ["exec-out", "screencap", "-p"], capture_output=True).stdout)
else:
    al_frente()
    if a[1] == "tap":
        exacto = len(a) > 3
        r = [(v, x, y) for t, d, x, y in nodos() for v in (t, d)
             if v and ((v == a[2]) if exacto else a[2].lower() in v.lower())]
        if not r:
            sys.exit(f"NO ENCONTRADO: {a[2]}")
        sh("shell", "input", "tap", str(r[0][1]), str(r[0][2]))
        print("tap", repr(r[0][0]))
    elif a[1] == "tapxy":
        sh("shell", "input", "tap", a[2], a[3])
    elif a[1] == "texto":
        sh("shell", "input", "text", a[2].replace(" ", "%s"))
    elif a[1] == "back":
        sh("shell", "input", "keyevent", "4")
    elif a[1] == "swipe":
        sh("shell", "input", "swipe", "540", a[2], "540", a[3], "400")
    time.sleep(1.3)
