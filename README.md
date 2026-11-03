# SnapQR

아이폰 / 안드로이드 기본 카메라처럼 **비추자마자 바로 잡히는** 빠릿한 QR 스캐너.

## 특징

- **즉시 인식**: CameraX + ML Kit *bundled* 모델 (첫 실행부터 모델 다운로드 없음)
  - QR 포맷만 탐색 → 프레임당 처리시간 최소화
  - 1080p 분석 해상도 → 작은/먼 코드도 **강제 줌 없이** 인식
  - `KEEP_ONLY_LATEST` 백프레셔, SurfaceView 프리뷰로 지연 최소화
- **노란 추적 마커**: 코드 꼭짓점을 따라가는 스프링 애니메이션 + 햅틱
- **URL 프리뷰 먼저**: 제목/설명/OG 이미지/파비콘, HTTPS 여부, 리다이렉트 최종 도메인 경고
- **의도에 맞게 실행**
  - 웹 링크 → 처리하는 네이티브 앱(App Links)이 있으면 앱으로, 없으면 Custom Tab
  - Play 스토어 링크 / `market://` → Play 스토어 앱
  - `intent://` → 대상 앱, 미설치 시 fallback URL → 스토어
  - 커스텀 스킴(`thinq://` 등) → 해당 앱
  - Wi-Fi → 시스템 네트워크 추가 다이얼로그, 전화/문자/메일/지도/연락처/일정
- 탭 포커스, 핀치 줌, 플래시, 갤러리 이미지 스캔, 빠른 설정 타일, 런처 숏컷

## 빌드

```bash
./gradlew assembleDebug
```

GitHub Actions(`.github/workflows/android.yml`)가 푸시마다 debug / release APK 를 빌드해
`SnapQR-apk` 아티팩트로 올립니다. (release 는 로컬 테스트용으로 debug 키 서명)

- minSdk 26 (Android 8.0), targetSdk 35
- Kotlin 2.0, Jetpack Compose, CameraX 1.4, ML Kit Barcode 17.3
