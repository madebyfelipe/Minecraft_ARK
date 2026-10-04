#!/usr/bin/env python3
"""Gera a arte das recompensas do Titanovenator (D57): o soro (a ampola com o sangue dele) e o projetor de êxtase.

- Ícones 16x16 da GUI, desenhados pixel a pixel num mapa de caracteres.
- Modelos 3D GeckoLib da mão, do chão e da moldura (D58, pedido do Felipe): geometria, textura, glowmask e
  animações, com a biblioteca de `geckoitem.py`.
  - Soro: ampola de vidro numa gaiola de aço com tampas, o sangue escuro aceso em âmbar por dentro, o rótulo
    "017" (o dossiê) e o êmbolo; um anel de luz âmbar sobe e desce devagar (`idle`).
  - Projetor: aparelho de mão do campo da contenção — corpo azul-aço com empunhadura, cabeça emissora com três
    garras em volta da lente ciano, o anel do campo que gira na frente, as luzes âmbar e a barra de carga.
    `ready` (anel girando, lente pulsando), `recharging` (anel lento, lente apagada) e `fire` (o disparo).

Prévia: `python3 tools/gen_titan_rewards.py --preview <pasta>`.
Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão. Arte autoral (nada do Revival).
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import geckoitem as gi  # noqa: E402
from geckoitem import Animation, Material, Model  # noqa: E402

ITEMS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures" / "item"

# Frasco de vidro com tampa de metal; o sangue escuro brilha em âmbar perto do vidro (a cor do ventre dele).
SERUM_PALETTE = {
    ".": None,
    "o": (34, 28, 30),      # contorno
    "m": (92, 98, 106),     # tampa
    "M": (150, 158, 166),   # brilho da tampa
    "g": (168, 206, 214),   # vidro
    "G": (232, 248, 252),   # reflexo
    "r": (88, 8, 10),       # sangue fundo
    "R": (140, 18, 14),     # sangue
    "a": (214, 132, 36),    # brilho âmbar
    "c": (46, 40, 38),      # rótulo carvão
}
SERUM = [
    "................",
    "......oooo......",
    ".....oMMmmo.....",
    ".....ommmmo.....",
    "......oggo......",
    "......oGgo......",
    ".....oGgggo.....",
    "....oGgRRggo....",
    "....oGRRRRgo....",
    "....ogRacRRo....",
    "....oGRccRRo....",
    "....oGRRRaro....",
    "....ogRRRRro....",
    "....ogrRRrro....",
    ".....orrrro.....",
    "......oooo......",
]

# Aparelho de mão: corpo de metal escuro com a lente ciano na frente e o cabo embaixo (na diagonal, como as armas).
PROJECTOR_PALETTE = {
    ".": None,
    "o": (20, 24, 28),      # contorno
    "d": (42, 52, 60),      # metal escuro
    "m": (74, 90, 100),     # metal
    "M": (128, 146, 156),   # brilho
    "c": (56, 198, 217),    # ciano do campo
    "C": (168, 244, 255),   # lente acesa
    "y": (255, 178, 30),    # luz âmbar
    "h": (60, 44, 34),      # cabo
}
PROJECTOR = [
    "................",
    "...........ooo..",
    "..........oCCco.",
    ".........ocCCCo.",
    "........omMccco.",
    ".......omMMmoo..",
    "......omMmmdo...",
    ".....omMmydo....",
    "....omMmmdo.....",
    "...omddddo......",
    "..ohoddo.o......",
    ".ohhoo..........",
    "ohhho...........",
    "ohho............",
    ".oo.............",
    "................",
]


def draw(grid, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, row
        for x, key in enumerate(row):
            color = palette[key]
            if color is not None:
                img.putpixel((x, y), color + (255,))
    return img


# ---------------------------------------------------------------- modelos 3D

def mat(base, noise=4, bevel=16, **kw):
    return Material(base, noise=noise, bevel=bevel, **kw)


STEEL = (150, 156, 164)
STEEL_DARK = (92, 98, 106)
CHARCOAL = (46, 40, 38)
BLOOD = (96, 10, 12)
AMBER = (214, 132, 36)
CYAN = (56, 198, 217)
CYAN_BRIGHT = (168, 244, 255)
BODY = (52, 66, 78)
BODY_DARK = (34, 42, 50)
GRIP = (30, 30, 34)


def pattern_blood():
    """O sangue escuro com o brilho âmbar perto do vidro: faixas verticais claras nas bordas e um veio no meio."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        if face in ("up", "down"):
            return
        for j in range(h):
            for i in range(w):
                edge = min(i, w - 1 - i)
                c = img.getpixel((x0 + i, y0 + j))
                t = 0.75 if edge == 0 else 0.35 if edge == 1 else 0.0
                if (i * 7 + j * 3) % 11 == 0:
                    t = max(t, 0.5)
                img.putpixel((x0 + i, y0 + j), gi.mix(c, AMBER, t) + (c[3],))
    return draw


