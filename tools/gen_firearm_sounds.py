#!/usr/bin/env python3
"""Sintetiza os sons das armas de fogo (D58): os tiros, as recargas (no ritmo das animações de
`gen_dc2_weapons.py`), o clique seco e o estouro da esfera do canhão sólido. Tudo feito aqui, de ruído filtrado,
senoides que decaem e uma sala curta — nada gravado nem copiado de jogo nenhum.

Escreve `assets/iceagesurvival/sounds/firearm/*.ogg` (mono, 44,1 kHz, Vorbis via ffmpeg) e as entradas de
`assets/iceagesurvival/sounds.json` (as que já existem lá ficam). A fúria usa o rugido do devastador do vanilla.

Rodar de novo SOBRESCREVE os arquivos. Precisa de numpy e do ffmpeg com libvorbis.
"""

import json
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
OUT = ASSETS / "sounds" / "firearm"
RATE = 44100
rng = np.random.default_rng(58)


def silence(seconds):
    return np.zeros(int(seconds * RATE))


def t_axis(n):
    return np.arange(n) / RATE


def band_noise(seconds, lo, hi, tilt=0.0):
    """Ruído branco filtrado na faixa [lo, hi] Hz (máscara suave na FFT); `tilt` > 0 puxa para os graves."""
    n = int(seconds * RATE)
    spectrum = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / RATE)
    mask = 1 / (1 + (lo / np.maximum(f, 1)) ** 4) / (1 + (f / hi) ** 4)
    if tilt:
        mask *= (np.maximum(f, 20) / 1000.0) ** -tilt
    out = np.fft.irfft(spectrum * mask, n)
    return out / (np.abs(out).max() or 1)


def env(n, attack, decay, hold=0.0):
    """Envelope: sobe em `attack` s, segura `hold` s e cai exponencial (constante `decay` s)."""
    t = t_axis(n)
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    d = np.where(t < attack + hold, 1.0, np.exp(-(t - attack - hold) / decay))
    return a * d


def tone(seconds, f0, f1=None, decay=0.1, harmonics=(1.0,), attack=0.001):
    """Senoide (com harmônicos) que varre de f0 a f1 e decai."""
    n = int(seconds * RATE)
    f1 = f0 if f1 is None else f1
    freq = np.geomspace(f0, f1, n) if f0 > 0 and f1 > 0 else np.linspace(f0, f1, n)
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    wave_ = sum(amp * np.sin(phase * (k + 1)) for k, amp in enumerate(harmonics))
    return wave_ * env(n, attack, decay)


def mixdown(*signals):
    """Soma sinais de tamanhos diferentes (o menor recebe silêncio no fim)."""
    out = np.zeros(max(len(x) for x in signals))
    for x in signals:
        out[:len(x)] += x
    return out


def place(track, sound, at, gain=1.0):
    i = int(at * RATE)
    end = min(len(track), i + len(sound))
    track[i:end] += sound[:end - i] * gain
    return track


def room(signal, size=0.35, mix=0.22):
    """Uma sala curta: convolução com ruído que decai (o eco do mato e das paredes)."""
    n = int(size * RATE)
    ir = band_noise(size, 200, 6000) * np.exp(-t_axis(n) / (size / 5))
    ir[0] = 0
    wet = np.convolve(signal, ir)
    dry = np.concatenate([signal, np.zeros(len(wet) - len(signal))])
    return dry + wet / (np.abs(wet).max() or 1) * np.abs(signal).max() * mix


# ---------------------------------------------------------------- peças

def crack(length, lo, hi, decay, gain=1.0):
    n = int(length * RATE)
    return band_noise(length, lo, hi) * env(n, 0.0008, decay) * gain


def click(freq=3200, decay=0.006, gain=0.5):
    """Clique metálico: três parciais desafinadas que somem rápido, com um sopro de ruído."""
    length = decay * 8
    n = int(length * RATE)
    body = sum(tone(length, freq * r, decay=decay * w) for r, w in ((1, 1), (1.63, 0.8), (2.71, 0.5)))
    return mixdown(body * 0.6, crack(length, 1500, 9000, decay * 0.6)) * gain


def clack(freq=1300, decay=0.018, gain=0.7):
    return mixdown(click(freq, decay, gain), crack(decay * 6, 400, 3000, decay * 0.8, gain * 0.5))


def clunk(freq=260, decay=0.05, gain=0.8):
    length = decay * 6
    return mixdown(tone(length, freq, freq * 0.8, decay, (1, 0.5, 0.25)), crack(length, 150, 1500, decay * 0.5, 0.6)) \
        * gain


