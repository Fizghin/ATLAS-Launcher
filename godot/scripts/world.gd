extends Node3D

const Art = preload("res://scripts/art.gd")
const DISTRICTS = ["The Tavern", "The Shipyard", "The Royal Vault", "The Observatory"]
const FONT = preload("res://assets/fonts/PirataOne-Regular.ttf")
var art = Art.new()
var bridge: Object
var catalog: Array = []
var layout: Dictionary = {}
var config := ConfigFile.new()
var centers: Array[Vector3] = []
var radii: Array[float] = []
var islands: Array[Node3D] = []
var buildings: Array[StaticBody3D] = []
var signs: Array[Label3D] = []
var camera: Camera3D
var look := Vector3.ZERO
var focus := -1
var yaw := 0.0
var distance := 26.0
var flight: Tween
var title: Label
var status: Label
var clock: Label3D
var battery: Label3D
var ui: Control
var modal: AcceptDialog
var music: AudioStreamPlayer
var ocean: AudioStreamPlayer
var effect: AudioStreamPlayer
var press := Vector2.ZERO
var held := 0.0
var moved := false
var pressing := false
var fingers: Dictionary = {}
var pinch := 0.0
var elapsed := 0.0
var refresh_time := 0.0
var revision := -1
var selected: Dictionary = {}
var sea: MeshInstance3D
var preview := false

func _ready() -> void:
    get_tree().auto_accept_quit = false
    config.load("user://captains_log.cfg")
    var saved = config.get_value("buildings", "layout", {})
    if saved is Dictionary: layout = saved
    if Engine.has_singleton("AtlasAndroid"): bridge = Engine.get_singleton("AtlasAndroid")
    preview = OS.get_name() != "Android"
    _catalog()
    _environment()
    _world()
    _interface()
    _audio()
    _fly(-1, true)
    if bridge: bridge.checkUpdates(false)
    if preview: status.text = "Desktop preview · Android apps appear on device"
    elif not bridge: status.text = "Android bridge unavailable. Please reinstall a verified ATLAS build."
    print("ATLAS_READY source=", "desktop" if preview else "android", " apps=", catalog.size(), " buildings=", buildings.size(), " bridge=", bridge != null)
    for arg in OS.get_cmdline_user_args():
        if arg.begins_with("--capture="): _capture(arg.trim_prefix("--capture="))

func _catalog() -> void:
    if bridge:
        var result = JSON.parse_string(bridge.getApps())
        if result is Array: catalog = result
        revision = bridge.catalogRevision()
    elif preview:
        # Explicit desktop demonstration only. Never inject fictional apps on Android.
        catalog.clear()
        for i in range(16):
            catalog.append({"id":"preview."+str(i),"component":"", "label":["Messages","Signal","Mail","Contacts","Maps","Travel","Rail","Harbor","Files","Notes","Books","Calendar","Settings","Clock","Weather","Camera"][i],"district":i/4})
    var occupied: Dictionary = {}
    for app in catalog:
        var id := str(app.id)
        if not layout.has(id): layout[id] = {"district":int(app.district),"slot":-1,"name":""}
        var entry: Dictionary = layout[id]
        entry.district = clampi(int(entry.get("district",app.district)),0,3)
        var key := str(entry.district)+":"+str(entry.get("slot",-1))
        if int(entry.get("slot",-1)) < 0 or occupied.has(key): entry.slot = -1
        else: occupied[key] = true
    for app in catalog:
        var entry: Dictionary = layout[str(app.id)]
        if int(entry.slot) >= 0: continue
        var slot := 0
        while occupied.has(str(entry.district)+":"+str(slot)): slot += 1
        entry.slot = slot
        occupied[str(entry.district)+":"+str(slot)] = true
    _save()

func _save() -> void:
    config.set_value("buildings","layout",layout)
    var error := config.save("user://captains_log.cfg")
    if error != OK and status: status.text = "Could not save layout: " + error_string(error)

