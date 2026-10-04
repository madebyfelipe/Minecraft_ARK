#!/usr/bin/env python3
"""Estudo local incremental do Titanovenator sobre o Rex instalado do Revival.

Não inclui assets de terceiros no script ou no jar. Lê o mod já instalado,
grava só em art/titanovenator/local/ (ignorado pelo Git). Não altera o mod.
Python 3, sem bibliotecas externas. Sem animações novas nesta passagem.
"""
from __future__ import annotations

import argparse
import base64
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import uuid
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'art/titanovenator/local'
SOURCE_GEO = 'assets/fossil/geo/entity/tyrannosaurus.geo.json'
SOURCE_ANIMATION = 'assets/fossil/animations/entity/tyrannosaurus.animation.json'
DEFAULT_JAR = Path.home()/'.local/share/PrismLauncher/instances/IceAgeSurvival/minecraft/mods/fossil-forge-1.20.1-9.3.4.0.jar'

# Primeira passagem conservadora; números de estudo, não balanceamento do jogo.
# Eixo Y ancorado no dorso mantém a linha dorsal e acrescenta ventre.
# As poses, os pivôs e a hierarquia originais permanecem intactos.
PASS_1 = {
    'lowerBodyBreathing': {'scale': [1.12, 1.10, 1], 'anchor': 'top'},
    'upperBodyBreathing': {'scale': [1.10, 1.06, 1], 'anchor': 'top'},
    'neck': {'scale': [1.10, 1.04, 1], 'anchor': 'center'},
    'head': {'scale': [1.06, 1.03, 1], 'anchor': 'center'},
    'upperJaw': {'scale': [1.06, 1, 1], 'anchor': 'center'},
    'lowerJaw': {'scale': [1.06, 1.10, 1], 'anchor': 'top'},
    'tail1': {'scale': [1.12, 1.08, 1], 'anchor': 'top'},
    'tail2': {'scale': [1.06, 1.04, 1], 'anchor': 'top'},
    'leftThigh': {'scale': [1.08, 1.04, 1.05], 'anchor': 'center'},
    'rightThigh': {'scale': [1.08, 1.04, 1.05], 'anchor': 'center'},
}


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL,'ias/titanovenator/revival-study/'+name))


def box_faces(cube):
    """Box UV → UV por face no espaço do Blockbench, sem mover a arte.

    Conforme js/outliner/cube.js updateUV do Blockbench v4.12.6.
    Converter ANTES de mudar os tamanhos evita deslocar olhos, dentes e escamas.
    """
    w,h,d = cube['size']; u,v = cube['uv']
    layout={
        'east': [0,d,d,h], 'west': [d+w,d,d,h],
        'up': [d+w,d,-w,-d], 'down': [d+2*w,0,-w,d],
        'south': [2*d+w,d,w,h], 'north': [d,d,w,h],
    }
    if cube.get('mirror',False):
        for f in layout.values(): f[0]+=f[2]; f[2]*=-1
        layout['east'],layout['west']=layout['west'],layout['east']
    result={}
    for name,(x,y,a,b) in layout.items():
        result[name]={'uv':[u+x,v+y,u+x+a,v+y+b],'texture':0}
    return result


def to_face_uv(geometry):
    result=deepcopy(geometry)
    for bone in result['minecraft:geometry'][0]['bones']:
        for c in bone.get('cubes',[]):
            if isinstance(c['uv'],list):
                faces=box_faces(c)
                c['uv']={}
                for key,f in faces.items():
                    u,v,U,V=f['uv']
                    if key in ('up','down'): u,v,U,V=U,V,u,v
                    c['uv'][key]={'uv':[u,v],'uv_size':[U-u,V-v]}
                c.pop('mirror',None)
    return result


def change_volumes(original):
    result=deepcopy(original)
    for bone in result['minecraft:geometry'][0]['bones']:
        params=PASS_1.get(bone['name'])
        if not params: continue
        for c in bone.get('cubes',[]):
            old=c['size'][:]; origin=c['origin'][:]
            new=[round(old[i]*params['scale'][i],5) for i in range(3)]
            c['size']=new
            for i in range(3):
                anchor=1 if i==1 and params['anchor']=='top' else .5
                c['origin'][i]=round(origin[i]+(old[i]-new[i])*anchor,5)
    return result


