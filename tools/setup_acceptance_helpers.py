"""Install pinned, hash-verified acceptance helpers into an isolated development client."""
from pathlib import Path
import argparse
import hashlib
import json
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / 'tools/acceptance_helpers.lock.json'
DEST = ROOT / 'run/acceptance-client/mods'
HEADERS = {'User-Agent': 'PiranPort-Acceptance/1.0 (development testing)'}

def request(url):
    if not url.startswith('https://'):
        raise ValueError('HTTPS download required')
    with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=60) as response:
        return response.read()

# 解析必需依赖并锁定兼容版本与哈希。
def refresh():
    mods, resolved, visiting = [], set(), set()
    query = urllib.parse.urlencode({'game_versions': json.dumps(['1.21.1']), 'loaders': json.dumps(['neoforge'])})
    def resolve(project, version_id=None):
        if project in resolved:
            return
        if project in visiting or len(resolved) > 12:
            raise RuntimeError('Unexpected dependency graph')
        visiting.add(project)
        if version_id:
            version = json.loads(request('https://api.modrinth.com/v2/version/' + version_id))
        else:
            versions = json.loads(request('https://api.modrinth.com/v2/project/' + project + '/version?' + query))
            releases = [v for v in versions if v['version_type'] == 'release']
            candidates = releases or (versions if project not in ['jei', 'spark'] else [])
            if not candidates:
                raise RuntimeError('No compatible NeoForge 1.21.1 release for ' + project)
            version = candidates[0]
        if '1.21.1' not in version['game_versions'] or 'neoforge' not in version['loaders']:
            raise RuntimeError('Incompatible dependency: ' + project)
        for dependency in version['dependencies']:
            if dependency['dependency_type'] == 'required':
                if not dependency['project_id']:
                    raise RuntimeError('Unresolved required dependency: ' + project)
                resolve(dependency['project_id'], dependency['version_id'])
        file = next((f for f in version['files'] if f['primary']), version['files'][0])
        metadata = json.loads(request('https://api.modrinth.com/v2/project/' + project))
        mods.append({'project': metadata['slug'], 'versionId': version['id'], 'version': version['version_number'], 'releaseType': version['version_type'],
                     'filename': file['filename'], 'url': file['url'], 'sha512': file['hashes']['sha512']})
        visiting.remove(project)
        resolved.add(project)
    for project in ['jei', 'spark']:
        resolve(project)
    data = {'minecraft': '1.21.1', 'loader': 'neoforge', 'mods': mods}
    LOCK.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    return data

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh', action='store_true', help='Resolve stable compatible releases and replace the lock')
    args = parser.parse_args()
    lock = refresh() if args.refresh else json.loads(LOCK.read_text(encoding='utf-8'))
    if lock['minecraft'] != '1.21.1' or lock['loader'] != 'neoforge':
        raise ValueError('Unexpected compatibility target')
    # 安装限定在独立客户端，写入前校验路径与哈希。
    DEST.mkdir(parents=True, exist_ok=True)
    for mod in lock['mods']:
        filename = mod['filename']
        if Path(filename).name != filename or not filename.endswith('.jar'):
            raise ValueError('Invalid artifact filename')
        target = DEST / filename
        if target.resolve().parent != DEST.resolve():
            raise ValueError('Artifact must remain in the isolated mods directory')
        contents = target.read_bytes() if target.exists() else request(mod['url'])
        if hashlib.sha512(contents).hexdigest() != mod['sha512']:
            raise RuntimeError('SHA-512 mismatch: ' + filename)
        if not target.exists():
            target.write_bytes(contents)
        print(mod['project'] + ' ' + mod['version'] + ': verified ' + filename)
    print('Launch with: gradlew.bat runAcceptanceClient')

if __name__ == '__main__':
    main()