def whoosh(length, lo=500, hi=4000, gain=0.25):
    n = int(length * RATE)
    shape = np.sin(np.linspace(0, np.pi, n)) ** 2
    return band_noise(length, lo, hi) * shape * gain


def normalize(signal, peak=0.92):
    m = np.abs(signal).max() or 1
    return signal / m * peak


# ---------------------------------------------------------------- tiros

def gunshot(boom_hz, crack_band, crack_decay, boom_decay, tail, room_size, body_gain=0.8, sub=None):
    length = tail + 0.05
    n = int(length * RATE)
    s = crack(length, *crack_band, crack_decay)
    s += tone(length, boom_hz * 1.6, boom_hz, boom_decay, (1, 0.35)) * body_gain
    s += band_noise(length, 80, 1200, tilt=0.5) * env(n, 0.002, boom_decay * 2.5) * 0.45
    if sub:
        s += tone(length, sub, sub * 0.7, boom_decay * 3, (1,)) * 0.6
    return normalize(room(s, room_size, 0.3))


def solid_cannon_shot():
    """O canhão: o estalo, a varredura que cai de 900 a 110 Hz com o tremor de 38 Hz (a vibração) e um brilho."""
    length = 0.9
    n = int(length * RATE)
    t = t_axis(n)
    sweep = tone(length, 900, 110, 0.28, (1, 0.5, 0.3, 0.2))
    tremolo = 0.55 + 0.45 * np.sin(2 * np.pi * 38 * t)
    shimmer = band_noise(length, 3000, 12000) * env(n, 0.01, 0.12) * 0.25
    s = sweep * tremolo + shimmer + crack(length, 600, 5000, 0.02, 0.7) + tone(length, 70, 45, 0.2) * 0.7
    return normalize(room(s, 0.4, 0.25))


def solid_impact():
    length = 0.7
    n = int(length * RATE)
    t = t_axis(n)
    buzz = np.sign(np.sin(2 * np.pi * (180 + 60 * np.sin(2 * np.pi * 9 * t)) * t)) * env(n, 0.002, 0.15) * 0.35
    s = buzz + crack(length, 300, 8000, 0.06) + tone(length, 140, 60, 0.18) * 0.8
    s += band_noise(length, 4000, 14000) * env(n, 0.001, 0.05) * 0.4
    return normalize(room(s, 0.35, 0.25))


def empty():
    track = silence(0.25)
    place(track, click(2600, 0.004, 0.8), 0.0)
    place(track, click(1800, 0.006, 0.5), 0.07)
    return normalize(track, 0.6)


# ---------------------------------------------------------------- recargas (no tempo das animações)

def reload_handgun():
    track = silence(1.6)
    place(track, click(2800, 0.005, 0.7), 0.12)            # botão do pente
    place(track, whoosh(0.3, 800, 5000, 0.2), 0.15)        # o pente desliza
    place(track, clunk(420, 0.03, 0.4), 0.48)              # cai
    place(track, clack(1500, 0.016, 0.9), 0.95)            # o novo entra
    place(track, clack(2100, 0.012, 1.0), 1.1)             # o ferrolho solta
    return normalize(room(track, 0.25, 0.15))


def reload_shotgun():
    track = silence(2.6)
    for k in range(4):
        t0 = 0.3 + k * 0.38
        place(track, whoosh(0.12, 1000, 6000, 0.12), t0)
        place(track, clack(1100, 0.02, 0.8), t0 + 0.2)     # cartucho no tubo
    place(track, clack(800, 0.03, 1.0), 1.95)              # bomba para trás
    place(track, clack(1200, 0.025, 1.0), 2.12)            # bomba para a frente
    return normalize(room(track, 0.25, 0.15))


def reload_submachine_gun():
    track = silence(2.3)
    place(track, click(2600, 0.006, 0.7), 0.25)
    place(track, whoosh(0.35, 700, 4000, 0.2), 0.25)
    place(track, clack(1400, 0.018, 0.9), 1.32)
    place(track, clack(900, 0.02, 0.8), 1.62)              # alavanca puxada
    place(track, clack(1700, 0.015, 1.0), 1.76)            # e solta
    return normalize(room(track, 0.25, 0.15))


def reload_heavy_machine_gun():
    track = silence(4.1)
    place(track, clack(700, 0.03, 0.9), 0.42)              # tampa abre
    place(track, clunk(220, 0.06, 1.0), 0.95)              # caixa sai
    for k in range(6):
        place(track, click(3000 + 200 * k, 0.004, 0.25), 0.9 + k * 0.03)  # a fita chacoalha
    place(track, clunk(240, 0.06, 1.0), 2.38)              # caixa nova
    for k in range(8):
        place(track, click(2800 + 150 * k, 0.004, 0.3), 2.8 + k * 0.025)
    place(track, clunk(380, 0.04, 1.0), 3.28)              # tampa fecha
    place(track, clack(900, 0.025, 0.9), 3.6)
    return normalize(room(track, 0.3, 0.18))