func _environment() -> void:
    var env := WorldEnvironment.new()
    env.environment = Environment.new()
    env.environment.background_mode = Environment.BG_COLOR
    env.environment.background_color = Color("#8db8bc")
    env.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.environment.ambient_light_color = Color("#b9d3d0")
    env.environment.ambient_light_energy = .65
    env.environment.tonemap_mode = Environment.TONE_MAPPER_LINEAR
    env.environment.fog_enabled = true
    env.environment.fog_light_color = Color("#8db8bc")
    env.environment.fog_density = .0015
    add_child(env)
    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-52,-30,0)
    sun.light_color = Color("#ffe2b1")
    sun.light_energy = .9
    sun.shadow_enabled = config.get_value("graphics","shadows",true)
    sun.name = "Sun"
    add_child(sun)
    camera = Camera3D.new()
    camera.fov = 54
    camera.far = 600
    add_child(camera)
    camera.current = true
    sea = MeshInstance3D.new()
    var plane := PlaneMesh.new()
    plane.size = Vector2(650,650)
    plane.subdivide_depth = 96
    plane.subdivide_width = 96
    sea.mesh = plane
    sea.position.y = -1
    var material := ShaderMaterial.new()
    material.shader = load("res://shaders/ocean.gdshader")
    sea.material_override = material
    add_child(sea)

# Expanding square rings provide a unique berth for every app without truncation.
func _slot(index: int) -> Vector3:
    var ring := 1
    var remaining := index
    while remaining >= ring*8:
        remaining -= ring*8
        ring += 1
    var side := remaining/(ring*2)
    var offset := remaining%(ring*2)-ring
    var p := Vector2.ZERO
    match side:
        0: p = Vector2(offset,-ring)
        1: p = Vector2(ring,offset)
        2: p = Vector2(-offset,ring)
        3: p = Vector2(-ring,-offset)
    # First ring surrounds the landmark; subsequent berths stay >= 3 units apart.
    var expansion := 3.0+2.6/ring
    return Vector3(p.x*expansion,.68,p.y*expansion)

func _world() -> void:
    for island in islands:
        remove_child(island)
        island.queue_free()
    islands.clear(); buildings.clear(); signs.clear(); centers.clear(); radii.clear()
    art.palms.clear(); art.ships.clear()
    for district in range(4):
        var radius := 11.5
        for app in catalog:
            var entry: Dictionary = layout[str(app.id)]
            if int(entry.district) == district: radius = maxf(radius,_slot(int(entry.slot)).length()+3.7)
        radii.append(radius)
    var span: float = radii.max()*2+11
    centers.assign([Vector3(-span*.5,0,-span*.55),Vector3(span*.5,0,-span*.55),Vector3(-span*.5,0,span*.55),Vector3(span*.5,0,span*.55)])
    for district in range(4):
        var island := Node3D.new()
        island.position = centers[district]
        add_child(island)
        islands.append(island)
        art.terrain(island,radii[district],district+1)
        var landmark := Node3D.new()
        landmark.position.y = .65
        island.add_child(landmark)
        art.landmark(landmark,district)
        art.dock(island,radii[district])
        art.ship(island,Vector3(radii[district]+3,-.6,3.1),district)
        for i in range(18):
            var angle := i*TAU/18+district*.3
            var p := Vector3(cos(angle)*(radii[district]-1.8),.3,sin(angle)*(radii[district]-1.8))
            if absf(p.z) > 2.0 or p.x < 0: art.palm(island,p,i+district*18)
            var rock := SphereMesh.new()
            rock.radius = .6
            rock.height = 1
            rock.radial_segments = 7
            rock.rings = 4
            art.mesh(island,rock,p*.96,"#879581").scale = Vector3(1.3,.7,.85)
        var district_sign := _sign(island,DISTRICTS[district],Vector3(0,8.6,0),.022)
        district_sign.font = FONT
        var area := StaticBody3D.new()
        area.set_meta("district",district)
        var collider := CollisionShape3D.new()
        var shape := CylinderShape3D.new()
        shape.radius = radii[district]
        shape.height = 1.6
        collider.shape = shape
        area.add_child(collider)
        island.add_child(area)
        for app in catalog:
            var entry: Dictionary = layout[str(app.id)]
            if int(entry.district) != district: continue
            var body := StaticBody3D.new()
            body.position = _slot(int(entry.slot))
            body.set_meta("app",app)
            island.add_child(body)
            buildings.append(body)
            art.house(body,abs(str(app.id).hash())%20,district)
            var collision := CollisionShape3D.new()
            var box := BoxShape3D.new()
            box.size = Vector3(2.3,3.4,2.3)
            collision.shape = box
            collision.position.y = 1.6
            body.add_child(collision)
            var label := _sign(body,_name(app),Vector3(0,3.8,0),.009)
            signs.append(label)
    clock = _sign(islands[2],"",Vector3(0,7.8,0),.014)
    battery = _sign(islands[3],"",Vector3(0,8,0),.014)
    _visibility()

