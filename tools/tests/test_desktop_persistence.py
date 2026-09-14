from pathlib import Path
import shutil, subprocess, tempfile, unittest
ROOT=Path(__file__).resolve().parents[2]
SHARED=ROOT/'shared/src/commonMain/kotlin'
DESKTOP_DIR=ROOT/'shared/src/desktopMain/kotlin/com/dubl/character/android/data'
HARNESS=ROOT/'tools/tests/kotlin/DesktopPersistenceHarness.kt'
class DesktopPersistenceTest(unittest.TestCase):
    def test_round_trip_snapshot_and_extras(self):
        kotlinc=shutil.which('kotlinc'); self.assertIsNotNone(kotlinc)
        sources=sorted((SHARED/'com/dubl/character/android/model').glob('*.kt'))
        sources += [
            SHARED/'com/dubl/character/android/data/CharacterStore.kt',
            SHARED/'com/dubl/character/android/data/CharacterExtrasStore.kt',
            SHARED/'com/dubl/character/android/data/MiniJson.kt',
            SHARED/'com/dubl/character/android/data/SnapshotCodec.kt',
            DESKTOP_DIR/'DesktopCharacterStore.kt',
            DESKTOP_DIR/'DesktopCharacterExtrasStore.kt',
            HARNESS,
        ]
        with tempfile.TemporaryDirectory() as td:
            jar=Path(td)/'persist.jar'
            r=subprocess.run([kotlinc,*map(str,sources),'-include-runtime','-d',str(jar)],cwd=ROOT,capture_output=True,text=True)
            self.assertEqual(0,r.returncode,r.stderr)
            run=subprocess.run(['java','-jar',str(jar)],cwd=ROOT,capture_output=True,text=True)
            self.assertEqual(0,run.returncode,run.stderr)
            self.assertIn('DESKTOP_PERSISTENCE_OK',run.stdout)
if __name__=='__main__': unittest.main()
