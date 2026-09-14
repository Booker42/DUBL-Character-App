from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
SESSION = SHARED / "com/dubl/character/android/state/CharacterSession.kt"
HARNESS = ROOT / "tools/tests/kotlin/CharacterSessionHarness.kt"

class CharacterSessionBehaviorTest(unittest.TestCase):
    def test_shared_session_cross_feature_behavior(self):
        kotlinc = shutil.which("kotlinc")
        self.assertIsNotNone(kotlinc)
        sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
        sources += [SHARED / "com/dubl/character/android/data/CharacterStore.kt", SESSION, HARNESS]
        with tempfile.TemporaryDirectory() as temp:
            jar = Path(temp) / "session.jar"
            compile_result = subprocess.run([kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)], cwd=ROOT, capture_output=True, text=True)
            self.assertEqual(0, compile_result.returncode, compile_result.stderr)
            result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn("CHARACTER_SESSION_OK", result.stdout)

if __name__ == "__main__":
    unittest.main()
