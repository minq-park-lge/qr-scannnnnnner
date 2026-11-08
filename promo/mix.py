import json, sys, numpy as np, soundfile as sf
from scipy import signal
P = sys.argv[1]
T = json.load(open(f"{P}/timeline.json")); END = T["end"]
SR = 48000; N = int(END * SR) + SR
rng = np.random.default_rng(42)
music = np.zeros((N, 2)); fx = np.zeros((N, 2))
tt = lambda d: np.arange(int(d * SR)) / SR

def put(buf, t0, x, gain=1.0, pan=0.0):
    i = int(t0 * SR); x = np.asarray(x)
    if x.ndim == 1: x = np.stack([x * (1 - max(0, pan)), x * (1 + min(0, pan))], 1)
    j = min(N, i + len(x)); buf[i:j] += x[: j - i] * gain

def lp(x, f, o=2): b, a = signal.butter(o, f / (SR / 2)); return signal.lfilter(b, a, x)
def hp(x, f, o=2): b, a = signal.butter(o, f / (SR / 2), "high"); return signal.lfilter(b, a, x)
def bp(x, lo, hi): b, a = signal.butter(2, [lo / (SR / 2), hi / (SR / 2)], "band"); return signal.lfilter(b, a, x)
def saw(f, t, det=0): return 2 * ((t * f * (1 + det)) % 1) - 1
def env(t, a, d):  # attack, exp decay
    return np.minimum(1, t / max(a, 1e-4)) * np.exp(-np.maximum(0, t - a) / d)

def drone(t0, t1, f=55.0, amp=.25):
    t = tt(t1 - t0)
    x = sum(saw(f * m, t, d) for m, d in [(1, 0), (1, .004), (1.5, -.003), (2, .002)]) / 4
    x = lp(x, 260, 4) + .5 * np.sin(2 * np.pi * f / 2 * t)
    e = np.minimum(1, t / 2.5) * np.minimum(1, (t1 - t0 - t) / 1.0) * (1 + .25 * np.sin(2 * np.pi * .23 * t))
    put(music, t0, x * e * amp)

def braam(t0, dur=3.2, amp=.9, f=55.0):
    t = tt(dur)
    x = sum(saw(f * m, t, d) for m, d in [(1, 0), (1, .006), (1, -.005), (2, .003), (2.38, 0), (3, -.002)])
    cut = 300 + 2200 * np.exp(-t / .5)
    out = np.zeros_like(x); step = 2400
    for i in range(0, len(x), step):  # time-varying lowpass
        b, a = signal.butter(2, cut[i] / (SR / 2)); out[i:i + step] = signal.lfilter(b, a, x[i:i + step])
    out = np.tanh(out * 2.2) * env(t, .06, 1.1)
    sub = np.sin(2 * np.pi * 36 * t) * env(t, .02, 1.4)
    put(music, t0, (out * .55 + sub * .9) * amp)

def hit(t0, amp=1.0):
    t = tt(2.2)
    fr = 38 + 90 * np.exp(-t / .06)
    kick = np.sin(2 * np.pi * np.cumsum(fr) / SR) * env(t, .003, .45)
    nz = lp(rng.standard_normal(len(t)), 900) * env(t, .002, .12)
    put(music, t0, (kick * 1.0 + nz * .5) * amp)

def whoosh(t0, dur=.9, amp=.35, up=True):
    t = tt(dur); n = rng.standard_normal(len(t))
    shape = np.sin(np.pi * t / dur) ** 2
    lo = 300 if up else 2500
    x = bp(n, 400, 5000) * shape
    pan = np.linspace(-.7, .7, len(t)) if up else np.linspace(.7, -.7, len(t))
    put(fx, t0, np.stack([x * (1 - pan) / 2, x * (1 + pan) / 2], 1) * amp * 2)

def riser(t0, t1, amp=.35):
    t = tt(t1 - t0); p = t / (t1 - t0)
    f = 180 * (8 ** p)
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * p ** 2
    nz = hp(rng.standard_normal(len(t)), 1500) * p ** 3
    put(fx, t0, (tone * .5 + nz * .35) * amp)

def blip(t0, f=1900, amp=.35):
    t = tt(.12)
    put(fx, t0, (np.sin(2 * np.pi * f * t) * env(t, .002, .03) + .3 * np.sin(2 * np.pi * f * 1.5 * t) * env(t, .002, .02)) * amp)

