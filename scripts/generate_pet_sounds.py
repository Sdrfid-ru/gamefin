"""Original quiet, stylized pet chirps (not recordings of real foxes). Stdlib only."""
import math
from pathlib import Path
import struct
import wave

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
RATE = 24000

def render(name, duration, start, end):
    phase = 0.0
    samples = []
    for i in range(int(RATE * duration)):
        t = i / RATE
        u = t / duration
        hz = start + (end - start) * math.sin(math.pi * u / 2) + 12 * math.sin(2 * math.pi * 5 * t)
        phase += 2 * math.pi * hz / RATE
        envelope = math.sin(math.pi * u) ** 2
        # Gentle rounded timbre, ample headroom, smooth zero crossings at both ends.
        value = .24 * envelope * (math.sin(phase) + .2 * math.sin(2 * phase))
        samples.append(struct.pack('<h', round(32767 * value)))
    with wave.open(str(ROOT / name), 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b''.join(samples))

render('sfx_pet.wav', .42, 350, 570)
render('sfx_rest.wav', .65, 310, 190)