def reload_solid_cannon():
    track = silence(2.6)
    place(track, whoosh(0.45, 2000, 12000, 0.35), 0.28)    # a célula sai chiando
    place(track, tone(0.6, 1400, 300, 0.25, (1, 0.4)) * 0.35, 0.3)
    place(track, clack(1200, 0.02, 0.9), 1.68)             # a nova trava
    place(track, tone(0.7, 180, 1600, 0.5, (1, 0.5, 0.25), attack=0.5) * 0.4, 1.75)  # carrega
    return normalize(room(track, 0.3, 0.18))


def reload_anti_tank_rifle():
    track = silence(3.1)
    place(track, clack(900, 0.025, 0.9), 0.25)             # alavanca sobe
    place(track, clack(700, 0.03, 0.9), 0.5)               # recua
    place(track, clunk(300, 0.05, 0.8), 0.8)               # carregador sai
    place(track, clunk(340, 0.05, 1.0), 1.92)              # o novo entra
    place(track, clack(1100, 0.025, 1.0), 2.35)            # alavanca vai
    place(track, clack(1500, 0.02, 1.0), 2.56)             # e desce
    return normalize(room(track, 0.3, 0.18))


SOUNDS = {
    "shot_handgun": lambda: gunshot(180, (1200, 9000), 0.012, 0.05, 0.45, 0.3, 0.6),
    "shot_shotgun": lambda: gunshot(95, (500, 6000), 0.03, 0.12, 0.9, 0.45, 1.0, sub=55),
    "shot_submachine_gun": lambda: gunshot(220, (1500, 10000), 0.008, 0.03, 0.3, 0.25, 0.45),
    "shot_heavy_machine_gun": lambda: gunshot(120, (700, 7000), 0.016, 0.06, 0.45, 0.3, 0.9, sub=60),
    "shot_solid_cannon": solid_cannon_shot,
    "shot_anti_tank_rifle": lambda: gunshot(60, (300, 6000), 0.05, 0.2, 1.6, 0.7, 1.2, sub=38),
    "reload_handgun": reload_handgun,
    "reload_shotgun": reload_shotgun,
    "reload_submachine_gun": reload_submachine_gun,
    "reload_heavy_machine_gun": reload_heavy_machine_gun,
    "reload_solid_cannon": reload_solid_cannon,
    "reload_anti_tank_rifle": reload_anti_tank_rifle,
    "empty": empty,
    "solid_impact": solid_impact,
}

# Evento de som (sounds.json) → arquivo e legenda.
EVENTS = {
    **{f"firearm.shot.{gun}": (f"shot_{gun}", "subtitles.iceagesurvival.firearm.shot")
       for gun in ("handgun", "shotgun", "submachine_gun", "heavy_machine_gun", "anti_tank_rifle")},
    "firearm.shot.solid_cannon": ("shot_solid_cannon", "subtitles.iceagesurvival.firearm.shot.solid_cannon"),
    **{f"firearm.reload.{gun}": (f"reload_{gun}", "subtitles.iceagesurvival.firearm.reload")
       for gun in ("handgun", "shotgun", "submachine_gun", "heavy_machine_gun", "solid_cannon", "anti_tank_rifle")},
    "firearm.empty": ("empty", "subtitles.iceagesurvival.firearm.empty"),
    "firearm.solid_impact": ("solid_impact", "subtitles.iceagesurvival.firearm.solid_impact"),
}


def write_ogg(name, signal):
    pcm = (np.clip(signal, -1, 1) * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f"{name}.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "5",
                        str(OUT / f"{name}.ogg")], check=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, make in SOUNDS.items():
        write_ogg(name, make())
    path = ASSETS / "sounds.json"
    data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    for event, (file, subtitle) in EVENTS.items():
        data[event] = {"subtitle": subtitle, "sounds": [{"name": f"iceagesurvival:firearm/{file}"}]}
    data["firearm.rage"] = {"subtitle": "subtitles.iceagesurvival.firearm.rage", "sounds": [
        {"name": "minecraft:mob/ravager/roar1", "pitch": 0.85}, {"name": "minecraft:mob/ravager/roar2", "pitch": 0.85},
        {"name": "minecraft:mob/ravager/roar3", "pitch": 0.85}]}
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"{len(SOUNDS)} sons em {OUT} e {len(EVENTS) + 1} eventos em sounds.json")


if __name__ == "__main__":
    main()
