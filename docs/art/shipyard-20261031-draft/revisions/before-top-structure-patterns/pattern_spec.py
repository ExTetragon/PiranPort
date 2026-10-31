# 从舰R的灰墙、钢壳、红船底和吊架配色出发，按真实表面尺寸绘制材质。
COLORS = {
    '.': None,
    'N': '#30363a', 'G': '#4b5458', 'S': '#6c7578', 'M': '#90989a',
    'C': '#b0b7b6', 'L': '#cbd0cc', 'H': '#e0e1d9',
    'a': '#969f9c', 'b': '#b7bcb7', 'c': '#a8b0ad',
    'd': '#c7c8c1', 'e': '#dadbd3', 'f': '#e6e6de',
    'D': '#8e631c', 'O': '#b47e21', 'Y': '#d4a02c', 'I': '#e6b949', 'J': '#efce78',
    '0': '#214b74', '1': '#2679b3', '2': '#329bd4', '3': '#54b1df', '4': '#81c9e8',
    'R': '#723326', 'T': '#a3422a', 'U': '#ba4f31', 'V': '#d27b50',
    'A': '#998461', 'B': '#b7a17a', 'E': '#cbb68e',
    'F': '#38515f', 'K': '#557680', 'Q': '#8fa8ad'
}

# 顶面用预留区域按每模型单位4像素取样，亮边宽度为0.25模型单位。
TOP_TRIMS = {
    'gantry_crossbeam': (32, 48, 56, 10, '2', '3', '4', '1'),
    'gantry_post': (88, 48, 8, 8, '2', '3', '4', '1'),
    'gantry_edge': (88, 56, 2, 1, '2', '3', '4', '1'),
    'jib_mast_cap': (96, 48, 8, 8, 'Y', 'I', 'J', 'O'),
    'jib_mast': (104, 48, 6, 6, 'Y', 'I', 'J', 'O'),
    'jib_arm': (96, 56, 21, 6, 'Y', 'I', 'J', 'O'),
}

# 短条与小折角补充顶面反光，所有笔画均为一个图集像素宽。
TOP_INNER_HIGHLIGHTS = {
    'gantry_crossbeam': [(6, 3, 14, 3), (6, 4, 6, 4),
                        (25, 6, 32, 6), (32, 5, 32, 5),
                        (43, 3, 49, 3), (43, 4, 43, 4)],
    'gantry_post': [(3, 3, 4, 3)],
    'jib_mast_cap': [(2, 3, 4, 3)],
    'jib_mast': [(2, 2, 3, 2)],
    'jib_arm': [(4, 2, 8, 2), (13, 3, 16, 3)],
}


