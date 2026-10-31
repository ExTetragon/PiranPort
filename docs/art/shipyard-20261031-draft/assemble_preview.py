import base64
import hashlib
import json
import shutil
from datetime import datetime, timedelta, timezone
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

# 保存原生导出并拼接评审图，不改动渲染图中的模型和生产资源。
HERE = Path(__file__).resolve().parent
ROOT = next(p for p in HERE.parents if (p / 'gradle.properties').exists())
TASK = ROOT / 'build/shipyard-v10-20261031'
spec = json.loads((HERE / 'design-spec.json').read_text(encoding='utf-8'))
export = json.loads((TASK / 'native-export.json').read_text(encoding='utf-8'))
metadata = export.get('structuredContent') or json.loads(export['content'][0]['text'])
assert not metadata['truncated']
model = json.loads(metadata['content'])
assert len(model['elements']) == spec['elementCount'] == 56
assert len(model['textures']) == 1
atlas = HERE / 'textures/shipyard_atlas.png'
assert base64.b64decode(model['textures'][0]['source'].split(',', 1)[1]) == atlas.read_bytes()
for expected, actual in zip(spec['elements'], model['elements']):
    assert expected['name'] == actual['name']
    assert expected['from'] == actual['from'] and expected['to'] == actual['to']
    for face in expected['faces']:
        assert all(abs(a-b) < 1e-8 for a, b in zip(actual['faces'][face]['uv'], expected['faces'][face]['uv']))
        assert actual['faces'][face]['texture'] == 0
(HERE / 'sources/shipyard_draft.bbmodel').write_text(metadata['content'], encoding='utf-8')

java = {'credit': 'PiranPort shipyard visual draft', 'ambientocclusion': True,
        'textures': {'0': 'piranport:block/shipyard_atlas', 'particle': 'piranport:block/shipyard_atlas'},
        'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [.6, .6, .6]}},
        'elements': [{'name': e['name'], 'from': e['from'], 'to': e['to'],
                      'faces': {f: {'uv': [n/(spec['atlasSize'][i % 2]/16) for i, n in enumerate(v['uv'])], 'texture': '#0'} for f, v in e['faces'].items()}}
                     for e in spec['elements']]}