func _name(app: Dictionary) -> String:
    var renamed := str(layout[str(app.id)].get("name",""))
    return renamed if not renamed.is_empty() else str(app.label)

func _sign(parent: Node3D, text: String, p: Vector3, pixel: float) -> Label3D:
    var label := Label3D.new()
    label.text = text
    label.position = p
    label.pixel_size = pixel
    label.font_size = 32
    label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
    label.modulate = Color("#fff1cd")
    label.outline_modulate = Color("#263f42")
    label.outline_size = 9
    label.no_depth_test = true
    parent.add_child(label)
    return label

func _interface() -> void:
    var layer := CanvasLayer.new()
    add_child(layer)
    ui = Control.new()
    ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    ui.mouse_filter = Control.MOUSE_FILTER_IGNORE
    layer.add_child(ui)
    var theme := Theme.new()
    theme.default_font_size = 22
    var normal := StyleBoxFlat.new()
    normal.bg_color = Color("#45372b")
    normal.border_color = Color("#c9a567")
    normal.set_border_width_all(2)
    normal.set_content_margin_all(14)
    var hover := normal.duplicate() as StyleBoxFlat
    hover.bg_color = Color("#6a5338")
    var focused := normal.duplicate() as StyleBoxFlat
    focused.border_color = Color("#fff1cd")
    focused.set_border_width_all(4)
    for style in ["normal","disabled"]: theme.set_stylebox(style,"Button",normal)
    for style in ["hover","pressed"]: theme.set_stylebox(style,"Button",hover)
    theme.set_stylebox("focus","Button",focused)
    theme.set_color("font_color","Button",Color("#fff1cd"))
    var parchment := StyleBoxFlat.new()
    parchment.bg_color = Color("#e6d2a3")
    parchment.border_color = Color("#c9a567")
    parchment.set_border_width_all(3)
    parchment.set_content_margin_all(20)
    theme.set_stylebox("panel","AcceptDialog",parchment)
    theme.set_stylebox("panel","PopupPanel",parchment)
    theme.set_color("font_color","Label",Color("#263f42"))
    ui.theme = theme
    var heading := VBoxContainer.new()
    heading.position = Vector2(24,42)
    heading.size.x = 672
    heading.mouse_filter = Control.MOUSE_FILTER_IGNORE
    ui.add_child(heading)
    title = Label.new()
    title.text = "ATLAS"
    title.add_theme_font_override("font",FONT)
    title.add_theme_font_size_override("font_size",76)
    title.add_theme_color_override("font_color",Color("#fff1cd"))
    title.add_theme_color_override("font_shadow_color",Color("#263f42"))
    title.add_theme_constant_override("shadow_offset_y",3)
    title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    heading.add_child(title)
    status = Label.new()
    status.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    status.custom_minimum_size.y = 62
    status.add_theme_color_override("font_color",Color("#fff1cd"))
    heading.add_child(status)
    var footer := VBoxContainer.new()
    footer.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_WIDE)
    footer.offset_top = -168
    footer.offset_left = 20
    footer.offset_right = -20
    ui.add_child(footer)
    var hint := Label.new()
    hint.text = "Tap an island · Hold a building to edit"
    hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    hint.add_theme_color_override("font_color",Color("#fff1cd"))
    footer.add_child(hint)
    var buttons := HBoxContainer.new()
    buttons.alignment = BoxContainer.ALIGNMENT_CENTER
    footer.add_child(buttons)
    for item in ["World","Find app","Settings"]:
        var button := _button(item,buttons)
        button.custom_minimum_size = Vector2(212,64)
        match item:
            "World": button.pressed.connect(func(): _fly(-1))
            "Find app": button.pressed.connect(_search)
            "Settings": button.pressed.connect(_settings)
    var compass := Label.new()
    compass.text = "· N ·"
    compass.add_theme_font_override("font",FONT)
    compass.add_theme_font_size_override("font_size",30)
    compass.add_theme_color_override("font_color",Color("#fff1cd"))
    compass.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    footer.add_child(compass)