def build_pattern_spec():
    tiles = []

    def tile(name, fill):
        grid = [[fill]*16 for _ in range(16)]
        tiles.append({'name': name, 'grid': grid})
        return grid

    def rect(g, x1, y1, x2, y2, color):
        for y in range(y1, y2+1):
            for x in range(x1, x2+1):
                g[y][x] = color

    # 四像素高的坞墙只表现浇筑板缝和墙脚，不套完整方形机壳。
    g = tile('cast_concrete_wall', 'd')
    for y0 in range(0, 16, 4):
        rect(g, 0, y0, 15, y0, 'e')
        rect(g, 0, y0+3, 15, y0+3, 'b')
        rect(g, 7, y0+1, 7, y0+3, 'a')
        rect(g, 8, y0+1, 8, y0+2, 'e')
        rect(g, 2, y0+2, 3, y0+2, 'b')
        rect(g, 11, y0+1, 12, y0+1, 'e')

    g = tile('longitudinal_coping', 'd')
    for x in range(16):
        rect(g, x, 0, x, 15, ['e', 'c', 'd', 'd'][x % 4])
    for y in [4, 12]:
        g[y][0], g[y][1] = 'c', 'M'
    rect(g, 0, 8, 15, 8, 'b')

    # 地板两侧为排水沟，中间保留与承台轨道对应的钢板。
    g = tile('drydock_floor_drains', 'S')
    rect(g, 0, 0, 11, 0, 'M')
    rect(g, 0, 0, 0, 13, 'M')
    rect(g, 11, 0, 11, 13, 'M')
    for x in [1, 10]:
        rect(g, x, 1, x, 12, 'G')
        for y in range(2, 13, 2):
            g[y][x] = 'N'
    for y in [6, 11]:
        rect(g, 3, y, 8, y, 'G')
        rect(g, 3, y+1, 8, y+1, 'M')

    g = tile('yellow_painted_beam', 'Y')
    rect(g, 0, 0, 15, 0, 'I')
    for x in [4, 11]:
        g[1][x] = 'O'
    rect(g, 0, 7, 15, 7, 'O')
    g = tile('yellow_beam_cap', 'Y')
    rect(g, 0, 0, 15, 0, 'I')
    rect(g, 0, 15, 15, 15, 'O')
    rect(g, 1, 8, 14, 8, 'O')

    # 保留双倍像素密度的黄黑斜纹及原有黄臂桁架节奏。
    g = tile('hazard_mast', 'Y')
    for y in range(16):
        for x in range(16):
            if (x+y) % 4 >= 2:
                g[y][x] = 'N'
    for y in [0, 4, 8, 12]:
        g[y][0] = 'I'

    g = tile('ship_hull_plates', 'M')
    rect(g, 0, 0, 15, 0, 'C')
    rect(g, 0, 1, 15, 1, 'M')
    for x in [3, 11]:
        g[1][x] = 'S'
    for y in [5, 11]:
        rect(g, 0, y, 15, y, 'S')
        rect(g, 0, y+1, 15, y+1, 'C')

    # 红船底的有效侧面不足两像素高，高光只局部点到为止。
    g = tile('red_antifouling_hull', 'T')
    rect(g, 0, 0, 15, 0, 'U')
    rect(g, 0, 2, 15, 2, 'R')
    rect(g, 1, 0, 2, 0, 'V')
    rect(g, 8, 1, 8, 15, 'R')

    g = tile('longitudinal_deck_boards', 'B')
    for x in range(16):
        if x % 4 == 3:
            rect(g, x, 0, x, 15, 'A')
        elif x % 4 == 0:
            rect(g, x, 0, x, 15, 'E')
    for x, y in [(1, 5), (5, 10), (9, 3), (13, 8)]:
        rect(g, x, y, x+1, y, 'A')

    g = tile('brushed_steel_rail', 'M')
    rect(g, 0, 0, 15, 0, 'L')
    rect(g, 0, 0, 0, 15, 'C')
    rect(g, 1, 1, 1, 15, 'S')
    rect(g, 0, 3, 15, 3, 'S')
    g = tile('control_panel', 'M')
    rect(g, 0, 0, 15, 0, 'C')
    rect(g, 0, 1, 2, 1, 'G')
    g[1][1] = 'Q'
    g[2][0], g[2][2] = 'Y', 'K'
    g = tile('yellow_lattice_girder', 'D')
    for y in range(16):
        for x in range(16):
            if x % 4 in (0, 3) or y % 4 == x % 4 or (y+x) % 4 == 3:
                g[y][x] = 'Y'
    for x in [0, 3, 8, 11]:
        g[0][x] = 'I'
    tile('cable', 'N')

    g = tile('bridge_glazing', 'M')
    rect(g, 0, 0, 15, 0, 'F')
    for x in range(16):
        g[0][x] = ['M', 'Q', 'F', 'M'][x % 4]
    rect(g, 0, 1, 15, 1, 'S')
    rect(g, 0, 2, 15, 2, 'K')

    g = tile('rim_runner', 'M')
    rect(g, 0, 0, 0, 15, 'C')
    for y in [3, 11]:
        g[y][0] = 'L'
    g = tile('yellow_hoist', 'G')
    rect(g, 0, 0, 15, 0, 'Y')
    rect(g, 0, 1, 15, 1, 'M')

    for name, source, remap in [
        ('blue_painted_beam', 3, {'Y': '2', 'I': '3', 'D': '0', 'O': '1'}),
        ('blue_beam_cap', 4, {'I': '3', 'J': '4', 'O': '1', 'Y': '2'}),
        ('blue_lattice_girder', 11, {'D': '0', 'Y': '2', 'I': '3'})]:
        g = tile(name, '2')
        for y in range(16):
            for x in range(16):
                g[y][x] = remap.get(tiles[source]['grid'][y][x], tiles[source]['grid'][y][x])

    # 底面承重梁与上面的两条轨道、两组承台位置对应。
    g = tile('underside_frame_and_hatch', 'M')
    rect(g, 0, 0, 15, 0, 'b')
    rect(g, 0, 15, 15, 15, 'b')
    rect(g, 0, 0, 0, 15, 'b')
    rect(g, 15, 0, 15, 15, 'b')
    for x in [4, 11]:
        rect(g, x, 1, x, 14, 'G')
        rect(g, x+1, 1, x+1, 14, 'C')
    for y in [5, 10]:
        rect(g, 1, y, 14, y, 'S')
        rect(g, 1, y+1, 14, y+1, 'C')
    rect(g, 6, 7, 9, 9, 'S')
    rect(g, 7, 7, 9, 8, 'M')
    g[7][6], g[9][9] = 'C', 'C'

    g = tile('inner_steel_lining', 'C')
    for y0 in range(0, 16, 4):
        rect(g, 0, y0, 15, y0, 'M')
        rect(g, 0, y0+3, 15, y0+3, 'S')
        for x in [7, 15]:
            g[y0+1][x], g[y0+2][x] = 'S', 'M'
    g = tile('rear_coping', 'e')
    rect(g, 0, 1, 15, 1, 'c')
    for x in [4, 9]:
        g[0][x] = 'M'
    g = tile('front_wall_corner', 'd')
    rect(g, 0, 0, 0, 15, 'e')
    rect(g, 1, 0, 1, 15, 'c')
    g = tile('foundation_band', 'c')
    rect(g, 0, 0, 15, 0, 'b')
    rect(g, 0, 1, 15, 1, 'S')
    for x in [1, 14]:
        g[0][x] = 'M'
    g = tile('blue_hoist', 'G')
    rect(g, 0, 0, 15, 0, '2')
    rect(g, 0, 1, 15, 1, 'M')

    g = tile('rear_concrete_bulkhead', 'd')
    for y0 in range(0, 16, 4):
        rect(g, 0, y0, 15, y0, 'e')
        rect(g, 0, y0+3, 15, y0+3, 'b')
        rect(g, 6, y0+1, 6, y0+3, 'a')
        g[y0+1][7] = 'e'
        g[y0+2][2], g[y0+1][10] = 'b', 'e'
    while len(tiles) < 32:
        tile('reserved_' + str(len(tiles)), 'M')

    def atlas_pixel(x, y, color):
        tiles[y//16*8+x//16]['grid'][y % 16][x % 16] = color

    for name, (x, y, w, h, base, light, spark, shade) in TOP_TRIMS.items():
        for yy in range(h):
            for xx in range(w):
                color = base
                if h > 1:
                    if xx == w-1 or yy == h-1:
                        color = shade
                    elif xx == 0 or yy == 0:
                        color = spark if xx == yy == 0 else light
                atlas_pixel(x+xx, y+yy, color)
        for x0, y0, x1, y1 in TOP_INNER_HIGHLIGHTS.get(name, []):
            assert x0 == x1 or y0 == y1
            assert 0 < x0 <= x1 < w-1 and 0 < y0 <= y1 < h-1
            for yy in range(y0, y1+1):
                for xx in range(x0, x1+1):
                    atlas_pixel(x+xx, y+yy, light)
    for i in range(26, 32):
        tiles[i]['name'] = 'edge_trim_atlas_' + str(i)
    ops = [{'kind': 'grid', 'x': i % 8*16, 'y': i // 8*16,
            'rows': [''.join(row) for row in t['grid']], 'legend': COLORS}
           for i, t in enumerate(tiles)]
    return COLORS, tiles, ops
