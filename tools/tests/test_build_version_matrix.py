from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ROOT_BUILD = (ROOT / 'build.gradle.kts').read_text(encoding='utf-8')
WRAPPER = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text(encoding='utf-8')
GRADLEW = (ROOT / 'gradlew').read_text(encoding='utf-8')
APP = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
SHARED = (ROOT / 'shared/build.gradle.kts').read_text(encoding='utf-8')


def test_kmp_gradle_agp_versions_are_a_supported_matrix():
    assert 'version "2.4.20"' in ROOT_BUILD
    assert 'version "9.3.0"' in ROOT_BUILD
    assert 'version "1.12.0"' in ROOT_BUILD
    assert 'gradle-9.7.0-bin.zip' in WRAPPER
    assert 'VERSION="9.7.0"' in GRADLEW


def test_compose_112_android_compiles_against_api_37_without_changing_target():
    assert 'compileSdk = 37' in APP
    assert 'compileSdk = 37' in SHARED
    assert 'targetSdk = 36' in APP


def test_compose_material3_matches_compose_112_dependency_set():
    desktop = (ROOT / 'desktopApp/build.gradle.kts').read_text(encoding='utf-8')
    shared = SHARED
    assert 'org.jetbrains.compose.material3:material3:1.12.0-alpha03' in desktop
    assert 'org.jetbrains.compose.material3:material3:1.12.0-alpha03' in shared
    assert 'org.jetbrains.compose.material3:material3:1.9.0' not in desktop + shared


def test_gradle_distribution_is_checksum_pinned_in_wrapper_and_bootstrap():
    checksum = '84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae'
    assert f'distributionSha256Sum={checksum}' in WRAPPER
    assert checksum in GRADLEW
    assert 'sha256sum -c -' in GRADLEW