def label_017():
    """O rótulo de carvão com o número do dossiê, em âmbar."""
    glyphs = {"0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
              "7": ["111", "001", "010", "010", "010"]}

    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        sx = x0 + (w - 11) // 2
        sy = y0 + (h - 5) // 2
        for k, ch in enumerate("017"):
            for j, row in enumerate(glyphs[ch]):
                for i, bit in enumerate(row):
                    if bit == "1" and x0 <= sx + k * 4 + i < x0 + w and y0 <= sy + j < y0 + h:
                        img.putpixel((sx + k * 4 + i, sy + j), AMBER + (255,))
    return draw


R = {
    "steel": mat(STEEL, 5, 20),
    "steel_dark": mat(STEEL_DARK, 4, 16),
    "charcoal": mat(CHARCOAL, 3, 10),
    "blood": Material(BLOOD, noise=8, bevel=0, glow=True, pattern=pattern_blood()),
    "amber": Material(AMBER, noise=6, bevel=0, glow=True, pattern=gi.pattern_glow_core((255, 230, 160))),
    "glass": mat((168, 206, 214), 4, 24),
    "rubber": mat((120, 24, 20), 3, 12),
    "body": mat(BODY, 4, 16),
    "body_dark": mat(BODY_DARK, 3, 12),
    "grip": mat(GRIP, 3, 12, pattern=gi.pattern_stipple(0.7)),
    "cyan": Material(CYAN, noise=6, bevel=0, glow=True, pattern=gi.pattern_glow_core(CYAN_BRIGHT)),
    "cyan_dim": mat(gi.shade(CYAN, -110), 4, 8),
    "lens": Material(CYAN_BRIGHT, noise=4, bevel=0, glow=True, pattern=gi.pattern_lens()),
    "amber_led": Material((255, 178, 30), noise=0, bevel=0, glow=True),
}


def build_serum():
    """A ampola em pé (+Y), ~9 px de altura: tampas de aço, gaiola de quatro barras, sangue aceso e o rótulo."""
    m = Model("titan_serum", density=8, seed=17)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 4, 0))
    m.octagon(body, "y", (0, 0), 0.0, 0.5, 3.0, R["steel_dark"], cap=R["steel_dark"])
    m.octagon(body, "y", (0, 0), 0.5, 1.2, 3.3, R["steel"], cap=R["steel"])
    m.octagon(body, "y", (0, 0), 1.2, 6.6, 2.3, R["blood"], cap=R["blood"])
    # Anéis de vidro no alto e embaixo do sangue (a borda da ampola).
    for y0 in (1.2, 6.2):
        m.octagon(body, "y", (0, 0), y0, y0 + 0.4, 2.55, R["glass"], cap=R["glass"])
    # Gaiola: quatro barras de aço nos cantos.
    for x, z in ((-1.25, -1.25), (1.25, -1.25), (-1.25, 1.25), (1.25, 1.25)):
        m.box(body, (x - 0.22, 1.2, z - 0.22), (x + 0.22, 6.6, z + 0.22), R["steel"])
    m.octagon(body, "y", (0, 0), 6.6, 7.3, 3.3, R["steel"], cap=R["steel"])
    m.octagon(body, "y", (0, 0), 7.3, 7.7, 2.6, R["steel_dark"], cap=R["steel_dark"])
    # Êmbolo de borracha vermelha e o LED âmbar.
    m.octagon(body, "y", (0, 0), 7.7, 8.9, 1.5, R["rubber"], cap=R["rubber"])
    m.box(body, (-0.25, 8.9, -0.25), (0.25, 9.1, 0.25), R["amber_led"])
    # Rótulo "017" na frente (+Z), entre as barras.
    m.box(body, (-0.95, 3.0, 1.12), (0.95, 4.6, 1.3), R["charcoal"], decals={"south": label_017()})
    # O anel de luz que corre pelo sangue.
    pulse = m.bone("pulse", "body", pivot=(0, 2.0, 0))
    m.octagon(pulse, "y", (0, 0), 1.8, 2.2, 2.42, R["amber"], cap=R["amber"])
    return m


