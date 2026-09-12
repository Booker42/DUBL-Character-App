#!/usr/bin/env bash
set -euo pipefail
app_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
python3 - "$app_dir" <<'PY'
import os,sys,shutil
from pathlib import Path
source=Path(sys.argv[1]);base=Path(os.environ.get('XDG_DATA_HOME',str(Path.home()/'.local/share')));target=base/'dubl-app'
if source.resolve()!=target.resolve():
    target.mkdir(parents=True,exist_ok=True)
    for name in ['dubl','data','tests']:
        shutil.copytree(source/name,target/name,dirs_exist_ok=True,ignore=shutil.ignore_patterns('__pycache__','*.pyc'))
    for name in ['run.sh','install.sh','requirements.txt','README.md','RULES_AUDIT.md','REDESIGN.md']:
        shutil.copy2(source/name,target/name)
(target/'run.sh').chmod(0o755)
menu=base/'applications';menu.mkdir(parents=True,exist_ok=True)
def quote(s):return '"'+str(s).replace('\\','\\\\').replace('"','\\"').replace('`','\\`').replace('$','\\$').replace('%','%%')+'"'
# Terminal is used for first-run dependency installation and clear startup errors.
(menu/'dubl-character.desktop').write_text('[Desktop Entry]\nType=Application\nName=Дубль — лист персонажа\nComment=Нативный лист персонажа Дубль 3.69\nExec='+quote(target/'run.sh')+'\nIcon=accessories-text-editor\nTerminal=true\nCategories=Game;Utility;\n',encoding='utf-8')
print('Установлено. Запуск: Дубль — лист персонажа в меню приложений.')
print('Персонажи хранятся отдельно от файлов программы.')
PY
