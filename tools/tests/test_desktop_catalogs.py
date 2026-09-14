from pathlib import Path
import shutil, subprocess, tempfile, unittest
ROOT=Path(__file__).resolve().parents[2]
SHARED=ROOT/'shared/src/commonMain/kotlin'
LOADER=ROOT/'shared/src/desktopMain/kotlin/com/dubl/character/desktop/data/DesktopCatalogLoader.kt'
HARNESS=ROOT/'tools/tests/kotlin/DesktopCatalogHarness.kt'
class DesktopCatalogsTest(unittest.TestCase):
    def test_canonical_android_assets_load_for_desktop(self):
        kotlinc=shutil.which('kotlinc'); self.assertIsNotNone(kotlinc)
        sources=sorted((SHARED/'com/dubl/character/android/model').glob('*.kt'))
        sources += [SHARED/'com/dubl/character/android/data/MiniJson.kt', SHARED/'com/dubl/character/android/data/CatalogData.kt', LOADER, HARNESS]
        with tempfile.TemporaryDirectory() as td:
            jar=Path(td)/'catalogs.jar'
            r=subprocess.run([kotlinc,*map(str,sources),'-include-runtime','-d',str(jar)],cwd=ROOT,capture_output=True,text=True)
            self.assertEqual(0,r.returncode,r.stderr)
            run=subprocess.run(['java','-jar',str(jar),str(ROOT)],cwd=ROOT,capture_output=True,text=True)
            self.assertEqual(0,run.returncode,run.stderr)
            self.assertIn('DESKTOP_CATALOGS_OK',run.stdout)
if __name__=='__main__': unittest.main()
