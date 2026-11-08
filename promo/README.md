# SnapQR 광고 쇼츠 (EN, 9:16)

`SnapQR_Ad_EN.mp4` — 39.6초, 1080×1920, 30fps, H.264 + AAC, -14 LUFS (YouTube Shorts / Reels / TikTok)

## 구성 (차별화 포인트)
| 시간 | 내레이션 | 포인트 |
|---|---|---|
| 0–5s | In a world… where every surface hides a code. | 훅 |
| 5–12s | One scan can take you anywhere. Even where you never meant to go. | 큐싱 문제 제기 |
| 12–18s | This winter… Snap Lab presents… SnapQR. | 타이틀 |
| 18–20s | It locks on… instantly. | 즉시 인식 (강제 줌 없음) |
| 20–24s | Shows you where a link really goes… before you tap. | 열기 전 프리뷰 / 단축 URL 최종 목적지 |
| 24–27s | Catches the fakes… right on your phone. | 기기 내 피싱 검사 |
| 27–30s | And opens the right app. Every time. | 의도에 맞는 앱 연결 |
| 30–33s | No ads. No tracking. No waiting. | 광고/추적 없음, 속도 |
| 33–40s | SnapQR. Scan smarter. Coming soon on Google Play. | 엔드 카드 |

## 다시 만들기
1. 내레이션: Kokoro TTS (am_onyx 60% + am_michael 40%) → 피치 -2.5반음, 저음 강조, 컴프레서, 리버브
   ```bash
   pip install kokoro-onnx soundfile scipy
   # kokoro-v1.0.onnx, voices-v1.0.bin: github.com/thewh1teagle/kokoro-onnx releases (model-files-v1.0)
   python make_vo.py <모델폴더> vo
   ```
2. 영상 프레임: `ad.html` 을 Playwright(Chromium)로 프레임 단위 렌더 — 타이밍은 `timeline.json`
   ```bash
   node render.js "$PWD" frames 30
   ```
3. 사운드: `mix.py` 가 BRAAM/히트/라이저/펄스 베드를 합성하고 내레이션 덕킹 후 믹스
   ```bash
   python mix.py "$PWD"
   ffmpeg -framerate 30 -i frames/f_%05d.jpg -i mix.wav -c:v libx264 -crf 19 -pix_fmt yuv420p \
     -af loudnorm=I=-14:TP=-1.5:LRA=7 -c:a aac -b:a 192k -movflags +faststart -shortest SnapQR_Ad_EN.mp4
   ```

화면 속 도메인은 전부 실제로 존재할 수 없는 `.example` 이며, 실존 브랜드 로고/상표는 사용하지 않음.
폰트: Bebas Neue, Inter (SIL OFL, @fontsource).
