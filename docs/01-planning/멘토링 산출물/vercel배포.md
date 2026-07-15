네, 단계별로 안내드릴게요. 계정 접근이 필요한 부분이라 직접 해드릴 수는 없지만, 정확히 따라하시면 됩니다.

## Vercel 설정 단계

### 1. 새 프로젝트로 추가 (기존 Threadia 앱용 프로젝트와는 별개)

1. [vercel.com](https://vercel.com) 로그인 (GitHub 계정으로)
2. **Add New → Project**
3. GitHub 저장소 목록에서 `SEUIL/Threadia` 선택 → **Import**

### 2. ⚠️ 가장 중요한 설정 — Root Directory

Import 화면에서 **"Root Directory"** 항목을 찾아서:

```
docs-site
```

로 지정하세요. **이걸 안 하면 Vercel이 저장소 루트에서 빌드를 시도하다가 실패합니다.** (`Edit` 버튼 눌러서 폴더 선택하면 됩니다.)

### 3. Framework Preset

`docs-site`로 지정하면 Vercel이 자동으로 **Next.js**를 감지할 거예요. Build Command, Output Directory 등은 기본값 그대로 두시면 됩니다(`npm run build` 등).

### 4. Production Branch 결정

Vercel 프로젝트 설정에서 어떤 브랜치를 "Production"으로 볼지 정해야 해요. Threadia는 `dev`가 통합 브랜치고 `main`이 제출/안정본이니:

- 지금 단계(개발 중)면 **`dev`를 Production Branch로** 설정 추천 — PR이 dev로 머지될 때마다 자동 반영
- 나중에 `main`으로 승격할 때 다시 바꿔도 됩니다

설정 위치: 프로젝트 생성 시 또는 이후 **Settings → Git → Production Branch**

### 5. Environment Variables

지금은 **필요 없어요.** API key나 secret을 안 쓰는 구조라(검색도 로컬 FlexSearch 인덱스) 빈 채로 두시면 됩니다.

### 6. Deploy

위 설정하고 **Deploy** 누르면 끝이에요. 끝나면 `*.vercel.app` 도메인이 자동으로 생깁니다.

### 7. 배포 후 꼭 확인할 것

배포된 URL에서 직접:
- 홈페이지(`/`)와 문서 페이지(`/03-design/ERD` 등) 잘 뜨는지
- **`/api/search?q=erd`를 브라우저에서 직접 열어서 검색 API가 실제로 동작하는지** (이게 제가 미리 고쳤던 file tracing 이슈가 실전에서도 잘 먹혔는지 확인하는 부분이에요)

배포해보시고 혹시 에러 나면 로그(Vercel 대시보드의 Deployment → Functions 탭) 같이 봐드릴게요.