from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
COMMON_MAIN = ROOT / "shared" / "src" / "commonMain" / "kotlin"


class CommonMainBoundaryTest(unittest.TestCase):
    def test_common_main_has_no_platform_imports(self):
        self.assertTrue(COMMON_MAIN.exists(), "shared/commonMain must exist")
        forbidden = re.compile(r"^import\s+(android\.|androidx\.activity|java\.|javax\.|javafx\.)")
        violations = []
        for source in COMMON_MAIN.rglob("*.kt"):
            for line_number, line in enumerate(source.read_text(encoding="utf-8").splitlines(), 1):
                if forbidden.match(line):
                    violations.append(f"{source.relative_to(ROOT)}:{line_number}: {line}")
        self.assertEqual([], violations, "Platform imports found in commonMain:\n" + "\n".join(violations))


if __name__ == "__main__":
    unittest.main()