def serum_animations():
    idle = Animation(3.0, loop=True)
    track = idle.bone("pulse")
    for k in range(13):
        t = 3.0 * k / 12
        f = 0.5 - 0.5 * math.cos(2 * math.pi * k / 12)
        track.move(t, (0, f * 3.8, 0))
    return {"idle": idle}


def build_projector():
    """O projetor deitado como uma arma: a lente aponta para −Z, a empunhadura desce. ~11 px."""
    m = Model("stasis_projector", density=6, seed=29)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 4, 2))
    # Corpo azul-aço com a barra de carga em cima e as luzes âmbar dos lados.
    m.box(body, (-1.3, 3.0, -2.0), (1.3, 5.8, 5.0), R["body"],
          decals={"east": gi.pattern_ribs(3, False, -18), "west": gi.pattern_ribs(3, False, -18)})
    m.box(body, (-1.0, 5.8, -1.5), (1.0, 6.3, 4.4), R["body_dark"])
    charge = m.bone("charge", "body", pivot=(0, 6.3, 4.2))
    m.box(charge, (-0.45, 6.3, -1.1), (0.45, 6.5, 4.2), R["cyan"])
    for x in (-1.32, 1.12):
        m.box(body, (x, 4.6, 3.4), (x + 0.2, 5.2, 4.2), R["amber_led"])
    m.box(body, (-1.0, 2.4, -1.4), (1.0, 3.0, 4.4), R["body_dark"])
    # Cabeça emissora: o colar, a lente acesa e as três garras.
    m.octagon(body, "z", (0, 4.4), -3.4, -2.0, 3.2, R["steel_dark"], cap=R["steel_dark"])
    lens = m.bone("lens", "body", pivot=(0, 4.4, -3.6))
    m.octagon(lens, "z", (0, 4.4), -3.8, -3.4, 1.8, R["lens"], cap=R["lens"])
    claws = m.bone("claws", "body", pivot=(0, 4.4, -3.4))
    for k in range(3):
        a = math.radians(90 + k * 120)
        x, y = math.cos(a) * 1.55, 4.4 + math.sin(a) * 1.55
        m.box(claws, (x - 0.3, y - 0.3, -5.6), (x + 0.3, y + 0.3, -3.2), R["steel"],
              rotation=(0, 0, math.degrees(a) - 90), pivot=(x, y, -4.4))
    # O anel do campo, que gira na frente da lente.
    ring = m.bone("ring", "body", pivot=(0, 4.4, -5.0))
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        x, y = math.cos(a) * 1.35, 4.4 + math.sin(a) * 1.35
        m.box(ring, (x - 0.55, y - 0.12, -5.15), (x + 0.55, y + 0.12, -4.85), R["cyan"],
              rotation=(0, 0, math.degrees(a) + 90), pivot=(x, y, -5.0))
    # Gatilho, guarda e empunhadura.
    m.box(body, (-0.3, 1.2, 0.6), (0.3, 2.4, 1.0), R["body_dark"])
    m.box(body, (-0.3, 0.9, 0.6), (0.3, 1.3, 3.4), R["body_dark"])
    m.box(body, (-0.16, 1.5, 1.6), (0.16, 2.4, 2.0), R["steel"])
    grip = m.bone("grip", "body", pivot=(0, 2.4, 3.6), rotation=(-14, 0, 0))
    m.box(grip, (-0.9, -1.8, 2.6), (0.9, 2.4, 4.8), R["grip"], faces={"up": R["body_dark"]})
    m.box(grip, (-1.0, -2.1, 2.5), (1.0, -1.7, 4.9), R["body_dark"])
    return m


