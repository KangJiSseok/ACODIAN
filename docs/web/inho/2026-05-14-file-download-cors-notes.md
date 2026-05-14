# 파일 목록 조회와 다운로드 CORS 정리

## 배경

파일 탭에서 백엔드 파일 목록 조회 API와 다운로드 버튼을 연결하면서 `storedPath`, 브라우저 다운로드 동작, CORS 정책이 섞여 헷갈릴 수 있는 지점이 있었다.

현재 프론트 작업은 백엔드 코드를 수정하지 않고, 이미 제공되는 파일 API 계약을 사용하는 방향으로 정리했다.

## 현재 사용하는 API 계약

파일 목록은 다음 API로 조회한다.

```text
GET /api/files
```

프론트는 페이지 조건과 필터를 query parameter로 보낸다.

```text
page=1
pageSize=10
fileType=PDF
period=30
```

파일 형식 필터 옵션은 하드코딩하지 않고 다음 API로 조회한다.

```text
GET /api/files/types
```

응답은 다음 형태를 기대한다.

```json
[
  {
    "fileType": "PDF",
    "extension": "pdf"
  }
]
```

## storedPath의 의미

파일 목록 응답의 `storedPath`는 현재 다운로드에 사용할 수 있는 완성 URL로 내려온다.

예시는 다음과 같다.

```text
https://d1sif143wcm5pd.cloudfront.net/staging/worklog/2026/05/12/report.pdf
```

따라서 프론트는 별도로 CDN base URL을 붙이지 않고, 응답의 `storedPath`를 그대로 사용한다.

## 왜 링크로 열면 새 창이 뜨는가

처음에는 다음 방식으로 다운로드를 시도했다.

```ts
const link = document.createElement("a");
link.href = file.storedPath;
link.download = file.originalName;
link.click();
```

하지만 `storedPath`가 현재 프론트 origin과 다른 CDN URL이면 Chrome이 `download` 속성을 무시할 수 있다.

이 경우 브라우저는 파일을 저장하지 않고 이미지나 PDF를 새 탭에서 열 수 있다.

즉, `storedPath`로 열 수 있다는 것과 파일 저장이 강제된다는 것은 다르다.

## Blob 다운로드 방식

새 탭 열림을 피하려고 프론트는 파일 URL을 직접 열지 않고, 파일 바이트를 읽어 Blob으로 만든 뒤 다운로드한다.

흐름은 다음과 같다.

```text
storedPath URL
-> fetch(storedPath)
-> response.blob()
-> URL.createObjectURL(blob)
-> <a download="originalName"> 클릭
-> URL.revokeObjectURL(objectUrl)
```

`Blob`은 브라우저가 메모리에서 다루는 파일 데이터 덩어리다.

`URL.createObjectURL(blob)`을 호출하면 현재 페이지에서 사용할 수 있는 임시 `blob:` URL이 만들어지고, 이 URL은 `<a download>`로 파일 저장을 트리거하기 좋다.

## CORS가 막히는 이유

Blob 방식은 `fetch(storedPath)`가 필요하다.

브라우저에서 JS가 다른 origin의 응답 본문을 읽으려면 CDN 또는 S3 응답에 CORS 허용 헤더가 있어야 한다.

로컬에서 확인한 에러는 다음 의미다.

```text
Access to fetch at 'https://d1sif143wcm5pd.cloudfront.net/...'
from origin 'http://localhost:3000'
has been blocked by CORS policy
```

이는 백엔드가 파일 URL을 내려줬는지와 별개로, CloudFront가 `http://localhost:3000` origin의 JS fetch를 허용하지 않았다는 뜻이다.

목록 권한과 다운로드 권한이 같아도 CORS는 브라우저와 CDN 사이의 별도 보안 정책이다.

## 로컬과 배포의 차이

CORS 허용 origin은 브라우저 주소창의 프론트 origin 기준이다.

로컬에서 테스트하려면 다음 origin이 필요하다.

```text
http://localhost:3000
```

배포 환경에서 테스트하려면 실제 프론트 접속 origin이 필요하다.

예시는 다음과 같다.

```text
https://k14s209.p.ssafy.io
```

CloudFront 또는 S3 CORS에 배포 origin만 허용되어 있다면 배포에서는 다운로드가 되고, 로컬에서는 같은 코드가 막힐 수 있다.

반대로 로컬 origin만 허용하면 배포에서는 막힐 수 있다.

## 해결 방향

현재 프론트 구현을 그대로 쓰려면 CloudFront 또는 S3 CORS에 프론트 origin들을 허용해야 한다.

최소 기준은 다음과 같다.

```text
AllowedOrigins:
- http://localhost:3000
- https://실제-프론트-배포-origin

AllowedMethods:
- GET
- HEAD

AllowedHeaders:
- *
```

응답 헤더를 프론트에서 읽어야 한다면 다음 expose header도 고려한다.

```text
ExposeHeaders:
- Content-Disposition
- Content-Type
- Content-Length
```

CDN CORS를 열지 않는 방향이라면 백엔드에서 별도 다운로드 API를 제공해야 한다.

예시는 다음과 같다.

```text
GET /api/files/{id}/download
```

이 방식에서는 백엔드가 권한 확인 후 파일을 스트리밍하거나 presigned URL을 발급한다.

다만 이번 프론트 작업에서는 백엔드 코드를 수정하지 않고, 현재 제공되는 `storedPath` URL과 `/files/types` API만 사용한다.

## 기억할 점

- `/files`는 파일 목록 조회 API다.
- `/files/types`는 파일 형식 필터 옵션 API다.
- `storedPath`가 완성 URL이면 프론트에서 base URL을 다시 붙이지 않는다.
- `<a href>`로 외부 파일을 열 수 있어도 다운로드가 강제되지는 않는다.
- `fetch -> Blob -> object URL` 방식은 다운로드 강제에 유리하지만 CDN/S3 CORS가 필요하다.
- 로컬과 배포는 origin이 다르므로 CORS 허용 목록도 각각 확인해야 한다.