func _button(text: String, parent: Node) -> Button:
    var button := Button.new()
    button.text = text
    button.custom_minimum_size.y = 58
    parent.add_child(button)
    button.pressed.connect(func():
        if effect and config.get_value("audio","effects",false): effect.play())
    return button

func _dialog(heading: String) -> VBoxContainer:
    if is_instance_valid(modal): modal.queue_free()
    modal = AcceptDialog.new()
    modal.title = heading
    modal.ok_button_text = "Close"
    modal.min_size = Vector2i(640,200)
    modal.max_size = Vector2i(680,1040)
    ui.add_child(modal)
    var scroll := ScrollContainer.new()
    scroll.custom_minimum_size = Vector2(590,640)
    modal.add_child(scroll)
    var box := VBoxContainer.new()
    box.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    box.add_theme_constant_override("separation",12)
    scroll.add_child(box)
    modal.popup_centered()
    return box

func _text(text: String, parent: Node) -> Label:
    var label := Label.new()
    label.text = text
    label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    label.custom_minimum_size.x = 550
    parent.add_child(label)
    return label

func _settings() -> void:
    var box := _dialog("Captain’s orders")
    _text("Sound & sea",box).add_theme_font_override("font",FONT)
    for entry in [["Music","music"],["Ocean ambience","ocean"],["Sound effects","effects"]]:
        var toggle := CheckButton.new()
        toggle.text = entry[0]
        toggle.button_pressed = config.get_value("audio",entry[1],false)
        box.add_child(toggle)
        toggle.toggled.connect(func(on): config.set_value("audio",entry[1],on); _save(); _apply_audio())
    _text("Music volume",box)
    var volume := HSlider.new()
    volume.min_value = 0
    volume.max_value = 1
    volume.step = .05
    volume.value = config.get_value("audio","volume",.4)
    volume.custom_minimum_size.y = 54
    box.add_child(volume)
    volume.value_changed.connect(func(value): config.set_value("audio","volume",value); _save(); _apply_audio())
    var shadows := CheckButton.new()
    shadows.text = "Detailed shadows"
    shadows.button_pressed = config.get_value("graphics","shadows",true)
    box.add_child(shadows)
    shadows.toggled.connect(func(on): config.set_value("graphics","shadows",on); get_node("Sun").shadow_enabled = on; _save())
    var motion := CheckButton.new()
    motion.text = "Reduce motion"
    motion.button_pressed = config.get_value("graphics","reduced_motion",false)
    box.add_child(motion)
    motion.toggled.connect(func(on): config.set_value("graphics","reduced_motion",on); _save())
    var fps := CheckButton.new()
    fps.text = "Save battery (30 FPS)"
    fps.button_pressed = config.get_value("graphics","low_power",false)
    box.add_child(fps)
    fps.toggled.connect(func(on): config.set_value("graphics","low_power",on); Engine.max_fps = 30 if on else 60; _save())
    _button("Choose ATLAS as home",box).pressed.connect(func():
        if bridge: bridge.selectDefaultHome()
        else: status.text = "Default home selection requires Android")
    _button("Captain’s cabin · Android widgets",box).pressed.connect(func():
        if bridge: bridge.openWidgets()
        else: status.text = "Widget hosting requires Android")
    _button("Check for updates",box).pressed.connect(_updates)
    _text("Weather, calendar and notifications: add a real Android widget in the Captain’s cabin. ATLAS does not invent their data.",box)

