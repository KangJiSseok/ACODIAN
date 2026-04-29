# 로그인 배경 영상 WebM 최적화 기록

## 배경

로그인 랜딩 화면의 배경 영상은 `web/public/videos/login-bg.mp4`만 제공하고 있었다.

Chrome DevTools Lighthouse 기준으로 첫 화면의 미디어 리소스는 네트워크 비용과 LCP 체감에 영향을 줄 수 있으므로, 압축 효율이 높은 AV1 WebM을 우선 제공하고 VP9 WebM과 MP4를 fallback으로 유지하기로 했다.

## 적용 내용

- 원본 MP4: `web/public/videos/login-bg.mp4`
- 추가 AV1 WebM: `web/public/videos/login-bg.av1.webm`
- 추가 VP9 WebM: `web/public/videos/login-bg.webm`
- 추가 WebP poster: `web/public/videos/login-bg-poster.webp`
- 적용 위치: `web/src/app/(public)/login/page.tsx`

```tsx
poster="/videos/login-bg-poster.webp"

<source src="/videos/login-bg.av1.webm" type='video/webm; codecs="av01"' />
<source src="/videos/login-bg.webm" type="video/webm" />
<source src="/videos/login-bg.mp4" type="video/mp4" />
```

브라우저는 위에서부터 지원 가능한 source를 선택한다.

- AV1 WebM 지원 브라우저: `login-bg.av1.webm` 사용
- AV1 미지원, VP9 WebM 지원 브라우저: `login-bg.webm` 사용
- WebM 미지원 브라우저: `login-bg.mp4` 사용

## 변환 명령

AV1 WebM:

```powershell
ffmpeg -y -i web\public\videos\login-bg.mp4 -c:v libaom-av1 -crf 38 -b:v 0 -cpu-used 6 -an web\public\videos\login-bg.av1.webm
```

VP9 WebM:

```powershell
ffmpeg -y -i web\public\videos\login-bg.mp4 -c:v libvpx-vp9 -b:v 0 -crf 32 -an web\public\videos\login-bg.webm
```

WebP poster:

```powershell
ffmpeg -y -i web\public\videos\login-bg-poster.jpg -c:v libwebp -quality 78 web\public\videos\login-bg-poster.webp
```

옵션 기준:

- `libaom-av1`: 압축 효율이 높은 AV1 코덱
- `libvpx-vp9`: WebM에서 범용적으로 쓰기 좋은 VP9 코덱
- `libwebp`: JPG/PNG poster를 WebP 이미지로 변환
- `-crf`: 품질/용량 균형값, 낮을수록 고화질/큰 용량
- `-quality 78`: 배경 poster 기준의 WebP 품질값
- `-cpu-used 6`: AV1 인코딩 시간을 줄이기 위한 속도 옵션
- `-an`: 배경 영상에 필요 없는 오디오 제거
- `-y`: 기존 출력 파일이 있으면 덮어쓰기

## 결과

| 파일 | 용량 |
|---|---:|
| `login-bg.mp4` | 약 3.56 MB |
| `login-bg.webm` | 약 2.18 MB |
| `login-bg.av1.webm` | 약 0.67 MB |
| `login-bg-poster.jpg` | 약 2.05 MB |
| `login-bg-poster.webp` | 약 0.19 MB |

AV1 WebM 기준으로 MP4 대비 약 2.89 MB를 줄였다.
WebP poster 기준으로 JPG 대비 약 1.86 MB를 줄였다.

## 다음 작업 기준

- 랜딩/히어로 배경 영상은 가능하면 `AV1 WebM + VP9 WebM + MP4 fallback` 구조로 둔다.
- 자동 재생 배경 영상은 `autoPlay`, `muted`, `loop`, `playsInline`, `poster`를 함께 확인한다.
- 영상이 첫 화면에 노출되면 poster 이미지를 WebP로 제공하고 용량을 함께 점검한다.
- AV1 WebM을 추가해도 LCP가 그대로라면 영상보다 poster 이미지, 폰트, 첫 화면 텍스트, JS hydration, 네트워크 우선순위를 먼저 의심한다.
- 변경 후에는 Lighthouse 또는 Playwright로 desktop/mobile 화면에서 영상 노출과 레이아웃 흔들림을 확인한다.
