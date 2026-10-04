#!/usr/bin/env python3
"""Biblioteca dos itens 3D do GeckoLib feitos à mão em código: geometria, textura pintada, animações, transformações
de exibição e prévia renderizada (usada por `gen_dc2_weapons.py` e `gen_titan_rewards.py`).

**Coordenadas.** Tudo aqui é escrito nas coordenadas do Java (o que o jogo desenha), em pixels de modelo (1/16 de
bloco): X para o lado, Y para cima, Z para a frente/trás. A conversão para o JSON Bedrock que o GeckoLib lê é feita na
exportação, conferida no bytecode do GeckoLib 4.7.2 (`BakedModelFactory$Builtin`, `GeoQuad`, `RenderUtils`,
`BakedAnimationsAdapter`):

- cubo: `origin.x` do JSON = −(x + largura); Y e Z iguais;
- rotação de cubo, de osso e de quadro de animação: o JSON leva (−rx, −ry, rz) da rotação Java desejada; o GeckoLib
  aplica Z, depois Y, depois X (no vértice: X primeiro), em torno do pivô;
- pivô: `x` do JSON = −x;
- posição de quadro de animação: `x` do JSON = −x;
- faces: `east` do JSON é a face +X do Java, `west` a −X, `north` −Z, `south` +Z.
- UV por face: vista de fora, u cresce para a direita e v para baixo; nas laterais o alto da região é +Y; em `up` o
  alto da região é +Z e u cresce para −X; em `down` o alto é −Z e u cresce para −X.

A prévia (`render`) refaz exatamente essa cadeia (e a da mão em primeira pessoa, da mão em terceira pessoa e da GUI),
com z-buffer e textura, para conferir a arte sem abrir o jogo. Não vai para o jar.

Arte autoral: nada copiado de jogo ou mod nenhum.
"""

import json
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
TEXTURES = ASSETS / "textures" / "item"
MODELS = ASSETS / "models" / "item"
GEO = ASSETS / "geo" / "item"
ANIMATIONS = ASSETS / "animations" / "item"

FACES = ("north", "south", "east", "west", "up", "down")


# ---------------------------------------------------------------- materiais

def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Material:
    """Uma cor-base com ruído e chanfro. `pattern` desenha um detalhe repetido; `glow` acende no escuro."""

    def __init__(self, base, noise=6, bevel=18, pattern=None, glow=False, speckle=0.0, alpha=255):
        self.base = base
        self.noise = noise
        self.bevel = bevel
        self.pattern = pattern
        self.glow = glow
        self.speckle = speckle
        self.alpha = alpha


def paint_face(img, rect, material, face, rng, density):
    """Pinta uma região: cor com ruído, chanfro claro no alto/esquerda e escuro embaixo/direita, e o padrão."""
    x0, y0, w, h = rect
    up_bias = {"up": 14, "down": -16}.get(face, 0)
    for j in range(h):
        for i in range(w):
            n = rng.randint(-material.noise, material.noise) if material.noise else 0
            c = shade(material.base, n + up_bias)
            if material.speckle and rng.random() < material.speckle:
                c = shade(c, rng.choice((-22, 18)))
            if material.bevel and w >= 2 and h >= 2:
                if j == 0 or i == 0:
                    c = shade(c, material.bevel)
                elif j == h - 1 or i == w - 1:
                    c = shade(c, -material.bevel)
            img.putpixel((x0 + i, y0 + j), c + (material.alpha,))
    if material.pattern:
        material.pattern(img, rect, face, rng, density)


# Padrões (desenhados por cima da cor-base, dentro da região).

