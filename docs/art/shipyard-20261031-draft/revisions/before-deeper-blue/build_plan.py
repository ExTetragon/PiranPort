import json
from pathlib import Path

# 船坞外观草稿：生成实时绘制规格与方块几何，不改生产资源。
HERE = Path(__file__).resolve().parent
ROOT = next(p for p in HERE.parents if (p / 'gradle.properties').exists())
TASK = ROOT / 'build/shipyard-v2-20261031'
TASK.mkdir(parents=True, exist_ok=True)
for name in ['sources', 'textures', 'models', 'previews', 'references']:
    (HERE / name).mkdir(exist_ok=True)

COLORS = {
    '.': None, 'N': '#293c43', 'G': '#47565a', 'S': '#607175', 'M': '#7d8d90',
    'C': '#a4b0b0', 'L': '#c2cbc7', 'H': '#dce1d5',
    'D': '#836c3d', 'O': '#a78a47', 'Y': '#c6aa59', 'I': '#e1c878', 'J': '#ead793',
    'R': '#673d38', 'T': '#895044', 'U': '#a36450', 'V': '#bd8564',
    'A': '#867451', 'B': '#b09b70', 'E': '#cbb98d',
    'F': '#315963', 'K': '#437b83', 'Q': '#74a7aa', 'P': '#e5a66a',
    '0': '#164c75', '1': '#327caf', '2': '#55bbff', '3': '#88d5ee', '4': '#99ddff'
}
tiles = []


def tile(name, fill):
    grid = [[fill] * 16 for _ in range(16)]
    tiles.append({'name': name, 'grid': grid})
    return grid


def rect(g, x1, y1, x2, y2, c):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            g[y][x] = c


g = tile('concrete_side', 'C')
rect(g, 0, 0, 15, 0, 'L')
rect(g, 0, 15, 15, 15, 'M')
rect(g, 7, 1, 7, 15, 'M')
rect(g, 8, 1, 8, 15, 'L')
for x, y in [(2, 2), (12, 2), (2, 12), (12, 12)]:
    g[y][x] = 'M'
    g[y-1][x] = 'L'
g = tile('concrete_cap', 'L')
rect(g, 0, 0, 15, 0, 'H')
rect(g, 0, 15, 15, 15, 'C')
for x, y in [(5, 6), (12, 3), (2, 11), (11, 12)]:
    g[y][x] = 'C'
g = tile('dock_floor', 'S')
for x in range(16):
    for y in range(16):
        if x % 4 == 3 or y % 4 == 3:
            g[y][x] = 'N'
        elif (x + y) % 4 == 0:
            g[y][x] = 'M'
g = tile('gantry_panel', 'Y')
rect(g, 0, 0, 15, 0, 'I')
rect(g, 0, 15, 15, 15, 'D')
for x in [3, 11]:
    g[2][x] = 'D'
    g[3][x] = 'I'
rect(g, 0, 7, 15, 7, 'O')
g = tile('gantry_cap', 'I')
rect(g, 0, 0, 15, 0, 'J')
rect(g, 0, 15, 15, 15, 'O')
rect(g, 1, 8, 14, 8, 'Y')
g = tile('hazard_mast', 'Y')
for y in range(16):
    for x in range(16):
        if (x + y) % 4 >= 2:
            g[y][x] = 'N'
for y in [0, 4, 8, 12]:
    g[y][0] = 'I'
g = tile('hull_plate', 'M')
rect(g, 0, 0, 15, 0, 'L')
rect(g, 0, 7, 15, 7, 'S')
rect(g, 7, 1, 7, 15, 'S')
for x in [3, 11]:
    g[3][x] = 'C'
g = tile('red_hull', 'T')
rect(g, 0, 0, 15, 0, 'V')
rect(g, 0, 1, 15, 1, 'U')
rect(g, 0, 14, 15, 15, 'R')
rect(g, 8, 2, 8, 13, 'R')
g = tile('wood_deck', 'B')
for y in range(16):
    if y % 4 == 3:
        rect(g, 0, y, 15, y, 'A')
    elif y % 4 == 0:
        rect(g, 0, y, 15, y, 'E')