def projector_animations():
    def spin(anim, length, turns, steps=12):
        ring = anim.bone("ring")
        for k in range(steps + 1):
            ring.turn(length * k / steps, (0, 0, 360.0 * turns * k / steps))

    ready = Animation(2.0, loop=True)
    spin(ready, 2.0, 1.0)
    lens = ready.bone("lens")
    for k in range(9):
        f = 1.0 + 0.12 * math.sin(2 * math.pi * k / 8)
        lens.size(2.0 * k / 8, (f, f, 1))
    ready.bone("charge").size(0.0, gi.SHOWN)

    recharging = Animation(4.0, loop=True)
    spin(recharging, 4.0, 0.25)
    recharging.bone("lens").size(0.0, (0.6, 0.6, 1))
    recharging.bone("charge").size(0.0, (1, 1, 0.08))

    fire = Animation(0.6)
    spin(fire, 0.6, 2.0)
    fire.bone("lens").size(0.0, (1.6, 1.6, 1)).size(0.15, (1.3, 1.3, 1)).size(0.6, (0.6, 0.6, 1))
    fire.bone("claws").tween("move", 0.0, 0.06, (0, 0, 0), (0, 0, 0.6), gi.ease_out, 2) \
        .tween("move", 0.06, 0.4, (0, 0, 0.6), (0, 0, 0), gi.ease_in_out, 4)
    fire.bone("body").tween("turn", 0.0, 0.05, (0, 0, 0), (8, 0, 0), gi.ease_out, 2) \
        .tween("turn", 0.05, 0.35, (8, 0, 0), (0, 0, 0), gi.ease_in_out, 4)
    fire.bone("charge").size(0.0, (1, 1, 0.08))
    return {"ready": ready, "recharging": recharging, "fire": fire}


def bounds(model):
    if not model.tex_w:
        model.pack()
    pts = np.vstack([q[0] for q in gi.posed_quads(model)]) * 16 - np.array([0, 0.16, 0])
    return pts.min(axis=0), pts.max(axis=0)


def centered(model, rotation, scale):
    lo, hi = bounds(model)
    return gi.anchored(rotation, scale, (lo + hi) / 2, (0, 0, 0))


FP_HAND = (0.56 * 16, -0.52 * 16, -0.72 * 16)


def display(model, cfg):
    """Mão em primeira e terceira pessoa ancoradas na pega; chão, moldura e cabeça centrados."""
    fp_rot, fp_scale = cfg["fp_rotation"], cfg["fp_scale"]
    fp_target = [cfg["fp_at"][i] - FP_HAND[i] for i in range(3)]
    tp_rot, tp_scale = cfg["tp_rotation"], cfg["tp_scale"]
    d = {
        "firstperson_righthand": {"rotation": fp_rot, "scale": [fp_scale] * 3,
                                  "translation": gi.anchored(fp_rot, fp_scale, cfg["hold"], fp_target)},
        "thirdperson_righthand": {"rotation": tp_rot, "scale": [tp_scale] * 3,
                                  "translation": gi.anchored(tp_rot, tp_scale, cfg["hold"], cfg["tp_at"])},
        "ground": {"rotation": [0, 0, 0], "translation": centered(model, [0, 0, 0], 0.5), "scale": [0.5] * 3},
        "fixed": {"rotation": cfg["fixed_rotation"], "translation": centered(model, cfg["fixed_rotation"], 1.0),
                  "scale": [1.0] * 3},
        "head": {"rotation": [0, 0, 0], "translation": centered(model, [0, 0, 0], 0.8), "scale": [0.8] * 3},
    }
    lo, hi = bounds(model)
    d["ground"]["translation"][1] -= (hi[1] - lo[1]) / 2 * 0.5 - 1.0
    d["head"]["translation"][1] += 14
    for entry in d.values():
        entry["translation"] = [round(v, 2) for v in entry["translation"]]
    return d


def item_model(name, display_entries):
    """3D em todo contexto; o ícone plano na GUI (como o Receptor e o Analisador)."""
    return {
        "loader": "forge:separate_transforms",
        "gui_light": "front",
        "textures": {"particle": f"iceagesurvival:item/{name}"},
        "base": {"parent": "builtin/entity", "display": display_entries},
        "perspectives": {"gui": {"loader": "forge:item_layers", "textures": {
            "layer0": f"iceagesurvival:item/{name}", "particle": f"iceagesurvival:item/{name}"}}},
    }


