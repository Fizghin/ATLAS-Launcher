"""
ATLAS procedural pirate-world asset generator.
Run with Blender 4.x: blender -b --python tools/generate_atlas_assets.py
Outputs textured-material GLB models in assets/generated.
All geometry and materials are original procedural work.
"""
import bpy, math, random, os
from mathutils import Vector
random.seed(17)
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)
ROOT=os.path.abspath("assets/generated")
os.makedirs(ROOT,exist_ok=True)
def mat(name,color,rough=0.86,metal=0):
 m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
 p=m.node_tree.nodes.get("Principled BSDF");p.inputs["Base Color"].default_value=(*color,1);p.inputs["Roughness"].default_value=rough;p.inputs["Metallic"].default_value=metal
 return m
wood=mat("Aged cedar planks",(.27,.15,.075)); wood2=mat("Sunbleached timber",(.48,.32,.16))
roof=mat("Oxidized terracotta",(.36,.14,.09));roof2=mat("Roof moss",(.21,.31,.15))
stone=mat("Warm limestone",(.56,.48,.34));dark=mat("Dark iron",(.10,.10,.095),.5,.5)
glass=mat("Amber window glass",(.95,.55,.16),.23);grass=mat("Coastal grass",(.25,.37,.17))
sand=mat("Sand",(.75,.61,.37));sea=mat("Lagoon",(.10,.39,.42),.2)
leaf=mat("Pine needles",(.11,.29,.18));leaf2=mat("Palm fronds",(.21,.43,.20))
sail=mat("Canvas sails",(.78,.69,.48));rope=mat("Rope",(.45,.32,.16))
def cube(name,loc,scale,material,bevel=0.04):
 bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.name=name
 o.dimensions=scale;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
 o.data.materials.append(material)
 if bevel:
  mod=o.modifiers.new("Soft worn edges","BEVEL");mod.width=bevel;mod.segments=2
  mod2=o.modifiers.new("Weighted normals","WEIGHTED_NORMAL")
 return o
def cyl(name,loc,r,depth,material,vertices=12):
 bpy.ops.mesh.primitive_cylinder_add(vertices=vertices,radius=r,depth=depth,location=loc)
 o=bpy.context.object;o.name=name;o.data.materials.append(material);return o
def cone(name,loc,r1,r2,depth,material,vertices=9):
 bpy.ops.mesh.primitive_cone_add(vertices=vertices,radius1=r1,radius2=r2,depth=depth,location=loc)
 o=bpy.context.object;o.name=name;o.data.materials.append(material);return o
def beam(name,a,b,r,material):
 a,b=Vector(a),Vector(b);mid=(a+b)/2
 o=cyl(name,mid,r,(b-a).length,material,8)
 o.rotation_euler=(b-a).to_track_quat('Z','Y').to_euler();return o
def sign(name,x,y,z,label):
 cube(name+" wooden sign",(x,y,z),(1.1,.12,.45),wood2)
 # Text object becomes real geometry and is exported in glb
 bpy.ops.object.text_add(location=(x-.46,y-.075,z-.11),rotation=(math.pi/2,0,0))
 o=bpy.context.object;o.name=name+" inscription";o.data.body=label;o.data.size=.16;o.data.extrude=.002;o.data.materials.append(dark)
def window(x,y,z):
 cube("Window frame",(x,y,z),(.55,.13,.68),wood)
 cube("Glowing amber glass",(x,y-.09,z),(.40,.06,.54),glass,.015)
 cube("Window mullion",(x,y-.14,z),(.06,.06,.54),wood)
 cube("Window crossbar",(x,y-.15,z),(.40,.06,.055),wood)
def house(label,wide=3.8,deep=3.2,height=2.7,kind="tavern"):
 cube("Stone foundation",(0,0,.22),(wide+.4,deep+.4,.45),stone)
 cube("Plastered walls",(0,0,height/2+.4),(wide,deep,height),stone)
 for x in [-wide/2,wide/2]:
  for y in [-deep/2,deep/2]:
   cube("Timber corner post",(x,y,height/2+.4),(.22,.22,height+.25),wood)
 for z in [1.0,height+.4]:
  for y in [-deep/2,deep/2]:cube("Horizontal half timber",(0,y,z),(wide,.16,.16),wood)
 for y in [-deep/2,deep/2]:
  for x in [-wide/3,wide/3]:
   beam("Diagonal timber",(x-.35,y,1.05),(x+.35,y,height+.35),.075,wood)
 for x in [-wide*.27,wide*.27]:window(x,-deep/2-.1,1.9)
 cube("Carved wooden door",(0,-deep/2-.13,1.1),(.78,.16,1.55),wood)
 cyl("Iron door handle",(.27,-deep/2-.27,1.15),.055,.11,dark)
 # Two gabled roof slopes, built as long shingles and wooden rafters
 pitch=1.45
 for side in [-1,1]:
  a=(-wide/2-.3,0,height+.48);b=(0,0,height+.48+pitch)
  for i in range(11):
   x=(i+.5)/11*(wide/2+.35)
   z=height+.48+pitch*(1-x/(wide/2+.35))
   plank=cube("Individual roof shingle", (side*x,0,z),(.39,deep+.6,.13),roof if i%3 else roof2,.02)
   plank.rotation_euler[1]=side*.58
  for y in [-deep/2-.22,0,deep/2+.22]:
   beam("Exposed roof beam",(side*(wide/2+.3),y,height+.48),(0,y,height+.48+pitch),.09,wood)
 for x in [-wide*.38,wide*.38]:
  cyl("Barrel", (x,-deep/2-.6,.45),.35,.9,wood2)
  for z in [.15,.73]:cyl("Barrel iron hoop",(x,-deep/2-.6,z),.365,.055,dark)
 sign("Building sign",0,-deep/2-.37,height+.15,label)
 if kind=="tavern":
  cube("Stone chimney",(wide*.28,deep*.12,height+1.3),(.65,.7,2.2),stone)
  for i in range(3):cube("Chimney brick", (wide*.28,deep*.12,height+2.4+i*.12),(.7,.72,.08),roof)
 if kind=="vault":
  for x in [-wide*.42,wide*.42]:
   cyl("Fortified tower",(x,deep*.36,1.8),.55,3.6,stone)
   cone("Tower conical roof",(x,deep*.36,4.05),.8,0,1.2,roof,12)
 if kind=="observatory":
  cyl("Observatory tower",(0,deep*.1,height+1.6),1.0,3.2,stone,16)
  cone("Copper dome",(0,deep*.1,height+3.55),1.3,.2,1.3,roof2,16)
  cyl("Telescope tube",(.3,-.8,height+3.9),.19,1.7,dark)