for x, y in [(5, 1), (10, 5), (3, 9), (9, 13)]:
    rect(g, x, y, x, y + 1, 'A')
g = tile('steel_rail', 'M')
rect(g, 0, 0, 15, 0, 'L')
rect(g, 0, 1, 15, 1, 'C')
rect(g, 0, 3, 15, 3, 'S')
g = tile('control_panel', 'M')
rect(g, 0, 0, 15, 0, 'L')
rect(g, 0, 1, 2, 1, 'N')
g[1][1] = 'Q'
g[2][0], g[2][2] = 'Y', 'K'
rect(g, 0, 4, 3, 4, 'S')
g = tile('lattice_girder', 'D')
for y in range(16):
    for x in range(16):
        if x % 4 in (0, 3) or y % 4 == x % 4 or (y + x) % 4 == 3:
            g[y][x] = 'Y'
rect(g, 0, 0, 15, 0, 'I')
g = tile('cable', 'N')
g = tile('bridge_glass', 'F')
rect(g, 0, 0, 15, 0, 'C')
for x in range(16):
    g[1][x] = 'Q' if x % 3 != 2 else 'M'
rect(g, 0, 2, 15, 2, 'K')
rect(g, 0, 3, 15, 15, 'M')
g = tile('walkway_marking', 'S')
rect(g, 0, 0, 15, 0, 'Y')
rect(g, 0, 2, 15, 2, 'C')
g = tile('hoist_casing', 'G')
rect(g, 0, 0, 15, 0, 'Y')
rect(g, 0, 1, 15, 1, 'M')
for x in [2, 6, 10, 14]:
    rect(g, x, 2, x, 5, 'N')

# 狭长坞墙单独绘制四像素高的嵌板，不把整张方形外壳压扁。
g = tiles[0]['grid']
for y0 in range(0, 16, 4):
    rect(g, 0, y0, 15, y0+3, 'M')
    rect(g, 1, y0+1, 14, y0+2, 'C')
    rect(g, 2, y0+1, 6, y0+1, 'L')
    rect(g, 9, y0+1, 13, y0+1, 'L')
    rect(g, 7, y0, 7, y0+3, 'S')
    g[y0+1][1], g[y0+1][14] = 'H', 'H'
    g[y0+2][1], g[y0+2][14] = 'S', 'S'
g = tiles[1]['grid']
for y in range(16):
    for x in range(16):
        g[y][x] = 'L' if x % 4 in [0, 3] else 'C'
    if y in [3, 11]:
        g[y][1] = 'S'
        g[y-1][1] = 'H'
    if y == 7:
        rect(g, 0, y, 15, y, 'M')
g = tiles[2]['grid']
rect(g, 0, 0, 15, 15, 'S')
rect(g, 0, 0, 11, 13, 'M')
rect(g, 1, 1, 10, 12, 'G')
rect(g, 0, 0, 11, 0, 'C')
rect(g, 0, 0, 0, 13, 'C')
for x in [2, 5, 8]:
    rect(g, x, 2, x+1, 11, 'N')
    rect(g, x, 2, x, 11, 'G')
    rect(g, x-1, 2, x-1, 11, 'S')
for x, y in [(1, 1), (10, 1), (1, 12), (10, 12)]:
    g[y][x] = 'C'

# 蓝色吊架与黄色悬臂吊使用独立材质。
for name, source, remap in [
    ('blue_gantry_panel', 3, {'Y': '2', 'I': '3', 'D': '0', 'O': '1'}),
    ('blue_gantry_cap', 4, {'I': '3', 'J': '4', 'O': '1', 'Y': '2'}),
    ('blue_gantry_lattice', 11, {'D': '0', 'Y': '2', 'I': '3'})]:
    g = tile(name, '2')
    for y in range(16):
        for x in range(16):
            g[y][x] = remap.get(tiles[source]['grid'][y][x], tiles[source]['grid'][y][x])