def to_bbmodel(geometry,texture):
    """Mesmo bind pose/UVs do geo; projeto editável sem plugin obrigatório."""
    geo=geometry['minecraft:geometry'][0]; desc=geo['description']
    groups={}; elements=[]
    for b in geo['bones']:
        p=b.get('pivot',[0,0,0]); r=b.get('rotation',[0,0,0])
        groups[b['name']]={'name':b['name'],'uuid':uid('bone/'+b['name']),
            'origin':[-p[0],p[1],p[2]],'rotation':[-r[0],-r[1],r[2]],
            'export':True,'visibility':True,'isOpen':False,'children':[]}
        for i,c in enumerate(b.get('cubes',[])):
            o=c['origin']; s=c['size']; cp=c.get('pivot',[0,0,0]); cr=c.get('rotation',[0,0,0])
            fr=[-o[0]-s[0],o[1],o[2]]
            faces={}
            for key,f in c['uv'].items():
                u,v=f['uv']; w,h=f['uv_size']; uv=[u,v,u+w,v+h]
                if key in ('up','down'): uv=uv[2:]+uv[:2]
                faces[key]={'uv':uv,'texture':0}
            element={'name':b['name']+f'_{i}','uuid':uid('cube/'+b['name']+f'/{i}'),
                'type':'cube','from':fr,'to':[fr[a]+s[a] for a in range(3)],
                'origin':[-cp[0],cp[1],cp[2]],'rotation':[-cr[0],-cr[1],cr[2]],
                'box_uv':False,'autouv':0,'faces':faces,'export':True,'visibility':True}
            if 'inflate' in c: element['inflate']=c['inflate']
            elements.append(element); groups[b['name']]['children'].append(element['uuid'])
    roots=[]
    for b in geo['bones']:
        if b.get('parent'): groups[b['parent']]['children'].append(groups[b['name']])
        else: roots.append(groups[b['name']])
    return {'meta':{'format_version':'4.10','model_format':'bedrock','box_uv':False},
        'name':'Titanovenator — estudo local sobre o Rex do Revival',
        'model_identifier':'titanovenator_study','resolution':{
            'width':desc['texture_width'],'height':desc['texture_height']},
        'elements':elements,'outliner':roots,'animations':[],
        'textures':[{'name':'titanovenator.png','id':'0','uuid':uid('texture'),
            'width':desc['texture_width'],'height':desc['texture_height'],
            'uv_width':desc['texture_width'],'uv_height':desc['texture_height'],
            'mode':'bitmap','saved':True,'source':'data:image/png;base64,'+base64.b64encode(texture).decode()}]}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar',type=Path,default=DEFAULT_JAR)
    parser.add_argument('--skin',choices=['male','female'],default='male')
    args=parser.parse_args()
    if not args.jar.is_file(): parser.error('Instale o Revival separadamente ou informe --jar.')
    OUT.mkdir(parents=True,exist_ok=True)
    with ZipFile(args.jar) as z:
        raw=z.read(SOURCE_GEO)
        original=to_face_uv(json.loads(raw))
        texture=z.read(f'assets/fossil/textures/entity/tyrannosaurus/tyrannosaurus_{args.skin}.png')
        animation=z.read(SOURCE_ANIMATION)
    modified=change_volumes(original)
    # Limite de visibilidade serve só à inspeção; nenhuma escala do jogo é aplicada.
    for filename,data in [('rex-original.geo.json',original),('titanovenator.geo.json',modified),
                          ('titanovenator.bbmodel',to_bbmodel(modified,texture)),
                          ('rex-original.bbmodel',to_bbmodel(original,texture))]:
        (OUT/filename).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
    (OUT/'titanovenator.png').write_bytes(texture)
    (OUT/'rex-original.animation.json').write_bytes(animation)
    manifest={'status':'Passo 1 — volumes para revisão; não integrado ao jogo',
        'base':'Fossils and Archeology: Revival 1.20.1-9.3.4.0',
        'source_geometry':SOURCE_GEO,'source_geometry_sha256':hashlib.sha256(raw).hexdigest(),
        'texture':f'original {args.skin}, sem alterações',
        'bones':len(original['minecraft:geometry'][0]['bones']),
        'cubes':sum(len(b.get('cubes',[])) for b in original['minecraft:geometry'][0]['bones']),
        'pass_1':PASS_1,'animations':'arquivo original preservado; não importado no bbmodel nem tocado na prévia',
        'pending':['lábios/tecidos moles e cristas','paleta marrom-oliva/ventre ocre','cerdas discretas','animações e escala final'],
        'distribution':'Estudo local derivado. Não versionar, redistribuir ou incluir no jar.'}
    (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:manifest[k] for k in ('status','bones','cubes','texture')},ensure_ascii=False))


if __name__=='__main__': main()
