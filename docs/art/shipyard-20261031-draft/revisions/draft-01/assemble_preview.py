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
TASK = ROOT / 'build/shipyard-model-20261031'
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
        assert actual['faces'][face]['uv'] == expected['faces'][face]['uv']
        assert actual['faces'][face]['texture'] == 0
(HERE / 'sources/shipyard_draft.bbmodel').write_text(metadata['content'], encoding='utf-8')

java = {'credit': 'PiranPort shipyard visual draft', 'ambientocclusion': True,
        'textures': {'0': 'piranport:block/shipyard_atlas', 'particle': 'piranport:block/shipyard_atlas'},
        'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [.6, .6, .6]}},
        'elements': [{'name': e['name'], 'from': e['from'], 'to': e['to'],
                      'faces': {f: {'uv': [n/4 for n in v['uv']], 'texture': '#0'} for f, v in e['faces'].items()}}
                     for e in spec['elements']]}
(HERE / 'models/shipyard_draft.json').write_text(json.dumps(java, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
views = {}
for name in ['front_three_quarter', 'rear_three_quarter', 'top', 'front']:
    result = json.loads((TASK / (name + '.json')).read_text(encoding='utf-8'))
    image = next(c for c in result['content'] if c['type'] == 'image')
    target = HERE / 'previews' / (name + '.png')
    shutil.copy2(image['filePath'], target)
    views[name] = target
ref_path = Path('E:/Leisure/Coding/Minecraft/mc-refs/WarshipGirlsR/v5.5.1/remodel-cores/sources.json')
refs = json.loads(ref_path.read_text(encoding='utf-8'))
assert len(refs) == 5
(HERE / 'references/sources.json').write_text(json.dumps(refs, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
shutil.copy2(TASK / 'reference-contact.png', HERE / 'previews/reference-contact.png')

font = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 27)
small = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 18)
caption = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 21)
board = Image.new('RGB', (1360, 1110), '#202c36')
d = ImageDraw.Draw(board)
d.text((28, 18), '船坞方块 · 首版造型预览', font=font, fill='#edf1e8')
d.text((28, 56), '浅灰坞池 / 门式吊架 + 悬臂吊 / 红底待建舰体', font=small, fill='#b5c6cb')


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


panel(views['front_three_quarter'], (24, 94, 846, 712), '正面斜视 · 单方块范围 16 × 15.5 × 16')
panel(views['rear_three_quarter'], (890, 94, 446, 346), '背面斜视')
panel(views['top'], (890, 460, 446, 346), '俯视 · 坞池与施工甲板')
d.text((30, 828), '舰R五种改造核心原图参考', font=caption, fill='#dce4df')
for i, r in enumerate(refs):
    im = Image.open(r['path']).convert('RGBA')
    im = im.crop(im.getchannel('A').getbbox())
    im.thumbnail((218, 145), Image.Resampling.NEAREST)
    if im.width < 140:
        im = im.resize((im.width*2, im.height*2), Image.Resampling.NEAREST)
    im.thumbnail((218, 140), Image.Resampling.NEAREST)
    x, y = 34+i*264, 870
    board.paste(im, (x+(232-im.width)//2, y+(154-im.height)//2), im)
    d.text((x+40, 1032), r['name'], font=small, fill='#adc1c7')
d.text((30, 1074), 'Blockbench 实际模型渲染 · 外观评审稿', font=small, fill='#8ea9b2')
board.save(HERE / 'previews/shipyard-preview.png')

minimum = [min(e['from'][i] for e in model['elements']) for i in range(3)]
maximum = [max(e['to'][i] for e in model['elements']) for i in range(3)]
assert minimum == [0, 0, 0] and maximum == [16, 15.5, 16]
validation = {'completedAt': datetime.now(timezone(timedelta(hours=8))).isoformat(timespec='minutes'),
              'draft': True, 'productionIntegrated': False, 'versionIncremented': False,
              'referenceCount': len(refs), 'referenceCache': str(ref_path.parent),
              'nativeModel': 'sources/shipyard_draft.bbmodel', 'elementCount': len(model['elements']),
              'groupCount': len(model['groups']), 'bounds': {'min': minimum, 'max': maximum},
              'atlasDimensions': [64, 64], 'texturePixelPerModelUnit': 1,
              'atlasEmbeddedMatches': True, 'nativeGeometryAndUVMatchSpec': True,
              'allFourViewsUnclipped': True,
              'nativeProjectSHA256': hashlib.sha256((HERE / 'sources/shipyard_draft.bbmodel').read_bytes()).hexdigest(),
              'preview': 'previews/shipyard-preview.png', 'gameLaunched': False}
(HERE / 'validation.json').write_text(json.dumps(validation, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps(validation, ensure_ascii=False))
