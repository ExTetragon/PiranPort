"""Validate authored/generated resources without starting Minecraft (Python 3 standard library)."""
from pathlib import Path
import json
import re
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
ROOTS = [ROOT / 'src/main/resources', ROOT / 'src/generated/resources']
errors = []
resources = {}
# 手写与生成资源统一检查，相同相对路径视为冲突。
for base in ROOTS:
    if not base.exists():
        continue
    for file in base.rglob('*'):
        if not file.is_file() or '.cache' in file.parts:
            continue
        name = file.relative_to(base).as_posix()
        if name in resources:
            errors.append(f'duplicate resource path: {name}')
        resources[name] = file

jsons = {}
for name, file in resources.items():
    if file.suffix == '.json':
        try:
            jsons[name] = json.loads(file.read_text(encoding='utf-8'))
        except (ValueError, UnicodeError) as exc:
            errors.append(f'{name}: {exc}')

def require(condition, message):
    if not condition:
        errors.append(message)

for name, data in jsons.items():
    if '/models/' in name:
        for ref in data.get('textures', {}).values():
            if isinstance(ref, str) and ref.startswith('piranport:'):
                target = 'assets/piranport/textures/' + ref.split(':', 1)[1] + '.png'
                require(target in resources, f'{name}: missing texture {target}')
        parent = data.get('parent', '')
        if parent.startswith('piranport:'):
            target = 'assets/piranport/models/' + parent.split(':', 1)[1] + '.json'
            require(target in jsons, f'{name}: missing parent {target}')

for block in ['bauxite_ore', 'bauxite_block', 'aluminum_block', 'saltpeter_block', 'steel_block', 'fuel_barrel']:
    require(f'data/piranport/loot_table/blocks/{block}.json' in jsons, f'missing block loot: {block}')
for parent, children in {'ores': ['bauxite', 'saltpeter'], 'storage_blocks': ['raw_bauxite', 'aluminum', 'steel']}.items():
    for registry in ['block', 'item']:
        path = f'data/c/tags/{registry}/{parent}.json'
        require(path in jsons, f'missing tag {path}')
        for child in children:
            require(f'data/c/tags/{registry}/{parent}/{child}.json' in jsons, f'missing {registry} {parent}/{child}')
            require(f'#c:{parent}/{child}' in jsons.get(path, {}).get('values', []), f'missing parent tag reference in {path}')

configured = {Path(p).stem: d for p, d in jsons.items() if p.startswith('data/piranport/worldgen/configured_feature/')}
placed = {Path(p).stem: d for p, d in jsons.items() if p.startswith('data/piranport/worldgen/placed_feature/')}
for name, data in placed.items():
    require(data['feature'].split(':', 1)[1] in configured, f'{name}: missing configured feature')
    require(any(p['type'] == 'minecraft:biome' for p in data['placement']), f'{name}: missing biome placement filter')
for kind in ['surface', 'underground']:
    selected = {}
    for path, modifier in jsons.items():
        if '/biome_modifier/add_oil_' not in path or modifier['step'] != ('lakes' if kind == 'surface' else 'underground_decoration'):
            continue
        biome = modifier['biomes']
        features = modifier['features'] if isinstance(modifier['features'], list) else [modifier['features']]
        selected.setdefault(biome, []).extend(features)
    expected = {'minecraft:desert': 'sandstone', '#minecraft:is_badlands': 'red_sandstone', '#minecraft:is_savanna': 'stone'}
    require(set(selected) == set(expected), f'{kind}: incorrect oil biome selection')
    for biome, material in expected.items():
        require(selected.get(biome) == [f'piranport:oil_lake_{material}_{kind}'], f'{biome}: must select exactly one {kind} lake')
        conf = configured.get('oil_lake_' + material + ('_underground' if kind == 'underground' else ''), {}).get('config', {})
        require(conf.get('min_radius') == 4 and conf.get('max_radius') == (6 if kind == 'underground' else 8), f'{material} {kind}: wrong radius')
        feature = placed.get(f'oil_lake_{material}_{kind}', {})
        require(any(p.get('type') == 'minecraft:rarity_filter' and p.get('chance') == (50 if kind == 'underground' else 150) for p in feature.get('placement', [])), f'{material} {kind}: wrong rarity')

for file in (ROOT / 'docs').glob('*.md'):
    for feature in re.findall(r'(?:^|\n)\s*(?:/execute[^\n]*?run\s+)?/?place feature piranport:([a-z0-9_]+)(?=\s|$)', file.read_text(encoding='utf-8')):
        require(feature in configured, f'{file.name}: invalid /place feature ID {feature}')

