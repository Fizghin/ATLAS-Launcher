"""Generate metadata from the built APK, never from an assumed release tag."""
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

apk, aapt, tag = sys.argv[1:]
info = subprocess.check_output([aapt, "dump", "badging", apk], text=True)
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", info)
assert package and package[1] == "com.atlas.launcher", "Unexpected APK package"
assert tag == "v" + package[3], "Tag must match built versionName"
data = {
    "versionCode": int(package[2]), "versionName": package[3],
    "url": f"https://github.com/Fizghin/ATLAS-Launcher/releases/download/{tag}/ATLAS-{package[3]}.apk",
    "sha256": hashlib.sha256(Path(apk).read_bytes()).hexdigest(),
}
Path("atlas-update.json").write_text(json.dumps(data, indent=2) + "\n")