func _updates() -> void:
    if not bridge:
        _text("GitHub updates are available in the Android build.",_dialog("Updates"))
        return
    bridge.checkUpdates(true)
    var box := _dialog("GitHub releases")
    var message := _text("Checking GitHub·",box)
    var timer := Timer.new()
    timer.wait_time = .6
    box.add_child(timer)
    timer.timeout.connect(func():
        var data = JSON.parse_string(bridge.updateStatus())
        if not data is Dictionary: return
        message.text = str(data.get("state",""))+"\n"+str(data.get("versionName",""))+"\n"+str(data.get("notes",data.get("message","")))
        if data.get("state") == "available" and not box.has_meta("download"):
            box.set_meta("download",true)
            _button("Download APK",box).pressed.connect(func(): message.text = bridge.downloadUpdate())
    )
    timer.start()
    _button("Install downloaded update",box).pressed.connect(func(): message.text = bridge.installUpdate())

func _search() -> void:
    var box := _dialog("Find an app building")
    var row := HBoxContainer.new()
    box.add_child(row)
    var search := LineEdit.new()
    search.placeholder_text = "App name"
    search.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    search.custom_minimum_size.y = 56
    row.add_child(search)
    _button("Clear",row).pressed.connect(func(): search.clear(); search.grab_focus())
    var result := VBoxContainer.new()
    box.add_child(result)
    var populate := func(query: String):
        for child in result.get_children(): result.remove_child(child); child.queue_free()
        var found := 0
        for app in catalog:
            if not query.is_empty() and not _name(app).to_lower().contains(query.to_lower()): continue
            found += 1
            var button := _button(_name(app)+" · "+DISTRICTS[int(layout[str(app.id)].district)],result)
            button.pressed.connect(func():
                modal.hide()
                _fly(int(layout[str(app.id)].district))
                status.text = "Find "+_name(app)+" · tap its building"
            )
        if found == 0: _text("No matching apps. Clear the search to see every building.",result)
    search.text_changed.connect(populate)
    populate.call("")
    search.grab_focus()

func _edit(app: Dictionary) -> void:
    selected = app
    var box := _dialog("Edit "+_name(app))
    _text("Building name",box)
    var name_input := LineEdit.new()
    name_input.text = _name(app)
    name_input.max_length = 60
    name_input.custom_minimum_size.y = 58
    box.add_child(name_input)
    _button("Save name",box).pressed.connect(func():
        if name_input.text.strip_edges().is_empty(): return
        layout[str(app.id)].name = name_input.text.strip_edges()
        _save(); _world(); modal.hide())
    _text("Move to another island",box)
    for i in range(4):
        var district := i
        _button(DISTRICTS[i],box).pressed.connect(func():
            layout[str(app.id)].district = district
            layout[str(app.id)].slot = -1
            _catalog(); _world(); modal.hide(); _fly(district))
    _button("Move to next berth",box).pressed.connect(func():
        var entry: Dictionary = layout[str(app.id)]
        var next := int(entry.slot)+1
        for other in catalog:
            var other_entry: Dictionary = layout[str(other.id)]
            if other.id != app.id and int(other_entry.district) == int(entry.district) and int(other_entry.slot) == next:
                other_entry.slot = entry.slot
                break
        entry.slot = next
        _save(); _world(); modal.hide(); _fly(focus))

