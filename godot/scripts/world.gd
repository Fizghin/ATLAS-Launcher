extends Node3D
## ATLAS cinematic archipelago prototype. All geometry is real MeshInstance3D.
## App buildings are populated through app_catalog.json until Android bridge is integrated.

const DISTRICTS = ["THE TAVERN", "THE SHIPYARD", "THE VAULT", "THE OBSERVATORY"]
const CENTERS = [Vector3(-13,0,-12),Vector3(13,0,-12),Vector3(-13,0,12),Vector3(13,0,12)]
var app_catalog: Array = []
var current_district := -1
var camera: Camera3D
var target_pos := Vector3(0,34,49)
var target_look := Vector3.ZERO
var camera_look := Vector3.ZERO
var camera_tween: Tween
var selected_buildings: Array[StaticBody3D] = []
var title: Label
var status: Label
var clock_label: Label
var settings_panel: PanelContainer
var drag_start := Vector2.ZERO
var dragging := false
var sea_material: ShaderMaterial
var elapsed := 0.0

func _ready() -> void:
    _load_catalog()
    _setup_environment()
    _build_world()
    _setup_ui()
    _fly_to(-1)

func _load_catalog() -> void:
    if FileAccess.file_exists("user://app_catalog.json"):
        var data = JSON.parse_string(FileAccess.get_file_as_string("user://app_catalog.json"))
        if data is Array: app_catalog = data
    if app_catalog.is_empty():
        app_catalog = [
            {"label":"Messages","package":"demo.messages","district":0},
            {"label":"Chat","package":"demo.chat","district":0},
            {"label":"Maps","package":"demo.maps","district":1},
            {"label":"Garage","package":"demo.garage","district":1},
            {"label":"Files","package":"demo.files","district":2},
            {"label":"Notes","package":"demo.notes","district":2},
            {"label":"Weather","package":"demo.weather","district":3},
            {"label":"Clock","package":"demo.clock","district":3}
        ]

func _material(color: Color, metallic := 0.0, roughness := 0.85) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = color
    m.metallic = metallic
    m.roughness = roughness
    return m

func _setup_environment() -> void:
    var env := WorldEnvironment.new()
    env.environment = Environment.new()
    env.environment.background_mode = Environment.BG_COLOR
    env.environment.background_color = Color("#83bac3")
    env.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.environment.ambient_light_color = Color("#b6d0c6")
    env.environment.ambient_light_energy = 0.65
    env.environment.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    add_child(env)
    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-47,-35,0)
    sun.light_color = Color("#ffdcaa")
    sun.light_energy = 1.6
    sun.shadow_enabled = true
    add_child(sun)
    camera = Camera3D.new()
    camera.fov = 48
    camera.current = true
    add_child(camera)
    var sea := MeshInstance3D.new()
    var plane := PlaneMesh.new()
    plane.size = Vector2(120,120)
    sea.mesh = plane
    sea.position.y = -1.35
    sea_material = ShaderMaterial.new()
    sea_material.shader = load("res://shaders/ocean.gdshader")
    sea.material_override = sea_material
    add_child(sea)

func _box(parent: Node3D, pos: Vector3, size: Vector3, mat: Material) -> MeshInstance3D:
    var node := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = size
    node.mesh = mesh
    node.material_override = mat
    node.position = pos
    parent.add_child(node)
    return node

func _cylinder(parent: Node3D, pos: Vector3, top: float, bottom: float, height: float, mat: Material, sides := 12) -> MeshInstance3D:
    var node := MeshInstance3D.new()
    var mesh := CylinderMesh.new()
    mesh.top_radius = top
    mesh.bottom_radius = bottom
    mesh.height = height
    mesh.radial_segments = sides
    node.mesh = mesh
    node.material_override = mat
    node.position = pos
    parent.add_child(node)
    return node

func _build_world() -> void:
    for district in range(4):
        _build_island(district)

func _build_island(index: int) -> void:
    var root := Node3D.new()
    root.name = DISTRICTS[index]
    root.position = CENTERS[index]
    add_child(root)
    var sand := _material(Color("#d9bd84"))
    var grass := _material(Color("#718b58"))
    var rock := _material(Color("#777a6c"))
    # Rounded coastline formed from concentric irregular rings, not square platforms.
    _terrain(root, 9.4, -0.7, sand, 0.23)
    _terrain(root, 8.1, 0.05, grass, 0.28)
    _terrain(root, 6.7, 0.58, grass, 0.17)
    for i in range(34):
        var angle := float(i)*TAU/34.0
        var radius := 5.4 + sin(i*3.8+index)*1.4
        var p := Vector3(cos(angle)*radius,0.8,sin(angle)*radius)
        _tree(root,p,0.7+float(i%4)*0.13)
    _landmark(root,index)
    var apps: Array = app_catalog.filter(func(a): return int(a.get("district",0)) == index)
    for i in range(apps.size()):
        var a: float = TAU*float(i)/max(1,apps.size()) + 0.2
        var ring: float = 2.8 + float(i%3)*1.1
        var pos := Vector3(cos(a)*ring,1.18,sin(a)*ring)
        _app_building(root,pos,apps[i],i)
    _dock(root,Vector3(7.5,0,0))

