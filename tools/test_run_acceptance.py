"""Guard against false green acceptance summaries and test-asset leakage."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import run_acceptance as acceptance

class AcceptanceEvidenceTests(unittest.TestCase):
    def test_complete_matching_execution(self):
        log = '51 tests are now running\n51 GAME TESTS COMPLETE\nAll 51 required tests passed'
        self.assertEqual(acceptance.parse_results(log, 51)['requiredPassed'], 51)

    def test_missing_empty_partial_and_mismatched_runs_fail(self):
        bad = ['', 'gameTestClasses NO-SOURCE\nBUILD SUCCESSFUL',
               'All 51 required tests passed',
               '0 tests are now running\n0 GAME TESTS COMPLETE\nAll 0 required tests passed',
               '50 tests are now running\n50 GAME TESTS COMPLETE\nAll 50 required tests passed',
               '51 tests are now running\n51 GAME TESTS COMPLETE\nAll 50 required tests passed']
        for log in bad:
            with self.subTest(log=log), self.assertRaises(ValueError):
                acceptance.parse_results(log, 51)

    def test_preflight_failure_replaces_stale_success_report(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            report = root / 'build/acceptance/reports/acceptance/summary.json'
            report.parent.mkdir(parents=True)
            report.write_text('{"success": true}')
            with patch.object(acceptance, 'ROOT', root), patch('sys.argv', ['run_acceptance.py']):
                self.assertEqual(acceptance.main(), 1)
            self.assertFalse(json.loads(report.read_text())['success'])

    def test_release_rejects_development_classes_and_templates(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'libs').mkdir()
            for leaked in ['com/lex0v0/piranport/MineralGameTests.class',
                           'com/lex0v0/piranport/WorldgenAcceptance.class',
                           'data/piranport/structure/empty.nbt', 'data/.cache/input']:
                with zipfile.ZipFile(root / 'libs/piranport-0.1.41.jar', 'w') as jar:
                    jar.writestr('META-INF/neoforge.mods.toml', 'version="0.1.41"')
                    jar.writestr(leaked, 'test')
                with self.subTest(leaked=leaked), self.assertRaises(ValueError):
                    acceptance.verify_jar(root, '0.1.41')

if __name__ == '__main__':
    unittest.main()