food_source = (ROOT / 'src/main/java/com/lex0v0/piranport/registry/ModItems.java').read_text(encoding='utf-8')
foods = re.findall(r'registerFood\("([a-z_]+)"', food_source) + ['royal_naval_salted_beef']
for food in foods:
    file = resources.get(f'assets/piranport/textures/item/{food}.png')
    require(file is not None, f'missing food texture: {food}')
    if file:
        content = file.read_bytes()
        require(content[:8] == bytes([137,80,78,71,13,10,26,10]) and struct.unpack('>II', content[16:24]) == (16,16), f'{food}: expected 16x16 PNG')
for recipe in ['bauxite_block', 'bauxite_from_block']:
    require(f'data/piranport/recipe/{recipe}.json' in jsons, f'missing bauxite compression recipe: {recipe}')

# 图纸资源同时检查配方、奖励来源和累计收集条件。
blueprint_base = 'data/piranport/'
for kind in ['ship', 'equipment']:
    require(f'assets/piranport/models/item/{kind}_blueprint.json' in jsons, f'missing blueprint model: {kind}')
    adv = jsons.get(f'{blueprint_base}advancement/blueprints/all_{kind}_types.json', {})
    criteria = adv.get('criteria', {})
    require(len(criteria) == (9 if kind == 'ship' else 14), f'{kind}: incorrect collection criterion count')
    requirements = adv.get('requirements', [])
    require(len(requirements) == len(criteria) and all(len(row) == 1 for row in requirements)
            and {row[0] for row in requirements} == set(criteria), f'{kind}: collection must require all criteria')
    for key, criterion in criteria.items():
        predicates = criterion.get('conditions', {}).get('items', [{}])[0].get('predicates', {})
        require(criterion.get('trigger') == 'minecraft:inventory_changed'
                and predicates.get('piranport:blueprint_permission', {}).get('permission') == f'piranport:{kind}/{key}',
                f'{kind}/{key}: collection must test permission containment')
for name in ['merge', 'expand']:
    require(jsons.get(f'{blueprint_base}recipe/blueprint_{name}.json', {}).get('type') == f'piranport:blueprint_{name}',
            f'missing dynamic blueprint recipe: {name}')
require('minecraft:paper' in jsons.get('data/c/tags/item/papers.json', {}).get('values', []), 'missing common paper tag')
glm = jsons.get('data/neoforge/loot_modifiers/global_loot_modifiers.json', {})
require(glm.get('replace') is False and set(glm.get('entries', [])) == {
    'piranport:blueprints_shipwreck', 'piranport:blueprints_buried_treasure'}, 'blueprint GLMs must append exactly two target tables')
for source, suffix in [('shipwreck_treasure', 'shipwreck'), ('buried_treasure', 'buried_treasure')]:
    modifier = jsons.get(f'{blueprint_base}loot_modifiers/blueprints_{suffix}.json', {})
    require(modifier.get('type') == 'neoforge:add_table'
            and modifier.get('table') == f'piranport:blueprints/{suffix}'
            and modifier.get('conditions') == [{'condition': 'neoforge:loot_table_id', 'loot_table_id': f'minecraft:chests/{source}'}],
            f'{suffix}: incorrect additive loot table target')
blueprint_keys = ['item.piranport.ship_blueprint', 'item.piranport.equipment_blueprint',
                  'tooltip.piranport.blueprint.group', 'tooltip.piranport.blueprint.separator']
for language in ['zh_cn', 'en_us']:
    translations = jsons.get(f'assets/piranport/lang/{language}.json', {})
    require(all(key in translations for key in blueprint_keys), f'{language}: missing blueprint base translations')
    require(sum(key.startswith('blueprint_type.piranport.') for key in translations) == 23, f'{language}: missing permission translations')
    require(translations.get('blueprint_type.piranport.equipment.missile_launcher')
            == ('导弹发射器' if language == 'zh_cn' else 'Missile Launcher'), f'{language}: incorrect missile launcher translation')
    require(sum(key.startswith('blueprint_subclass.piranport.') for key in translations) == 6, f'{language}: missing subclass translations')

if errors:
    print('\n'.join('ERROR: ' + error for error in errors))
    sys.exit(1)
print(f'PASS: {len(jsons)} JSON resources; {len(foods)} food textures; complete solid-block loot and common tags; oil biome/radius/rarity mappings; documented feature commands; blueprint recipes, loot targets, collection criteria and translations')
