from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RELEASE = ROOT / '.github/workflows/release.yml'
ANDROID_RELEASE = ROOT / '.github/workflows/android-release.yml'
APP_BUILD = ROOT / 'app/build.gradle.kts'
DESKTOP_BUILD = ROOT / 'desktopApp/build.gradle.kts'


def text(path: Path) -> str:
    assert path.exists(), f'missing {path}'
    return path.read_text(encoding='utf-8')


def test_v_tag_drives_one_fury_release_workflow_for_all_platforms():
    workflow = text(RELEASE)
    assert 'name: FURY Release' in workflow
    assert "- 'v*.*'" in workflow or '- "v*.*"' in workflow
    for job in ('validate:', 'android:', 'linux:', 'windows:', 'publish:'):
        assert job in workflow
    assert 'FURY-${{ needs.validate.outputs.version }}-Android.apk' in workflow
    assert 'FURY-${{ needs.validate.outputs.version }}-Linux-x86_64.AppImage' in workflow
    assert 'FURY-${{ needs.validate.outputs.version }}-Windows-x64.exe' in workflow
    assert 'FURY-${{ needs.validate.outputs.version }}-Windows-x64.msi' in workflow
    assert '--title "FURY $VERSION"' in workflow


def test_release_workflow_installs_the_versioned_android_17_sdk_package():
    workflow = text(RELEASE)
    assert "platforms;android-37.0" in workflow
    assert "build-tools;37.0.0" in workflow
    assert "platforms;android-37 " not in workflow


def test_release_workflow_normalizes_public_0_5_for_native_desktop_packages():
    workflow = text(RELEASE)
    assert 'NATIVE_VERSION="${VERSION}.0"' in workflow
    assert 'DUBL_VERSION: ${{ needs.validate.outputs.native_version }}' in workflow
    assert 'DUBL_VERSION_NAME: ${{ needs.validate.outputs.version }}' in workflow
    assert '100000000 +' in workflow


def test_legacy_android_release_no_longer_publishes_tag_releases():
    if not ANDROID_RELEASE.exists():
        return
    workflow = text(ANDROID_RELEASE)
    assert 'gh release create' not in workflow
    assert 'gh release upload' not in workflow
    assert 'tags:' not in workflow


def test_local_fallback_versions_are_fury_0_5_line():
    app = text(APP_BUILD)
    desktop = text(DESKTOP_BUILD)
    assert '?: "0.5"' in app
    assert '?: 105000000' in app
    assert '?: "0.5.0"' in desktop
    assert 'TargetFormat.Exe' in desktop
    assert 'TargetFormat.Msi' in desktop


def test_release_workflow_invokes_apksigner_from_installed_build_tools():
    workflow = text(RELEASE)
    assert '"$ANDROID_SDK_ROOT/build-tools/37.0.0/apksigner" verify' in workflow
    assert '\n          apksigner verify' not in workflow
