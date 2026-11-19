extends SceneTree

func _initialize() -> void:
    var world = load("res://scripts/world.gd").new()
    var slots: Array[Vector3] = []
    for i in range(500):
        var p: Vector3 = world._slot(i)
        assert(Vector2(p.x,p.z).length() >= 5.5, "Landmark exclusion violated")
        for previous in slots:
            assert(p.distance_to(previous) >= 2.9, "Building berths overlap")
        slots.append(p)
    world.preview = true
    world._catalog()
    assert(world.catalog.size() == 16)
    var id: String = world.catalog[0].id
    world.layout[id].name = "Captain's custom name"
    world.layout[id].district = 2
    world.layout[id].slot = -1
    world._catalog()
    assert(world.catalog.size() == 16, "Refresh duplicated catalog")
    assert(world.layout[id].name == "Captain's custom name")
    var occupied := {}
    for app in world.catalog:
        var entry: Dictionary = world.layout[str(app.id)]
        var key := str(entry.district)+":"+str(entry.slot)
        assert(not occupied.has(key), "Duplicate berth after reassignment")
        occupied[key] = true
    var disk := ConfigFile.new()
    assert(disk.load("user://captains_log.cfg") == OK)
    assert(disk.get_value("buildings","layout")[id].name == "Captain's custom name")
    world.free()
    print("ATLAS_TEST_PASS: 500 non-overlapping berths; catalog refresh; reassignment; persisted rename")
    quit(0)
