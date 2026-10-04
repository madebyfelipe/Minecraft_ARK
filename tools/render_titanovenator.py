#!/usr/bin/env python3
"""Renderizador ortográfico de revisão do estudo local (NumPy + Pillow).

Lê os projetos bbmodel e rasteriza sua geometria/textura reais com z-buffer.
Não reconstrói o desenho do animal. Saídas em local/, ignoradas pelo Git.
"""
import base64
import io
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'art/titanovenator/local'


def rotation(degrees):
    x,y,z=np.radians(degrees)
    rx=np.array([[1,0,0],[0,np.cos(x),-np.sin(x)],[0,np.sin(x),np.cos(x)]])
    ry=np.array([[np.cos(y),0,np.sin(y)],[0,1,0],[-np.sin(y),0,np.cos(y)]])
    rz=np.array([[np.cos(z),-np.sin(z),0],[np.sin(z),np.cos(z),0],[0,0,1]])
    return rx@ry@rz


def matrix(pivot,angles):
    r=rotation(angles); p=np.asarray(pivot); m=np.eye(4)
    m[:3,:3]=r; m[:3,3]=p-r@p
    return m


def scene(path,jaw=0,pose=None):
    """pose: osso -> (rotação, posição) no formato do arquivo de animação, somados ao repouso."""
    model=json.loads(path.read_text())
    texture=np.asarray(Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',')[1]))).convert('RGBA'))
    elements={c['uuid']:c for c in model['elements']}; faces=[]; head=[]
    def walk(items,parent,is_head=False):
        for item in items:
            if isinstance(item,dict):
                r=item.get('rotation',[0,0,0])[:]
                if item['name']=='lowerJaw':r[0]-=jaw
                shift=np.zeros(3);zoom=np.ones(3)
                if pose and item['name'] in pose:
                    dr,dp,ds=pose[item['name']]
                    r=[r[0]-dr[0],r[1]-dr[1],r[2]+dr[2]];shift=np.array([0,dp[1],0]);zoom=np.array(ds)
                transform=parent@matrix(item.get('origin',[0,0,0]),r)
                transform[:3,3]+=shift
                if (zoom!=1).any():
                    o=np.asarray(item.get('origin',[0,0,0]));S=np.diag(zoom)
                    scale=np.eye(4);scale[:3,:3]=S;scale[:3,3]=o-S@o
                    transform=transform@scale
                walk(item.get('children',[]),transform,is_head or item['name']=='head')
                continue
            c=elements[item]; x,y,z=c['from']; X,Y,Z=c['to']
            corners={
                'north':[(X,Y,z),(x,Y,z),(x,y,z),(X,y,z)],
                'south':[(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],
                'east':[(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],
                'west':[(x,Y,z),(x,Y,Z),(x,y,Z),(x,y,z)],
                'up':[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],
                'down':[(x,y,Z),(X,y,Z),(X,y,z),(x,y,z)],
            }
            normals={'north':[0,0,-1],'south':[0,0,1],'east':[1,0,0],'west':[-1,0,0],'up':[0,1,0],'down':[0,-1,0]}
            transform=parent@matrix(c.get('origin',[0,0,0]),c.get('rotation',[0,0,0]))
            for name,pts in corners.items():
                points=np.asarray(pts)@transform[:3,:3].T+transform[:3,3]
                if is_head:head.extend(points)
                normal=transform[:3,:3]@normals[name]
                uv=c['faces'][name]['uv']
                sx=texture.shape[1]/model['resolution']['width']
                sy=texture.shape[0]/model['resolution']['height']
                faces.append((points,normal,[uv[0]*sx,uv[1]*sy,uv[2]*sx,uv[3]*sy]))
    walk(model['outliner'],np.eye(4))
    return faces,texture,np.array(head)


def camera(yaw=-1.15,pitch=.10):
    z=np.array([math.cos(pitch)*math.sin(yaw),math.sin(pitch),-math.cos(pitch)*math.cos(yaw)])
    x=np.cross([0,1,0],z);x/=np.linalg.norm(x);y=np.cross(z,x)
    return np.array([x,y,z])


def render(data,reference,width=960,height=620):
    faces,texture,_=data;cam=camera()
    projected=reference@cam.T;lo=projected.min(axis=0);hi=projected.max(axis=0)
    scale=min(width/(hi[0]-lo[0]),height/(hi[1]-lo[1]))*.82
    center=(lo+hi)/2
    canvas=np.full((height,width,3),[243,241,235],dtype=np.uint8)
    depth=np.full((height,width),-np.inf)
    light1=np.array([-.5,1,-.7]);light1/=np.linalg.norm(light1)
    light2=np.array([.6,.4,1]);light2/=np.linalg.norm(light2)
    for points,normal,uv in faces:
        q=points@cam.T
        screen=np.column_stack(((q[:,0]-center[0])*scale+width/2,
                                -(q[:,1]-center[1])*scale+height/2,q[:,2]))
        u,v,U,V=uv;texcoords=np.array([[u,v],[U,v],[U,V],[u,V]])
        illumination=.62+.38*max(0,normal@light1)+.12*max(0,normal@light2)
        for ids in ((0,1,2),(0,2,3)):
            t=screen[list(ids)];tc=texcoords[list(ids)]
            xmin=max(0,math.floor(t[:,0].min()));xmax=min(width-1,math.ceil(t[:,0].max()))
            ymin=max(0,math.floor(t[:,1].min()));ymax=min(height-1,math.ceil(t[:,1].max()))
            if xmin>xmax or ymin>ymax:continue
            xx,yy=np.meshgrid(np.arange(xmin,xmax+1)+.5,np.arange(ymin,ymax+1)+.5)
            a,b,c=t
            den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(den)<1e-9:continue
            w0=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den
            w1=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;w2=1-w0-w1
            zz=w0*a[2]+w1*b[2]+w2*c[2]
            uu=np.clip((w0*tc[0,0]+w1*tc[1,0]+w2*tc[2,0]).astype(int),0,texture.shape[1]-1)
            vv=np.clip((w0*tc[0,1]+w1*tc[1,1]+w2*tc[2,1]).astype(int),0,texture.shape[0]-1)
            pixels=texture[vv,uu]
            dep=depth[ymin:ymax+1,xmin:xmax+1]
            mask=(w0>=-1e-7)&(w1>=-1e-7)&(w2>=-1e-7)&(zz>dep)&(pixels[:,:,3]>50)
            target=canvas[ymin:ymax+1,xmin:xmax+1]
            target[mask]=np.clip(pixels[:,:,:3]*illumination,0,255).astype('uint8')[mask]
            dep[mask]=zz[mask]
    return Image.fromarray(canvas)


def font(size):
    path='/usr/share/fonts/adwaita-sans-fonts/AdwaitaSans-Regular.ttf'
    return ImageFont.truetype(path,size) if Path(path).exists() else ImageFont.load_default(size=size)


def main():
    old=scene(OUT/'passo-1.bbmodel');new=scene(OUT/'titanovenator.bbmodel')
    opened=scene(OUT/'titanovenator.bbmodel',30)
    reference=np.concatenate([old[2],new[2],opened[2]])
    sheet=Image.new('RGB',(1920,760),(243,241,235));draw=ImageDraw.Draw(sheet)
    draw.text((44,24),'TITANOVENATOR  /  REVISÃO DA CABEÇA',fill=(61,66,49),font=font(25))
    for i,(label,data) in enumerate([('PASSO 1 · BASE APROVADA',old),('PASSO 2 · CABEÇA E LÁBIOS',new)]):
        sheet.paste(render(data,reference),(i*960,100))
        draw.text((44+i*960,76),label,fill=(83,89,71),font=font(19))
    draw.text((44,718),'Mesma câmera e escala · textura original preservada · base: Fossils & Archaeology: Revival',fill=(93,98,84),font=font(19))
    sheet.save(OUT/'passo-2-cabeca.png')
    render(opened,reference).save(OUT/'passo-2-mandibula.png')
    print(OUT/'passo-2-cabeca.png')


def render_texture():
    data=scene(OUT/'titanovenator.bbmodel')
    points=np.concatenate([face[0] for face in data[0]])
    render(data,points,1600,900).save(OUT/'passo-3-corpo.png')
    opened=scene(OUT/'titanovenator.bbmodel',30)
    reference=np.concatenate([data[2],opened[2]])
    render(data,reference).save(OUT/'passo-3-cabeca.png')
    render(opened,reference).save(OUT/'passo-3-mandibula.png')


if __name__=='__main__':
    if '--texture' in sys.argv: render_texture()
    else: main()