func _terrain(parent: Node3D, radius: float, height: float, mat: Material, wobble: float) -> void:
    var mesh := ArrayMesh.new()
    var verts := PackedVector3Array()
    var normals := PackedVector3Array()
    var indices := PackedInt32Array()
    var n := 64
    verts.append(Vector3(0,height+0.2,0))
    for i in range(n):
        var angle := TAU*i/n
        var rr := radius*(1.0+wobble*0.22*sin(i*0.91)+wobble*0.14*cos(i*1.77))
        verts.append(Vector3(cos(angle)*rr,height,sin(angle)*rr))
    for i in range(n):
        indices.append(0)
        indices.append(1+i)
        indices.append(1+(i+1)%n)
    for i in range(verts.size()): normals.append(Vector3.UP)
    var arrays := []
    arrays.resize(Mesh.ARRAY_MAX)
    arrays[Mesh.ARRAY_VERTEX] = verts
    arrays[Mesh.ARRAY_NORMAL] = normals
    arrays[Mesh.ARRAY_INDEX] = indices
    mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES,arrays)
    var node := MeshInstance3D.new()
    node.mesh = mesh
    node.material_override = mat
    parent.add_child(node)

func _tree(parent: Node3D, p: Vector3, scale_factor: float) -> void:
    var tree := Node3D.new()
    tree.position = p
    tree.scale = Vector3.ONE*scale_factor
    parent.add_child(tree)
    _cylinder(tree,Vector3(0,0.85,0),0.13,0.19,1.7,_material(Color("#6b4930")))
    for j in range(3):
        _cylinder(tree,Vector3(0,1.9+j*0.62,0),0.05,1.15-j*0.22,1.65,_material(Color("#365d41")),9)

func _landmark(parent: Node3D, index: int) -> void:
    var stone := _material(Color("#a99c7b"))
    var wood := _material(Color("#785039"))
    var roof := _material(Color("#68403b"))
    if index == 3:
        _cylinder(parent,Vector3(0,3.4,0),1.25,1.8,6.3,stone,16)
        _cylinder(parent,Vector3(0,7,0),1.8,1.8,1.0,_material(Color("#d3b976")),16)
        _cylinder(parent,Vector3(0,8,0),0.0,2.0,1.1,roof,16)
    elif index == 2:
        _box(parent,Vector3(0,2.7,0),Vector3(4.8,4.0,4.0),stone)
        for x in [-2.4,2.4]:
            for z in [-2.0,2.0]:
                _cylinder(parent,Vector3(x,3.7,z),0.9,1.0,5.7,stone)
                _cylinder(parent,Vector3(x,6.8,z),0,1.3,1.4,roof)
    else:
        _box(parent,Vector3(0,2.1,0),Vector3(4.3,2.7,3.5),wood)
        _box(parent,Vector3(0,3.7,0),Vector3(4.8,0.75,3.9),roof)
        if index == 1:
            _box(parent,Vector3(2,2.2,0),Vector3(0.25,3.6,0.25),stone)
            _box(parent,Vector3(3.3,4.1,0),Vector3(3,0.2,0.2),wood)

func _dock(parent: Node3D,p: Vector3) -> void:
    var wood := _material(Color("#855f3e"))
    for i in range(8):
        _box(parent,p+Vector3(i*.8,-.25,0),Vector3(.72,.16,2.0),wood)
        for z in [-.85,.85]:
            _box(parent,p+Vector3(i*.8,-.65,z),Vector3(.14,1.2,.14),wood)

func _app_building(parent: Node3D,p: Vector3,app: Dictionary,index: int) -> void:
    var body := StaticBody3D.new()
    body.position = p
    body.set_meta("package",app.get("package",""))
    body.set_meta("label",app.get("label","App"))
    parent.add_child(body)
    selected_buildings.append(body)
    var w := 1.0+float(index%3)*0.18
    var wall := _material(Color("#b69a72") if index%2==0 else Color("#987858"))
    var roof := _material(Color("#754336") if index%2==0 else Color("#4f5546"))
    _box(body,Vector3(0,.75,0),Vector3(w,1.5,w),wall)
    _box(body,Vector3(0,1.65,0),Vector3(w+0.24,.45,w+0.24),roof)
    _box(body,Vector3(0,.4,w/2+.02),Vector3(.28,.8,.07),_material(Color("#493024")))
    for x in [-w*.28,w*.28]:
        _box(body,Vector3(x,1.08,w/2+.05),Vector3(.16,.23,.07),_material(Color("#f6d69d"),0.05,.3))
    var shape := BoxShape3D.new()
    shape.size = Vector3(w,2.0,w)
    var collision := CollisionShape3D.new()
    collision.shape = shape
    collision.position.y = 1
    body.add_child(collision)
    var label := Label3D.new()
    label.text = str(app.get("label","App"))
    label.font_size = 40
    label.pixel_size = 0.006
    label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
    label.position = Vector3(0,2.4,0)
    label.modulate = Color("#fff0c6")
    label.outline_size = 8
    body.add_child(label)