REWARDS = {
    # O soro: na primeira pessoa em pé, inclinado; na terceira, em pé no punho (X +90°, ver geckoitem).
    "titan_serum": {"build": build_serum, "animations": serum_animations, "hold": (0, 3.5, 0),
                    "fp_rotation": [0, 0, -12], "fp_scale": 0.85, "fp_at": (6.2, -7.2, -13.0),
                    "tp_rotation": [90, 0, 0], "tp_scale": 0.55, "tp_at": (0, -1.5, 1.6),
                    "fixed_rotation": [0, 0, 0], "idle": "idle"},
    # O projetor: segurado como uma arma de uma mão, a lente para a frente.
    "stasis_projector": {"build": build_projector, "animations": projector_animations, "hold": (0, 2.0, 3.6),
                         "fp_rotation": [0, 12, 0], "fp_scale": 1.0, "fp_at": (7.4, -6.2, -15.0),
                         "tp_rotation": [90, 0, 0], "tp_scale": 0.55, "tp_at": (0, -0.6, 1.8),
                         "fixed_rotation": [0, -90, 0], "idle": "ready"},
}


def write_3d(name):
    cfg = REWARDS[name]
    model = cfg["build"]()
    tex, glow = model.textures()
    tex.save(ITEMS / f"{name}_3d.png")
    glow.save(ITEMS / f"{name}_3d_glowmask.png")
    gi.write_json(gi.GEO / f"{name}.geo.json", model.geometry())
    gi.write_animations(name, cfg["animations"]())
    gi.write_json(gi.MODELS / f"{name}.json", item_model(name, display(model, cfg)))
    print(f"{name}: modelo 3D, textura {model.tex_w}x{model.tex_h}")


def preview(out_dir):
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, cfg in REWARDS.items():
        model = cfg["build"]()
        tex, glow = model.textures()
        anims = cfg["animations"]()
        disp = display(model, cfg)
        idle = anims[cfg["idle"]]
        views = []
        for yaw, pitch in ((-90, 0), (90, 0), (-40, 22), (150, -18)):
            quads = gi.posed_quads(model, idle, 0.4)
            Rm = gi.rx(math.radians(pitch)) @ gi.ry(math.radians(yaw))
            pts = np.vstack([q[0] for q in quads])
            center = pts.mean(axis=0)
            cam = np.array([gi.apply4(Rm, p - center) for p in pts])
            lo, hi = cam.min(axis=0), cam.max(axis=0)
            size = (480, 400)
            sc = 0.85 * min(size[0] / (hi[0] - lo[0]), size[1] / (hi[1] - lo[1]))
            mid = ((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2)
            views.append(gi.rasterize(quads, tex, glow, lambda p, Rm=Rm, c=center: gi.apply4(Rm, p - c),
                                      gi.orthographic(size[0], size[1], sc, mid), size, supersample=2))
        grid = Image.new("RGB", (960, 800))
        for k, img in enumerate(views):
            grid.paste(img, ((k % 2) * 480, (k // 2) * 400))
        grid.save(out_dir / f"{name}_model.png")
        W, H = 854, 480
        fp = gi.first_person_chain(disp["firstperson_righthand"])
        img = gi.rasterize(gi.posed_quads(model, idle, 0.4), tex, glow, lambda p: gi.apply4(fp, p),
                           gi.perspective(W, H), (W, H), background=(128, 160, 196), supersample=2)
        d = ImageDraw.Draw(img)
        d.line((W / 2 - 6, H / 2, W / 2 + 6, H / 2), fill=(255, 255, 255))
        d.line((W / 2, H / 2 - 6, W / 2, H / 2 + 6), fill=(255, 255, 255))
        img.save(out_dir / f"{name}_firstperson.png")
    print(f"prévias em {out_dir}")


def main():
    if len(sys.argv) >= 3 and sys.argv[1] == "--preview":
        preview(Path(sys.argv[2]))
        return
    ITEMS.mkdir(parents=True, exist_ok=True)
    outputs = {
        ITEMS / "titan_serum.png": draw(SERUM, SERUM_PALETTE),
        ITEMS / "stasis_projector.png": draw(PROJECTOR, PROJECTOR_PALETTE),
    }
    for path, img in outputs.items():
        img.save(path)
        print(path.name)
    for name in REWARDS:
        write_3d(name)


if __name__ == "__main__":
    main()
