import json
from pathlib import Path
from pattern_spec import BEAM_FACE_UV, BEAM_SURFACES, TOP_TRIMS, build_pattern_spec

# 保留既有船坞几何，按舰R配色与结构材质重新绘制外观。
HERE = Path(__file__).resolve().parent
ROOT = next(p for p in HERE.parents if (p / 'gradle.properties').exists())
TASK = ROOT / 'build/shipyard-v9-20261031'
TASK.mkdir(parents=True, exist_ok=True)
for name in ['sources', 'textures', 'models', 'previews', 'references']:
    (HERE / name).mkdir(exist_ok=True)
COLORS, tiles, ops = build_pattern_spec()

elements = []


def cube(name, start, end, material, group, top=None, side_overrides=None):
    if group == 'Hull':
        start = [round(8+(start[0]-8)*1.2, 3), round(3+(start[1]-3)*1.12, 3), round(7.5+(start[2]-7.5)*1.15, 3)]
        end = [round(8+(end[0]-8)*1.2, 3), round(3+(end[1]-3)*1.12, 3), round(7.5+(end[2]-7.5)*1.15, 3)]
    if group == 'Gantry':
        blue = {3: 16, 4: 17, 11: 18, 15: 24}
        material = blue.get(material, material)
        top = blue.get(top, top)
    dx, dy, dz = [b - a for a, b in zip(start, end)]
    faces = {}
    for face in ['north', 'east', 'south', 'west', 'up', 'down']:
        tile_id = top if face == 'up' and top is not None else material
        if side_overrides and face in side_overrides:
            tile_id = side_overrides[face]
        u, v = (tile_id % 8) * 16, (tile_id // 8) * 16
        w, h = (dx, dz) if face in ['up', 'down'] else (dz, dy) if face in ['east', 'west'] else (dx, dy)
        if tile_id == 5:
            w, h = w * 2, h * 2
        # 窄侧面从中间色区域取样，避免首行高光覆盖整面；保持原有像素密度。
        if face not in ['up', 'down'] and h < 2:
            if tile_id in [3, 16]:
                v += 2
            elif tile_id in [11, 18]:
                v += 3
            elif tile_id in [4, 17]:
                v += 2
        if face == 'down' and tile_id in [3, 4, 11, 16, 17, 18]:
            v += 8
        if face == 'up' and tile_id in [4, 17]:
            trim = name
            if name.startswith('gantry_') and name.endswith('_post'):
                trim = 'gantry_post'
            elif name.startswith('gantry_') and name.endswith('_edge'):
                trim = 'gantry_edge'
            u, v, w, h = TOP_TRIMS[trim][:4]
            assert abs(w-dx*4) < 1e-8 and abs(h-dz*4) < 1e-8
        if name in BEAM_FACE_UV:
            u, v, w, h = BEAM_SURFACES[BEAM_FACE_UV[name][face]][:4]
            face_w, face_h = (dx, dz) if face in ['up', 'down'] else (dz, dy) if face in ['east', 'west'] else (dx, dy)
            assert abs(w-face_w*4) < 1e-8 and abs(h-face_h*4) < 1e-8
        faces[face] = {'uv': [round(n, 6) for n in [u, v, u + w, v + h]], 'texture': 'shipyard_atlas'}
    elements.append({'name': name, 'from': start, 'to': end, 'group': group, 'faces': faces})


cube('foundation', [0, 0, 0], [16, 1.5, 16], 23, 'Dock', 1, {'down': 19})
cube('floor_grating', [2, 1.5, 0], [14, 2, 14], 2, 'Dock')
cube('port_wall', [0, 1.5, 0], [2, 5.5, 16], 0, 'Dock', 1, {'east': 20, 'north': 22})
cube('starboard_wall', [14, 1.5, 0], [16, 5.5, 16], 0, 'Dock', 1, {'west': 20, 'north': 22})
cube('rear_bulkhead', [2, 1.5, 14], [14, 5, 16], 25, 'Dock', 21, {'north': 20, 'east': 22, 'west': 22})
for x, name in [(0, 'port'), (14, 'starboard')]:
    cube(name + '_wall_cap', [x, 5.5, 0], [x+2, 6, 16], 1, 'Dock')
    cube(name + '_toe_rail', [x+.5, 6, 1], [x+1, 6.25, 13], 14, 'Dock')
cube('entrance_lip', [2, 1.5, 0], [14, 2, .5], 9, 'Dock')
for x in [4, 11]:
    cube('floor_track_' + str(x), [x, 2, 1.5], [x+.5, 2.25, 13.5], 9, 'Cradle')
for z in [4.5, 9.5]:
    cube('keel_block_' + str(z), [5, 2.25, z], [11, 3.1, z+1], 9, 'Cradle')
cube('red_keel', [6, 3, 3], [10, 4, 12], 7, 'Hull')
cube('red_hull_center', [4.5, 3.75, 5], [11.5, 5.25, 12], 7, 'Hull')
cube('red_bow_shoulder', [5.25, 3.75, 3], [10.75, 5.25, 5], 7, 'Hull')
cube('red_bow_tip', [6.25, 4, 2], [9.75, 5.25, 3], 7, 'Hull')
cube('red_stern', [5.25, 3.75, 12], [10.75, 5.25, 13], 7, 'Hull')
cube('upper_hull_center', [4, 5.25, 5], [12, 6.5, 12], 6, 'Hull', 9)
cube('upper_bow_shoulder', [5, 5.25, 3], [11, 6.5, 5], 6, 'Hull', 9)
cube('upper_bow_tip', [6, 5.25, 2], [10, 6.5, 3], 6, 'Hull', 9)
cube('upper_stern', [4.75, 5.25, 12], [11.25, 6.5, 13], 6, 'Hull', 9)
cube('main_deck', [4.5, 6.5, 5], [11.5, 6.75, 12], 8, 'Hull')
cube('fore_deck', [5.5, 6.5, 3], [10.5, 6.75, 5], 8, 'Hull')
cube('bow_deck', [6.5, 6.5, 2.25], [9.5, 6.75, 3], 8, 'Hull')
for x in [4, 11.5]:
    cube('gunwale_' + str(x), [x, 6.5, 5], [x+.5, 7, 12], 9, 'Hull')
cube('bridge_lower', [6.25, 6.75, 8], [9.75, 7.75, 10.5], 6, 'Hull', 9)
cube('bridge_windows', [6.75, 7.75, 8.5], [9.25, 8.75, 10], 13, 'Hull', 9)
cube('bridge_roof', [6.5, 8.75, 8.25], [9.5, 9, 10.25], 9, 'Hull')
cube('unfinished_frame_1', [5, 6.75, 11], [5.5, 7.75, 11.5], 9, 'Hull')
cube('unfinished_frame_2', [10.5, 6.75, 11], [11, 7.75, 11.5], 9, 'Hull')
cube('unfinished_cross_member', [5, 7.5, 11], [11, 7.75, 11.5], 9, 'Hull')
for x, name in [(1, 'port'), (13, 'starboard')]:
    shoe_start = x-.25 if name == 'port' else x
    shoe_end = x+2 if name == 'port' else x+2.25
    cube('gantry_' + name + '_shoe', [shoe_start, 6, 11.25], [shoe_end, 6.5, 14], 9, 'Gantry')
    cube('gantry_' + name + '_post', [x, 6.5, 11.5], [x+2, 13.75, 13.5], 11, 'Gantry', 4)
    cube('gantry_' + name + '_edge', [x, 6.5, 11.25], [x+.5, 13.75, 11.5], 3, 'Gantry', 4)
cube('gantry_crossbeam', [1, 13.75, 11.25], [15, 15.25, 13.75], 3, 'Gantry', 4)
cube('gantry_underside_rail', [2.5, 13.25, 12], [13.5, 13.75, 13], 9, 'Gantry')
cube('gantry_trolley', [8.5, 12.5, 11.75], [10.5, 13.25, 13.25], 15, 'Gantry')
cube('gantry_cable', [9.375, 9.75, 12.375], [9.625, 12.5, 12.625], 12, 'Gantry')
cube('gantry_hook', [9.125, 9.25, 12.25], [9.875, 9.75, 12.75], 9, 'Gantry')
cube('jib_base', [.25, 6, 5], [2.25, 6.5, 7], 9, 'Jib')
cube('jib_mast', [.5, 6.5, 5.25], [2, 14.25, 6.75], 5, 'Jib', 4)
cube('jib_mast_cap', [.25, 14.25, 5], [2.25, 15.5, 7], 4, 'Jib')
cube('jib_arm', [2.25, 14.25, 5.25], [7.5, 15.25, 6.75], 11, 'Jib', 4)
cube('jib_trolley', [6, 13.75, 5.5], [7.25, 14.25, 6.5], 15, 'Jib')
cube('jib_cable', [6.5, 9, 5.875], [6.75, 13.75, 6.125], 12, 'Jib')
cube('jib_hook_vertical', [6.25, 8.5, 5.75], [6.75, 9, 6.25], 9, 'Jib')
cube('jib_hook_return', [6.75, 8.5, 5.75], [7.25, 8.75, 6.25], 9, 'Jib')
cube('control_box', [14, 6, .5], [16, 8.5, 3], 0, 'Controls', 1, {'north': 10})
cube('control_screen_rim', [14.25, 6.75, .35], [15.75, 8, .5], 10, 'Controls')
cube('service_duct', [14.75, 3, 13.5], [15.75, 5.5, 15], 9, 'Controls')

for e in elements:
    assert all(0 <= a < b <= 16 for a, b in zip(e['from'], e['to'])), e['name']

plan = {'draft': False, 'revision': 9, 'productionIntegrated': True, 'runtimeVersion': '0.1.125',
        'modelBounds': [16, 15.5, 16], 'atlasSize': [128, 128],
        'texelDensity': 'Main surfaces: 1 pixel per model unit; warning stripes: 2; crane cap edges and horizontal beam faces: 4',
        'palette': COLORS, 'atlasOps': ops,
        'tiles': [{'name': t['name'], 'index': i} for i, t in enumerate(tiles)],
        'elements': elements, 'elementCount': len(elements),
        'hullScaleFromDraft01': [1.2, 1.12, 1.15],
        'gantryColorReference': 'Warship Girls R core_10441, adjusted for scene harmony',
        'colorDirection': 'Neutral concrete and steel unchanged; saturated blue and gold cranes and red hull; localized highlights',
        'narrowFaceUV': 'Thin painted sides sample midtone rows; lattice sides sample row 3; undersides avoid the top highlight row; texel density unchanged',
        'capEdgeUV': {'density': 4, 'edgeWidthInModelUnits': .25,
                      'regions': TOP_TRIMS, 'style': 'Thin perimeter rims and restrained recess panels',
                      'recessColors': {'blue': COLORS['1'], 'yellow': COLORS['O']},
                      'deepSideColors': {'blue': COLORS['0'], 'yellow': COLORS['D'], 'mastWarning': COLORS['N']}},
        'beamFaces': {'density': 4, 'regions': BEAM_SURFACES, 'faceUV': BEAM_FACE_UV,
                      'blueWeldJoints': [18, 37], 'blueServiceHatches': 1, 'blueServicePlacards': 1,
                      'yellowCouplingJoints': 1,
                      'manufacturerReference': 'references/real-crane-girders.json',
                      'style': 'Real box-girder interpretation: broad painted panels, longitudinal cover seam, sparse weld joints, service hatch/placard and bolted ends; separate from column slot motifs'},
        'patternReference': 'GregTechCEu/GregTech-Modern style only; patterns follow actual dock construction',
        'surfacePatternRoles': {'outside': 'Cast panel joints and wall footing',
            'coping': 'Concrete lip, rubbed steel runner and anchors',
            'inside': 'Steel lining and drainage channels beside the tracks',
            'bottom': 'Longitudinal support beams, cradle crossmembers and a small access cover',
            'ship': 'Steel hull seams, localized red-hull highlights, longitudinal deck boards'},
        'preservedPatternTiles': [5],
        'preservedLatticeBodyTiles': [11, 18],
        'scope': 'Editable source of the integrated shipyard; runtime model, texture and shape changes must be synchronized separately'}
(HERE / 'design-spec.json').write_text(json.dumps(plan, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
(TASK / 'atlas-ops.json').write_text(json.dumps(ops, ensure_ascii=False)+'\n', encoding='utf-8')
print(json.dumps({'elementCount': len(elements), 'atlasSize': plan['atlasSize'], 'paletteColors': len(COLORS)-1}))