func _audio() -> void:
    music = AudioStreamPlayer.new()
    music.stream = load("res://assets/audio/harbor.wav")
    music.stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
    music.stream.loop_end = 24*22050
    add_child(music)
    ocean = AudioStreamPlayer.new()
    ocean.stream = load("res://assets/audio/ocean.wav")
    ocean.stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
    ocean.stream.loop_end = 24*22050
    add_child(ocean)
    effect = AudioStreamPlayer.new()
    effect.stream = load("res://assets/audio/bell.wav")
    effect.volume_db = -12
    add_child(effect)
    _apply_audio()
    Engine.max_fps = 30 if config.get_value("graphics","low_power",false) else 60

func _apply_audio() -> void:
    if not music: return
    music.volume_db = linear_to_db(maxf(.001,float(config.get_value("audio","volume",.4))))
    ocean.volume_db = -18
    for pair in [[music,"music"],[ocean,"ocean"]]:
        if config.get_value("audio",pair[1],false):
            if not pair[0].playing: pair[0].play()
        else: pair[0].stop()

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_GO_BACK_REQUEST or what == NOTIFICATION_WM_CLOSE_REQUEST:
        if is_instance_valid(modal) and modal.visible: modal.hide()
        else: _fly(-1)
    if what == NOTIFICATION_APPLICATION_PAUSED:
        if music: music.stream_paused = true
        if ocean: ocean.stream_paused = true
    if what == NOTIFICATION_APPLICATION_RESUMED:
        if music: music.stream_paused = false
        if ocean: ocean.stream_paused = false

func _fly(district: int, immediate := false) -> void:
    focus = district
    yaw = 0
    if flight and flight.is_running(): flight.kill()
    var destination := Vector3.ZERO if district < 0 else centers[district]
    distance = radii.max()*7.8 if district < 0 else radii[district]*2.6
    var position_target := destination+Vector3(0,distance*1.1,distance*.72) if district < 0 else destination+Vector3(0,distance*.78,distance)
    if immediate or config.get_value("graphics","reduced_motion",false):
        camera.position = position_target
        look = destination
    else:
        flight = create_tween().set_parallel(true).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_IN_OUT)
        flight.tween_property(camera,"position",position_target,1.15)
        flight.tween_property(self,"look",destination,1.15)
    status.text = "The lost archipelago · choose your island" if district < 0 else DISTRICTS[district]+" · tap a building to open"
    _visibility()
    if OS.get_cmdline_user_args().has("--atlas-test"): _test_state()

func _visibility() -> void:
    for i in range(buildings.size()): signs[i].visible = focus == int(layout[str(buildings[i].get_meta("app").id)].district)

func _test_state() -> void:
    await get_tree().create_timer(2).timeout
    var state := {"focus":focus,"apps":catalog.size(),"buildings":buildings.size(),"bridge":bridge != null,"viewport":[get_viewport().get_visible_rect().size.x,get_viewport().get_visible_rect().size.y],"islands":[],"targets":[]}
    for center in centers:
        var point := camera.unproject_position(center)
        state.islands.append([point.x,point.y])
    for building in buildings:
        var app: Dictionary = building.get_meta("app")
        if state.targets.size() >= 8: break
        if focus >= 0 and int(layout[str(app.id)].district) != focus: continue
        var point := camera.unproject_position(building.global_position+Vector3(0,1,0))
        state.targets.append({"component":app.component,"name":_name(app),"district":layout[str(app.id)].district,"point":[point.x,point.y]})
    print("ATLAS_TEST_STATE=",JSON.stringify(state))