def pattern_serrations(step=2, depth=-34):
    """Ranhuras verticais (o serrilhado do ferrolho)."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        if face in ("up", "down"):
            return
        for i in range(1, w - 1):
            if i % step == 0:
                for j in range(1, h - 1):
                    c = img.getpixel((x0 + i, y0 + j))
                    img.putpixel((x0 + i, y0 + j), shade(c, depth) + (c[3],))
    return draw


def pattern_stipple(amount=0.45):
    """Textura de empunhadura: pontos claros e escuros alternados."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        for j in range(1, h - 1):
            for i in range(1, w - 1):
                if (i + j) % 2 == 0 and rng.random() < amount:
                    c = img.getpixel((x0 + i, y0 + j))
                    img.putpixel((x0 + i, y0 + j), shade(c, -18 if (i // 2 + j) % 2 else 14) + (c[3],))
    return draw


def pattern_ribs(step=3, horizontal=True, depth=-30):
    """Frisos: linhas escuras a cada `step` texels (guarda-mão, telha do cano)."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        for j in range(1, h - 1):
            for i in range(1, w - 1):
                k = j if horizontal else i
                if k % step == 0:
                    c = img.getpixel((x0 + i, y0 + j))
                    img.putpixel((x0 + i, y0 + j), shade(c, depth) + (c[3],))
    return draw


def pattern_holes(step=4, size=2, dark=(10, 10, 12)):
    """Furos de ventilação (o escudo térmico da escopeta), em quincôncio."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        if face in ("north", "south") and w < 6:
            return
        row = 0
        for j in range(1, h - size, step):
            offset = (step // 2) if row % 2 else 0
            for i in range(1 + offset, w - size, step):
                for dj in range(size):
                    for di in range(size):
                        edge = dj == 0 or di == 0
                        img.putpixel((x0 + i + di, y0 + j + dj), (shade(dark, 26) if edge else dark) + (255,))
            row += 1
    return draw


def pattern_hazard(step=4, dark=(24, 22, 20)):
    """Faixas diagonais de alerta (preto sobre amarelo)."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        for j in range(h):
            for i in range(w):
                if ((i + j) // step) % 2 == 0:
                    img.putpixel((x0 + i, y0 + j), shade(dark, rng.randint(-4, 4)) + (255,))
    return draw


def pattern_glow_core(bright=(255, 255, 255)):
    """Miolo aceso: o centro mais claro que a borda."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for j in range(h):
            for i in range(w):
                d = math.hypot((i - cx) / max(1, w / 2), (j - cy) / max(1, h / 2))
                c = img.getpixel((x0 + i, y0 + j))
                img.putpixel((x0 + i, y0 + j), mix(c, bright, max(0.0, 0.85 - d) * 0.9) + (c[3],))
    return draw


def pattern_lens():
    """Vidro de lente: escuro com um reflexo em diagonal."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        for j in range(h):
            for i in range(w):
                c = img.getpixel((x0 + i, y0 + j))
                if abs((i - j) - (w - h) / 2 - w * 0.2) <= 0.6:
                    img.putpixel((x0 + i, y0 + j), mix(c, (230, 250, 255), 0.7) + (c[3],))
    return draw


def pattern_combine(*patterns):
    def draw(img, rect, face, rng, density):
        for p in patterns:
            p(img, rect, face, rng, density)
    return draw


# ---------------------------------------------------------------- geometria

class Cube:
    def __init__(self, lo, hi, material, faces=None, hide=(), rotation=None, pivot=None, inflate=0.0, decals=None):
        self.lo = tuple(lo)
        self.hi = tuple(hi)
        self.material = material
        self.faces = faces or {}
        self.hide = set(hide)
        self.rotation = rotation
        self.pivot = pivot
        self.inflate = inflate
        self.decals = decals or {}
        self.uv = {}

    @property
    def size(self):
        return tuple(self.hi[i] - self.lo[i] for i in range(3))

    def face_dims(self, face):
        sx, sy, sz = self.size
        return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy),
                "up": (sx, sz), "down": (sx, sz)}[face]

    def material_for(self, face):
        return self.faces.get(face, self.material)


class Bone:
    def __init__(self, name, parent, pivot, rotation=None):
        self.name = name
        self.parent = parent
        self.pivot = tuple(pivot)
        self.rotation = rotation
        self.cubes = []


class Model:
    """Ossos e cubos em coordenadas Java (pixels de modelo)."""

    def __init__(self, name, density=4, seed=7):
        self.name = name
        self.density = density
        self.seed = seed
        self.bones = []
        self.by_name = {}
        self.tex_w = self.tex_h = 0

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        b = Bone(name, parent, pivot, rotation)
        self.bones.append(b)
        self.by_name[name] = b
        return b

    def box(self, bone, lo, hi, material, **kw):
        lo, hi = list(lo), list(hi)
        for i in range(3):
            if lo[i] > hi[i]:
                lo[i], hi[i] = hi[i], lo[i]
        c = Cube(lo, hi, material, **kw)
        (self.by_name[bone] if isinstance(bone, str) else bone).cubes.append(c)
        return c

    def octagon(self, bone, axis, center, start, end, diameter, material, cap=None, hide_caps=()):
        """Cilindro de 8 lados ao longo de `axis` ('x', 'y' ou 'z'): quatro caixas giradas de 0, 45, 90 e 135°.

        `center` são as duas outras coordenadas do eixo; `start`/`end`, a extensão ao longo dele.
        """
        w = diameter * math.tan(math.radians(22.5))
        a, b = center
        cubes = []
        for k, (da, db) in enumerate(((diameter, w), (w, diameter), (diameter, w), (w, diameter))):
            angle = 45.0 if k >= 2 else 0.0
            if axis == "z":
                lo, hi = (a - da / 2, b - db / 2, start), (a + da / 2, b + db / 2, end)
                rot = (0, 0, angle)
                pivot = (a, b, (start + end) / 2)
                caps = ("north", "south")
            elif axis == "x":
                lo, hi = (start, a - da / 2, b - db / 2), (end, a + da / 2, b + db / 2)
                rot = (angle, 0, 0)
                pivot = ((start + end) / 2, a, b)
                caps = ("east", "west")
            else:
                lo, hi = (a - da / 2, start, b - db / 2), (a + da / 2, end, b + db / 2)
                rot = (0, angle, 0)
                pivot = (a, (start + end) / 2, b)
                caps = ("up", "down")
            faces = {face: cap for face in caps} if cap else None
            # As tampas das quatro caixas caem no mesmo plano: só a primeira desenha, sem briga de profundidade.
            hide = set(hide_caps) | (set(caps) if k > 0 else set())
            cubes.append(self.box(bone, lo, hi, material, faces=faces, hide=hide,
                                  rotation=rot if angle else None, pivot=pivot if angle else None))
        return cubes

    # ---- textura

    def pack(self):
        """Uma região de textura por face visível, empacotadas em prateleiras; acha o menor lado que serve."""
        regions = []
        for bone in self.bones:
            for cube in bone.cubes:
                for face in FACES:
                    if face in cube.hide:
                        continue
                    fw, fh = cube.face_dims(face)
                    w = max(1, int(round(fw * self.density)))
                    h = max(1, int(round(fh * self.density)))
                    regions.append((h, w, cube, face))
        regions.sort(key=lambda r: (-r[0], -r[1]))
        for side in (32, 64, 128, 256, 512, 1024):
            x = y = shelf = 0
            placed = []
            ok = True
            for h, w, cube, face in regions:
                if w > side:
                    ok = False
                    break
                if x + w > side:
                    x, y, shelf = 0, y + shelf, 0
                if y + h > side:
                    ok = False
                    break
                placed.append((cube, face, (x, y, w, h)))
                x += w
                shelf = max(shelf, h)
            if ok:
                self.tex_w = self.tex_h = side
                for cube, face, rect in placed:
                    cube.uv[face] = rect
                return
        raise ValueError(f"{self.name}: textura não cabe em 1024")

    def textures(self):
        self.pack()
        rng = random.Random(self.seed)
        tex = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        glow = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        for bone in self.bones:
            for cube in bone.cubes:
                for face, rect in cube.uv.items():
                    material = cube.material_for(face)
                    paint_face(tex, rect, material, face, rng, self.density)
                    decal = cube.decals.get(face) or cube.decals.get("*")
                    if decal:
                        decal(tex, rect, face, rng, self.density)
                    if material.glow:
                        x0, y0, w, h = rect
                        for j in range(h):
                            for i in range(w):
                                glow.putpixel((x0 + i, y0 + j), tex.getpixel((x0 + i, y0 + j)))
        return tex, glow

    # ---- exportação

    def geometry(self):
        bones = []
        for bone in self.bones:
            entry = {"name": bone.name, "pivot": [r(-bone.pivot[0]), r(bone.pivot[1]), r(bone.pivot[2])]}
            if bone.parent:
                entry["parent"] = bone.parent
            if bone.rotation:
                rx, ry, rz = bone.rotation
                entry["rotation"] = [r(-rx), r(-ry), r(rz)]
            cubes = []
            for c in bone.cubes:
                (x0, y0, z0), (sx, sy, sz) = c.lo, c.size
                cube = {"origin": [r(-(x0 + sx)), r(y0), r(z0)], "size": [r(sx), r(sy), r(sz)], "uv": {}}
                for face, (u, v, w, h) in c.uv.items():
                    cube["uv"][face] = {"uv": [u, v], "uv_size": [w, h]}
                if c.rotation:
                    rx, ry, rz = c.rotation
                    cube["rotation"] = [r(-rx), r(-ry), r(rz)]
                    px, py, pz = c.pivot
                    cube["pivot"] = [r(-px), r(py), r(pz)]
                if c.inflate:
                    cube["inflate"] = c.inflate
                cubes.append(cube)
            if cubes:
                entry["cubes"] = cubes
            bones.append(entry)
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": f"geometry.{self.name}",
                    "texture_width": self.tex_w,
                    "texture_height": self.tex_h,
                    "visible_bounds_width": 4,
                    "visible_bounds_height": 3,
                    "visible_bounds_offset": [0, 0.5, 0],
                },
                "bones": bones,
            }],
        }


def r(v):
    v = round(v, 4)
    return 0.0 if v == 0 else v


# ---------------------------------------------------------------- animações

HIDDEN = [0.001, 0.001, 0.001]   # escala "apagada" (zero estraga as normais do PoseStack)
SHOWN = [1, 1, 1]


def ease_out(t):
    return 1 - (1 - t) ** 3


def ease_in_out(t):
    return t * t * (3 - 2 * t)


class Track:
    """Quadros de um osso em coordenadas Java: `move` (posição), `turn` (graus) e `size` (escala)."""

    def __init__(self):
        self.position = {}
        self.rotation = {}
        self.scale = {}

    def move(self, t, xyz):
        self.position[t] = list(xyz)
        return self

    def turn(self, t, xyz):
        self.rotation[t] = list(xyz)
        return self

    def size(self, t, xyz):
        self.scale[t] = list(xyz)
        return self

    def tween(self, kind, t0, t1, a, b, curve=ease_in_out, steps=6):
        """Quadros intermediários de `a` a `b` entre `t0` e `t1` pela curva (o GeckoLib interpola reto entre eles)."""
        target = {"move": self.position, "turn": self.rotation, "size": self.scale}[kind]
        for k in range(steps + 1):
            t = t0 + (t1 - t0) * k / steps
            f = curve(k / steps)
            target[t] = [a[i] + (b[i] - a[i]) * f for i in range(3)]
        return self


class Animation:
    def __init__(self, length, loop=False):
        self.length = length
        self.loop = loop
        self.tracks = {}

    def bone(self, name):
        return self.tracks.setdefault(name, Track())

    def export(self):
        bones = {}
        for name, track in self.tracks.items():
            entry = {}
            if track.position:
                entry["position"] = {f"{t:.4f}": [r(-v[0]), r(v[1]), r(v[2])] for t, v in sorted(track.position.items())}
            if track.rotation:
                entry["rotation"] = {f"{t:.4f}": [r(-v[0]), r(-v[1]), r(v[2])] for t, v in sorted(track.rotation.items())}
            if track.scale:
                entry["scale"] = {f"{t:.4f}": [r(v[0]), r(v[1]), r(v[2])] for t, v in sorted(track.scale.items())}
            bones[name] = entry
        data = {"animation_length": self.length, "bones": bones}
        if self.loop:
            data["loop"] = True
        return data

    def sample(self, name, t):
        """Posição, rotação e escala do osso no tempo `t` (interpolação reta, como o GeckoLib), em coordenadas Java."""
        track = self.tracks.get(name)
        if not track:
            return (0, 0, 0), (0, 0, 0), (1, 1, 1)

        def at(keys, default):
            if not keys:
                return default
            times = sorted(keys)
            if t <= times[0]:
                return keys[times[0]]
            for a, b in zip(times, times[1:]):
                if a <= t <= b:
                    f = 0 if b == a else (t - a) / (b - a)
                    return [keys[a][i] + (keys[b][i] - keys[a][i]) * f for i in range(3)]
            return keys[times[-1]]
        return at(track.position, (0, 0, 0)), at(track.rotation, (0, 0, 0)), at(track.scale, (1, 1, 1))


def write_animations(name, animations):
    data = {"format_version": "1.8.0",
            "animations": {f"animation.{name}.{key}": anim.export() for key, anim in animations.items()}}
    write_json(ANIMATIONS / f"{name}.animation.json", data)


# ---------------------------------------------------------------- matrizes

def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], float)


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], float)


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], float)


def tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def sc(x, y=None, z=None):
    y = x if y is None else y
    z = x if z is None else z
    return np.diag([x, y, z, 1.0])


def rot_xyz_deg(rot):
    """A rotação de exibição do JSON de item: JOML rotationXYZ = Rx·Ry·Rz."""
    a, b, c = (math.radians(v) for v in rot)
    return rx(a) @ ry(b) @ rz(c)


def rot_zyx_java_deg(rot):
    """Rotação Java (graus) de cubo/osso como o GeckoLib aplica: mulPose Z, depois Y, depois X."""
    a, b, c = (math.radians(v) for v in rot)
    return rz(c) @ ry(b) @ rx(a)


def display_matrix(entry):
    """ItemTransform.apply: translação (/16, limitada a ±80), rotação XYZ e escala."""
    t = [max(-80.0, min(80.0, v)) / 16 for v in entry.get("translation", (0, 0, 0))]
    s = entry.get("scale", (1, 1, 1))
    return tr(*t) @ rot_xyz_deg(entry.get("rotation", (0, 0, 0))) @ sc(*s)


# ---------------------------------------------------------------- quads posados

def posed_quads(model, animation=None, t=0.0, hidden_bones=()):
    """As faces do modelo em blocos, no espaço do item depois do deslocamento do GeckoLib (−0,5 + 0,5, +0,01 em Y)."""
    world = {}
    out = []
    for bone in model.bones:
        parent = world[bone.parent] if bone.parent else tr(0, 0.01, 0)
        px, py, pz = (v / 16 for v in bone.pivot)
        pos, rot, scl = animation.sample(bone.name, t) if animation else ((0, 0, 0), (0, 0, 0), (1, 1, 1))
        base_rot = bone.rotation or (0, 0, 0)
        total_rot = [base_rot[i] + rot[i] for i in range(3)]
        m = parent @ tr(pos[0] / 16, pos[1] / 16, pos[2] / 16) @ tr(px, py, pz) @ rot_zyx_java_deg(total_rot) \
            @ sc(*scl) @ tr(-px, -py, -pz)
        world[bone.name] = m
        if bone.name in hidden_bones or min(scl) < 0.01:
            continue
        for c in bone.cubes:
            cm = m
            if c.rotation:
                cx, cy, cz = (v / 16 for v in c.pivot)
                cm = m @ tr(cx, cy, cz) @ rot_zyx_java_deg(c.rotation) @ tr(-cx, -cy, -cz)
            x0, y0, z0 = (v / 16 for v in c.lo)
            x1, y1, z1 = (v / 16 for v in c.hi)
            quads = {
                "west": [(x0, y1, z1), (x0, y1, z0), (x0, y0, z0), (x0, y0, z1)],
                "east": [(x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0)],
                "north": [(x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0)],
                "south": [(x1, y1, z1), (x0, y1, z1), (x0, y0, z1), (x1, y0, z1)],
                "up": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
                "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
            }
            for face, (u, v, w, h) in c.uv.items():
                pts = np.array([list(p) + [1.0] for p in quads[face]]).T
                p = (cm @ pts)[:3].T
                uvs = [(u + w, v), (u, v), (u, v + h), (u + w, v + h)]
                out.append((p, uvs, face))
    return out


# ---------------------------------------------------------------- rasterizador

def rasterize(quads, tex, glow, to_camera, project, size, background=(70, 80, 96), light=(-0.35, 0.75, 0.55),
              ambient=0.45, supersample=1, mirrored=False):
    """z-buffer com textura (vizinho mais próximo, correção de perspectiva) e luz por face; o brilho ignora a luz."""
    W, H = size[0] * supersample, size[1] * supersample
    color = np.zeros((H, W, 3), float)
    color[:] = background
    depth = np.full((H, W), np.inf)
    T = np.asarray(tex.convert("RGBA"), float)
    G = np.asarray(glow.convert("RGBA"), float) if glow is not None else None
    th, tw = T.shape[:2]
    L = np.array(light, float)
    L /= np.linalg.norm(L)
    for pts, uvs, face in quads:
        cam = np.array([to_camera(p) for p in pts])
        n = np.cross(cam[1] - cam[0], cam[3] - cam[0])
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            continue
        n /= ln
        if mirrored:
            n = -n
        bright = ambient + (1 - ambient) * max(0.0, float(n @ L))
        scr = np.array([project(p) for p in cam])  # (x, y, w) com w = profundidade positiva
        if np.any(scr[:, 2] <= 1e-4):
            continue
        sx = scr[:, 0] * supersample
        sy = scr[:, 1] * supersample
        for tri in ((0, 1, 2), (0, 2, 3)):
            xs, ys, ws = sx[list(tri)], sy[list(tri)], scr[list(tri), 2]
            area = (xs[1] - xs[0]) * (ys[2] - ys[0]) - (xs[2] - xs[0]) * (ys[1] - ys[0])
            if abs(area) < 1e-9:
                continue
            x_lo, x_hi = max(0, int(math.floor(xs.min()))), min(W - 1, int(math.ceil(xs.max())))
            y_lo, y_hi = max(0, int(math.floor(ys.min()))), min(H - 1, int(math.ceil(ys.max())))
            if x_lo > x_hi or y_lo > y_hi:
                continue
            gx, gy = np.meshgrid(np.arange(x_lo, x_hi + 1) + 0.5, np.arange(y_lo, y_hi + 1) + 0.5)
            w0 = ((xs[1] - gx) * (ys[2] - gy) - (xs[2] - gx) * (ys[1] - gy)) / area
            w1 = ((xs[2] - gx) * (ys[0] - gy) - (xs[0] - gx) * (ys[2] - gy)) / area
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            inv = w0 / ws[0] + w1 / ws[1] + w2 / ws[2]
            z = 1 / inv
            u = (w0 * uvs[tri[0]][0] / ws[0] + w1 * uvs[tri[1]][0] / ws[1] + w2 * uvs[tri[2]][0] / ws[2]) * z
            v = (w0 * uvs[tri[0]][1] / ws[0] + w1 * uvs[tri[1]][1] / ws[1] + w2 * uvs[tri[2]][1] / ws[2]) * z
            ui = np.clip(np.floor(u).astype(int), 0, tw - 1)
            vi = np.clip(np.floor(v).astype(int), 0, th - 1)
            texel = T[vi, ui]
            region = depth[y_lo:y_hi + 1, x_lo:x_hi + 1]
            mask = inside & (texel[..., 3] > 127) & (z < region)
            if not mask.any():
                continue
            rgb = texel[..., :3] * bright
            if G is not None:
                g = G[vi, ui]
                lit = g[..., 3] > 127
                rgb = np.where(lit[..., None], texel[..., :3], rgb)
            region[mask] = z[mask]
            color[y_lo:y_hi + 1, x_lo:x_hi + 1][mask] = rgb[mask]
    img = Image.fromarray(np.clip(color, 0, 255).astype(np.uint8), "RGB")
    if supersample > 1:
        img = img.resize(size, Image.LANCZOS)
    return img


def perspective(width, height, fov_deg=70.0):
    f = (height / 2) / math.tan(math.radians(fov_deg) / 2)

    def project(p):
        x, y, z = p
        d = -z
        return (width / 2 + x / d * f, height / 2 - y / d * f, d)
    return project


def orthographic(width, height, scale, center=(0.0, 0.0)):
    def project(p):
        x, y, z = p
        return (width / 2 + (x - center[0]) * scale, height / 2 - (y - center[1]) * scale, 100.0 - z)
    return project


def apply4(m, p):
    v = m @ np.array([p[0], p[1], p[2], 1.0])
    return v[:3]


# ---------------------------------------------------------------- cenas de prévia

def first_person_chain(display_fp, hand_transform=None, side=1):
    """Matriz da mão direita em primeira pessoa, no espaço da câmera (olhando para −Z): a transformação da mão (a
    nossa `applyForgeHandTransform` em repouso, igual à do vanilla) e a exibição `firstperson_righthand`. O −0,5 do
    ItemRenderer e o +0,5 do GeckoLib já estão em `posed_quads`."""
    hand = hand_transform if hand_transform is not None else tr(side * 0.56, -0.52, -0.72)
    return hand @ display_matrix(display_fp)


def third_person_chain(display_tp, arm_x_rot=-math.pi / 2 + 0.1, arm_y_rot=-0.3):
    """Mão direita em terceira pessoa com o braço na pose de mira da besta, no espaço do modelo do jogador (Y para
    baixo, como o ModelPart): o braço (pivô no ombro, rotação ZYX), o ItemInHandLayer (X −90°, Y 180°, translação até a
    mão) e a exibição `thirdperson_righthand`. Devolve a matriz do item e a do braço (para o braço de referência)."""
    arm = tr(-5 / 16, 2 / 16, 0) @ rz(0) @ ry(arm_y_rot) @ rx(arm_x_rot)
    item = arm @ rx(math.radians(-90)) @ ry(math.radians(180)) @ tr(1 / 16, 0.125, -0.625) @ display_matrix(display_tp)
    return item, arm


def anchored(rotation, scale, point, target):
    """A translação de exibição (em px) que leva o ponto `point` do modelo (px) a `target` (px, no espaço da exibição)."""
    R = rot_xyz_deg(rotation)[:3, :3]
    p = R @ (np.array(point, float) * scale + np.array([0.0, 0.16 * scale, 0.0]))
    return [round(float(target[i] - p[i]), 3) for i in range(3)]


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
