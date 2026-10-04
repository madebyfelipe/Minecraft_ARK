#!/usr/bin/env python3
"""Animações autorais do Titanovenator (Python 3, sem bibliotecas externas).

Escreve art/titanovenator/animations/titanovenator.animation.json no formato do
GeckoLib (Bedrock 1.8.0), com o prefixo `animation.titanovenator.`. Todo movimento é
escrito aqui; nada vem do arquivo de animação do Revival. O arquivo só nomeia os ossos
do rig de base (wholebody, lowerBody, neck, head, lowerJaw, tail1-4, coxas, braços…) e
soma valores aos ângulos de repouso do modelo.

Convenção (a do arquivo geo): rotação X positiva abaixa a frente do osso; nas coxas e
canelas, positiva leva o pé para trás. Só usamos posição em Y (eixo sem ambiguidade).

Nomes seguem os ganchos que o LandCreature já tem: idle, walk, run, attack (mordida +
tração), attack_2 (pancada de corpo), speak (rugido de ameaça), call (chamado
territorial), eat, unconscious. Fases do boss: sufixos _f2 e _f3 e `roar_phase`.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/iceagesurvival/animations/entity/titanovenator.animation.json'
PREFIX = 'animation.titanovenator.'
TAU = 2 * math.pi

# Fases do boss. Premissa de trabalho (Felipe definiu 3 fases, não o que cada uma faz):
# cada fase escala cadência, postura e amplitude. Mude só esta tabela para reajustar.
PHASES = {
    1: dict(rate=1.00, stance=0.0, amp=1.00, tail=1.00, pant=0.0),
    2: dict(rate=1.15, stance=6.0, amp=1.10, tail=1.25, pant=0.0),
    3: dict(rate=1.30, stance=14.0, amp=1.20, tail=1.60, pant=1.0),
}

TAIL_LAG = {'tail1': 0.0, 'tail2': 0.55, 'tail3': 1.05, 'tail4': 1.55}
LEFT, RIGHT = 'left', 'right'


def smooth(a, b, x):
    x = min(1.0, max(0.0, (x - a) / (b - a)))
    return x * x * (3 - 2 * x)


def keyed(points):
    """Curva suave por pontos (tempo, valor); fora do intervalo, mantém a ponta."""
    pts = sorted(points)

    def f(t):
        if t <= pts[0][0]:
            return pts[0][1]
        for (t0, v0), (t1, v1) in zip(pts, pts[1:]):
            if t <= t1:
                return v0 + (v1 - v0) * smooth(t0, t1, t)
        return pts[-1][1]
    return f


class Clip:
    def __init__(self, length, loop, step):
        self.length, self.loop, self.step = length, loop, step
        self.channels = {}  # (bone, channel) -> func(t) -> [x, y, z]

    def rot(self, bone, fn):
        self.channels[(bone, 'rotation')] = fn

    def pos(self, bone, fn):
        self.channels[(bone, 'position')] = fn

    def scale(self, bone, fn):
        self.channels[(bone, 'scale')] = fn

    def times(self):
        n = max(2, round(self.length / self.step))
        return [round(self.length * i / n, 4) for i in range(n + 1)]

    def build(self):
        bones = {}
        for (bone, channel), fn in self.channels.items():
            frames = {}
            for t in self.times():
                vec = [round(v, 3) for v in fn(t)]
                frames[f'{t:g}'] = vec
            bones.setdefault(bone, {})[channel] = frames
        entry = {'animation_length': self.length, 'bones': bones}
        if self.loop:
            entry['loop'] = True
        else:
            entry['loop'] = False
        return entry


def keys3(table):
    """Três curvas por pontos -> função de vetor. table = (xs, ys, zs), cada uma [(t, v)...]."""
    fx, fy, fz = (keyed(c) if c else (lambda t: 0.0) for c in table)
    return lambda t: [fx(t), fy(t), fz(t)]


# ---------------------------------------------------------------- locomoção em ciclo

def gait(period, amp, flex, lean, neck_up, tail_lift, stance, tail_amp, arms, bob, pant=0.0):
    """Marcha bípede: pernas em oposição, quadril balança, cauda é contrapeso.

    period   duração do ciclo completo (dois passos), s
    amp      varredura da coxa, graus
    flex     dobra do joelho na fase de balanço, graus
    lean     inclinação do tronco para a frente (investida)
    neck_up  quanto o pescoço sobe (+) em relação ao repouso; negativo abaixa
    tail_lift quanto a cauda sobe, graus (contrapeso da investida)
    stance   postura extra da fase do boss: cabeça mais baixa
    """
    c = Clip(period, True, period / 12)

    def phase(t, side):
        return TAU * (t / period + (0.5 if side == LEFT else 0.0))

    for side in (RIGHT, LEFT):
        s = lambda t, side=side: phase(t, side)
        c.rot(f'{side}Thigh', lambda t, s=s: [amp * math.sin(s(t)), 0, 0])
        # Joelho dobra durante o balanço (coxa voltando), estica no apoio.
        c.rot(f'{side}Leg', lambda t, s=s: [flex * max(0.0, -math.cos(s(t))) ** 1.2
                                           + 0.18 * amp * math.sin(s(t)), 0, 0])
        # Pé compensa a canela para a sola chegar plana e sair com leve levantada do calcanhar.
        c.rot(f'{side}Foot', lambda t, s=s: [-0.55 * flex * max(0.0, -math.cos(s(t))) ** 1.2
                                            - 0.22 * amp * math.sin(s(t))
                                            + 9 * max(0.0, math.sin(s(t))) ** 3, 0, 0])
        sign = 1 if side == RIGHT else -1
        c.rot(f'{side}UpperArm', lambda t, s=s, a=arms: [a * math.sin(s(t)) - 0.5 * lean, 0, 0])
        c.rot(f'{side}LowerArm', lambda t, s=s, a=arms: [0.6 * a * math.sin(s(t) + 0.8), 0, 0])

    # Peso passa de uma perna para a outra: balanço lateral do quadril e quique em 2x.
    c.pos('wholebody', lambda t: [0, bob * math.cos(2 * TAU * t / period), 0])
    c.rot('lowerBody', lambda t: [lean + 0.8 * math.sin(2 * TAU * t / period + 0.4),
                                  1.6 * math.sin(TAU * t / period),
                                  1.5 * (amp / 24) * math.sin(TAU * t / period + 0.3)])
    c.rot('upperBody', lambda t: [0.35 * lean - 0.6 * math.sin(2 * TAU * t / period + 0.9),
                                  -1.4 * math.sin(TAU * t / period), 0])
    # Pescoço e cabeça desfazem o balanço do tronco: a cabeça é a unidade que mira.
    c.rot('neck', lambda t: [-neck_up + stance * 0.55 + 1.2 * math.sin(2 * TAU * t / period + 1.3),
                             -2.2 * math.sin(TAU * t / period), 0])
    c.rot('head', lambda t: [-0.5 * (stance * 0.55) - 0.7 * math.sin(2 * TAU * t / period + 1.3) + neck_up * 0.35,
                             1.8 * math.sin(TAU * t / period), 0])
    c.rot('lowerJaw', lambda t: [pant * (5 + 3 * math.sin(2 * TAU * t / period)), 0, 0])
    c.scale('upperBodyBreathing', lambda t: [1, 1 + 0.018 * math.sin(TAU * t / period * 2), 1 + 0.012 * math.sin(TAU * t / period * 2)])

    # Cauda: onda lateral com atraso crescente; ergue na investida para equilibrar a frente.
    for i, (bone, lag) in enumerate(TAIL_LAG.items()):
        a = tail_amp * (3.0 + 2.2 * i)
        lift = tail_lift * (0.55 if i == 0 else 0.30)
        c.rot(bone, lambda t, a=a, lag=lag, lift=lift: [-lift + 0.9 * math.sin(2 * TAU * t / period - lag * 1.2),
                                                       a * math.sin(TAU * t / period - lag), 0])
    return c


def idle_clip(phase):
    p = PHASES[phase]
    length = 4.0 / p['rate']
    c = Clip(length, True, length / 16)
    w = TAU / length
    c.pos('wholebody', lambda t: [0, 0.12 * math.sin(w * t), 0])
    c.rot('lowerBody', lambda t: [0.4 * math.sin(w * t), 0, 0])
    c.rot('upperBody', lambda t: [0.5 * math.sin(w * t - 0.4), 0.8 * math.sin(w * t / 2), 0])
    # Respiração por sacos aéreos: o tórax expande e o pescoço acompanha.
    c.scale('upperBodyBreathing', lambda t: [1 + 0.012 * math.sin(w * t), 1 + 0.025 * math.sin(w * t), 1 + 0.016 * math.sin(w * t)])
    c.scale('lowerBodyBreathing', lambda t: [1 + 0.008 * math.sin(w * t - 0.5), 1 + 0.014 * math.sin(w * t - 0.5), 1])
    c.rot('neck', lambda t: [p['stance'] * 0.55 + 1.6 * math.sin(w * t - 0.9), 3.2 * math.sin(w * t / 2), 0])
    c.rot('head', lambda t: [-p['stance'] * 0.30 + 1.2 * math.sin(w * t - 1.3), 4.5 * math.sin(w * t / 2 + 0.5), 1.0 * math.sin(w * t / 2)])
    # Olha em volta: a cabeça gira devagar, com pausas.
    look = keyed([(0, 0), (0.8, 0), (1.3, 1), (2.0, 1), (2.5, -1), (3.2, -1), (3.7, 0), (4.0, 0)])
    c.rot('neck', lambda t, base=c.channels[('neck', 'rotation')]: [base(t)[0], base(t)[1] + 7 * look(t * 4 / length), 0])
    c.rot('lowerJaw', lambda t: [p['pant'] * (6 + 4 * math.sin(w * t * 5)) + 0.6 * max(0, math.sin(w * t + 1)), 0, 0])
    for bone, lag in TAIL_LAG.items():
        c.rot(bone, lambda t, lag=lag: [0.5 * math.sin(w * t - lag), (1.8 + lag) * p['tail'] * math.sin(w * t / 2 - lag), 0])
    for side, sg in ((RIGHT, 1), (LEFT, -1)):
        c.rot(f'{side}UpperArm', lambda t, sg=sg: [2 * math.sin(w * t + sg), 0, 0])
        c.rot(f'{side}LowerArm', lambda t, sg=sg: [3 * math.sin(w * t + sg + 0.8), 0, 0])
    # Alivia o peso de um pé para o outro de tempos em tempos.
    shift = keyed([(0, 0), (1.2, 0), (1.6, 1), (2.4, 1), (2.8, 0), (4.0, 0)])
    c.rot('rightThigh', lambda t: [-2.0 * shift(t * 4 / length), 0, 0])
    c.rot('rightLeg', lambda t: [3.0 * shift(t * 4 / length), 0, 0])
    c.rot('leftThigh', lambda t: [0.0, 0, 0])
    return c


def walk_clip(phase):
    p = PHASES[phase]
    return gait(period=1.7 / p['rate'], amp=22 * p['amp'], flex=30 * p['amp'], lean=1.0 + p['stance'] * 0.2,
                neck_up=0, tail_lift=0, stance=p['stance'], tail_amp=p['tail'], arms=5, bob=0.30, pant=p['pant'])


def run_clip(phase):
    """Investida (dossiê §11): explosão curta de 25–35 km/h. Tronco inclinado, pescoço
    estendido à frente, cauda erguida como contrapeso, braços recolhidos."""
    p = PHASES[phase]
    return gait(period=0.85 / p['rate'], amp=36 * p['amp'], flex=52 * p['amp'], lean=9 + p['stance'] * 0.35,
                neck_up=-18 - p['stance'] * 0.8, tail_lift=9 + p['stance'] * 0.3, stance=p['stance'] * 0.5,
                tail_amp=0.55 * p['tail'], arms=-6, bob=0.55, pant=p['pant'])


# ---------------------------------------------------------------- gestos de uma vez

def attack_clip(phase):
    """Mordida com ancoragem e tração cervical (dossiê §7): recuo → bote → mordida →
    ancoragem → puxão com o pescoço e o corpo → recuperação."""
    p = PHASES[phase]
    k = 1 / p['rate']
    L = 1.6 * k
    c = Clip(L, False, 0.05)

    def T(*pts):  # escala os tempos pela cadência da fase
        return [(t * k, v * (1 if not isinstance(v, tuple) else 1)) for t, v in pts]

    def curve(*pts):
        return keyed([(t * k, v) for t, v in pts])

    body_lean = curve((0, 0), (0.35, -4), (0.55, 11), (0.75, 12), (1.05, 3), (1.35, -1), (1.6, 0))
    neck = curve((0, 0), (0.35, -16), (0.55, 38 + p['stance'] * 0.3), (0.75, 34), (1.0, -10), (1.18, -4), (1.6, 0))
    head = curve((0, 0), (0.35, 10), (0.55, -30), (0.75, -26), (1.0, 12), (1.18, 6), (1.6, 0))
    jaw = curve((0, 0), (0.30, 36), (0.55, 42), (0.68, 3), (1.0, 5), (1.15, 14), (1.35, 0), (1.6, 0))
    shake = curve((0, 0), (0.70, 0), (0.80, 1), (0.90, -1), (1.00, 1), (1.10, -1), (1.2, 0), (1.6, 0))
    twist = curve((0, 0), (0.68, 0), (0.92, 1), (1.12, 0), (1.6, 0))
    c.rot('lowerBody', lambda t: [body_lean(t) * 0.6, 3 * twist(t), 0])
    c.rot('upperBody', lambda t: [body_lean(t) * 0.5, 6 * twist(t), 0])
    c.pos('wholebody', lambda t: [0, -0.9 * smooth(0.0, 0.35 * k, t) + 0.9 * smooth(0.35 * k, 0.6 * k, t) - 0.3 * smooth(0.75 * k, 1.1 * k, t) * 0, 0])
    c.rot('neck', lambda t: [neck(t), 14 * twist(t) + 8 * shake(t), 6 * shake(t)])
    c.rot('head', lambda t: [head(t), 8 * shake(t), 12 * shake(t)])
    c.rot('lowerJaw', lambda t: [jaw(t), 0, 0])
    # Pernas empurram no bote e travam no puxão.
    c.rot('rightThigh', lambda t: [-14 * smooth(0.0, 0.4 * k, t) + 34 * smooth(0.4 * k, 0.6 * k, t) - 22 * smooth(0.9 * k, 1.4 * k, t), 0, 0])
    c.rot('leftThigh', lambda t: [-8 * smooth(0.0, 0.4 * k, t) + 22 * smooth(0.4 * k, 0.6 * k, t) - 14 * smooth(0.9 * k, 1.4 * k, t), 0, 0])
    c.rot('rightLeg', lambda t: [12 * smooth(0.0, 0.4 * k, t) - 6 * smooth(0.4 * k, 0.8 * k, t), 0, 0])
    c.rot('leftLeg', lambda t: [8 * smooth(0.0, 0.4 * k, t) - 4 * smooth(0.4 * k, 0.8 * k, t), 0, 0])
    c.rot('rightFoot', lambda t: [-6 * smooth(0.0, 0.4 * k, t) + 6 * smooth(0.4 * k, 0.8 * k, t), 0, 0])
    c.rot('leftFoot', lambda t: [-4 * smooth(0.0, 0.4 * k, t) + 4 * smooth(0.4 * k, 0.8 * k, t), 0, 0])
    # Braços estabilizam a presa junto ao peito.
    arm = curve((0, 0), (0.45, -4), (0.62, -22), (1.0, -20), (1.4, 0), (1.6, 0))
    c.rot('rightUpperArm', lambda t: [arm(t), 0, 4 * arm(t) / 22])
    c.rot('leftUpperArm', lambda t: [arm(t), 0, -4 * arm(t) / 22])
    c.rot('rightLowerArm', lambda t: [-0.8 * arm(t), 0, 0])
    c.rot('leftLowerArm', lambda t: [-0.8 * arm(t), 0, 0])
    # Cauda contrabalança o bote: sobe quando a cabeça desce.
    tail = curve((0, 0), (0.35, 3), (0.55, -10), (0.9, -6), (1.3, 1), (1.6, 0))
    for bone, lag in TAIL_LAG.items():
        c.rot(bone, lambda t, lag=lag: [tail(t) * (1 - 0.18 * lag), 5 * twist(t) * (1 + lag), 0])
    c.scale('upperBodyBreathing', lambda t: [1, 1 + 0.035 * smooth(0, 0.35 * k, t) - 0.035 * smooth(0.4 * k, 0.6 * k, t), 1])
    return c


def attack2_clip(phase):
    """Pancada de corpo (dossiê §7, passo 3 'Impacto'): ombro e quadril giram, cauda varre."""
    p = PHASES[phase]
    k = 1 / p['rate']
    L = 1.25 * k
    c = Clip(L, False, 0.05)

    def curve(*pts):
        return keyed([(t * k, v) for t, v in pts])

    wind = curve((0, 0), (0.30, -1), (0.52, 1), (0.8, 0.0), (1.25, 0))
    swing = curve((0, 0), (0.30, -22), (0.52, 24), (0.8, 6), (1.25, 0))
    dip = curve((0, 0), (0.30, 3), (0.52, 8), (0.9, 1), (1.25, 0))
    c.rot('lowerBody', lambda t: [dip(t) * 0.6, swing(t) * -0.45, 3.5 * wind(t)])
    c.rot('upperBody', lambda t: [dip(t) * 0.7, swing(t) * 0.7, 5 * wind(t)])
    c.pos('wholebody', lambda t: [0, -0.7 * smooth(0, 0.3 * k, t) + 0.7 * smooth(0.3 * k, 0.55 * k, t), 0])
    c.rot('neck', lambda t: [dip(t) * 1.0 - 10 * (1 - smooth(0, 0.3 * k, t)) * 0 - 5, swing(t) * 0.55, -2 * swing(t) / 24])
    c.rot('head', lambda t: [-dip(t) * 0.5 + 4 * wind(t), swing(t) * 0.30, 0])
    c.rot('lowerJaw', lambda t: [10 * smooth(0.3 * k, 0.5 * k, t) - 10 * smooth(0.55 * k, 0.8 * k, t), 0, 0])
    for bone, lag in TAIL_LAG.items():
        gain = (1.0 + 0.7 * lag)
        # A cauda gira no sentido contrário ao ombro, com atraso por segmento.
        c.rot(bone, lambda t, lag=lag, gain=gain: [-3 * dip(t) / 8, -0.9 * gain * swing(max(0.0, t - 0.06 * k * lag * 2)), 0])
    for side in (RIGHT, LEFT):
        c.rot(f'{side}UpperArm', lambda t: [-14 * smooth(0.25 * k, 0.5 * k, t) * (1 - smooth(0.7 * k, 1.2 * k, t)), 0, 0])
        c.rot(f'{side}Thigh', lambda t: [-8 * smooth(0, 0.3 * k, t) + 12 * smooth(0.3 * k, 0.55 * k, t) - 4 * smooth(0.7 * k, 1.2 * k, t), 0, 0])
        c.rot(f'{side}Leg', lambda t: [8 * smooth(0, 0.3 * k, t) - 8 * smooth(0.4 * k, 0.8 * k, t), 0, 0])
    return c


def roar_clip(phase, length, jaw_open, strength, name='speak'):
    """Ameaça (dossiê §16): inspira, cabeça sobe, expiração explosiva e curta com a
    garganta tremendo. Vocalização grave: o peito pulsa mais do que a mandíbula abre."""
    p = PHASES[phase]
    k = 1 / p['rate']
    L = length * k
    c = Clip(L, False, 0.04)

    def curve(*pts):
        return keyed([(t * L, v) for t, v in pts])

    inhale = curve((0, 0), (0.30, 1), (0.40, 1), (0.50, 0), (1.0, 0))
    roar = curve((0, 0), (0.28, 0), (0.40, 1), (0.82, 1), (0.95, 0), (1.0, 0))
    settle = curve((0, 0), (0.30, 0.0), (0.45, 1), (0.85, 1), (1.0, 0))
    buzz = lambda t: math.sin(TAU * 11 * t) * roar(t)

    c.pos('wholebody', lambda t: [0, -0.5 * inhale(t) + 0.15 * roar(t), 0])
    c.rot('lowerBody', lambda t: [-2.5 * inhale(t) + 3 * roar(t) + 0.5 * buzz(t) * strength, 0, 0])
    c.rot('upperBody', lambda t: [-6 * inhale(t) + 2 * roar(t), 0, 0])
    c.rot('neck', lambda t: [-16 * strength * inhale(t) + 4 * roar(t) * strength + 12 * strength * 0, 0.8 * buzz(t), 0])
    c.rot('head', lambda t: [-22 * strength * settle(t) * 0.6 - 6 * strength * inhale(t) + 1.2 * buzz(t) * strength, 0, 0.7 * buzz(t)])
    c.rot('lowerJaw', lambda t: [jaw_open * roar(t) + 0.6 * jaw_open * 0.15 * buzz(t) + 6 * inhale(t), 0, 0])
    c.scale('upperBodyBreathing', lambda t: [1 + 0.05 * inhale(t) * strength, 1 + 0.09 * inhale(t) * strength - 0.03 * roar(t) * strength, 1 + 0.06 * inhale(t) * strength])
    c.scale('lowerBodyBreathing', lambda t: [1 + 0.02 * inhale(t), 1 + 0.04 * inhale(t), 1])
    for bone, lag in TAIL_LAG.items():
        c.rot(bone, lambda t, lag=lag: [-4 * roar(t) * strength, 2.0 * buzz(t) * (1 + lag) * 0.4 + 4 * strength * math.sin(TAU * 1.4 * t - lag) * roar(t), 0])
    for side, sg in ((RIGHT, 1), (LEFT, -1)):
        c.rot(f'{side}UpperArm', lambda t, sg=sg: [-10 * strength * roar(t), 0, -5 * sg * roar(t)])
        c.rot(f'{side}LowerArm', lambda t: [-8 * strength * roar(t), 0, 0])
        c.rot(f'{side}Thigh', lambda t: [-6 * roar(t) * strength, 0, 0])
        c.rot(f'{side}Leg', lambda t: [6 * roar(t) * strength, 0, 0])
    return c


def call_clip():
    """Chamado territorial grave e prolongado: boca meio aberta, peito pulsando."""
    L = 2.4
    c = Clip(L, False, 0.05)
    env = keyed([(0, 0), (0.5, 1), (1.9, 1), (2.4, 0)])
    pulse = lambda t: math.sin(TAU * 2.2 * t) * env(t)
    c.rot('neck', lambda t: [-9 * env(t) + 1.2 * pulse(t), 0, 0])
    c.rot('head', lambda t: [-14 * env(t), 0, 0])
    c.rot('lowerJaw', lambda t: [11 * env(t) + 2.5 * pulse(t), 0, 0])
    c.scale('upperBodyBreathing', lambda t: [1 + 0.02 * env(t), 1 + 0.045 * env(t) + 0.02 * pulse(t), 1 + 0.03 * env(t)])
    c.rot('upperBody', lambda t: [-3 * env(t), 0, 0])
    for bone, lag in TAIL_LAG.items():
        c.rot(bone, lambda t, lag=lag: [-2 * env(t), 2.5 * math.sin(TAU * 0.6 * t - lag) * env(t), 0])
    return c


def eat_clip():
    """Arranca e engole (carcaça): a cabeça desce, o corpo puxa para trás em três
    rasgadas e o pescoço sobe para engolir. Braços seguram a carcaça."""
    L = 2.4
    c = Clip(L, False, 0.05)
    down = keyed([(0, 0), (0.35, 1), (1.85, 1), (2.15, 0), (2.4, 0)])
    tug = lambda t: down(t) * (max(0, math.sin(TAU * 1.25 * (t - 0.35))) if 0.35 < t < 1.85 else 0)
    gulp = keyed([(0, 0), (1.9, 0), (2.05, 1), (2.3, 0), (2.4, 0)])
    c.rot('neck', lambda t: [34 * down(t) - 26 * tug(t) - 18 * gulp(t), 0, 0])
    c.rot('head', lambda t: [-12 * down(t) + 5 * tug(t) + 8 * gulp(t), 0, 4 * math.sin(TAU * 1.25 * t) * tug(t)])
    c.rot('lowerJaw', lambda t: [14 * down(t) * (1 - tug(t)) + 22 * gulp(t), 0, 0])
    c.rot('lowerBody', lambda t: [6 * down(t) - 4 * tug(t), 0, 0])
    c.rot('upperBody', lambda t: [4 * down(t) - 3 * tug(t), 0, 0])
    c.pos('wholebody', lambda t: [0, -0.3 * down(t), 0])
    for side in (RIGHT, LEFT):
        c.rot(f'{side}UpperArm', lambda t: [-24 * down(t), 0, 0])
        c.rot(f'{side}LowerArm', lambda t: [-12 * down(t), 0, 0])
        c.rot(f'{side}Thigh', lambda t: [-7 * down(t) + 5 * tug(t), 0, 0])
        c.rot(f'{side}Leg', lambda t: [8 * down(t), 0, 0])
    for bone, lag in TAIL_LAG.items():
        c.rot(bone, lambda t, lag=lag: [-4 * down(t), 2 * math.sin(TAU * 0.5 * t - lag) * down(t), 0])
    return c


def unconscious_clip():
    """Nocaute/torpor (e doma): deitado de barriga, pernas dobradas sob o corpo, cabeça
    caída e a respiração lenta de quem dorme pesado. Também serve de descanso."""
    L = 5.0
    c = Clip(L, True, 0.3125)
    w = TAU / L
    sink = -5.8  # unidades do modelo: barriga no chão
    c.pos('wholebody', lambda t: [0, sink + 0.10 * math.sin(w * t), 0])
    c.rot('lowerBody', lambda t: [-3, 0, 0])
    c.rot('upperBody', lambda t: [0.8 * math.sin(w * t), 0, 0])
    c.scale('upperBodyBreathing', lambda t: [1 + 0.012 * math.sin(w * t), 1 + 0.03 * math.sin(w * t), 1 + 0.02 * math.sin(w * t)])
    c.scale('lowerBodyBreathing', lambda t: [1 + 0.008 * math.sin(w * t), 1 + 0.016 * math.sin(w * t), 1])
    c.rot('neck', lambda t: [30 + 0.8 * math.sin(w * t), 0, 0])
    c.rot('head', lambda t: [-8 + 0.5 * math.sin(w * t + 0.4), 0, 9])
    c.rot('lowerJaw', lambda t: [5 + 1.2 * math.sin(w * t), 0, 0])
    for side in (RIGHT, LEFT):
        c.rot(f'{side}Thigh', lambda t: [-64, 0, 0])
        c.rot(f'{side}Leg', lambda t: [120, 0, 0])
        c.rot(f'{side}Foot', lambda t: [-48, 0, 0])
        c.rot(f'{side}UpperArm', lambda t: [-8, 0, 0])
        c.rot(f'{side}LowerArm', lambda t: [10, 0, 0])
    for bone, lag in TAIL_LAG.items():
        c.rot(bone, lambda t, lag=lag: [0.5, 5 + 2 * lag + 0.6 * math.sin(w * t - lag), 0])
    return c


def build():
    clips = {'unconscious': unconscious_clip(), 'call': call_clip(), 'eat': eat_clip()}
    for phase in (1, 2, 3):
        suffix = '' if phase == 1 else f'_f{phase}'
        clips['idle' + suffix] = idle_clip(phase)
        clips['walk' + suffix] = walk_clip(phase)
        clips['run' + suffix] = run_clip(phase)
        clips['attack' + suffix] = attack_clip(phase)
        clips['attack_2' + suffix] = attack2_clip(phase)
        strength = (0.85, 1.0, 1.2)[phase - 1]
        clips['speak' + suffix] = roar_clip(phase, 1.5, 24 + 4 * phase, strength)
    # Troca de fase: rugido longo e mais aberto que o de ameaça.
    clips['roar_phase'] = roar_clip(1, 3.0, 40, 1.35)
    return clips


def check(clips):
    """Confere os ossos usados contra o geo local do passo 3, se existir."""
    geo = ROOT / 'art/titanovenator/local/titanovenator.geo.json'
    if not geo.exists():
        print('geo local ausente: nomes de ossos não conferidos')
        return
    bones = {b['name'] for b in json.loads(geo.read_text())['minecraft:geometry'][0]['bones']}
    used = {bone for clip in clips.values() for bone, _ in clip.channels}
    missing = sorted(used - bones)
    if missing:
        raise SystemExit(f'ossos inexistentes no rig: {missing}')
    print(f'{len(used)} ossos animados, todos presentes no rig')


def main():
    clips = build()
    check(clips)
    data = {'format_version': '1.8.0',
            'animations': {PREFIX + name: clip.build() for name, clip in clips.items()}}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n')
    print(f'{len(clips)} animações -> {OUT.relative_to(ROOT)}')
    for name, clip in clips.items():
        print(f'  {name:14s} {clip.length:5.2f}s {"loop" if clip.loop else "uma vez"}')


if __name__ == '__main__':
    main()
