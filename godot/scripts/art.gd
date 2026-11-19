extends RefCounted
## Original modeled architecture assembled from reusable meshes, never bare box houses.
var materials: Dictionary = {}
var palms: Array[Node3D] = []
var ships: Array[Node3D] = []

func mat(hex: String, metal := 0.0) -> StandardMaterial3D:
    if materials.has(hex): return materials[hex]
    var m := StandardMaterial3D.new()
    m.albedo_color = Color(hex)
    m.roughness = 0.8
    m.metallic = metal
    materials[hex] = m
    return m

func mesh(parent: Node3D, shape: Mesh, p: Vector3, color: String) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    n.mesh = shape
    n.position = p
    n.material_override = mat(color)
    parent.add_child(n)
    return n

func box(parent: Node3D, p: Vector3, size: Vector3, color: String) -> MeshInstance3D:
    var shape := BoxMesh.new()
    shape.size = size
    return mesh(parent, shape, p, color)

func cylinder(parent: Node3D, p: Vector3, bottom: float, top: float, height: float, color: String, sides := 12) -> MeshInstance3D:
    var shape := CylinderMesh.new()
    shape.bottom_radius = bottom
    shape.top_radius = top
    shape.height = height
    shape.radial_segments = sides
    return mesh(parent, shape, p, color)

func roof(parent: Node3D, width: float, depth: float, y: float, color: String) -> void:
    var st := SurfaceTool.new()
    st.begin(Mesh.PRIMITIVE_TRIANGLES)
    var v := [Vector3(-width,y,-depth), Vector3(width,y,-depth), Vector3(0,y+width*.85,-depth), Vector3(-width,y,depth), Vector3(width,y,depth), Vector3(0,y+width*.85,depth)]
    for i in [0,1,2, 5,4,3, 0,2,5, 0,5,3, 2,1,4, 2,4,5]: st.add_vertex(v[i])
    st.generate_normals()
    mesh(parent, st.commit(), Vector3.ZERO, color)
    # Roof ribs and overhanging eaves make the silhouette legible from above.
    for side in [-1,1]:
        box(parent,Vector3(side*width,y,0),Vector3(.10,.13,depth*2+.18),"#50382a")
    box(parent,Vector3(0,y+width*.85,0),Vector3(.13,.13,depth*2+.2),"#d7b87c")

func house(parent: Node3D, variant: int, district := 0, size := 1.0) -> Node3D:
    var root := Node3D.new()
    root.scale = Vector3.ONE*size
    parent.add_child(root)
    var walls: Array[String] = ["#dcc69b", "#c6b194", "#b7c1b7", "#e1c78c"]
    var roofs: Array[String] = ["#aa513b", "#3a797b", "#526a89", "#537957"]
    var height := 1.45 + (variant%3)*.22
    box(root,Vector3(0,.10,0),Vector3(1.85,.2,1.65),"#9a9a87")
    box(root,Vector3(0,height/2+.2,0),Vector3(1.55,height,1.35),walls[district])
    roof(root,.95,.85,height+.2,roofs[district])
    for x in [-.77,.77]:
        for z in [-.68,.68]: box(root,Vector3(x,height/2+.2,z),Vector3(.10,height+.12,.10),"#5b4233")
    for y in [.35,.95]: box(root,Vector3(0,y,.70),Vector3(1.55,.07,.08),"#7e6045")
    box(root,Vector3(0,.65,.72),Vector3(.42,.95,.09),"#4b392e")
    for x in [-.5,.5]:
        box(root,Vector3(x,1.1,.73),Vector3(.28,.39,.09),"#4f4737")
        box(root,Vector3(x,1.1,.79),Vector3(.20,.29,.04),"#f3cd78")
        box(root,Vector3(x,1.1,.82),Vector3(.035,.30,.025),"#72513a")
    box(root,Vector3(.47,height+.65,-.2),Vector3(.26,.8,.30),"#b79a7b")
    if variant%2 == 0:
        var awning := box(root,Vector3(0,1.3,1.05),Vector3(1.65,.08,.65),roofs[district])
        awning.rotation.x = -.17
        for x in [-.76,.76]: cylinder(root,Vector3(x,.6,1.3),.045,.045,1.2,"#72513a",6)
    cylinder(root,Vector3(1,.35,.4),.22,.19,.6,"#8c6143",10)
    for y in [.17,.55]: cylinder(root,Vector3(1,y,.4),.225,.225,.05,"#3d4c4b",10)
    return root

func palm(parent: Node3D, p: Vector3, seed_value: int) -> void:
    var root := Node3D.new()
    parent.add_child(root)
    root.position = p
    root.rotation.y = seed_value*1.73
    root.scale = Vector3.ONE*(.8+(seed_value%4)*.12)
    cylinder(root,Vector3(.12,1.2,0),.18,.10,2.4,"#8d7350",7).rotation.z = -.11
    for i in range(7):
        var leaf := Node3D.new()
        root.add_child(leaf)
        leaf.position = Vector3(.25,2.4,0)
        leaf.rotation.y = i*TAU/7
        var st := SurfaceTool.new()
        st.begin(Mesh.PRIMITIVE_TRIANGLES)
        for v in [Vector3(0,0,0),Vector3(-.32,.2,.8),Vector3(0,.27,1.5),Vector3(0,0,0),Vector3(0,.27,1.5),Vector3(.32,.2,.8),Vector3(-.32,.2,.8),Vector3(-.18,-.15,1.8),Vector3(0,.27,1.5),Vector3(0,.27,1.5),Vector3(-.18,-.15,1.8),Vector3(0,-.4,2.1)]: st.add_vertex(v)
        st.generate_normals()
        var n := mesh(leaf,st.commit(),Vector3.ZERO,"#39845b" if i%2 else "#75a05d")
        var m := n.material_override.duplicate() as StandardMaterial3D
        m.cull_mode = BaseMaterial3D.CULL_DISABLED
        n.material_override = m
    palms.append(root)

