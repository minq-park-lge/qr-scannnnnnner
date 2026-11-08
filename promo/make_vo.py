# usage: python make_vo.py <kokoro_dir> <out_dir> <script.json>
# script.json: {"voice": {...}, "lines": [[key, text, speed, gap_before], ...]}
import json, sys, numpy as np, soundfile as sf, subprocess
from kokoro_onnx import Kokoro
tts_dir, out, script = sys.argv[1], sys.argv[2], json.load(open(sys.argv[3]))
k = Kokoro(f"{tts_dir}/kokoro-v1.0.onnx", f"{tts_dir}/voices-v1.0.bin")
voice = sum(w * k.get_voice_style(v) for v, w in script["voice"].items())
sr_out = 48000
T, t = {}, 0.35
for key, text, speed, gap in script["lines"]:
    a, sr = k.create(text, voice=voice, speed=speed, lang=script.get("lang", "en-us"))
    idx = np.where(np.abs(a) > 0.01)[0]
    a = a[max(0, idx[0] - 240): idx[-1] + 1200]
    raw = f"{out}/{key}_raw.wav"; sf.write(raw, a, sr)
    ratio = 2 ** (-2.5 / 12)
    af = (f"asetrate={sr}*{ratio:.5f},aresample={sr_out},atempo={1/ratio:.5f},"
          "highpass=f=55,lowshelf=f=140:g=6,equalizer=f=3200:t=q:w=1.2:g=2,"
          "acompressor=threshold=-20dB:ratio=4:attack=5:release=120:makeup=4,"
          "aecho=0.8:0.5:50|100:0.15|0.08,alimiter=limit=0.9")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", raw, "-af", af, "-ac", "1", f"{out}/{key}.wav"], check=True)
    d = sf.info(f"{out}/{key}.wav").duration
    t += gap; T[key] = round(t, 3); t += d - 0.12   # 리버브 꼬리와 살짝 겹치게
    print(f"{key:9s} {T[key]:6.2f}  +{d:.2f}")
T["end"] = round(t + 1.6, 3)
json.dump(T, open(f"{out}/timeline.json", "w"), indent=1)
print("END", T["end"])
