"""Exercise the real HOME activity and launch an installed app by ray-casting a building.
Use only with a disposable emulator: this test changes its default HOME application.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument("apk")
parser.add_argument("--output", default="smoke")
args = parser.parse_args()
out = Path(args.output)
out.mkdir(parents=True, exist_ok=True)
adb = os.environ.get("ADB", "adb")
def run(*command):
    return subprocess.check_output([adb, *command], text=True, stderr=subprocess.STDOUT)
def state(after=None):
    for _ in range(90):
        logs = run("logcat", "-d", "-s", "godot")
        lines = re.findall(r"ATLAS_TEST_STATE=(\{[^\r\n]+\})", logs)
        if lines:
            value = json.loads(lines[-1])
            if after is None or value["focus"] == after: return value
        time.sleep(1)
    (out / "logcat.txt").write_text(run("logcat", "-d"), encoding="utf8")
    raise AssertionError("No valid Godot test state; inspect logcat")
def capture(name):
    (out / name).write_bytes(subprocess.check_output([adb, "exec-out", "screencap", "-p"]))

run("install", "-r", args.apk)
run("shell", "am", "force-stop", "com.atlas.launcher")
run("logcat", "-c")
run("shell", "am", "start", "-n", "com.atlas.launcher/.MainActivity", "--esa", "command_line_params", "--,--atlas-test")
initial = state(-1)
assert initial["bridge"] and initial["apps"] == initial["buildings"] and initial["apps"] > 0, initial
capture("android-world.png")
run("shell", "cmd", "package", "set-home-activity", "com.atlas.launcher/.MainActivity")
resolved = run("shell", "cmd", "package", "resolve-activity", "--brief", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
assert "com.atlas.launcher" in resolved, resolved
physical = list(map(int, re.search(r"(\d+)x(\d+)", run("shell", "wm", "size")).groups()))
def tap(point, s):
    x, y = [int(point[i] * physical[i] / s["viewport"][i]) for i in range(2)]
    run("shell", "input", "tap", str(x), str(y))
# Prefer utilities (Settings is widely present), then any occupied island.
district = next((t["district"] for t in initial["targets"] if "settings" in t["component"].lower()), initial["targets"][0]["district"])
tap(initial["islands"][district], initial)
focused = state(district)
capture("android-district.png")
targets = [t for t in focused["targets"] if t["district"] == district]
targets.sort(key=lambda t: "settings" not in t["component"].lower())
launched = None
for target in targets:
    tap(target["point"], focused)
    time.sleep(2)
    activities = run("shell", "dumpsys", "activity", "activities")
    resumed = "\n".join(line for line in activities.splitlines() if "topResumedActivity" in line or "mResumedActivity" in line)
    if target["component"].split("/")[0] in resumed:
        launched = target["component"]
        break
assert launched, "No app launched through a 3D building tap"
run("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(2)
activities = run("shell", "dumpsys", "activity", "activities")
assert any("com.atlas.launcher" in line for line in activities.splitlines() if "topResumedActivity" in line or "mResumedActivity" in line)
logs = run("logcat", "-d")
assert "FATAL EXCEPTION" not in logs and "SCRIPT ERROR" not in logs, "Runtime errors found"
(out / "logcat.txt").write_text(logs, encoding="utf8")
report = {"result":"PASS", "apps":initial["apps"], "buildings":initial["buildings"], "home":True, "cameraDistrict":district, "launchedByBuilding":launched, "returnedHome":True}
(out / "smoke.json").write_text(json.dumps(report, indent=2)+"\n")
print(json.dumps(report))
