#!/usr/bin/env python3
"""Simula a luta de uma tropa de Rex contra o Titanovenator, para calibrar o couro (core/titan/TitanPhase).

Modelo grosseiro, de propósito: todos os Rex mordem uma vez por segundo (ChaseGoal), sempre no alcance; o boss morde
um por vez (42, sangramento por fase), varre a cauda em todos a cada 5 s com dois ou mais em volta, investe na fase 2
e 3 e ruge 3 s parado em cada troca de fase. Armadura do vanilla dos dois lados. Golpe no boss:
max(0, ataque - R) * S * (1 - redução da fase), sem a pausa de invulnerabilidade; ele regenera G por segundo.
Rex de nível 100 domesticado típico: ~37 pontos de ataque e 871 de vida; cada mutação de ataque soma 2 pontos.

Uso: python3 tools/sim_titan_fight.py [R S G T]   (padrão: os números do jogo, 50 0.25 4 42)
Na prática a tropa perde tempo (empurrões, caminho), então a luta real é um pouco mais dura que a simulada.
"""
import random
import statistics
import sys

def armor_mult(dmg, armor, tough=0.0):
    f = min(20.0, max(armor / 5.0, armor - dmg / (2.0 + tough / 4.0)))
    return 1.0 - f / 25.0

PHASES = [(0.0, 1, 3, 8.0, None), (0.30, 1, 3, 8.0, 8.0), (0.50, 2, 5, 10.0, 5.0)]  # red, bleed/bite, cap, dur, charge cd

def fight(n, A, H=871.0, ra=12.7, R=50, S=0.12, G=6.0, T=90.0, tarm=12.0, seed=0, iframes=False, dt=0.05, tmax=600):
    rnd = random.Random(seed)
    hp = 1024.0
    rex = [dict(hp=H, next=rnd.random(), bleed=0, bleed_until=0.0) for _ in range(n)]
    t = 0.0; bite_next = 0.5; slam_next = 3.0; charge_next = 0.0; roar_until = 0.0; phase = 0
    target = 0; last_hit_t = -99; last_hurt = 0.0
    while t < tmax:
        alive = [r for r in rex if r['hp'] > 0]
        if not alive: return False, t, 0
        if hp <= 0: return True, t, sum(1 for r in rex if r['hp'] > 0)
        frac = hp / 1024.0
        ph = 2 if frac < 0.33 else 1 if frac < 0.66 else 0
        if ph > phase:
            phase = ph; roar_until = t + 3.0; charge_next = t + 3.0
        red, bpb, cap, dur, ccd = PHASES[phase]
        # rex attacks
        for r in alive:
            if t >= r['next']:
                r['next'] += 1.0
                amt = max(0.0, A - R) * S * (1 - red)
                if iframes and t - last_hit_t < 0.5:
                    if amt > last_hurt:
                        hp -= (amt - last_hurt) * armor_mult(amt, tarm); last_hurt = amt
                else:
                    hp -= amt * armor_mult(amt, tarm); last_hit_t = t; last_hurt = amt
        hp = min(1024.0, hp + G * dt)
        # titan
        if t >= roar_until:
            if t >= bite_next:
                bite_next = t + 1.0
                tgt = alive[0]
                tgt['hp'] -= T * armor_mult(T, ra)
                tgt['bleed'] = min(cap, tgt['bleed'] + bpb) if t < tgt['bleed_until'] else min(cap, bpb)
                tgt['bleed_until'] = t + dur
            if t >= slam_next and len(alive) >= 2:
                slam_next = t + 5.0
                for r in alive: r['hp'] -= 0.6 * T * armor_mult(0.6 * T, ra)
            if ccd and t >= charge_next:
                charge_next = t + ccd
                r = rnd.choice(alive); r['hp'] -= 0.75 * T * armor_mult(0.75 * T, ra)
        for r in alive:
            if t < r['bleed_until'] and r['bleed'] > 0:
                r['hp'] -= 0.004 * r['bleed'] * H * dt
        t += dt
    return False, t, -1

def summary(**kw):
    res = [fight(seed=s, **kw) for s in range(20)]
    wins = [r for r in res if r[0]]
    return len(wins), (statistics.mean(r[1] for r in wins) if wins else None), (statistics.mean(r[2] for r in wins) if wins else None), statistics.mean(r[1] for r in res)

if __name__ == '__main__':
    args = [float(a) for a in sys.argv[1:5]] if len(sys.argv) >= 5 else [50, 0.25, 4, 42]
    cfg = dict(R=args[0], S=args[1], G=args[2], T=args[3])
    print('couro', cfg)
    for muts in (0, 4, 8, 12, 15, 20, 25):
        A = 28 * (1 + 0.04 * (37 + 2 * muts))
        w, tw, surv, tall = summary(n=5, A=A, **cfg)
        print(f'5 Rex, {muts:2d} mutações (ataque {A:5.1f}): vencem {w:2d}/20, em {tw and round(tw)} s, '
              f'sobram {surv and round(surv, 1)}')
    for n in (8, 10):
        w, tw, surv, _ = summary(n=n, A=28 * (1 + 0.04 * 37), **cfg)
        print(f'{n} Rex sem mutação: vencem {w:2d}/20')
