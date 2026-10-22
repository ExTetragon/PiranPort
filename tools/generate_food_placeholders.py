"""Generate reproducible 16x16 food placeholders using only the Python standard library."""
from pathlib import Path
import hashlib
import json
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/piranport/models/item'
TEXTURES = ROOT / 'src/main/resources/assets/piranport/textures/item'

def png(pixels, width=16, height=16):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    raw = b''.join(bytes([0]) + bytes(c for pixel in pixels[y * width:(y + 1) * width] for c in pixel) for y in range(height))
    return bytes([137,80,78,71,13,10,26,10]) + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')

def icon(name):
    pixels = [(0,0,0,0)] * 256
    seed = hashlib.sha256(name.encode()).digest()
    filling = (125 + seed[0] % 85, 65 + seed[1] % 85, 30 + seed[2] % 55, 255)
    dark = (77,43,26,255)
    bread = (224,166,78,255)
    light = (250,218,150,255)
    green = (91,151,63,255)
    def dot(x,y,color):
        if 0 <= x < 16 and 0 <= y < 16: pixels[y*16+x] = color
    def rect(x0,y0,x1,y1,color):
        for y in range(y0,y1+1):
            for x in range(x0,x1+1): dot(x,y,color)
    def oval(x0,y0,x1,y1,color):
        rx=(x1-x0+1)/2; ry=(y1-y0+1)/2; cx=(x0+x1)/2; cy=(y0+y1)/2
        for y in range(y0,y1+1):
            for x in range(x0,x1+1):
                if ((x-cx)/rx)**2+((y-cy)/ry)**2 <= 1:dot(x,y,color)
    if any(k in name for k in ['kvass','juice','with_beer']):
        rect(6,1,9,3,dark);rect(5,4,10,5,dark);rect(4,6,11,13,dark)
        rect(5,5,10,12,filling);rect(5,8,10,10,(232,221,186,255));rect(5,6,5,7,light)
    elif 'ice_cream' in name:
        for y in range(8,15):rect(5+(y-8)//2,y,10-(y-8)//2,y,bread)
        oval(3,2,12,9,(237,190,191,255));oval(5,1,10,6,(248,224,183,255));dot(7,3,(255,244,222,255))
    elif any(k in name for k in ['burger','sandwich','hot_dog']):
        oval(2,3,13,9,dark);oval(3,3,12,8,bread);rect(2,8,13,9,green);rect(3,10,12,11,filling);rect(3,12,12,13,bread)
        for x,y in [(5,5),(9,4),(11,6)]:dot(x,y,light)
    elif any(k in name for k in ['bread','scone','baozi']):
        oval(2,3,13,13,dark);oval(3,3,12,12,bread);rect(5,4,6,8,light);rect(9,4,10,7,light)
    elif any(k in name for k in ['pie','pizza','gateau','tatin','macaron','yokan']):
        oval(1,10,14,14,(210,219,224,255));rect(3,6,12,11,dark);rect(3,5,12,9,bread);rect(4,4,11,7,filling)
        for x,y in [(5,5),(8,4),(10,6)]:dot(x,y,light)
    else:
        oval(1,8,14,14,dark);oval(2,5,13,12,(214,221,222,255));oval(3,5,12,10,filling)
        for i in range(7):dot(4+seed[i]%8,6+seed[i+8]%4,light if i%2 else green)
        rect(4,12,11,13,(174,185,188,255))
    return pixels

if __name__ == '__main__':
    count = 0
    for model in sorted(MODELS.glob('*.json')):
        reference = json.loads(model.read_text(encoding='utf-8'))
        target = TEXTURES / (model.stem + '.png')
        if reference.get('parent') == 'minecraft:item/generated' and not target.exists():
            target.write_bytes(png(icon(model.stem)))
            count += 1
    print(f'Created {count} food placeholder textures')
