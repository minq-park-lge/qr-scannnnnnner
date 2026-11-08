# SnapQR 광고 쇼츠 (9:16)

| 파일 | 내용 |
|---|---|
| `SnapQR_Ad_EN.mp4` | 영어 내레이션 + 영어 화면, 28.9초 |
| `SnapQR_Ad_KO.mp4` | 영어 예고편 내레이션 + 한국어 자막/앱 화면, 28.9초 |

1080×1920, 30fps, H.264 + AAC, -14 LUFS (YouTube Shorts / Reels / TikTok)

## 구성 (차별화 포인트)
| 내레이션 (EN) | 한국어 화면 | 포인트 |
|---|---|---|
| In a world… where every surface hides a code. | 세상 모든 곳에 / 코드가 숨어 있다 | 훅 |
| One scan can take you anywhere. Even where you never meant to go. | 한 번의 스캔이 어디로든 데려간다 / 원치 않던 곳까지도 | 큐싱 문제 제기 |
| This winter. Snap Lab presents… SnapQR. | 이번 겨울 | 타이틀 |
| It locks on. Instantly. | 비추는 순간 바로 잡는다 | 즉시 인식 (강제 줌 없음) |
| Shows where a link really goes, before you tap. | 누르기 전에 먼저 보여준다 | 프리뷰 / 단축 URL 최종 목적지 |
| Catches the fakes, right on your phone. | 가짜는 걸러낸다 · 폰 안에서 | 기기 내 피싱 검사 |
| And opens the right app. Every time. | 언제나 딱 맞는 앱으로 | 의도에 맞는 앱 연결 |
| No ads. No tracking. No waiting. | 광고 없이. 추적 없이. 기다림 없이. | 광고/추적 없음, 속도 |
| SnapQR. Scan smarter. Coming soon on Google Play. | 더 똑똑한 스캔. / Google Play 출시 예정 | 엔드 카드 |

## 파이프라인
- `script_en.json` → `make_vo.py` (Kokoro TTS, am_onyx 60% + am_michael 40%, 피치 -2.5반음 + 저음/컴프/리버브)
  → 문장별 wav + `timeline.json` (문장 길이에서 장면 타이밍 자동 계산)
- `ad.html` + `copy_<lang>.json` → `render.js` (Playwright 프레임 렌더)
- `mix.py` → BRAAM/히트/라이저/글리치/128bpm 펄스 합성 + 내레이션 덕킹
- `script_ko.json`: 한국어 더빙용 대본 (한국어 TTS 확보 시 사용)

```bash
./fetch_fonts.sh                                   # 한국어 폰트
python make_vo.py <kokoro_dir> vo_en script_en.json  # kokoro-v1.0.onnx / voices-v1.0.bin (kokoro-onnx GitHub releases)
node render.js "$PWD" frames_en 30 vo_en/timeline.json copy_en.json
node render.js "$PWD" frames_ko 30 vo_en/timeline.json copy_ko.json
python mix.py "$PWD" vo_en mix_en.wav
ffmpeg -framerate 30 -i frames_en/f_%05d.jpg -i mix_en.wav -c:v libx264 -crf 19 -pix_fmt yuv420p \
  -af loudnorm=I=-14:TP=-1.5:LRA=7 -c:a aac -b:a 192k -movflags +faststart -shortest SnapQR_Ad_EN.mp4
```

화면 속 도메인은 전부 예약 TLD(`.example`)이며 실존 브랜드 로고/상표는 사용하지 않음.
폰트: Bebas Neue, Inter, Black Han Sans, Noto Sans KR (SIL OFL, @fontsource).