(HERE / 'models/shipyard_draft.json').write_text(json.dumps(java, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
views = {}
for name in ['front_three_quarter', 'rear_three_quarter', 'top', 'front', 'bottom', 'low_three_quarter']:
    result = json.loads((TASK / (name + '.json')).read_text(encoding='utf-8'))
    image = next(c for c in result['content'] if c['type'] == 'image')
    target = HERE / 'previews' / (name + '.png')
    shutil.copy2(image['filePath'], target)
    views[name] = target
    bounds = Image.open(target).getchannel('A').getbbox()
    assert bounds and bounds[0] > 4 and bounds[1] > 4 and bounds[2] < 996 and bounds[3] < 896
ref_path = Path('E:/Leisure/Coding/Minecraft/mc-refs/WarshipGirlsR/v5.5.1/remodel-cores/sources.json')
refs = json.loads(ref_path.read_text(encoding='utf-8'))
assert len(refs) == 5
(HERE / 'references/sources.json').write_text(json.dumps(refs, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
modern_path = Path('E:/Leisure/Coding/Minecraft/mc-refs/GregTech-Modern/reference-snapshots/ec40d99d6a6e24d130bf4ddae0d7243281afad45/_metadata/shipyard-patterns.json')
modern_refs = json.loads(modern_path.read_text(encoding='utf-8'))
(HERE / 'references/gregtech-modern.json').write_text(json.dumps(modern_refs, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
if (TASK / 'modern-contact.png').exists():
    shutil.copy2(TASK / 'modern-contact.png', HERE / 'previews/gregtech-modern-reference.png')

font = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 27)
small = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 18)
caption = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 21)
board = Image.new('RGB', (1360, 1240), '#202c36')
d = ImageDraw.Draw(board)
d.text((28, 18), '船坞方块 · 蓝梁尺度与黄臂暖色修订', font=font, fill='#edf1e8')
d.text((28, 56), '放大中央检修盖、接头与铭牌 / 黄臂无灰蓝装饰点 / 保留疏排结构、细亮边与配色', font=small, fill='#b5c6cb')


def panel(path, box, label):
    x, y, w, h = box
    d.rounded_rectangle((x, y, x+w, y+h), radius=10, fill='#293841')
    im = Image.open(path).convert('RGBA')
    bounds = im.getchannel('A').getbbox()
    assert bounds and bounds[0] > 4 and bounds[1] > 4 and bounds[2] < im.width-4 and bounds[3] < im.height-4
    im = im.crop(bounds)
    im.thumbnail((w-40, h-65), Image.Resampling.LANCZOS)
    board.paste(im, (x+(w-im.width)//2, y+20+(h-65-im.height)//2), im)
    d.text((x+18, y+h-35), label, font=caption, fill='#dbe4e2')


panel(views['front_three_quarter'], (24, 94, 846, 810), '正面斜视 · 中性浅灰坞墙 / 钢灰舰体')
panel(views['rear_three_quarter'], (890, 94, 446, 256), '背面 · 混凝土板缝与金属压边')
panel(views['top'], (890, 370, 446, 256), '俯视 · 纵向甲板、轨道与排水槽')
panel(views['low_three_quarter'], (890, 646, 446, 258), '低角度 · 横档底部导轨与端板')
d.text((30, 925), '本轮调整前与造型／花纹参考', font=caption, fill='#dce4df')
reference_panels = [
    (HERE / 'revisions/before-scaled-blue-and-warm-yellow/previews/front_three_quarter.png', '本轮调整前'),
    (Path(next(r['path'] for r in refs if r['name'] == 'core_10441')), '舰R core_10441'),
    (Path(next(r['path'] for r in refs if r['name'] == 'core_10141')), '舰R core_10141'),
    (HERE / 'textures/shipyard_atlas.png', '本版结构纹理图集'),
    (Path(modern_refs[4]['local']), 'GTM 工业细节风格')]
for i, (path, label) in enumerate(reference_panels):
    im = Image.open(path).convert('RGBA')
    im = im.crop(im.getchannel('A').getbbox())
    if i == 4:
        im = im.resize((160, 160), Image.Resampling.NEAREST)
    elif i == 3:
        im = im.resize((160, 160), Image.Resampling.NEAREST)
    else:
        ratio = min(218/im.width, 170/im.height)
        im = im.resize((round(im.width*ratio), round(im.height*ratio)), Image.Resampling.LANCZOS if i == 0 else Image.Resampling.NEAREST)
    x, y = 34+i*264, 969
    board.paste(im, (x+(232-im.width)//2, y+(174-im.height)//2), im)
    d.text((x+16, 1160), label, font=small, fill='#adc1c7')
d.text((30, 1204), 'Blockbench 实际模型渲染 · 外观评审稿 · 仍在单方块范围内', font=small, fill='#8ea9b2')
board.save(HERE / 'previews/shipyard-preview.png')
board.save(HERE / 'previews/shipyard-preview-v10-scaled-details.png')

minimum = [min(e['from'][i] for e in model['elements']) for i in range(3)]
maximum = [max(e['to'][i] for e in model['elements']) for i in range(3)]
assert minimum == [0, 0, 0] and maximum == [16, 15.5, 16]
for hull in [e for e in spec['elements'] if e['group'] == 'Hull']:
    for crane in [e for e in spec['elements'] if e['group'] in ['Gantry', 'Jib']]:
        assert not all(min(hull['to'][i], crane['to'][i])-max(hull['from'][i], crane['from'][i]) > 1e-6 for i in range(3)), (hull['name'], crane['name'])
validation = {'completedAt': datetime.now(timezone(timedelta(hours=8))).isoformat(timespec='minutes'),
              'revision': spec['revision'], 'hullScaleFromDraft01': spec['hullScaleFromDraft01'],
              'gantryColorReference': spec['gantryColorReference'], 'patternReference': spec['patternReference'],
              'colorDirection': spec['colorDirection'],
              'surfacePatternRoles': spec['surfacePatternRoles'],
              'preservedPatternTiles': spec['preservedPatternTiles'],
              'preservedLatticeBodyTiles': spec['preservedLatticeBodyTiles'],
              'narrowFaceUV': spec['narrowFaceUV'],
              'capEdgeUV': spec['capEdgeUV'],
              'beamFaces': spec['beamFaces'],
              'gregtechReferenceCommit': modern_refs[0]['commit'],
              'draft': False, 'productionIntegrated': True, 'runtimeVersion': '0.1.125', 'artEditsIncrementVersion': False,
              'runtimeValidation': 'runtime-validation.json', 'scope': 'Native art exports and Blockbench previews; runtime checks recorded separately',
              'referenceCount': len(refs), 'referenceCache': str(ref_path.parent),
              'nativeModel': 'sources/shipyard_draft.bbmodel', 'elementCount': len(model['elements']),
              'groupCount': len(model['groups']), 'bounds': {'min': minimum, 'max': maximum},
              'atlasDimensions': spec['atlasSize'], 'texturePixelPerModelUnit': 1,
              'mastStripePixelPerModelUnit': 2,
              'capEdgePixelPerModelUnit': 4,
              'beamFacePixelPerModelUnit': 4,
              'atlasEmbeddedMatches': True, 'nativeGeometryAndUVMatchSpec': True,
              'hullDoesNotIntersectCranes': True,
              'allSixViewsUnclipped': True,
              'nativeProjectSHA256': hashlib.sha256((HERE / 'sources/shipyard_draft.bbmodel').read_bytes()).hexdigest(),
              'preview': 'previews/shipyard-preview-v10-scaled-details.png', 'gameLaunched': False}
(HERE / 'validation.json').write_text(json.dumps(validation, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps(validation, ensure_ascii=False))