def glitch(t0, t1, amp=.25):
    t = t0
    while t < t1:
        d = rng.uniform(.03, .12); s = tt(d)
        x = np.sign(np.sin(2 * np.pi * rng.uniform(80, 900) * s)) * (rng.random() < .5) + rng.standard_normal(len(s)) * .5
        x = np.round(x * 4) / 4  # bitcrush
        put(fx, t, x * amp * env(s, .002, d / 2), pan=rng.uniform(-.6, .6))
        t += d + rng.uniform(.05, .35)

def pulse_bed(t0, t1, bpm=120, amp=.5):
    beat = 60 / bpm; k = 0; t = t0
    bass_notes = [55, 55, 65.41, 49.0]
    while t < t1 - .05:
        tb = tt(beat)
        kick_f = 45 + 70 * np.exp(-tb / .04)
        kick = np.sin(2 * np.pi * np.cumsum(kick_f) / SR) * env(tb, .002, .18)
        put(music, t, kick * amp)
        th = tt(.06); hat = hp(rng.standard_normal(len(th)), 7000) * env(th, .001, .015)
        put(music, t + beat / 2, hat * amp * .35, pan=.3)
        for e in range(2):
            ts = tt(beat / 2); f = bass_notes[(k // 4) % 4]
            b = lp(saw(f, ts) + saw(f, ts, .005), 380) * env(ts, .005, .12)
            put(music, t + e * beat / 2, b * amp * .55)
        k += 1; t += beat

# ---- score ----
drone(0.0, T["winter"] - .4, 55, .22)
for k in range(8):  # heartbeat in B/C
    hit(T["anywhere"] + .2 + k * .78, .35)
glitch(T["never"] - .2, T["winter"] - .5, .22)
braam(T["winter"] - .45, 3.0, .9, 55)
drone(T["winter"], T["title"] - .2, 41.2, .18)
riser(T["presents"] - .3, T["title"] - .27, .45)
braam(T["title"] - .27, 3.6, 1.0, 49)
hit(T["title"] - .27, 1.0)
whoosh(T["lock"] - .45, .8, .35)
blip(T["lock"] + .45, 1900, .4)
pulse_bed(T["lock"] + .45, T["noads"] - .35, 120, .42)
whoosh(T["preview"] - .45, .8, .3)
whoosh(T["fakes"] - .45, .8, .3, up=False)
glitch(T["fakes"] - .2, T["fakes"] + .3, .2)
for i in range(5): blip(T["rightapp"] - .2 + i * .6, 1300 + 120 * i, .22)
for i, d in enumerate([.02, .78, 1.62]): hit(T["noads"] + d - .02, .85 + .1 * i)
braam(T["tagline"] - .3, 4.2, .85, 55); hit(T["tagline"] - .3, 1.0)
drone(T["tagline"], END + .5, 55, .16)

# ---- reverb on music + fx ----
ir_t = tt(2.2); ir = rng.standard_normal(len(ir_t)) * np.exp(-ir_t / .55); ir = lp(ir, 5000); ir /= np.abs(ir).sum() / 6
bus = music + fx * .9
wet = np.stack([signal.fftconvolve(bus[:, c], ir)[:N] for c in range(2)], 1)
bus = bus + wet * .22

# ---- VO + ducking ----
vo = np.zeros(N)
meta = {m["key"]: m for m in json.load(open(f"{P}/vo/vo.json"))}
for key in meta:
    x, sr = sf.read(f"{P}/vo/{key}.wav"); assert sr == SR
    i = int(T[key] * SR); j = min(N, i + len(x)); vo[i:j] += x[: j - i]
venv = np.convolve(np.abs(vo), np.ones(2400) / 2400, "same")
venv = np.minimum(1, venv / (np.percentile(venv[venv > 1e-4], 90) + 1e-9))
venv = signal.lfilter([1 - .9995], [1, -.9995], venv)  # 부드럽게
duck = 1 - .5 * np.clip(venv * 3, 0, 1)
bus *= duck[:, None]
bus /= np.max(np.abs(bus)) + 1e-9
vo /= np.max(np.abs(vo)) + 1e-9
mix = bus * .55 + np.stack([vo, vo], 1) * .9
mix = mix[: int((END + .3) * SR)]
fade = int(.6 * SR); mix[-fade:] *= np.linspace(1, 0, fade)[:, None]
mix = np.tanh(mix * 1.2) / np.tanh(1.2)
sf.write(f"{P}/mix.wav", mix.astype(np.float32), SR)
print("mix", round(len(mix) / SR, 2), "s")
