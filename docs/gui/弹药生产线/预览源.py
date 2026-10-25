from pathlib import Path
import base64
import hashlib
import io
import json
import subprocess
import time
import zipfile
from xml.sax.saxutils import escape
from PIL import Image

# 复用游戏现有背景与原版字形，生成可编辑SVG和布局预览，不修改运行代码。
ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
CACHE = Path('C:/Users/dengl/.gradle/caches/neoformruntime')
BACKGROUND = ROOT / 'src/main/resources/assets/piranport/textures/gui/manufacturing_background.png'
WIDTH, HEIGHT, SCALE = 348, 252, 4
RIGHT_CENTER, RIGHT_TOP, RIGHT_BOTTOM = 296, 40, 148
CACHE_WIDTH, CACHE_HEIGHT, ARROW_SIZE = 72, 36, 20
RIGHT_GAP = (RIGHT_BOTTOM - RIGHT_TOP - CACHE_HEIGHT * 2 - ARROW_SIZE) / 4
CACHE_X = RIGHT_CENTER - CACHE_WIDTH / 2
INPUT_Y = RIGHT_TOP + RIGHT_GAP
ARROW_Y = INPUT_Y + CACHE_HEIGHT + RIGHT_GAP
OUTPUT_Y = ARROW_Y + ARROW_SIZE + RIGHT_GAP
TEXT, MUTED, CYAN, EDGE, ORANGE = '#e3eaf2', '#a4bacb', '#29b9e5', '#47758f', '#edb05d'
states = [('待机', 'idle', 'zh', False), ('生产中', 'running', 'zh', False),
          ('总需求', 'running', 'zh', True), ('本次完成后停止', 'stopping', 'zh', False),
          ('产物空间不足', 'blocked', 'zh', False), ('英文生产中', 'running', 'en', False),
          ('英文总需求', 'running', 'en', True),
          ('单位省略示意', 'idle', 'zh', False, True), ('总需求换算示意', 'idle', 'zh', True, True)]
assets = {}
glyphs = {}
glyph_defs = {}
used_text = []

def data_url(raw):
    return 'data:image/png;base64,' + base64.b64encode(raw).decode()

game_jar = None
for candidate in (CACHE / 'intermediate_results').glob('stripClient_*_resourcesOutput.jar'):
    with zipfile.ZipFile(candidate) as archive:
        if 'assets/minecraft/font/include/default.json' in archive.namelist():
            game_jar = candidate
            break
if game_jar is None:
    raise RuntimeError('Minecraft default font resources not found in the existing cache')

with zipfile.ZipFile(game_jar) as archive:
    providers = json.loads(archive.read('assets/minecraft/font/include/default.json'))['providers']
    for provider in providers:
        if provider['type'] != 'bitmap':
            continue
        rows = provider['chars']
        texture = provider['file'].split(':', 1)[1]
        image = Image.open(io.BytesIO(archive.read('assets/minecraft/textures/' + texture))).convert('RGBA')
        cell_w, cell_h = image.width // len(rows[0]), image.height // len(rows)
        scale = provider.get('height', 8) / cell_h
        offset_y = 7 - provider['ascent']
        for row, chars in enumerate(rows):
            for column, char in enumerate(chars):
                if char == '\x00' or char in glyphs:
                    continue
                pixels = []
                for y in range(cell_h):
                    for x in range(cell_w):
                        if image.getpixel((column * cell_w + x, row * cell_h + y))[3] > 0:
                            pixels.append((x * scale, y * scale + offset_y, scale))
                if pixels:
                    right = max(x + size for x, y, size in pixels)
                    glyphs[char] = (pixels, int(right + 0.5) + 1)
    for name in ['copper_ingot', 'gunpowder']:
        assets[name] = archive.read(f'assets/minecraft/textures/item/{name}.png')

index = json.loads((CACHE / 'assets/indexes/17.json').read_text())['objects']
font_hash = index['minecraft/font/unifont.zip']['hash']
with zipfile.ZipFile(CACHE / 'assets/objects' / font_hash[:2] / font_hash) as archive:
    name = next(n for n in archive.namelist() if n.endswith('.hex'))
    hex_font = {}
    for line in archive.read(name).decode().splitlines():
        code, bits = line.split(':', 1)
        hex_font[int(code, 16)] = bits

for name in ['high_explosive_shell', 'armor_piercing_shell', 'anti_aircraft_shell', 'depth_charge',
             'straight_running_torpedo', 'guided_torpedo', 'anti_ship_missile', 'anti_aircraft_missile',
             'steel_ingot', 'aluminum_ingot']:
    assets[name] = (ROOT / f'src/main/resources/assets/piranport/textures/item/{name}.png').read_bytes()

