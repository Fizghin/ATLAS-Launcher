"""Original ATLAS score and filtered surf; deterministic, no third-party samples."""
from pathlib import Path
import math
import random
import struct
import wave

ROOT = Path(__file__).resolve().parents[1] / "godot/assets/audio"
ROOT.mkdir(parents=True, exist_ok=True)
RATE = 22050

def write(name, seconds, sample):
    with wave.open(str(ROOT / name), "wb") as out:
        out.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        out.writeframes(b"".join(struct.pack("<h", max(-32767, min(32767, int(sample(i / RATE) * 32767)))) for i in range(int(seconds * RATE))))

notes = [196, 246.9417, 293.6648, 391.9954, 329.6276, 293.6648, 246.9417, 220]
def score(t):
    beat = int(t / 1.5)
    phase = t % 1.5
    f = notes[beat % 8]
    envelope = (1 - math.exp(-phase * 16)) * math.exp(-phase * 2.5)
    melody = sum(math.sin(math.tau * f * harmonic * phase) / harmonic**2 for harmonic in [1, 2, 3, 4])
    drone = math.sin(math.tau * 98 * t) * .035 * math.sin(math.pi * t / 24)**2
    return melody * envelope * .24 + drone

rng = random.Random(24)
filtered = 0.0
def surf(t):
    global filtered
    filtered = filtered * .965 + rng.uniform(-1, 1) * .035
    fade = min(1, t / .5, (24 - t) / .5)
    return filtered * (1.4 + .7 * math.sin(math.tau * t / 8)) * fade

write("harbor.wav", 24, score)
write("ocean.wav", 24, surf)
write("bell.wav", .18, lambda t: math.sin(math.tau * 880 * t) * math.exp(-t * 25) * .2)
