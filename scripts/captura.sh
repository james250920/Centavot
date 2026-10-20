#!/bin/bash
# Captura la pantalla del teléfono sin la barra de estado ni la de navegación, en 540x1050 (jpg).
# Uso: scripts/captura.sh ARCHIVO.jpg
set -e
cd "$(dirname "$0")/.."
T=$(mktemp -d)
python3 -I scripts/ui.py shot "$T/c.png"
python3 -I -c "
import sys
from PIL import Image
Image.open(sys.argv[1]).convert('RGB').crop((0, 80, 1080, 2180)).resize((540, 1050), Image.LANCZOS).save(sys.argv[2], quality=88)
" "$T/c.png" "$1"
rm -r "$T"
echo "captura $1"
