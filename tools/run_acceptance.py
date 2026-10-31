"""Run reproducible acceptance checks and emit evidence without editing manual checkboxes."""
from pathlib import Path
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]

# 从保留源码统计用例，拒绝空集或无法核对数量的动态测试。
def source_tests():
    tests = []
    for file in sorted((ROOT / 'src/gameTest/java').rglob('*.java')):
        source = file.read_text(encoding='utf-8')
        if '@GameTestGenerator' in source:
            raise ValueError('Register generated tests explicitly before changing the expected test count')
        methods = re.findall(r'@GameTest\s*\([^)]*\)\s+public static void\s+(\w+)\s*\(', source)
        tests.extend(file.stem + '.' + method for method in methods)
    if not tests:
        raise ValueError('No source GameTests found; refusing an empty acceptance run')
    return tests

# 对齐源码、发现、完成与 required 通过数，避免空跑误报。
def parse_results(log, expected):
    running = re.findall(r'(\d+) tests are now running', log)
    complete = re.findall(r'(\d+) GAME TESTS COMPLETE', log)
    passed = re.findall(r'All (\d+) required tests passed', log)
    if not running or not complete or not passed:
        raise ValueError('Missing discovered, completed or required-pass summary')
    counts = [int(running[-1]), int(complete[-1]), int(passed[-1])]
    if expected <= 0 or counts != [expected] * 3:
        raise ValueError('Source/discovered/completed/required counts differ: ' + str([expected] + counts))
    return {'discovered': counts[0], 'completed': counts[1], 'requiredPassed': counts[2]}

def run(command, log, env):
    print('Running ' + log.name, flush=True)
    with log.open('wb') as output:
        completed = subprocess.run(command, cwd=ROOT, env=env, stdout=output, stderr=subprocess.STDOUT)
    text = log.read_bytes().decode('utf-8', errors='replace')
    if completed.returncode:
        raise RuntimeError(str(command[0]) + ' failed (' + str(completed.returncode) + '); see ' + str(log))
    return text

