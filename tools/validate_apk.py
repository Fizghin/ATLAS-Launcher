"""Fail closed when the build lacks Godot's world or Android bridge."""
import argparse
import hashlib
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("apk", type=Path)
args = parser.parse_args()
with zipfile.ZipFile(args.apk) as archive:
    names = archive.namelist()
    for entry in ["assets/atlas.pck", "lib/arm64-v8a/libgodot_android.so", "classes.dex", "AndroidManifest.xml"]:
        assert entry in names, f"Missing required APK member: {entry}"
    pack = archive.read("assets/atlas.pck")
    assert pack[:4] == b"GDPC", "Invalid Godot pack"
    for resource in [b"archipelago", b"world.gd", b"art.gd", b"ocean.gdshader", b"harbor.wav"]:
        assert resource in pack, f"Missing Godot resource: {resource!r}"
    dex = b"".join(archive.read(name) for name in names if name.endswith(".dex"))
    assert b"com/atlas/bridge/AtlasAndroid" in dex, "Missing Kotlin bridge"
print("PASS: Godot scene, architecture, shader, soundtrack, native engine, and Kotlin bridge packaged")
print("SHA256", hashlib.sha256(args.apk.read_bytes()).hexdigest())