def tree(x,y,s=1):
 cyl("Tree trunk",(x,y,.75*s),.14*s,1.5*s,wood)
 for z,r in [(1.3,.85),(1.85,.65),(2.35,.43)]:
  cone("Layered pine foliage",(x,y,z*s),r*s,0,1.1*s,leaf if z!=1.85 else leaf2)
def palm(x,y,s=1):
 beam("Curved palm trunk",(x,y,0),(x+.4*s,y,2.7*s),.14*s,wood2)
 for i in range(8):
  a=i*math.tau/8
  start=(x+.4*s,y,2.7*s);end=(x+.4*s+math.cos(a)*1.5*s,y+math.sin(a)*1.5*s,2.05*s)
  beam("Palm leaf spine",start,end,.055*s,leaf2)
def lantern(x,y):
 cyl("Lantern post",(x,y,1.25),.07,2.5,wood)
 cube("Lantern light",(x,y,2.45),(.33,.33,.42),glass)
 cone("Lantern cap",(x,y,2.76),.29,0,.25,dark)
def dock():
 for j in range(2):
  yy=-3.3-j*1.1
  for i in range(9):
   cube("Dock plank",((i-4)*.56,yy,.35),(.5,1.05,.14),wood2)
  for x in [-2.3,2.3]:cyl("Dock piling",(x,yy,.05),.16,1.25,wood)
 for x in [-2,0,2]:lantern(x,-3.8)
def island_base():
 # Raised terrain with natural irregular perimeter
 n=48;verts=[];faces=[]
 for z,r in [(0,1),(.48,.93),(1.0,.55)]:
  for i in range(n):
   a=math.tau*i/n
   wave=1+.12*math.sin(5*a)+.08*math.sin(11*a+.5)
   verts.append((math.cos(a)*r*8*wave,math.sin(a)*r*6.6*wave,z))
 for layer in range(2):
  for i in range(n):
   j=(i+1)%n
   faces.append((layer*n+i,layer*n+j,(layer+1)*n+j,(layer+1)*n+i))
 faces.append(tuple(range(n*2,n*3)))
 mesh=bpy.data.meshes.new("Terrain");mesh.from_pydata(verts,[],faces);mesh.materials.append(stone);mesh.materials.append(grass)
 obj=bpy.data.objects.new("Hand sculpted island terrain",mesh);bpy.context.collection.objects.link(obj)
 for p in obj.data.polygons:p.material_index=1 if p.index>=n else 0
 cyl("Sandy coastal shelf",(0,0,-.15),7.3,.24,sand,48)
def make(kind,label):
 bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
 island_base()
 house(label,kind=kind)
 for i in range(14):
  a=i*math.tau/14+.23
  r=4.6+random.uniform(-.5,.5)
  if i%3==0:palm(math.cos(a)*r,math.sin(a)*r,.75)
  else:tree(math.cos(a)*r,math.sin(a)*r,.7+random.random()*.35)
 for i in range(14):
  a=i*math.tau/14
  x,y=math.cos(a)*5.3,math.sin(a)*4.1
  cube("Shore boulder",(x,y,.5),(.7,.55,.45),stone)
 if kind=="shipyard":dock()
 if kind=="tavern":
  for x in [-2.3,2.3]:lantern(x,-2.7)
 bpy.ops.object.select_all(action='SELECT')
 bpy.ops.export_scene.gltf(filepath=os.path.join(ROOT,kind+".glb"),export_format='GLB',export_apply=True)
for kind,label in [("tavern","TAVERN"),("shipyard","SHIPYARD"),("vault","VAULT"),("observatory","OBSERVATORY")]:
 make(kind,label)
print("Exported four detailed GLB island scenes to",ROOT)