func _process(delta: float) -> void:
    elapsed += delta
    refresh_time += delta
    if pressing: held += delta
    if not config.get_value("graphics","reduced_motion",false): art.animate(elapsed)
    if camera.position.distance_to(look) > .01: camera.look_at(look)
    if refresh_time > 1.0:
        refresh_time = 0
        clock.text = "Captain’s clock · "+Time.get_time_string_from_system().substr(0,5)
        battery.text = "Lighthouse · "+str(bridge.getBattery())+"%" if bridge else "Lighthouse · desktop preview"
        if bridge and revision != bridge.catalogRevision():
            _catalog(); _world(); _fly(focus)

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventScreenTouch:
        if event.pressed:
            fingers[event.index] = event.position
            if fingers.size() == 1:
                press = event.position; held = 0; moved = false; pressing = true
            else: moved = true; pressing = false
        else:
            fingers.erase(event.index)
            if fingers.is_empty():
                if not moved: _tap(event.position,held > .55)
                pressing = false
                pinch = 0
    elif event is InputEventScreenDrag:
        fingers[event.index] = event.position
        if fingers.size() >= 2:
            var points := fingers.values()
            var span: float = points[0].distance_to(points[1])
            if pinch > 0: _orbit(0,(pinch-span)*.055)
            pinch = span
            moved = true
        elif event.position.distance_to(press) > 12:
            moved = true
            _orbit(-event.relative.x*.004,0)
    elif event is InputEventMouseButton:
        if event.button_index == MOUSE_BUTTON_LEFT:
            if event.pressed: press = event.position; held = 0; pressing = true; moved = false
            else:
                if not moved: _tap(event.position,held>.55)
                pressing = false
        elif event.pressed: _orbit(0,-2 if event.button_index == MOUSE_BUTTON_WHEEL_UP else 2)
    elif event is InputEventMouseMotion and pressing and event.position.distance_to(press)>12:
        moved = true
        _orbit(-event.relative.x*.004,0)
    elif event is InputEventMagnifyGesture: _orbit(0,(1-event.factor)*8)
    elif event.is_action_pressed("ui_cancel"):
        if is_instance_valid(modal) and modal.visible: modal.hide()
        else: _fly(-1)

func _orbit(turn: float, zoom: float) -> void:
    if flight and flight.is_running(): flight.kill()
    yaw = clampf(yaw+turn,-PI,PI)
    var radius: float = radii.max() if focus<0 else radii[focus]
    distance = clampf(distance+zoom,radius*(3.8 if focus<0 else 1.1),radius*(10 if focus<0 else 4))
    look = Vector3.ZERO if focus<0 else centers[focus]
    camera.position = look+Vector3(sin(yaw)*distance,distance*.78,cos(yaw)*distance)

func _tap(point: Vector2, long_press := false) -> void:
    var origin := camera.project_ray_origin(point)
    var hit := get_world_3d().direct_space_state.intersect_ray(PhysicsRayQueryParameters3D.create(origin,origin+camera.project_ray_normal(point)*600))
    if hit.is_empty(): return
    var body: Node = hit.collider
    if focus < 0:
        if body.has_meta("district"): _fly(int(body.get_meta("district")))
        elif body.has_meta("app"): _fly(int(layout[str(body.get_meta("app").id)].district))
        return
    if body.has_meta("app"):
        var app: Dictionary = body.get_meta("app")
        if focus != int(layout[str(app.id)].district): return
        if long_press: _edit(app)
        elif bridge:
            if not bridge.launchApp(str(app.component)): status.text = "App unavailable. Refreshing installed buildings·"; revision = -1
        else: status.text = "Desktop preview · "+_name(app)+" opens on Android"

func _capture(path: String) -> void:
    await get_tree().create_timer(3).timeout
    await RenderingServer.frame_post_draw
    get_viewport().get_texture().get_image().save_png(path+"-world.png")
    _fly(0)
    await get_tree().create_timer(2).timeout
    await RenderingServer.frame_post_draw
    get_viewport().get_texture().get_image().save_png(path+"-tavern.png")
    get_tree().quit()