g = tile('foundation_bottom', 'S')
rect(g, 0, 0, 15, 15, 'G')
rect(g, 1, 1, 14, 14, 'M')
rect(g, 2, 2, 13, 13, 'C')
rect(g, 3, 3, 12, 12, 'S')
rect(g, 4, 4, 11, 11, 'M')
for x in [5, 10]:
    rect(g, x, 4, x, 11, 'L')
    rect(g, x+1, 4, x+1, 11, 'G')
for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
    g[y][x] = 'H'
    g[y+1 if y < 13 else y-1][x] = 'S'
g = tile('inner_wall', 'C')
for y0 in range(0, 16, 4):
    rect(g, 0, y0, 15, y0, 'M')
    rect(g, 0, y0+1, 15, y0+1, 'L')
    rect(g, 0, y0+3, 15, y0+3, 'S')
    for x in [3, 11]:
        g[y0+2][x] = 'M'
g = tile('horizontal_rim', 'L')
rect(g, 0, 0, 15, 0, 'H')
rect(g, 0, 1, 15, 1, 'M')
for x in [3, 11]:
    g[0][x] = 'S'
    g[1][x] = 'C'
g = tile('wall_endcap', 'L')
rect(g, 0, 0, 0, 15, 'H')
rect(g, 1, 0, 1, 15, 'M')
for y in [2, 6, 10, 14]:
    g[y][0] = 'C'
g = tile('foundation_skirt', 'M')
rect(g, 0, 0, 15, 0, 'C')
rect(g, 0, 1, 15, 1, 'G')
for x in [3, 11]:
    g[0][x] = 'L'
g = tile('blue_hoist_casing', 'G')
rect(g, 0, 0, 15, 0, '2')
rect(g, 0, 1, 15, 1, 'M')
while len(tiles) < 32:
    tile('reserved_' + str(len(tiles)), 'M')

ops = [{'kind': 'grid', 'x': (i % 8) * 16, 'y': (i // 8) * 16,
        'rows': [''.join(row) for row in t['grid']], 'legend': COLORS} for i, t in enumerate(tiles)]
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
        faces[face] = {'uv': [round(n, 6) for n in [u, v, u + w, v + h]], 'texture': 'shipyard_atlas'}
    elements.append({'name': name, 'from': start, 'to': end, 'group': group, 'faces': faces})


cube('foundation', [0, 0, 0], [16, 1.5, 16], 23, 'Dock', 1, {'down': 19})
cube('floor_grating', [2, 1.5, 0], [14, 2, 14], 2, 'Dock')
cube('port_wall', [0, 1.5, 0], [2, 5.5, 16], 0, 'Dock', 1, {'east': 20, 'north': 22})
cube('starboard_wall', [14, 1.5, 0], [16, 5.5, 16], 0, 'Dock', 1, {'west': 20, 'north': 22})
cube('rear_bulkhead', [2, 1.5, 14], [14, 5, 16], 0, 'Dock', 21, {'north': 20})
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

plan = {'draft': True, 'revision': 2, 'productionIntegrated': False, 'modelBounds': [16, 15.5, 16],
        'texelDensity': '1 texture pixel per model unit; mast warning stripes use 2 pixels per model unit',
        'atlasSize': [128, 64], 'palette': COLORS, 'atlasOps': ops,
        'hullScaleFromDraft01': [1.2, 1.12, 1.15], 'gantryColorReference': 'core_10441',
        'patternReference': 'GregTechCEu/GregTech-Modern',
        'tiles': [{'name': t['name'], 'index': i} for i, t in enumerate(tiles)],
        'elements': elements, 'elementCount': len(elements),
        'referenceFeatures': ['浅灰敞口坞池', '后部门式吊架', '侧面黄黑悬臂吊', '灰色舰体与红色船底', '木色施工甲板'],
        'scope': '外观预览，无动画、状态切换、碰撞或交互代码变更'}
(HERE / 'design-spec.json').write_text(json.dumps(plan, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
(TASK / 'atlas-ops.json').write_text(json.dumps(ops, ensure_ascii=False)+'\n', encoding='utf-8')
print(json.dumps({'elementCount': len(elements), 'atlasSize': [128, 64], 'paletteColors': len(COLORS)-1}))