# 检查发行包版本，并排除测试类、场景和生成缓存。
def verify_jar(output, version):
    jar = output / 'libs' / ('piranport-' + version + '.jar')
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        source_classes = [p.relative_to(ROOT / 'src/gameTest/java').as_posix()[:-5]
                          for p in (ROOT / 'src/gameTest/java').rglob('*.java')]
        source_resources = {p.relative_to(ROOT / 'src/gameTest/resources').as_posix()
                            for p in (ROOT / 'src/gameTest/resources').rglob('*') if p.is_file()}
        unwanted = [n for n in names if '/.cache/' in n or '/structure/empty.nbt' in n
                    or n in source_resources
                    or any(n == cls + '.class' or n.startswith(cls + '$') for cls in source_classes)
                    or (n.startswith('com/lex0v0/piranport/') and ('GameTests' in n or 'WorldgenAcceptance' in n))]
        if unwanted:
            raise ValueError('Development assets leaked into release jar: ' + str(unwanted))
        metadata = archive.read('META-INF/neoforge.mods.toml').decode()
        if not re.search(r'^version\s*=\s*"' + re.escape(version) + '"', metadata, re.M):
            raise ValueError('Release jar metadata version mismatch')
    return {'file': str(jar.relative_to(ROOT)), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
            'jsonResources': sum(n.endswith('.json') for n in names), 'developmentAssetsExcluded': True}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--gradle-user-home')
    parser.add_argument('--java-home')
    parser.add_argument('--offline', action='store_true')
    parser.add_argument('--client-smoke', action='store_true', help='Launch isolated helper client, check title screen, then exit')
    parser.add_argument('--worldgen', action='store_true', help='Also generate and scan a new isolated natural world')
    parser.add_argument('--build-output', default='build/acceptance')
    args = parser.parse_args()
    output = (ROOT / args.build_output).resolve()
    if not output.is_relative_to((ROOT / 'build').resolve()):
        raise ValueError('Build output must be inside the project build directory')
    reports = output / 'reports/acceptance'
    reports.mkdir(parents=True, exist_ok=True)
    execution = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    version = 'unknown'
    tests = []
    coverage = {'checks': {}}
    report = {'executionId': execution, 'version': version, 'expectedTests': 0, 'tests': [],
              'sourceSha256': {}, 'checks': {}, 'success': False}
    try:
        tests = source_tests()
        coverage = json.loads((ROOT / 'tools/acceptance_coverage.json').read_text(encoding='utf-8'))
        known = set(tests)
        for item in coverage['checks'].values():
            for test in item['tests']:
                if test not in known:
                    raise ValueError('Coverage references a missing source test: ' + test)
        version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.M)[1].strip()
        metadata = (ROOT / 'src/main/resources/META-INF/neoforge.mods.toml').read_text(encoding='utf-8')
        if not re.search(r'^version\s*=\s*"' + re.escape(version) + '"', metadata, re.M):
            raise ValueError('Source metadata version mismatch')
        env = os.environ.copy()
        if args.java_home:
            env['JAVA_HOME'] = args.java_home
        base = [str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')), '--no-configuration-cache',
                '--console=plain', '-PbuildOutput=' + str(output.relative_to(ROOT)).replace('\\', '/')]
        if args.gradle_user_home:
            base += ['--gradle-user-home', args.gradle_user_home]
        if args.offline:
            base += ['--offline']
        report.update(version=version, expectedTests=len(tests), tests=tests, checks=coverage['checks'])
        for file in sorted((ROOT / 'src/gameTest').rglob('*')):
            if file.is_file():
                report['sourceSha256'][file.relative_to(ROOT).as_posix()] = hashlib.sha256(file.read_bytes()).hexdigest()
        run([sys.executable, str(ROOT / 'tools/verify_resources.py')], reports / 'resources.log', env)
        log = run(base + ['build', 'runGameTestServer'], reports / 'gametest.log', env)
        report['gameTests'] = parse_results(log, len(tests))
        report['jar'] = verify_jar(output, version)
        for marker, key in [('PIRANPORT_WORLDGEN_SHAPES=', 'oilShapeSamples'), ('PIRANPORT_SALTPETER_SAMPLES=', 'saltpeterSamples')]:
            match = re.search(re.escape(marker) + r'(\{[^\r\n]+\})', log)
            if not match:
                raise ValueError('Missing sampling evidence: ' + marker)
            report[key] = json.loads(match[1])
        if args.client_smoke:
            client = run(base + ['-PacceptanceClientSmoke=true', 'runAcceptanceClient'], reports / 'client-smoke.log', env)
            match = re.search(r'PIRANPORT_CLIENT_HELPERS_LOADED=(\{[^\r\n]+\})', client)
            if not match:
                raise ValueError('Client did not reach the title screen with the startup check enabled')
            helpers = json.loads(match[1])
            if not all(helpers.get(key) for key in ['jei', 'spark', 'titleScreen', 'blueprints', 'riggingCore', 'modTooltips', 'equipment', 'ammunition', 'shipyardModel']):
                raise ValueError('Acceptance helpers or client model/resources failed to load')
            report['clientSmoke'] = helpers
        if args.worldgen:
            world = 'run/acceptance-worldgen-' + execution
            world_report = reports / 'worldgen.json'
            if world_report.exists():
                world_report.unlink()
            run(base + ['-PacceptanceWorldDir=' + world, 'runWorldgenAcceptance'], reports / 'worldgen.log', env)
            natural = json.loads(world_report.read_text(encoding='utf-8'))
            if not natural.get('completed') or not natural.get('basicDimensionChecksPassed'):
                raise ValueError('Natural world generation did not complete its basic dimension checks')
            report['naturalWorldgen'] = natural
            report['naturalWorldDirectory'] = world
        report['success'] = True
    except Exception as error:
        report['error'] = str(error)
    finally:
        # 每轮都覆盖报告，失败时不保留旧的成功状态。
        (reports / 'summary.json').write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        status = 'PASS' if report['success'] else 'FAIL'
        lines = ['# Acceptance ' + status, '', 'Version: ' + version,
                 'Source GameTests: ' + str(len(tests)), 'Execution: ' + execution, '',
                 '| Check | Coverage | Evidence tests |', '|---|---|---|']
        lines += ['| ' + key + ' | ' + item['coverage'] + ' | ' + ', '.join(item['tests']) + ' |'
                  for key, item in coverage['checks'].items()]
        if 'error' in report:
            lines += ['', 'Failure: ' + report['error']]
        lines += ['', 'Partial coverage and visual/compatibility checks remain pending. This report does not modify the manual checklist.']
        (reports / 'summary.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
        print(status + ': ' + str(reports / 'summary.json'), flush=True)
    return 0 if report['success'] else 1

if __name__ == '__main__':
    raise SystemExit(main())
