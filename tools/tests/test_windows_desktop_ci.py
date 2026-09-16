from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESKTOP_BUILD = ROOT / 'desktopApp/build.gradle.kts'
WINDOWS_WORKFLOW = ROOT / '.github/workflows/windows-desktop.yml'
LINUX_WORKFLOW = ROOT / '.github/workflows/linux-appimage.yml'


def read(path: Path) -> str:
    assert path.exists(), f'missing {path}'
    return path.read_text(encoding='utf-8')


def test_compose_desktop_declares_both_windows_installers():
    text = read(DESKTOP_BUILD)
    assert 'TargetFormat.Msi' in text
    assert 'TargetFormat.Exe' in text


def test_windows_desktop_ci_builds_on_main_push():
    text = read(WINDOWS_WORKFLOW)
    assert 'name: Windows Desktop Compose' in text
    assert 'push:' in text
    assert 'branches: [main]' in text
    assert 'runs-on: windows-latest' in text
    assert 'java-version: "17"' in text or "java-version: '17'" in text


def test_windows_desktop_ci_uses_project_wrapper_and_runs_shared_gate():
    text = read(WINDOWS_WORKFLOW)
    assert '.\\gradlew.bat' in text
    assert ':shared:desktopTest' in text
    assert ':desktopApp:compileKotlin' in text
    assert ':desktopApp:packageExe' in text
    assert ':desktopApp:packageMsi' in text


def test_windows_desktop_ci_uploads_exe_and_msi():
    text = read(WINDOWS_WORKFLOW)
    assert 'desktopApp/build/compose/binaries/main/exe/*.exe' in text
    assert 'desktopApp/build/compose/binaries/main/msi/*.msi' in text
    assert 'if-no-files-found: error' in text


def test_linux_appimage_also_builds_on_main_push():
    text = read(LINUX_WORKFLOW)
    assert 'push:' in text
    assert 'branches: [main]' in text
    assert "'desktop-v*'" in text
