# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 빌드 및 개발 명령어

```bash
./gradlew build          # 빌드 + 테스트 + ktlint 검사
./gradlew bootRun        # 개발 서버 실행 (포트 8080, 아래 환경변수 필요)
./gradlew test           # 전체 테스트 실행
./gradlew ktlintCheck    # 린트 검사만 실행
./gradlew ktlintFormat   # 린트 자동 수정
```

**필수 환경변수**: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, `KAKAO_API_KEY`

**사전 요구사항**: Java 21, MongoDB `localhost:27017` 실행 중 (데이터베이스: `bookview`)

## 아키텍처

Spring Boot 4.0 + Kotlin 서버 렌더링 앱. Thymeleaf 템플릿 + Tailwind CSS. MongoDB 사용. JWT 쿠키 기반 무상태 인증 (세션 없음).

### 인증 흐름

Google OAuth2 로그인 → `CustomOAuth2UserService`에서 유저 생성/갱신 → `OAuth2SuccessHandler`에서 JWT 쿠키 발급 → `JwtAuthenticationFilter`가 매 요청마다 JWT를 읽어 `SecurityContextHolder`에 `Map<String, String>` principal 설정 (`sub`=googleId, `email`, `nickname`).

컨트롤러에서 현재 유저의 googleId 추출: `(principal as Map<*, *>)["sub"].toString()`

### 보안 규칙 (SecurityConfig)

- 페이지 라우트(`/`, `/r/**`, `/u/**` 등) 및 정적 자산: `permitAll`
- 특정 API 엔드포인트(`/api/reviews`, `/api/users`, `/api/external/**`): `permitAll`
- 나머지 `/api/**` 엔드포인트: `authenticated`
- `/api/**` 하위에 새 엔드포인트를 추가하면 자동으로 인증 보호됨

### 데이터 레이어 패턴

- **도메인 모델**: `@Document`에 `common/Constants.kt`의 `MongoCollections` 상수 사용
- **자동 증가 ID**: `SequenceService`가 `sequences` 컬렉션에 `MongoTemplate.findAndModify` + `$inc`로 `reviewNo`, `userNo` 생성 (URL용 순차 번호)
- **MongoConfig**: `DefaultMongoTypeMapper(null)`로 문서에서 `_class` 필드 제거
- **소프트 삭제**: 엔티티에 `status: Status` 필드(`ACTIVE`/`DELETED`) 사용; 조회 시 항상 `Status.ACTIVE`로 필터링

### 컨트롤러 패턴

- `ViewController` (페이지 렌더링): `ControllerExtensions.kt`의 `addUserToModel()`로 principal에서 유저를 추출하여 Thymeleaf 모델에 추가. `isNicknameSet` 미설정 시 항상 `/setup-nickname`으로 리다이렉트.
- `/api/` 하위 REST 컨트롤러: `ResponseEntity` 반환, 인증 필요 엔드포인트는 `@AuthenticationPrincipal principal: Any` 사용.
- XSS 방지: 리치 텍스트(content)는 `HtmlSanitizer.sanitize()`, 일반 텍스트(title, quote)는 `HtmlSanitizer.toPlainText()` 사용.

### 프론트엔드

- Thymeleaf 템플릿: `resources/templates/`, 공유 레이아웃은 `layout.html` (header, footer, 모바일 메뉴 fragment)
- JavaScript: `resources/static/js/`, 공통 유틸리티는 `common.js` (`API` fetch 헬퍼, `Modal`, `Alert`, `Validator`)
- 스타일링: Tailwind CSS CDN (`<script src="https://cdn.tailwindcss.com">`)

## 컨벤션

- **커밋 메시지**: Gitmoji + Jira 티켓: `:sparkles: BKVW-8 : 설명`
- **브랜치 네이밍**: `feature/BKVW-{번호}`
- **Ktlint**: v1.4.1, 빌드 시 강제 적용 (무시 불가). 커밋 전 반드시 `./gradlew ktlintFormat` 실행.
- **언어**: 사용자 대면 문자열 및 주석은 한국어, 코드 식별자는 영어.
- **커밋 작성자**: `Co-Authored-By` 줄 포함하지 않음. 작성자는 git config 사용자만.
