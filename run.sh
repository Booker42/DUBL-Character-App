#!/usr/bin/env bash
set -euo pipefail
app_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$app_dir"
if [[ -x "$app_dir/.venv/bin/python" ]] && "$app_dir/.venv/bin/python" -c 'from PySide6.QtWidgets import QApplication' 2>/dev/null; then
    exec "$app_dir/.venv/bin/python" -m dubl "$@"
fi
if command -v python3 >/dev/null && python3 -c 'from PySide6.QtWidgets import QApplication' 2>/dev/null; then
    exec python3 -m dubl "$@"
fi
printf '%s\n' 'Первый запуск: устанавливаем Qt в отдельное окружение приложения.'
if ! python3 -m venv "$app_dir/.venv"; then
    printf '%s\n' 'Не удалось создать окружение. Для Debian/Ubuntu установите пакет python3-venv.' 'Для Arch/CachyOS можно установить pyside6 и запустить run.sh повторно.'
    exit 1
fi
"$app_dir/.venv/bin/python" -m pip install -r "$app_dir/requirements.txt"
exec "$app_dir/.venv/bin/python" -m dubl "$@"