func terrain(parent: Node3D, radius: float, seed_value: int) -> void:
    var st := SurfaceTool.new()
    st.begin(Mesh.PRIMITIVE_TRIANGLES)
    # Matching irregular rings form cliffs, sloping grass, and a sandy shelf.
    var radii := [0.0,.75,.87,1.0,1.035]
    var heights := [.65,.65,.18,-.4,-1.25]
    var colors := [Color("#789267"),Color("#789267"),Color("#cfbc8c"),Color("#e6d2a3"),Color("#737f72")]
    for ring in range(4):
        for i in range(64):
            var points: Array[Vector3] = []
            for pair in [Vector2i(ring,i),Vector2i(ring,i+1),Vector2i(ring+1,i),Vector2i(ring+1,i+1)]:
                var a: float = pair.y*TAU/64
                var irregular := 1.0+.055*sin(a*5+seed_value)+.065*cos(a*3+seed_value*2)
                points.append(Vector3(cos(a)*radius*radii[pair.x]*irregular,heights[pair.x],sin(a)*radius*radii[pair.x]*irregular))
            for k in [0,2,1,1,2,3]:
                st.set_color(colors[ring].lerp(colors[ring+1], .5))
                st.add_vertex(points[k])
    st.generate_normals()
    var n := MeshInstance3D.new()
    n.mesh = st.commit()
    var m := StandardMaterial3D.new()
    m.vertex_color_use_as_albedo = true
    m.roughness = .95
    n.material_override = m
    parent.add_child(n)

func dock(parent: Node3D, radius: float) -> void:
    for i in range(12):
        box(parent,Vector3(radius*.82+i*.42,-.08,0),Vector3(.38,.16,1.7),"#9a774e")
        if i%3 == 0:
            for z in [-.72,.72]: cylinder(parent,Vector3(radius*.82+i*.42,-.30,z),.09,.09,1.65,"#634c34",8)

func ship(parent: Node3D, p: Vector3, variant: int) -> void:
    var root := Node3D.new()
    parent.add_child(root)
    root.position = p
    root.rotation.y = variant*.8
    var hull := SphereMesh.new()
    hull.radius = 1
    hull.height = 2
    hull.radial_segments = 16
    hull.rings = 8
    mesh(root,hull,Vector3.ZERO,"#553d31").scale = Vector3(.85,.55,2.5)
    box(root,Vector3(0,.32,0),Vector3(1.4,.13,3.9),"#bc9561")
    cylinder(root,Vector3(0,2.3,0),.07,.05,4.2,"#715034",8)
    box(root,Vector3(0,3,0),Vector3(2.7,.09,.10),"#715034")
    var sail := PlaneMesh.new()
    sail.size = Vector2(2.45,2.15)
    var n := mesh(root,sail,Vector3(0,2.1,.08),"#eee0ba")
    n.rotation.x = PI/2
    var m := n.material_override.duplicate() as StandardMaterial3D
    m.cull_mode = BaseMaterial3D.CULL_DISABLED
    n.material_override = m
    box(root,Vector3(.35,4.1,0),Vector3(.7,.4,.04),"#293e43")
    ships.append(root)

func landmark(parent: Node3D, district: int) -> void:
    if district < 2:
        house(parent,3,district,2.0)
        if district == 0:
            cylinder(parent,Vector3(-2.7,1,0),.6,.6,1.5,"#795235")
            box(parent,Vector3(0,2,2),Vector3(2.0,.7,.10),"#4c3526")
        else:
            box(parent,Vector3(3,2.5,0),Vector3(.25,5,.25),"#80623f")
            box(parent,Vector3(3,4.7,1.5),Vector3(.22,.23,4),"#80623f")
            cylinder(parent,Vector3(3,3.3,3),.025,.025,2.6,"#443c30",6)
    elif district == 2:
        house(parent,5,2,2)
        for x in [-2.1,2.1]:
            for z in [-1.8,1.8]:
                cylinder(parent,Vector3(x,2.3,z),.7,.65,4.1,"#aeb5a2")
                cylinder(parent,Vector3(x,4.9,z),.9,0,1.4,"#536d8c")
                for i in range(6):
                    var a := i*TAU/6
                    box(parent,Vector3(x+cos(a)*.6,4.25,z+sin(a)*.6),Vector3(.22,.4,.22),"#c2c6ad")
    else:
        cylinder(parent,Vector3(0,3,0),1.4,.85,5.3,"#d5c8aa",20)
        for y in [1.4,3.3,5.3]: cylinder(parent,Vector3(0,y,0),1.2,1.2,.18,"#447b80",20)
        cylinder(parent,Vector3(0,6.2,0),1.3,1.3,.95,"#f5d488",16)
        cylinder(parent,Vector3(0,7,0),1.5,0,.8,"#426c73",16)
        cylinder(parent,Vector3(3,1.3,0),1.4,1.4,1.2,"#b2aa8d",16)
        var dome := SphereMesh.new()
        dome.radius = 1.5
        dome.height = 3
        mesh(parent,dome,Vector3(3,2,0),"#5f8990")

func animate(time: float) -> void:
    for i in range(palms.size()): palms[i].rotation.z = sin(time*.7+i)*.025
    for i in range(ships.size()):
        ships[i].position.y = -.6+sin(time*.8+i)*.10
        ships[i].rotation.z = sin(time*.6+i)*.035
