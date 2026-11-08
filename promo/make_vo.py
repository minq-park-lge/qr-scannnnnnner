import json, sys, numpy as np, soundfile as sf, subprocess, os
from kokoro_onnx import Kokoro
tts_dir, out = sys.argv[1], sys.argv[2]
k = Kokoro(f"{tts_dir}/kokoro-v1.0.onnx", f"{tts_dir}/voices-v1.0.bin")
# 깊이(onyx) + 자연스러움(michael) 블렌딩
voice = 0.6 * k.get_voice_style("am_onyx") + 0.4 * k.get_voice_style("am_michael")
LINES = [
  ("world",    "In a world...",                                     0.82),
  ("surface",  "where every surface... hides a code.",              0.82),
  ("anywhere", "One scan... can take you anywhere.",                0.84),
  ("never",    "Even... where you never meant to go.",              0.82),
  ("winter",   "This winter...",                                    0.80),
  ("presents", "Snap Lab presents...",                              0.84),
  ("title",    "Snap Q R.",                                         0.80),
  ("lock",     "It locks on... instantly.",                         0.90),
  ("preview",  "Shows you where a link really goes... before you tap.", 0.90),
  ("fakes",    "Catches the fakes... right on your phone.",         0.90),
  ("rightapp", "And opens the right app. Every time.",              0.92),
  ("noads",    "No ads. No tracking. No waiting.",                  0.86),
  ("tagline",  "Snap Q R. Scan smarter.",                           0.84),
  ("soon",     "Coming soon... on Google Play.",                    0.88),
]
meta = []
for key, text, speed in LINES:
    a, sr = k.create(text, voice=voice, speed=speed, lang="en-us")
    # 앞뒤 무음 트림
    idx = np.where(np.abs(a) > 0.01)[0]
    a = a[max(0, idx[0]-480): idx[-1]+2400]
    raw = f"{out}/{key}_raw.wav"; sf.write(raw, a, sr)
    # 예고편 톤: 피치 -2.5 반음(길이 유지), 저음 강조, 압축, 약간의 홀 리버브
    ratio = 2 ** (-2.5/12)
    af = (f"asetrate={sr}*{ratio:.5f},aresample=48000,atempo={1/ratio:.5f},"
          "highpass=f=55,lowshelf=f=140:g=6,equalizer=f=3200:t=q:w=1.2:g=2,"
          "acompressor=threshold=-20dB:ratio=4:attack=5:release=120:makeup=4,"
          "aecho=0.8:0.55:60|120:0.18|0.10,alimiter=limit=0.9")
    fx = f"{out}/{key}.wav"
    subprocess.run(["ffmpeg","-y","-loglevel","error","-i",raw,"-af",af,"-ac","1",fx], check=True)
    d = sf.info(fx).duration
    meta.append({"key": key, "text": text, "dur": round(d, 3)})
    print(key, round(d,2))
json.dump(meta, open(f"{out}/vo.json","w"), indent=1)