func _setup_ui() -> void:
    var layer := CanvasLayer.new()
    add_child(layer)
    var root := Control.new()
    root.set_anchors_preset(Control.PRESET_FULL_RECT)
    layer.add_child(root)
    var heading := VBoxContainer.new()
    heading.set_anchors_preset(Control.PRESET_TOP_WIDE)
    root.add_child(heading)
    title = Label.new()
    title.text = "A T L A S"
    title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    title.add_theme_font_size_override("font_size",52)
    title.add_theme_color_override("font_color",Color("#ffe3ad"))
    heading.add_child(title)
    status = Label.new()
    status.text = "THE LOST ARCHIPELAGO"
    status.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    status.add_theme_font_size_override("font_size",23)
    status.add_theme_color_override("font_color",Color("#f5e3b7"))
    heading.add_child(status)
    var bottom := HBoxContainer.new()
    bottom.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
    bottom.alignment = BoxContainer.ALIGNMENT_CENTER
    root.add_child(bottom)
    for entry in ["WORLD MAP","ALL APPS","SETTINGS"]:
        var button := Button.new()
        button.text = entry
        button.custom_minimum_size = Vector2(180,65)
        bottom.add_child(button)
        match entry:
            "WORLD MAP": button.pressed.connect(func(): _fly_to(-1))
            "ALL APPS": button.pressed.connect(_show_apps)
            "SETTINGS": button.pressed.connect(_show_settings)
    clock_label = Label.new()
    clock_label.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
    clock_label.position = Vector2(-250,-110)
    root.add_child(clock_label)

func _process(delta: float) -> void:
    elapsed += delta
    # Ocean animation is performed on the GPU by the shader.
    if clock_label: clock_label.text = Time.get_time_string_from_system().substr(0,5)

func _fly_to(index: int) -> void:
    current_district = index
    if index < 0:
        target_pos = Vector3(0,34,49)
        target_look = Vector3.ZERO
        status.text = "THE LOST ARCHIPELAGO"
    else:
        target_pos = CENTERS[index]+Vector3(0,13,16)
        target_look = CENTERS[index]
        status.text = DISTRICTS[index]
    if camera_tween and camera_tween.is_running(): camera_tween.kill()
    camera_tween = create_tween().set_parallel(true)
    camera_tween.set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_IN_OUT)
    camera_tween.tween_property(camera,"position",target_pos,1.6)
    camera_tween.tween_property(self,"camera_look",target_look,1.6)
    camera_tween.finished.connect(func(): camera.look_at(camera_look))

func _physics_process(_delta: float) -> void:
    if camera and camera.position.distance_to(camera_look) > 0.1:
        camera.look_at(camera_look)

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventScreenTouch:
        if event.pressed:
            drag_start = event.position
            dragging = false
        elif not dragging:
            _tap(event.position)
    elif event is InputEventScreenDrag:
        dragging = true
        camera.position += camera.global_transform.basis.x * (-event.relative.x*.013)
    elif event is InputEventMagnifyGesture:
        camera.position += camera.global_transform.basis.z * ((1.0-event.factor)*4.0)
    elif event.is_action_pressed("ui_cancel"):
        _fly_to(-1)

func _tap(point: Vector2) -> void:
    var from := camera.project_ray_origin(point)
    var to := from+camera.project_ray_normal(point)*150
    var query := PhysicsRayQueryParameters3D.create(from,to)
    var hit := get_world_3d().direct_space_state.intersect_ray(query)
    if hit and hit.collider is StaticBody3D and hit.collider.has_meta("package"):
        _launch_app(str(hit.collider.get_meta("package")))
        return
    if current_district < 0:
        var nearest := 0
        var best := INF
        for i in range(4):
            var screen := camera.unproject_position(CENTERS[i])
            var d := screen.distance_to(point)
            if d < best: best=d;nearest=i
        if best < 400: _fly_to(nearest)

func _launch_app(package_name: String) -> void:
    # The native Android launcher plugin must implement this method for installed apps.
    if Engine.has_singleton("AtlasAndroid"):
        Engine.get_singleton("AtlasAndroid").launchApp(package_name)
    else:
        status.text = "Preview only: " + package_name

func _show_apps() -> void:
    status.text = "APP BUILDINGS: " + str(app_catalog.size()) + " • TAP A BUILDING TO LAUNCH"

func _show_settings() -> void:
    status.text = "SETTINGS • MUSIC / WIDGETS / UPDATES: NATIVE BRIDGE PENDING"