def glyph(char):
    if char == ' ':
        return [], 4
    if char not in glyphs:
        bits = hex_font.get(ord(char), hex_font[ord('?')])
        width = len(bits) // 4
        pixels = []
        for y in range(16):
            row = int(bits[y * width // 4:(y + 1) * width // 4], 16)
            for x in range(width):
                if row & (1 << (width - 1 - x)):
                    pixels.append((x / 2, y / 2, .5))
        glyphs[char] = (pixels, width / 2 + 1)
    return glyphs[char]

def text_width(value):
    return sum(glyph(char)[1] for char in value)

def truncated(value, width):
    if text_width(value) <= width:
        return value
    while value and text_width(value + '…') > width:
        value = value[:-1]
    return value + '…'

def text(svg, x, y, value, color=TEXT, align='left', max_width=None):
    original = value
    if max_width is not None:
        value = truncated(value, max_width)
    if align == 'center':
        x -= text_width(value) / 2
    elif align == 'right':
        x -= text_width(value)
    used_text.append({'text': original, 'display': value, 'x': x, 'y': y, 'width': text_width(value)})
    uses = []
    for char in value:
        pixels, advance = glyph(char)
        if pixels:
            identifier = 'g' + hex(ord(char))[2:]
            if identifier not in glyph_defs:
                path = ''.join(f'M{px:g},{py:g}h{size:g}v{size:g}h{-size:g}z' for px, py, size in pixels)
                glyph_defs[identifier] = f'<path id="{identifier}" d="{path}"/>'
            uses.append(f'<use xlink:href="#{identifier}" x="{x:g}" y="{y:g}"/>')
        x += advance
    svg.append(f'<g fill="{color}" shape-rendering="crispEdges" data-text="{escape(original)}"><title>{escape(original)}</title>{"".join(uses)}</g>')

def rect(svg, x, y, width, height, fill, stroke=None):
    svg.append(f'<rect x="{x:g}" y="{y:g}" width="{width:g}" height="{height:g}" fill="{fill}"' + (f' stroke="{stroke}" stroke-width="1"' if stroke else '') + '/>')

def outline(svg, x, y, width, height, color):
    rect(svg, x, y, width, 1, color)
    rect(svg, x, y + height - 1, width, 1, color)
    rect(svg, x, y, 1, height, color)
    rect(svg, x + width - 1, y, 1, height, color)

def icon(svg, name, x, y, size=16):
    svg.append(f'<image x="{x}" y="{y}" width="{size}" height="{size}" xlink:href="{data_url(assets[name])}" image-rendering="pixelated"/>')

def button(svg, x, y, width, height, label='', selected=False, active=True):
    rect(svg, x, y, width, height, 'url(#button)' if active else 'url(#disabled)')
    outline(svg, x, y, width, height, CYAN if selected else EDGE)
    if active:
        rect(svg, x + 1, y + 1, width - 2, 1, '#658ba5')
    if label:
        text(svg, x + width / 2, y + (height - 8) / 2, label, TEXT if active else MUTED, 'center', width - 8)

def slot(svg, x, y, item=None, count=None):
    rect(svg, x + 1, y + 1, 16, 16, 'url(#slot)')
    rect(svg, x, y, 18, 1, '#081c2c')
    rect(svg, x, y + 1, 1, 17, '#0b2234')
    rect(svg, x + 1, y + 17, 17, 1, EDGE)
    rect(svg, x + 17, y + 1, 1, 17, '#315e7c')
    if item:
        icon(svg, item, x + 1, y + 1)
    if count is not None and count != 1:
        text(svg, x + 18, y + 10, str(count), '#111820', 'right')
        text(svg, x + 17, y + 9, str(count), '#ffffff', 'right')

def background(svg, language):
    source_x, source_y = [3, 48, 336, 381], [3, 48, 204, 249]
    dest_x, dest_y = [1, 46, WIDTH - 46, WIDTH - 1], [1, 46, HEIGHT - 46, HEIGHT - 1]
    for row in range(3):
        for column in range(3):
            x, y = dest_x[column], dest_y[row]
            width, height = dest_x[column + 1] - x, dest_y[row + 1] - y
            sx, sy = source_x[column], source_y[row]
            sw, sh = source_x[column + 1] - sx, source_y[row + 1] - sy
            identifier = f'background-slice-{row}-{column}'
            svg.append(f'<clipPath id="{identifier}" clipPathUnits="userSpaceOnUse"><rect x="{x}" y="{y}" width="{width}" height="{height}"/></clipPath>')
            svg.append(f'<g clip-path="url(#{identifier})"><use xlink:href="#backdrop" transform="matrix({width/sw} 0 0 {height/sh} {x-sx*width/sw} {y-sy*height/sh})"/></g>')
    title = '弹药生产线' if language == 'zh' else 'Ammunition Line'
    end = max(60, min(text_width(title) + 24, WIDTH - 60))
    for row in range(18):
        t = row / 17
        first, second = (44, 131, 185), (22, 81, 128)
        color = '#' + ''.join(f'{int(a*(1-t)+b*t):02x}' for a, b in zip(first, second))
        right = end - row // 2
        rect(svg, 1, 1 + row, right - 1, 1, color)
        rect(svg, right, 1 + row, 3, 1, '#102f49')
    rect(svg, 1, 19, WIDTH - 2, 1, '#2888b9')
    outline(svg, 0, 0, WIDTH, HEIGHT, '#397fa6')
    text(svg, 8, 6, title)

def material_quantity(amount, metal, zh):
    if amount is None:
        return '—'
    if not metal:
        return f'{amount}个' if zh else str(amount)
    ingots, nuggets = divmod(amount, 9)
    return ''.join(f'{value}{unit}' for value, unit in
                   [(ingots, '锭' if zh else 'i'), (nuggets, '粒' if zh else 'n')] if value)

def render(label, state, language, total, sample=False):
    svg = []
    start = len(used_text)
    zh = language == 'zh'
    background(svg, language)
    text(svg, 8, 26, '弹药类型' if zh else 'Category', MUTED)
    categories = [('high_explosive_shell', '炮弹' if zh else 'SH'), ('depth_charge', '深弹' if zh else 'DC'),
                  ('straight_running_torpedo', '鱼雷' if zh else 'TP'), ('anti_ship_missile', '导弹' if zh else 'MS')]
    for index, (item, caption) in enumerate(categories):
        x = 8 + index * 28
        button(svg, x, 38, 26, 24, selected=index == 0)
        icon(svg, item, x + 7, 40, 12)
        text(svg, x + 13, 53, caption, TEXT if index == 0 else MUTED, 'center')
    subtypes = [('high_explosive_shell', '高爆弹' if zh else 'High-Explosive Shell'),
                ('armor_piercing_shell', '穿甲弹' if zh else 'Armor-Piercing Shell'),
                ('anti_aircraft_shell', '防空弹' if zh else 'Anti-Aircraft Shell')]
    for index, (item, name) in enumerate(subtypes):
        y = 70 + index * 18
        button(svg, 8, y, 112, 16, selected=index == 0)
        icon(svg, item, 10, y + 1, 14)
        text(svg, 28, y + 4, name, max_width=86)
    rect(svg, 8, 128, 32, 16, 'url(#frame)')
    outline(svg, 8, 128, 32, 16, EDGE)
    text(svg, 24, 132, '40', TEXT, 'center')
    text(svg, 43, 132, '枚' if zh else 'rds', MUTED)
    track_x = 59 if zh else 65
    rect(svg, track_x, 134, 120 - track_x, 4, '#0a2032')
    outline(svg, track_x, 133, 120 - track_x, 6, '#345f7a')
    rect(svg, track_x + 1, 134, 30, 4, '#24799f')
    button(svg, track_x + 28, 129, 7, 14)
    rect(svg, 128, 24, 116, 62, 'url(#frame)')
    outline(svg, 128, 24, 116, 62, '#345f7a')
    text(svg, 134, 28, ('总需求' if total else '单次需求') if zh else ('Total' if total else 'Per cycle'), MUTED)
    rect(svg, 134, 38, 104, 1, '#4b6578')
    rect(svg, 220, 30, 18, 4, '#0a2032')
    outline(svg, 220, 29, 18, 6, '#345f7a')
    rect(svg, 221, 30, 13 if total else 2, 4, '#24799f')
    button(svg, 231 if total else 220, 26, 7, 12, selected=True)
    materials = [('copper_ingot', '铜材' if zh else 'Copper'),
                 ('steel_ingot', '钢材' if zh else 'Steel'), ('aluminum_ingot', '铝材' if zh else 'Aluminum'),
                 ('gunpowder', '火药' if zh else 'Gunpowder')]
    # 示例只用于检查零单位和零需求行的省略，不作为弹药费用。
    amounts = [4, 36, 0, 2] if sample else [None] * 4
    row = 0
    for (item, name), amount in zip(materials, amounts):
        if amount == 0:
            continue
        if amount is not None and total:
            amount *= 10
        y = 42 + row * 10
        quantity = material_quantity(amount, item != 'gunpowder', zh)
        icon(svg, item, 134, y, 8)
        text(svg, 146, y, name)
        text(svg, 238, y, quantity, TEXT, 'right')
        row += 1
    text(svg, 186, 90, '单次4枚 · 用时 —' if zh else '4 / cycle · Time —', MUTED, 'center', 116)
    button_label = ('开始生产' if zh else 'Start production') if state == 'idle' else ('等待本次完成' if zh else 'Stopping…') if state == 'stopping' else ('中断生产' if zh else 'Stop after cycle')
    button(svg, 128, 103, 116, 18, button_label, active=state != 'stopping')
    if state == 'idle':
        text(svg, 186, 126, '高爆弹 [待机]', TEXT, 'center', 116)
        text(svg, 186, 140, '计划40枚 · 共10次', MUTED, 'center', 116)
    elif state == 'stopping':
        text(svg, 186, 126, '本次完成后停止', ORANGE, 'center', 116)
        text(svg, 186, 140, '已完成16枚 · 结束20枚', MUTED, 'center', 116)
    elif state == 'blocked':
        text(svg, 186, 126, '等待产物空间', ORANGE, 'center', 116)
        text(svg, 186, 140, '16 / 40枚 · 待第5次', MUTED, 'center', 116)
    else:
        text(svg, 186, 126, '高爆弹 [生产中]' if zh else 'HE Shell [Running]', ORANGE, 'center', 116)
        text(svg, 186, 140, '16 / 40枚 · 第5 / 10次' if zh else '16 / 40 · Cycle 5 / 10', MUTED, 'center', 116)
    text(svg, RIGHT_CENTER, 26, '块锭粒可自动转换' if zh else 'Auto-convert', MUTED, 'center', 88)
    inputs = [('copper_ingot', 64), ('steel_ingot', 32), ('aluminum_ingot', 16), ('gunpowder', 48)]
    svg.append(f'<g data-role="input-cache" data-bounds="{CACHE_X:g} {INPUT_Y:g} {CACHE_WIDTH} {CACHE_HEIGHT}">')
    for index in range(8):
        slot(svg, CACHE_X + index % 4 * 18, INPUT_Y + index // 4 * 18, *(inputs[index] if index < len(inputs) else (None, None)))
    svg.append('</g>')
    x, y = RIGHT_CENTER, ARROW_Y
    arrow = f'M{x-5:g},{y+.5:g}H{x+5:g}V{y+10.5:g}H{x+9.5:g}L{x:g},{y+19.5:g}L{x-9.5:g},{y+10.5:g}H{x-5:g}Z'
    svg.append(f'<g data-role="production-progress" data-bounds="{x-10:g} {y:g} {ARROW_SIZE} {ARROW_SIZE}">')
    svg.append(f'<path d="{arrow}" fill="#0a2235" stroke="{CYAN}" stroke-width="1"/>')
    if state not in ['blocked', 'idle']:
        svg.append(f'<clipPath id="arrow"><path d="{arrow}"/></clipPath><rect x="{x-10:g}" y="{y:g}" width="20" height="12" fill="url(#progress)" clip-path="url(#arrow)"/>')
    svg.append('</g>')
    outputs = [('high_explosive_shell', 16), ('armor_piercing_shell', 8), ('straight_running_torpedo', 3), ('anti_aircraft_missile', 1)]
    if state == 'blocked':
        outputs = [('high_explosive_shell', 64)] * 8
    svg.append(f'<g data-role="output-cache" data-bounds="{CACHE_X:g} {OUTPUT_Y:g} {CACHE_WIDTH} {CACHE_HEIGHT}">')
    for index in range(8):
        slot(svg, CACHE_X + index % 4 * 18, OUTPUT_Y + index // 4 * 18, *(outputs[index] if index < len(outputs) else (None, None)))
    svg.append('</g>')
    rect(svg, 8, 151, WIDTH - 16, 1, '#397496')
    text(svg, 93, 157, '物品栏' if zh else 'Inventory', MUTED)
    for row in range(3):
        for column in range(9):
            slot(svg, 93 + column * 18, 170 + row * 18)
    for column in range(9):
        slot(svg, 93 + column * 18, 228)
    gradients = '''<linearGradient id="button" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0" stop-color="#496d88"/><stop offset="1" stop-color="#294c67"/></linearGradient>
<linearGradient id="disabled" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0" stop-color="#233f55"/><stop offset="1" stop-color="#162e43"/></linearGradient>
<linearGradient id="frame" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0" stop-color="#0e2a3e"/><stop offset="1" stop-color="#0b2032"/></linearGradient>
<linearGradient id="slot" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0" stop-color="#193c54"/><stop offset="1" stop-color="#123148"/></linearGradient>
<linearGradient id="progress" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0" stop-color="#4bd2ef"/><stop offset="1" stop-color="#158fbd"/></linearGradient>'''
    definition = f'<image id="backdrop" width="384" height="252" preserveAspectRatio="none" xlink:href="{data_url(BACKGROUND.read_bytes())}"/>'
    description = '复用既有背景和默认字形；本布局保留数值定稿前占位，正式配方见实现方案。' + ('本图费用仅为零单位省略与总需求换算的排版示例，不作为配方。' if sample else '费用以破折号表示。') + '数量与库存为排版示例。'
    document = f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{WIDTH*SCALE}" height="{HEIGHT*SCALE}" viewBox="0 0 {WIDTH} {HEIGHT}"><title>弹药生产线 {label} 布局预览</title><desc>{description}</desc><defs>{gradients}{definition}{"".join(glyph_defs.values())}</defs>{"".join(svg)}</svg>'
    path = OUT / f'弹药生产线-{label}.svg'
    path.write_text(document, encoding='utf-8')
    # 使用浏览器原生SVG渲染，保留九宫格裁切与渐变；独立配置不接触用户浏览器。
    edge = Path('C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe')
    png = path.with_suffix('.png')
    png.unlink(missing_ok=True)
    profile = ROOT / 'build/ammunition-production-line-preview' / f'edge-{state}-{language}-{"total" if total else "single"}'
    profile.mkdir(parents=True, exist_ok=True)
    subprocess.run([str(edge), '--headless', '--disable-gpu', '--hide-scrollbars', '--no-first-run',
                    '--no-default-browser-check', f'--user-data-dir={profile}',
                    f'--screenshot={png}', f'--window-size={WIDTH*SCALE},{HEIGHT*SCALE}', path.as_uri()],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True,
                   creationflags=subprocess.CREATE_NO_WINDOW)
    deadline = time.monotonic() + 25
    while not png.is_file() or png.stat().st_size == 0:
        if time.monotonic() > deadline:
            raise RuntimeError(f'Preview render timed out: {png.name}')
        time.sleep(.2)
    return {'state': state, 'language': language, 'demandMode': 'total' if total else 'single', 'illustrativeCostsOnly': sample,
            'svg': path.name, 'png': path.with_suffix('.png').name, 'text': used_text[start:]}

if __name__ == '__main__':
    OUT.mkdir(parents=True, exist_ok=True)
    previews = [render(*state) for state in states]
    manifest = {'date': '2026-10-26', 'status': '布局预览归档，非游戏截图；正式费用见弹药生产线实现方案',
                'logicalSize': [WIDTH, HEIGHT], 'scale': SCALE,
                'background': str(BACKGROUND.relative_to(ROOT)), 'backgroundSha256': hashlib.sha256(BACKGROUND.read_bytes()).hexdigest(),
                'sampling': {'trim': 3, 'corner': 45, 'outlineWidth': 1, 'outlineColor': '#397FA6'},
                'font': 'Minecraft 1.21.1 default bitmap providers and cached unifont.zip, glyph paths embedded in SVG',
                'quantity': {'unit': '枚', 'shellAndDepthChargeYield': 4, 'torpedoAndMissileYield': 1,
                             'correction': '向上取整到完整一次的产量，再限制到材料可支持的完整次数',
                             'progressSpacing': '比例统一为a / b，单位紧跟数字：16 / 40枚 · 第5 / 10次'},
                'rightColumn': {'centerX': RIGHT_CENTER, 'top': RIGHT_TOP, 'bottom': RIGHT_BOTTOM,
                                'hint': '块锭粒可自动转换', 'hintY': 26, 'hintColor': MUTED,
                                'inputBounds': [CACHE_X, INPUT_Y, CACHE_WIDTH, CACHE_HEIGHT],
                                'progressBounds': [RIGHT_CENTER - 10, ARROW_Y, ARROW_SIZE, ARROW_SIZE],
                                'outputBounds': [CACHE_X, OUTPUT_Y, CACHE_WIDTH, CACHE_HEIGHT],
                                'equalVerticalGap': RIGHT_GAP},
                'demand': {'follows': '当前浏览弹种及数量输入／滑条', 'changesWithJobProgress': False,
                           'zeroUnits': '省略', 'zeroCostRows': '隐藏', 'alignment': '图标名称居左，数量居右',
                           'toggle': '两档滑动开关，左上标题随模式切换'},
                'previews': previews}
    (OUT / '布局与素材说明.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('Generated', len(previews), 'SVG/PNG previews in', OUT)
