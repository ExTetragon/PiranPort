import base64
import copy
import json
import uuid
from pathlib import Path

# 仅生成可编辑外观草稿，未改动生产方块及流体逻辑。
ROOT = Path(__file__).resolve().parent
REPOSITORY = next(parent for parent in ROOT.parents if (parent / 'gradle.properties').is_file())
TEMPLATE = json.loads((REPOSITORY / 'docs/art-samples/2026-10-30/sources/ammunition_press.bbmodel').read_text(encoding='utf-8'))
TEXTURES = ['fuel_barrel_side_single', 'fuel_barrel_side_pair', 'fuel_barrel_end', 'fuel_barrel_lining', 'fuel_oil_window', 'crude_oil_window', 'fuel_barrel_side_pair_horizontal']
textures = []
for i, name in enumerate(TEXTURES):
    texture = copy.deepcopy(TEMPLATE['textures'][0])
    w, h = (64, 32) if i == 6 else (32, 64) if i == 1 else (32, 32) if i < 3 else (16, 16)
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
    if axis == 'x':
        # 横放只延长桶体，侧面图标朝世界上方，油体始终沿Y轴升高。
        elements = [cube(name + '_shell', [0, 0, 0], [height, 16, 16], 6, uvheight=32, uvwidth=height * 2)]
        for face in ['up', 'down']:
            elements[0]['faces'][face] = {'uv': [0, 0, height * 2, 32], 'texture': 6}
        for face in ['east', 'west']:
            elements[0]['faces'][face] = {'uv': [0, 0, 32, 32], 'texture': 2}
        elements.append(cube(name + '_lining', [.3, .3, .3], [height - .3, 15.7, 15.7], 3, uvheight=16, uvwidth=16))
        if fill > 0:
            elements.append(cube(name + '_oil', [.15, .15, .15], [height - .15, .15 + 15.7 * fill, 15.85], 4 if oil == 'fuel' else 5, uvheight=16, uvwidth=16))
    else:
        elements = [cube(name + '_shell', [0, 0, 0], [16, height, 16], side, 2, height * 2)]
        elements.append(cube(name + '_lining', [.3, .3, .3], [15.7, height - .3, 15.7], 3, uvheight=16, uvwidth=16))
        if fill > 0:
            elements.append(cube(name + '_oil', [.15, .15, .15], [15.85, .15 + (height - .3) * fill, 15.85], 4 if oil == 'fuel' else 5, uvheight=16, uvwidth=16))
    for element in elements:
        for key in ['from', 'to']:
            element[key] = [element[key][i] + offset[i] for i in range(3)]
        center = [height / 2, 8, 8] if axis == 'x' else [8, height / 2, 8]
        element['origin'] = [offset[i] + center[i] for i in range(3)]
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
    'palette': {'grayGreen': '#788b70', 'white': '#ccd4c5', 'iron': '#7e8e84'},
    'singleBounds': [16, 16, 16], 'pairBounds': [16, 32, 16],
    'bands': 'gray-green / white / gray-green, shared across pair',
    'revision': 'gregtech-v3',
    'sidePattern': 'straight pressed ribs and inset label, inspired by GregTech Modern drums',
    'endcap': 'rectangular pressed plate with two-pixel chamfers, offset rectangular filling port',
    'window': 'one shared vertical slot on each visible lateral face; no duplicate gauges in pair',
    'horizontalWindow': 'upright shared vertical window; fluid fills along world Y from bottom to top',
    'symbolOrientation': 'flame tip points up on both vertical side faces, independent of barrel axis',
    'cases': [{'name': n, 'axis': a, 'fill': f, 'fluid': o} for n, h, f, o, a, offset in cases]
}, ensure_ascii=False, indent=2), encoding='utf-8')
print('Generated seven individual models and one design board.')
