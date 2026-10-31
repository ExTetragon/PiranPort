import base64
import copy
import json
import uuid
from pathlib import Path

# 仅生成可编辑外观草稿，未改动生产方块及流体逻辑。
ROOT = Path(__file__).resolve().parent
TEMPLATE = json.loads((ROOT.parents[1] / 'art-samples/2026-10-30/sources/ammunition_press.bbmodel').read_text(encoding='utf-8'))
TEXTURES = ['fuel_barrel_side_single', 'fuel_barrel_side_pair', 'fuel_barrel_end', 'fuel_barrel_lining', 'fuel_oil_window', 'crude_oil_window']
textures = []
for i, name in enumerate(TEXTURES):
    texture = copy.deepcopy(TEMPLATE['textures'][0])
    w, h = (32, 64) if i == 1 else (32, 32) if i < 3 else (16, 16)
    texture.update(name=name, id=str(i), folder='', namespace='', width=w, height=h, uv_width=w, uv_height=h,
                   uuid=str(uuid.uuid4()), internal=True, saved=True, particle=i == 2,
                   path=str(ROOT / 'textures' / (name + '.png')),
                   source='data:image/png;base64,' + base64.b64encode((ROOT / 'textures' / (name + '.png')).read_bytes()).decode())
    textures.append(texture)


def cube(name, start, end, side, cap=None, uvheight=32, uvwidth=32):
    element = copy.deepcopy(TEMPLATE['elements'][0])
    element.update(name=name, uuid=str(uuid.uuid4()), type='cube', autouv=0, color=0,
                   **{'from': list(start), 'to': list(end)}, origin=[8, 8, 8])
    for face in ['north', 'east', 'south', 'west', 'up', 'down']:
        texture = cap if face in ['up', 'down'] and cap is not None else side
        size = 32 if cap is not None and face in ['up', 'down'] else uvwidth
        height = size if face in ['up', 'down'] else uvheight
        element['faces'][face] = {'uv': [0, 0, size, height], 'texture': texture}
    return element


def barrel(name, height, fill, oil, axis='y', offset=(0, 0, 0)):
    side = 0 if height == 16 else 1
    elements = [cube(name + '_shell', [0, 0, 0], [16, height, 16], side, 2, height * 2)]
    elements.append(cube(name + '_lining', [.3, .3, .3], [15.7, height - .3, 15.7], 3, uvheight=16, uvwidth=16))
    if fill > 0:
        elements.append(cube(name + '_oil', [.15, .15, .15], [15.85, .15 + (height - .3) * fill, 15.85], 4 if oil == 'fuel' else 5, uvheight=16, uvwidth=16))
    # 横放样机沿长轴显示共用总量，作为连续液位指示条。
    for element in elements:
        start, end = element['from'], element['to']
        if axis == 'x':
            points = [(y, 16 - x, z) for x in [start[0], end[0]] for y in [start[1], end[1]] for z in [start[2], end[2]]]
            element['from'] = [min(p[i] for p in points) for i in range(3)]
            element['to'] = [max(p[i] for p in points) for i in range(3)]
            old = copy.deepcopy(element['faces'])
            element['faces']['north'] = dict(old['north'], rotation=90)
            element['faces']['south'] = dict(old['south'], rotation=270)
            element['faces']['up'] = dict(old['west'], rotation=90)
            element['faces']['down'] = dict(old['east'], rotation=90)
            element['faces']['east'] = dict(old['up'], rotation=90)
            element['faces']['west'] = dict(old['down'], rotation=90)
        for key in ['from', 'to']:
            element[key] = [element[key][i] + offset[i] for i in range(3)]
        element['origin'] = [offset[0] + 8, offset[1] + height / 2, offset[2] + 8]
    return elements


def save(name, elements):
    project = copy.deepcopy(TEMPLATE)
    project['meta'] = {'format_version': '5.0', 'model_format': 'free', 'box_uv': False}
    project.update(name=name, resolution={'width': 32, 'height': 32}, elements=elements, textures=textures,
                   groups=[], outliner=[e['uuid'] for e in elements], ambientocclusion=True)
    (ROOT / 'models' / (name + '.bbmodel')).write_text(json.dumps(project, ensure_ascii=False, indent=2), encoding='utf-8')
    return project


board = []
cases = [
    ('single_empty', 16, 0, 'fuel', 'y', (0, 0, 0)),
    ('single_fuel_half', 16, .5, 'fuel', 'y', (24, 0, 0)),
    ('single_fuel_full', 16, 1, 'fuel', 'y', (48, 0, 0)),
    ('single_crude_half', 16, .5, 'crude', 'y', (72, 0, 0)),
    ('pair_fuel_half', 32, .5, 'fuel', 'y', (0, 0, 32)),
    ('pair_crude_three_quarters', 32, .75, 'crude', 'y', (28, 0, 32)),
    ('pair_horizontal_fuel_half', 32, .5, 'fuel', 'x', (56, 0, 32)),
]
for name, height, fill, oil, axis, offset in cases:
    save(name, barrel(name, height, fill, oil, axis))
    board += barrel(name, height, fill, oil, axis, offset)
save('fuel_barrel_design_board', board)
(ROOT / 'design-spec.json').write_text(json.dumps({
    'draft': True, 'productionIntegrated': False, 'textureResolution': 32,
    'palette': {'grayGreen': '#6c7d61', 'white': '#e0e4d4', 'iron': '#778383'},
    'singleBounds': [16, 16, 16], 'pairBounds': [16, 32, 16],
    'bands': 'gray-green / white / gray-green, shared across pair',
    'window': 'one continuous slot on each lateral face; no middle frame in pair',
    'horizontalWindow': 'axial total-quantity gauge for this visual prototype',
    'cases': [{'name': n, 'axis': a, 'fill': f, 'fluid': o} for n, h, f, o, a, offset in cases]
}, ensure_ascii=False, indent=2), encoding='utf-8')
print('Generated seven individual models and one design board.')
