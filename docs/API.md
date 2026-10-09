# Campus Table - API 명세서

> 기준: `feature/order` (main + 인증·마일리지·주문·관리자 구현). 이 문서의 모든 요청/응답 예시는 실제 서버 호출 결과를 기준으로 작성했습니다.
> 상태 표기: ✅ 구현됨 / 🕓 예정(미구현)

## 1. 공통 규칙

| 항목 | 내용 |
|---|---|
| Base URL | `/api` |
| 필드명 | JSON `camelCase` (DB 컬럼은 `snake_case`) |
| 인증 | Spring Security **세션 로그인**. 로그인하면 `SESSION` 쿠키가 발급됨 (세션은 Redis 저장, 30분) |
| 프론트 요청 | 쿠키를 보내야 하므로 `credentials: "include"` (axios: `withCredentials: true`) 필요 |
| CORS | `CORS_ALLOWED_ORIGINS` 환경변수의 주소만 허용 (기본 `http://localhost:3000`) |
| 권한 | `USER` / `ADMIN`. `/api/auth/**`만 공개, `/api/admin/**`은 ADMIN, **나머지는 모두 로그인 필요** |
| 날짜·시간 | 한국 시간(KST) 기준, 시간대 표기 없음. `LocalDateTime` → `"2026-10-07T20:53:22.746737"` (소수점 자릿수는 가변), `LocalTime` → `"11:00:00"`, `LocalDate` → `"2026-10-07"` |
| 성공 응답 | 래핑 없이 **DTO를 그대로** 반환 |
| 금액 | 정수(원). 마일리지 1점 = 1원 |

### 에러 응답 (모든 API 공통)

```json
{
  "status": 404,
  "code": "STORE_NOT_FOUND",
  "message": "해당 가게를 찾을 수 없습니다."
}
```

- 프론트는 `code`로 분기하고, `message`는 사용자에게 그대로 보여줄 수 있는 문구입니다.
- `VALIDATION_FAILED`의 `message`는 **첫 번째로 실패한 입력 항목**의 안내 문구입니다 (예: `학번은 숫자 8자리여야 합니다.`).
- 존재하지 않는 경로(404), 허용되지 않은 메서드(405)는 `code: INVALID_REQUEST`로 내려옵니다.
- 경로 변수/쿼리 값의 타입이 틀리면(예: `/cafeterias/abc`, `?status=BAD`) `400 INVALID_REQUEST`.
- **`429`(요청 제한)** 응답에는 `Retry-After` 헤더(초)가 함께 내려가고, `message`에 "N분/N초 후"가 포함됩니다. 제한 규칙은 3장 "요청 제한"을 참고하세요.

### 에러 코드 목록

| code | HTTP | message |
|---|---|---|
| `INVALID_REQUEST` | 400 | 잘못된 요청입니다. |
| `VALIDATION_FAILED` | 400 | (입력 항목별 안내 문구) |
| `UNAUTHORIZED` | 401 | 로그인이 필요합니다. |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. |
| `INTERNAL_ERROR` | 500 | 서버 오류가 발생했습니다. |
| `INVALID_IMAGE` | 400 | JPEG, PNG, WebP 이미지 파일만 업로드할 수 있습니다. |
| `IMAGE_TOO_LARGE` | 413 | 이미지는 2MB 이하여야 합니다. |
| `STORAGE_NOT_CONFIGURED` | 503 | 이미지 저장소가 설정되지 않았습니다. |
| `STORAGE_FAILED` | 502 | 이미지 저장소 처리에 실패했습니다. |
| `CLOVA_NOT_CONFIGURED` | 503 | CLOVA API 키가 설정되지 않았습니다. |
| `ANALYSIS_FAILED` | 502 | 이미지 인원 분석에 실패했습니다. |
| `INVALID_CREDENTIALS` | 401 | 학번 또는 비밀번호가 올바르지 않습니다. |
| `DUPLICATE_STUDENT_NUMBER` | 409 | 이미 가입된 학번입니다. |
| `MAIL_SEND_FAILED` | 500 | 인증 메일 발송에 실패했습니다. |
| `EMAIL_NOT_VERIFIED` | 400 | 이메일 인증이 필요합니다. |
| `INVALID_VERIFICATION_CODE` | 400 | 인증 코드가 올바르지 않거나 만료되었습니다. |
| `USER_NOT_FOUND` | 404 | 해당 사용자를 찾을 수 없습니다. |
| `INVALID_CURRENT_PASSWORD` | 400 | 현재 비밀번호가 올바르지 않습니다. |
| `PASSWORD_UNCHANGED` | 400 | 새 비밀번호는 현재 비밀번호와 달라야 합니다. |
| `TOO_MANY_LOGIN_ATTEMPTS` | 429 | 로그인 시도가 너무 많습니다. N분 후 다시 시도해주세요. |
| `TOO_MANY_REQUESTS` | 429 | 요청이 너무 많습니다. (상황별 안내 문구: 인증 메일 요청 제한, 인증 코드 시도 제한) |
| `CAFETERIA_NOT_FOUND` | 404 | 해당 식당을 찾을 수 없습니다. |
| `STORE_NOT_FOUND` | 404 | 해당 가게를 찾을 수 없습니다. |
| `MENU_NOT_FOUND` | 404 | 해당 메뉴를 찾을 수 없습니다. |
| `MENU_UNAVAILABLE` | 400 | 주문할 수 없는 메뉴입니다. |
| `ORDER_NOT_FOUND` | 404 | 해당 주문을 찾을 수 없습니다. |
| `ACTIVE_ORDER_EXISTS` | 409 | 진행 중인 주문이 있습니다. |
| `INVALID_ORDER_STATUS` | 400 | 현재 주문 상태에서는 처리할 수 없습니다. |
| `INVALID_ARRIVAL_CODE` | 400 | 현장 번호가 올바르지 않습니다. |
| `INSUFFICIENT_MILEAGE` | 400 | 마일리지가 부족합니다. |
| `PAYMENT_FAILED` | 400 | 결제 승인에 실패했습니다. |
| `NOTIFICATION_NOT_FOUND` | 404 | 해당 알림을 찾을 수 없습니다. |

## 2. API 목록

| 분류 | Method | URL | 권한 | 설명 |
|---|---|---|---|---|
| Auth | POST | `/api/auth/email/send` | Public | 학번 학교 메일로 인증 코드 발송 |
| Auth | POST | `/api/auth/email/verify` | Public | 인증 코드 확인 |
| Auth | POST | `/api/auth/signup` | Public | 회원가입 |
| Auth | POST | `/api/auth/login` | Public | 로그인 |
| Auth | POST | `/api/auth/logout` | Public | 로그아웃 |
| Auth | POST | `/api/auth/password/reset/send` | Public | 비밀번호 재설정 코드 발송 |
| Auth | POST | `/api/auth/password/reset/confirm` | Public | 코드 확인 + 새 비밀번호 설정 |
| User | GET | `/api/users/me` | 로그인 | 내 정보 |
| User | PATCH | `/api/users/me/password` | 로그인 | 비밀번호 변경 |
| Cafeteria | GET | `/api/cafeterias` | 로그인 | 학식당 목록 |
| Cafeteria | GET | `/api/cafeterias/{cafeteriaId}` | 로그인 | 학식당 상세 |
| Cafeteria | GET | `/api/cafeterias/{cafeteriaId}/stores` | 로그인 | 학식당의 가게 목록 (필드 확장: 8-0 A) |
| Cafeteria | GET | `/api/cafeterias/{cafeteriaId}/status` | 로그인 | 혼잡도(이용/대기 인원) |
| Store | GET | `/api/stores/{storeId}` | 로그인 | 가게 상세 (필드 확장: 8-0 A) |
| Store | GET | `/api/stores/{storeId}/menus` | 로그인 | 가게 메뉴 목록 |
| Mileage | GET | `/api/mileage` | 로그인 | 마일리지 잔액 |
| Mileage | GET | `/api/mileage/transactions` | 로그인 | 마일리지 내역 |
| Mileage | POST | `/api/mileage/charge/confirm` | 로그인 | 결제 승인 후 충전 |
| Order | POST | `/api/orders` | 로그인 | 선주문 + 마일리지 결제 |
| Order | GET | `/api/orders/me` | 로그인 | 내 주문 목록 |
| Order | GET | `/api/orders/me/current` | 로그인 | 진행 중인 내 주문 1건 (8-0 F) |
| Order | GET | `/api/orders/{orderId}` | 로그인 | 내 주문 상세 |
| Order | POST | `/api/orders/{orderId}/arrival` | 로그인 | 현장 번호 도착 인증 |
| Order | POST | `/api/orders/{orderId}/cancel` | 로그인 | 주문 취소(도착 인증 전) |
| Notification | GET | `/api/notifications` | 로그인 | 내 알림 목록 (8-0 E) |
| Notification | GET | `/api/notifications/unread-count` | 로그인 | 읽지 않은 알림 수 (8-0 E) |
| Notification | PATCH | `/api/notifications/{notificationId}/read` | 로그인 | 알림 읽음 처리 (8-0 E) |
| Notification | PATCH | `/api/notifications/read-all` | 로그인 | 알림 모두 읽음 (8-0 E) |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/arrival-code` | ADMIN | 오늘의 현장 번호 |
| Admin | POST | `/api/admin/cafeterias/{cafeteriaId}/arrival-code/regenerate` | ADMIN | 현장 번호 재발급 |
| Admin | PATCH | `/api/admin/cafeterias/{cafeteriaId}` | ADMIN | 식당 설정 수정 (이름, 좌석 수, 식사 시간, 영업시간) |
| Admin | PATCH | `/api/admin/stores/{storeId}` | ADMIN | 가게 정보 부분 수정 (이름, 설명, 분류, 평균 대기시간, 이미지 URL) |
| Admin | PUT | `/api/admin/stores/{storeId}` | ADMIN | 가게 정보 전체 교체 (이미지는 유지) — 아래 "관리자 이미지 업로드 및 가게 수정" |
| Admin | POST | `/api/admin/stores/{storeId}/image` | ADMIN | 가게 이미지 업로드 (multipart) |
| Admin | POST | `/api/admin/menus/{menuId}/image` | ADMIN | 메뉴 이미지 업로드 (multipart) |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/dashboard` | ADMIN | 이용·대기 인원, 이용률, 혼잡도 (`todayOrders` 추가: 8-0 B) |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/queues` | ADMIN | 가게별 현재 대기번호 |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/store-stats` | ADMIN | 가게별 오늘 주문·대기·인기 메뉴 (8-0 B) |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/waitings` | ADMIN | 현재 대기 목록 (8-0 C) |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/usage/hourly` | ADMIN | 시간대별 이용 현황 (`peakPeople`/`avgPeople` 추가: 8-0 D) |
| Admin | GET | `/api/admin/orders` | ADMIN | 주문 현황 |
| Admin | GET | `/api/admin/orders/menu-counts` | ADMIN | 메뉴별 주문 인원 |
| Admin | POST | `/api/admin/stores/{storeId}/menus` | ADMIN | 메뉴 추가 |
| Admin | PUT | `/api/admin/menus/{menuId}` | ADMIN | 메뉴 수정 |
| Admin | PATCH | `/api/admin/menus/{menuId}/availability` | ADMIN | 메뉴 판매 중지/재개 |

---

## 3. Auth

로그인 ID는 **학번(8자리 숫자)**입니다. 이메일은 별도로 입력하지 않고 **`{학번}@dankook.ac.kr`로 자동 생성**됩니다.

### 가입 흐름

```text
1) POST /api/auth/email/send    학번 → {학번}@dankook.ac.kr 로 6자리 코드 발송 (5분 유효)
2) POST /api/auth/email/verify  학번 + 코드 확인 (인증 상태 30분 유지)
3) POST /api/auth/signup        학번 + 비밀번호 + 이름으로 가입
4) POST /api/auth/login         로그인 → SESSION 쿠키 발급
```

### POST `/api/auth/email/send` — 인증 코드 발송

Request
```json
{ "studentNumber": "32201234" }
```
Response — `200 OK`
```json
{ "message": "Verification code sent" }
```
| 에러 | 상황 |
|---|---|
| `VALIDATION_FAILED` | 학번이 숫자 8자리가 아님 |
| `DUPLICATE_STUDENT_NUMBER` | 이미 가입된 학번 |
| `MAIL_SEND_FAILED` | 메일 발송 실패 |
| `TOO_MANY_REQUESTS` (429) | 인증 메일 요청 제한 초과 (아래 "요청 제한") |

### POST `/api/auth/email/verify` — 인증 코드 확인

Request
```json
{ "studentNumber": "32201234", "code": "123456" }
```
Response — `200 OK`
```json
{ "message": "Email verified" }
```
| 에러 | 상황 |
|---|---|
| `INVALID_VERIFICATION_CODE` | 코드가 틀리거나 5분이 지남 |
| `TOO_MANY_REQUESTS` (429) | 같은 코드로 5번까지 틀림 → 코드가 폐기되므로 인증 코드를 **다시 요청**해야 함 |

### POST `/api/auth/signup` — 회원가입

Request
```json
{ "studentNumber": "32201234", "password": "test1234", "name": "홍길동" }
```
Response — `201 Created`
```json
{ "userId": 1, "studentNumber": "32201234", "name": "홍길동", "role": "USER" }
```
- 입력 규칙: 학번 숫자 8자리 / 비밀번호 8~50자 / 이름 필수(50자 이하)
- 비밀번호는 BCrypt 해시로 저장, 권한은 항상 `USER`(클라이언트가 지정 불가)
- 이메일 인증을 완료한 학번만 가입할 수 있고, 인증 상태는 가입 시 1회 소비됩니다.

| 에러 | 상황 |
|---|---|
| `EMAIL_NOT_VERIFIED` | 이메일 인증을 하지 않음 (또는 30분 경과) |
| `DUPLICATE_STUDENT_NUMBER` | 이미 가입된 학번 |
| `VALIDATION_FAILED` | 입력 규칙 위반 |

### POST `/api/auth/login` — 로그인

Request
```json
{ "studentNumber": "32201234", "password": "test1234" }
```
Response — `200 OK` + `Set-Cookie: SESSION=...`
```json
{ "userId": 1, "studentNumber": "32201234", "name": "홍길동", "role": "USER" }
```
| 에러 | 상황 |
|---|---|
| `INVALID_CREDENTIALS` | 학번이 없거나 비밀번호가 틀림 (어느 쪽인지 구분해서 알려주지 않음) |
| `TOO_MANY_LOGIN_ATTEMPTS` (429) | 로그인 시도 제한 초과. **잠금 중에는 비밀번호가 맞아도 거절**됩니다 |

### POST `/api/auth/logout` — 로그아웃

Response — `200 OK`
```json
{ "message": "Logout successful" }
```

### GET `/api/users/me` — 내 정보 (로그인)

Response — `200 OK`
```json
{ "userId": 1, "studentNumber": "32201234", "name": "홍길동", "role": "USER" }
```

### PATCH `/api/users/me/password` — 비밀번호 변경 (로그인)

Request
```json
{ "currentPassword": "test1234", "newPassword": "newpass99" }
```
Response — `200 OK`
```json
{ "message": "Password changed" }
```
- 새 비밀번호는 8~50자이고 현재 비밀번호와 달라야 합니다.
- **변경한 기기는 로그인이 유지되고, 다른 모든 기기의 로그인은 해제**됩니다 (다른 기기에서는 이후 요청이 `401`).
- 현재 비밀번호를 틀리는 것은 **로그인 실패와 같은 한도**로 셉니다 (세션을 탈취당한 경우의 무차별 대입 방지).

| 에러 | 상황 |
|---|---|
| `INVALID_CURRENT_PASSWORD` | 현재 비밀번호가 틀림 |
| `PASSWORD_UNCHANGED` | 새 비밀번호가 현재와 같음 |
| `VALIDATION_FAILED` | 새 비밀번호가 8~50자가 아님 / 필드 누락 |
| `TOO_MANY_LOGIN_ATTEMPTS` (429) | 시도 제한 초과 |
| `UNAUTHORIZED` | 로그인하지 않음 |

### 비밀번호 재설정 (비밀번호를 잊었을 때)

```text
1) POST /api/auth/password/reset/send     학번 → 가입된 학교 메일로 6자리 코드 발송 (5분 유효)
2) POST /api/auth/password/reset/confirm  학번 + 코드 + 새 비밀번호 → 변경 완료
3) POST /api/auth/login                   새 비밀번호로 로그인
```

#### POST `/api/auth/password/reset/send` — 재설정 코드 발송
Request
```json
{ "studentNumber": "32201234" }
```
Response — `200 OK`
```json
{ "message": "Verification code sent" }
```
- **가입된 학번이 아니어도 똑같이 `200`** 을 돌려줍니다 (메일은 보내지 않음). 학번의 가입 여부를 알아낼 수 없게 하기 위함입니다.
- 코드는 가입 시 사용한 학교 메일로 발송됩니다.

| 에러 | 상황 |
|---|---|
| `VALIDATION_FAILED` | 학번이 숫자 8자리가 아님 |
| `TOO_MANY_REQUESTS` (429) | 인증 메일 요청 제한 초과 (아래 "요청 제한") |
| `MAIL_SEND_FAILED` | 메일 발송 실패 |

#### POST `/api/auth/password/reset/confirm` — 코드 확인 + 새 비밀번호
Request
```json
{ "studentNumber": "32201234", "code": "123456", "newPassword": "newpass99" }
```
Response — `200 OK`
```json
{ "message": "Password reset" }
```
- 성공하면 **모든 기기의 로그인이 해제**되고, 로그인 시도 잠금도 풀립니다. 코드는 **한 번만** 쓸 수 있습니다.
- 새 비밀번호가 8~50자가 아니거나 필드가 빠졌을 때는 코드가 소비되지 않습니다.

| 에러 | 상황 |
|---|---|
| `INVALID_VERIFICATION_CODE` | 코드가 틀리거나 만료됨 / 가입되지 않은 학번 / 이미 사용한 코드 |
| `TOO_MANY_REQUESTS` (429) | 같은 코드로 5번까지 틀림 → 코드 폐기, 다시 발송 필요 |
| `VALIDATION_FAILED` | 입력 규칙 위반 |

### 요청 제한 (무차별 대입·남용 방지)

제한은 Redis에 저장되어 서버가 여러 대여도 공유됩니다. 제한에 걸리면 `429` + `Retry-After` 헤더가 내려갑니다.

| 대상 | 규칙 | 에러 코드 |
|---|---|---|
| 로그인 (학번당) | 15분 동안 **5번까지만** 비밀번호를 확인합니다. 6번째부터 **15분간 잠금** (비밀번호가 맞아도 거절). 로그인에 성공하면 초기화 | `TOO_MANY_LOGIN_ATTEMPTS` |
| 로그인 (IP당) | 실패가 **50번** 쌓이면 15분간 차단 (성공은 세지 않음) | `TOO_MANY_LOGIN_ATTEMPTS` |
| 인증 메일 발송 (학번·용도당) | **60초 간격**, 시간당 **5회** | `TOO_MANY_REQUESTS` |
| 인증 메일 발송 (IP당) | 시간당 **20회** | `TOO_MANY_REQUESTS` |
| 인증 코드 확인 | 코드 하나당 **5번까지만** 비교. 5번째까지 틀리면 코드 폐기 | `TOO_MANY_REQUESTS` |

- 존재하지 않는 학번의 실패도 똑같이 세므로, 응답만으로 학번의 가입 여부를 알 수 없습니다.
- 가입용 코드와 재설정용 코드는 발송 한도가 서로 독립입니다.
- 한계: 한 계정에 일부러 6번 시도하면 그 계정이 15분간 잠깁니다. 비밀번호 재설정으로 즉시 풀 수 있습니다.
- 프론트 처리: `429`이면 `Retry-After`(초)만큼 버튼을 비활성화하고 `message`를 그대로 보여주세요.

---

## 4. Cafeteria / Store / Menu

학식당(`cafeteria`) 안에 여러 가게(`store`)가 있고, 가게마다 고정 메뉴(`menu`)가 있습니다. 혼잡도는 **학식당 전체에 대해서만** 제공합니다.

### GET `/api/cafeterias` — 학식당 목록
```json
[
  {
    "id": 1,
    "name": "Main",
    "seatCount": 100,
    "diningMinutes": 30,
    "openingTime": "11:00:00",
    "closingTime": "14:30:00"
  }
]
```
- `diningMinutes`: 음식이 나온 뒤 이용 종료로 간주하는 시간(분)
- `openingTime`, `closingTime`은 값이 없으면 `null`

### GET `/api/cafeterias/{cafeteriaId}` — 학식당 상세
응답 형식은 목록의 항목과 같습니다. 없으면 `404 CAFETERIA_NOT_FOUND`.

### GET `/api/cafeterias/{cafeteriaId}/stores` — 가게 목록
```json
[
  {
    "id": 1, "cafeteriaId": 1, "name": "Korean", "description": "든든한 한식", "category": "한식",
    "avgWaitMinutes": 2, "imageUrl": null, "minPrice": 5500, "representativeMenuName": "Jeyuk"
  },
  {
    "id": 2, "cafeteriaId": 1, "name": "Noodle", "description": null, "category": null,
    "avgWaitMinutes": 3, "imageUrl": null, "minPrice": 4500, "representativeMenuName": "Ramen"
  }
]
```
- `avgWaitMinutes`: 가게의 1인분 평균 대기시간(분). 예상 대기시간 계산에 사용됩니다.
- `description`, `category`: 가게 소개와 분류. 값이 없으면 `null`입니다.
- `minPrice`: **판매 중인** 메뉴의 최저가 (판매 중인 메뉴가 없으면 `null`) / `representativeMenuName`: 가게의 첫 번째 메뉴(등록 순) 이름. 홈 화면 가게 카드용이라 메뉴 API를 따로 호출할 필요가 없습니다.
- 없는 식당: `404 CAFETERIA_NOT_FOUND`

### GET `/api/cafeterias/{cafeteriaId}/status` — 혼잡도
```json
{
  "cafeteriaId": 1,
  "seatCount": 100,
  "currentPeople": 1,
  "waitingPeople": 1,
  "usageRate": 1.0,
  "congestionLevel": "RELAXED",
  "recordedAt": "2026-10-07T21:23:25.1113992"
}
```
계산 규칙 (`currentPeople`은 8-1의 최근 인원 스냅샷이 우선이고, 없으면 아래 주문 기반 값)
- `currentPeople`(주문 기반 예비값): 음식이 나왔고(예상 완료 시각 경과) 아직 이용 종료 시각(`예상 완료 시각 + diningMinutes`) 전인 주문 수
- `waitingPeople`: 도착 인증 후 조리 중인(예상 완료 시각 전) 주문 수
- `usageRate`: `currentPeople / seatCount × 100` (소수 첫째 자리 반올림)
- `congestionLevel`: `RELAXED`(30% 미만) / `NORMAL`(60% 미만) / `CROWDED`(85% 미만) / `VERY_CROWDED`
  - ⚠️ 구간 기준은 **임시값**이며 확정 시 변경됩니다.
- `recordedAt`: 응답 생성 시각

### GET `/api/stores/{storeId}` — 가게 상세
```json
{
  "id": 1, "cafeteriaId": 1, "name": "Korean", "description": "든든한 한식", "category": "한식",
  "avgWaitMinutes": 2, "imageUrl": null, "minPrice": 5500, "representativeMenuName": "Jeyuk"
}
```
필드 설명은 가게 목록과 같습니다. 없으면 `404 STORE_NOT_FOUND`.

### GET `/api/stores/{storeId}/menus` — 가게 메뉴 목록
```json
[
  { "id": 1, "storeId": 1, "name": "Jeyuk", "price": 5500, "imageUrl": null, "available": true },
  { "id": 3, "storeId": 1, "name": "SoldOut", "price": 4000, "imageUrl": null, "available": false }
]
```
- 판매 중지된 메뉴(`available: false`)도 목록에 포함됩니다. 화면에서는 품절로 표시하고 주문 시 선택하지 못하게 해주세요.
- 없는 가게: `404 STORE_NOT_FOUND`

---

## 5. Mileage

결제는 마일리지 방식입니다. 충전은 토스페이먼츠 결제창에서 결제한 뒤 서버가 승인을 확정하는 흐름입니다.

### GET `/api/mileage` — 잔액
```json
{ "balance": 14500 }
```

### GET `/api/mileage/transactions` — 내역 (최신순)
```json
[
  { "id": 8, "type": "USE", "amount": 5500, "balanceAfter": 14500, "orderId": 7, "createdAt": "2026-10-07T20:53:22.755429" }
]
```
| type | 의미 | orderId |
|---|---|---|
| `CHARGE` | 충전 | `null` |
| `USE` | 주문 결제 | 주문 ID |
| `REFUND` | 주문 취소 환불 | 주문 ID |

`amount`는 항상 양수이며 증감은 `type`으로 구분합니다.

### POST `/api/mileage/charge/confirm` — 충전 (결제 승인)

흐름
```text
1) 프론트: POST /api/mileage/charge/prepare에 amount 전달 → 서버가 clientKey, customerKey, orderId, amount 반환
2) 반환된 값으로 토스 테스트 결제창 호출
3) 결제 완료 후 successUrl로 paymentKey, orderId, amount 전달됨
4) 프론트 → 이 API 호출 → 서버가 토스에 승인 요청 → 성공 시 마일리지 적립
```
Request
```json
{ "paymentKey": "tgen_20261007...", "orderId": "order-20261007-0001", "amount": 5000 }
```
Response — `200 OK`
```json
{ "balance": 19500 }
```
- 충전 금액: **1,000원 이상 100,000원 이하**
- 토스가 승인을 거절하면 `400 PAYMENT_FAILED`. 동일한 완료 결제를 다시 보내면 최초 결과를 반환하며 중복 적립하지 않습니다. 준비 기록이 없거나 사용자·금액·paymentKey가 다르면 `400 INVALID_REQUEST`.
- ⚠️ 승인은 토스 **테스트 키** 기준입니다. 실제 결제는 일어나지 않습니다.

#### 프론트 연동 가이드 (토스 테스트 결제창 → 임시 결제)
- 충전은 **토스 테스트 결제창**을 거쳐 진행합니다. 테스트 키를 쓰므로 결제창은 실제와 같게 동작하지만 **실제 청구는 일어나지 않고**, 승인이 성공하면 마일리지가 실제로 적립됩니다. (화면에서는 결제가 된 것처럼 보임)
- 프론트가 준비할 것: 토스페이먼츠 결제 SDK 연동, 충전 금액 선택 화면, 결제 성공/실패 화면 (`successUrl` → 이 API 호출 → 잔액 갱신, `failUrl` → 안내). 클라이언트 키는 프론트 환경변수로 관리합니다.
- `paymentKey`는 **토스 결제창에서 결제를 진행해야만 생성**되므로, 결제창 없이 서버만으로 충전을 완료하는 방법은 없습니다. (개발용 우회 충전 API는 만들지 않습니다.)
- 충전이 완료되면 알림 `MILEAGE_CHARGED`가 생성됩니다 (8-0 E).

---

## 6. Order

### 주문 상태

```text
PAID ──(현장 번호 인증)──▶ COOKING ──(예상 시간 경과: 음식이 나옴)──▶ RECEIVED
 │
 └──(취소 / 1시간 만료)──▶ CANCELLED
```

| status | 의미 |
|---|---|
| `PAID` | 선주문 + 마일리지 결제 완료. 아직 조리 시작 전 |
| `COOKING` | 도착 인증 완료 → 대기번호 발급, 조리 시작 |
| `RECEIVED` | 음식이 나옴 (예상 대기시간 경과 시 **자동으로 바뀝니다. 사용자·관리자가 누르는 버튼 없음**, 휴게소 번호 호출 방식). 수령 확인 절차는 없음 |
| `READY` | 더 이상 만들어지지 않는 값 (이전 방식의 호환용. 서버가 조회하면 `RECEIVED`로 정리) |
| `CANCELLED` | 취소됨 |

- 상태 전이는 서버가 **조회 시점에** 반영합니다. 프론트는 주문 상세를 주기적으로 조회(폴링)해서 `COOKING`에서 `RECEIVED`로 바뀌면(또는 `FOOD_READY` 알림이 오면) "음식이 나왔어요, 대기번호 N번" 안내를 띄우면 됩니다. 수령 버튼/API는 없습니다.
- **한 사용자는 진행 중인 주문(`PAID`/`COOKING`)을 1개만** 가질 수 있습니다. 음식이 나오면(`RECEIVED`) 바로 새 주문을 할 수 있습니다.
- 주문은 **가게별로 분리**됩니다 (한 주문에 한 가게의 메뉴만).

### 주문 응답 형식 (공통)

```json
{
  "orderId": 7,
  "storeId": 1,
  "storeName": "Korean",
  "status": "COOKING",
  "totalPrice": 5500,
  "items": [
    { "menuId": 1, "menuName": "Jeyuk", "quantity": 1, "unitPrice": 5500 }
  ],
  "waitingNumber": 2,
  "expectedReadyAt": "2026-10-07T21:28:10",
  "remainingSeconds": 300,
  "orderedAt": "2026-10-07T20:53:22.746737",
  "arrivedAt": "2026-10-07T20:53:22.858744",
  "receivedAt": null,
  "leaveAt": null,
  "cancelledAt": null
}
```
| 필드 | 설명 |
|---|---|
| `waitingNumber` | 도착 인증 후 발급되는 대기번호 (가게별·날짜별 1번부터). 그 전에는 `null` |
| `expectedReadyAt` | 예상 완료 시각. 도착 인증 전에는 `null` |
| `remainingSeconds` | `COOKING` 상태일 때 남은 초(0 이상). 그 외에는 `null` |
| `leaveAt` | 이용 종료 예정 시각 (`receivedAt` + `diningMinutes`). 음식이 나온 뒤에만 값이 있음 |
| `unitPrice` | 주문 시점의 메뉴 가격 (이후 가격이 바뀌어도 유지) |

### POST `/api/orders` — 선주문 + 결제

Request
```json
{
  "storeId": 1,
  "items": [
    { "menuId": 1, "quantity": 2 },
    { "menuId": 2, "quantity": 1 }
  ]
}
```
Response — `201 Created` (주문 응답 형식, `status: "PAID"`)

처리 규칙
- 메뉴 가격 × 수량의 합계를 마일리지에서 차감합니다 (주문 생성과 차감은 한 트랜잭션).
- 같은 메뉴를 여러 번 보내면 수량이 합산됩니다. 수량은 항목당 1~20.

| 에러 | 상황 |
|---|---|
| `VALIDATION_FAILED` | `items`가 비었거나 수량이 범위 밖 |
| `STORE_NOT_FOUND` | 없는 가게 |
| `MENU_NOT_FOUND` | 없는 메뉴, 또는 다른 가게의 메뉴 |
| `MENU_UNAVAILABLE` | 판매 중지된 메뉴 |
| `ACTIVE_ORDER_EXISTS` | 진행 중인 주문이 이미 있음 |
| `INSUFFICIENT_MILEAGE` | 마일리지 부족 (주문은 생성되지 않음) |

### GET `/api/orders/me` — 내 주문 목록 (최신순)
주문 응답 형식의 배열입니다.

### GET `/api/orders/{orderId}` — 내 주문 상세
주문 응답 형식. 

| 에러 | 상황 |
|---|---|
| `ORDER_NOT_FOUND` | 없는 주문 |
| `FORBIDDEN` | 다른 사람의 주문 |

### POST `/api/orders/{orderId}/arrival` — 현장 번호 도착 인증

학식당에서 확인한 **오늘의 현장 번호**(매일 바뀌는 4자리)를 입력합니다.

Request
```json
{ "code": "4114" }
```
Response — `200 OK` (주문 응답 형식, `status: "COOKING"`)

처리 규칙
- `PAID` 상태일 때만 가능하며, 성공하면 대기번호가 발급되고 조리가 시작됩니다.
- 예상 대기시간 = **(앞 대기 인원 + 1) × 가게 평균 대기시간(`avgWaitMinutes`)**
  - 앞 대기 인원: 같은 가게에서 아직 조리 중인 주문 수
  - 예) 앞에 1명, 평균 2분 → 4분 후 `expectedReadyAt`
- 현장 번호는 이 API의 입력값일 뿐이며, 대기번호와는 다른 값입니다.

| 에러 | 상황 |
|---|---|
| `INVALID_ARRIVAL_CODE` | 현장 번호가 틀림 |
| `INVALID_ORDER_STATUS` | `PAID`가 아닌 주문 (이미 인증했거나 취소됨) |
| `VALIDATION_FAILED` | `code`가 비어 있음 |
| `FORBIDDEN` / `ORDER_NOT_FOUND` | 남의 주문 / 없는 주문 |

### POST `/api/orders/{orderId}/cancel` — 주문 취소

Request body 없음. Response — `200 OK` (주문 응답 형식, `status: "CANCELLED"`)
- **도착 인증 전(`PAID`)에만** 취소할 수 있습니다.
- 도착 인증 전 만료되지 않은 주문의 사용자 직접 취소는 **100% 환불**됩니다 (`REFUND` 마일리지 내역 생성).
- 미인증 주문은 날짜와 관계없이 **주문 후 1시간에 만료되어 100% 환불**됩니다. 매분 자동 작업과 조회 시 동기화로 `CANCELLED` 처리합니다.

| 에러 | 상황 |
|---|---|
| `INVALID_ORDER_STATUS` | `PAID`가 아님 (이미 도착 인증했거나 취소됨) |

---

## 7. Admin (ADMIN 전용)

일반 사용자가 호출하면 `403 FORBIDDEN`, 로그인하지 않았으면 `401 UNAUTHORIZED`.
날짜 파라미터는 `YYYY-MM-DD` 형식이며 생략하면 오늘입니다.

### GET `/api/admin/cafeterias/{cafeteriaId}/arrival-code` — 오늘의 현장 번호
```json
{ "cafeteriaId": 1, "date": "2026-10-07", "code": "4114" }
```
- 그날 처음 조회하거나 사용자가 처음 인증할 때 자동 생성되며, 하루 동안 같은 값입니다.
- 관리자 화면에 표시하거나 학식당 현장에 게시하는 용도입니다.

### POST `/api/admin/cafeterias/{cafeteriaId}/arrival-code/regenerate` — 현장 번호 재발급
Request body 없음. Response — `200 OK` (조회와 같은 형식, 새 번호)
```json
{ "cafeteriaId": 1, "date": "2026-10-09", "code": "1693" }
```
- 오늘의 번호가 **이전 번호와 다른 새 4자리 번호**로 바뀝니다. 번호가 아직 없으면 새로 만듭니다.
- **이미 도착 인증을 마친 주문(`COOKING` 등)에는 영향이 없고**, 아직 인증하지 않은(`PAID`) 사용자는 새 번호를 입력해야 합니다. 이전 번호로 인증하면 `INVALID_ARRIVAL_CODE`입니다.
- 번호가 유출되었을 때나 현장 안내판을 바꿀 때 사용합니다. 동시에 여러 번 호출해도 식당·날짜당 번호는 하나만 유지됩니다.
- 없는 식당: `404 CAFETERIA_NOT_FOUND`

### PATCH `/api/admin/cafeterias/{cafeteriaId}` — 식당 설정 수정
**보낸 항목만** 수정됩니다 (생략한 항목은 그대로). 최소 한 항목은 보내야 합니다.
```json
{ "name": "혜당관 학생식당", "seatCount": 120, "diningMinutes": 25, "openingTime": "11:00", "closingTime": "14:30" }
```
Response — `200 OK`: 수정된 식당 (`GET /api/cafeterias/{id}`와 같은 형식)
```json
{ "id": 1, "name": "혜당관 학생식당", "seatCount": 120, "diningMinutes": 25, "openingTime": "11:00:00", "closingTime": "14:30:00" }
```
| 항목 | 규칙 |
|---|---|
| `name` | 1~100자 (공백만은 불가) |
| `seatCount` | 1~10,000 |
| `diningMinutes` | 1~240. **이후에 음식이 나오는 주문부터** 적용됩니다 (이미 수령한 주문의 이용 종료 시각은 그대로) |
| `openingTime`, `closingTime` | `HH:mm` 또는 `HH:mm:ss`. **변경 후** 시작이 종료보다 빨라야 합니다 (하나만 보내도 기존 값과 비교). 영업시간을 비우는 기능은 없습니다 |

- `seatCount`를 바꾸면 이용률·혼잡도 계산에 바로 반영됩니다.

| 에러 | 상황 |
|---|---|
| `VALIDATION_FAILED` | 변경할 항목이 없음 / 범위·형식 위반 / 영업 종료가 시작보다 늦지 않음 |
| `CAFETERIA_NOT_FOUND` | 없는 식당 |

### PATCH `/api/admin/stores/{storeId}` — 가게 정보 수정
**보낸 항목만** 수정됩니다. 최소 한 항목은 보내야 합니다.
```json
{ "name": "51장국밥", "description": "든든한 국밥과 한식 메뉴", "category": "한식", "avgWaitMinutes": 3, "imageUrl": "https://..." }
```
Response — `200 OK`: 수정된 가게 (`GET /api/stores/{id}`와 같은 형식, `minPrice`·`representativeMenuName` 포함)

| 항목 | 규칙 |
|---|---|
| `name` | 1~100자 (공백만은 불가, 앞뒤 공백은 제거) |
| `description` | 200자 이하. **빈 문자열 `""`을 보내면 값이 지워집니다(`null`)** |
| `category` | 30자 이하. `""`이면 지워집니다 |
| `avgWaitMinutes` | 1~120분. 이후 도착 인증하는 주문의 예상 대기시간 계산에 쓰입니다 |
| `imageUrl` | 500자 이하. **URL 문자열**로 이미지를 바꾸거나 `""`로 지웁니다. 파일을 올리려면 `POST /api/admin/stores/{id}/image`를 쓰세요 (아래 "관리자 이미지 업로드 및 가게 수정") |

**이미지 URL을 바꾸거나 지우면 이전에 업로드한 파일이 함께 정리됩니다.** 이전 이미지가 업로드한 파일이면 DB 커밋 후 저장소에서 삭제되고 저장된 객체 키도 지워집니다 (외부 URL은 삭제하지 않음). 현재와 **같은 URL을 그대로 보내거나** `imageUrl`을 생략하면 이미지는 그대로 유지됩니다. 요청이 검증에 실패(400)하면 아무것도 바뀌지 않고 파일도 삭제되지 않습니다. 같은 가게의 이미지 업로드·수정은 DB 행 잠금으로 직렬화됩니다.

| 에러 | 상황 |
|---|---|
| `VALIDATION_FAILED` | 변경할 항목이 없음 / 범위·길이 위반 |
| `STORE_NOT_FOUND` | 없는 가게 |
| `STORAGE_NOT_CONFIGURED` (503) | 업로드한 이미지가 있는 가게인데 이미지 저장소가 설정되지 않아 이전 파일을 정리할 수 없음 |

#### `PUT`과 `PATCH`의 차이 (가게 정보 수정)
| | `PUT /api/admin/stores/{id}` | `PATCH /api/admin/stores/{id}` |
|---|---|---|
| 방식 | **전체 교체**: `name`, `avgWaitMinutes` 필수, 생략한 `description`/`category`는 비워짐 | **부분 수정**: 보낸 항목만 바뀜 (생략 = 그대로, `""` = 지움) |
| 이미지 | 건드리지 않음 (유지) | `imageUrl`로 교체/삭제 가능 (이전 업로드 파일은 정리) |
| 평균 대기시간 범위 | 0 이상 | 1~120분 |
| 응답 | `id, cafeteriaId, name, description, category, avgWaitMinutes, imageUrl` | `GET /api/stores/{id}`와 같은 형식 (`minPrice` 등 포함) |

화면에서 "이름만 바꾸기"처럼 일부만 수정할 때는 `PATCH`가 편하고, 폼 전체를 저장할 때는 `PUT`을 쓰면 됩니다.

### GET `/api/admin/cafeterias/{cafeteriaId}/dashboard` — 현황
```json
{
  "currentPeople": 1,
  "waitingPeople": 1,
  "seatCount": 100,
  "usageRate": 1.0,
  "congestionLevel": "RELAXED",
  "todayOrders": 124
}
```
계산 규칙은 `GET /api/cafeterias/{id}/status`와 같습니다. `todayOrders`는 오늘 주문한 수(전 가게 합계, **취소 제외**)입니다. 없는 식당은 `404 CAFETERIA_NOT_FOUND`.

### GET `/api/admin/cafeterias/{cafeteriaId}/queues` — 가게별 대기번호 현황
```json
[
  { "storeId": 1, "storeName": "Korean", "waitingPeople": 2, "waitingNumbers": [3, 4] }
]
```
- 현재 조리 중인 주문만 포함합니다. 대기 중인 주문이 없는 가게는 목록에 없습니다.

### GET `/api/admin/cafeterias/{cafeteriaId}/usage/hourly` — 시간대별 이용 현황
Query: `date`(선택)
```json
[
  { "hour": 0, "orderCount": 0, "arrivalCount": 0, "receivedCount": 0, "peakPeople": 0, "avgPeople": 0.0 },
  { "hour": 12, "orderCount": 31, "arrivalCount": 28, "receivedCount": 27, "peakPeople": 87, "avgPeople": 62.5 }
]
```
- 항상 0~23시 24개 항목. 각 시간대에 주문한 수 / 도착 인증한 수 / 수령한 수입니다.
- `peakPeople`: 그 시간대의 최대 이용 인원, `avgPeople`: 평균 이용 인원(소수 첫째 자리). 이용 구간(수령 시각 ~ 이용 종료 시각)이 겹치는 주문으로 계산합니다. 같은 시각에 끝나고 시작하는 구간은 겹치지 않는 것으로 봅니다. (자세한 설명은 8-0 D)

### GET `/api/admin/orders` — 주문 현황
Query: `date`(선택), `storeId`(선택), `status`(선택: `PAID` `COOKING` `READY` `RECEIVED` `CANCELLED`)

`date`는 **주문한 날짜** 기준입니다.
```json
[
  {
    "orderId": 7,
    "userId": 2,
    "studentNumber": "33333333",
    "userName": "관리자",
    "storeId": 1,
    "storeName": "Korean",
    "status": "READY",
    "totalPrice": 5500,
    "waitingNumber": 2,
    "items": [ { "menuId": 1, "menuName": "Jeyuk", "quantity": 1, "unitPrice": 5500 } ],
    "orderedAt": "2026-10-07T20:53:22.746737",
    "arrivedAt": "2026-10-07T20:53:22.858744",
    "expectedReadyAt": "2026-10-07T21:28:10",
    "receivedAt": null
  }
]
```
- 최신 주문이 먼저 옵니다. 조리 완료 예상 시각이 지났지만 아직 `RECEIVED`로 정리되지 않은(사용자가 조회하기 전) 주문은 관리자 화면에서만 `READY`로 표시됩니다.
- `status` 필터는 DB에 저장된 상태 기준이라, 아직 조회되지 않아 `COOKING`으로 남아 있는 지난 주문은 `COOKING` 필터에 포함될 수 있습니다.

### GET `/api/admin/orders/menu-counts` — 메뉴별 주문 인원
Query: `date`(선택), `storeId`(선택). 취소된 주문은 제외합니다.
```json
[
  { "menuId": 1, "menuName": "Jeyuk", "storeId": 1, "storeName": "Korean", "orderCount": 2, "totalQuantity": 3 },
  { "menuId": 2, "menuName": "Bibimbap", "storeId": 1, "storeName": "Korean", "orderCount": 1, "totalQuantity": 1 }
]
```
- `orderCount`: 해당 메뉴가 포함된 주문 수 / `totalQuantity`: 총 수량 (수량 많은 순)

### 메뉴 관리

메뉴는 주문 내역이 참조하므로 **삭제하지 않고** 판매 중지(`available: false`)로 관리합니다.

#### POST `/api/admin/stores/{storeId}/menus` — 메뉴 추가
Request
```json
{ "name": "김치찌개", "price": 5000, "imageUrl": "https://..." }
```
Response — `201 Created`
```json
{ "id": 5, "storeId": 1, "name": "김치찌개", "price": 5000, "imageUrl": "https://...", "available": true }
```
- `name` 필수(100자 이하), `price` 0 이상, `imageUrl` 선택(500자 이하)
- 없는 가게: `404 STORE_NOT_FOUND`

#### PUT `/api/admin/menus/{menuId}` — 메뉴 수정
Request는 추가와 같고 `name`, `price`, `imageUrl`을 모두 덮어씁니다. Response는 `200 OK` + 수정된 메뉴(위 형식). 없는 메뉴: `404 MENU_NOT_FOUND`.

#### PATCH `/api/admin/menus/{menuId}/availability` — 판매 중지/재개
Request
```json
{ "available": false }
```
Response — `200 OK` + 변경된 메뉴(위 형식)

---

## 8. 🕓 아직 없는 기능 (명세 대상)

회의록 기준으로 아직 구현되지 않았거나 확정되지 않은 항목입니다. 구현 전에 이 문서에 먼저 추가합니다. (8-0은 구현이 끝나 상세 명세로 남겨둔 항목입니다.)

- ✅ **프론트엔드 코드 분석으로 추가된 API** (알림, 가게 정보 확장, 관리자 통계 등) — 구현 완료, 아래 8-0 참고
- ✅ 메뉴·가게 이미지 업로드 (NAVER Cloud Object Storage) — 구현 완료, 아래 "관리자 이미지 업로드 및 가게 수정" 참고
- **CCTV 이미지 기반 인원 추정 (CLOVA)** 및 시뮬레이션 데이터 주입 → 아래 8-1 참고
- 혼잡도 구간 확정
- 식사 시간(`diningMinutes`)을 가게/메뉴별로 다르게 할지 여부

### 8-0. ✅ 프론트엔드 연동에서 추가된 API (구현 완료)

프론트엔드(`frontend/`, React) 화면 코드를 분석해, 화면이 필요로 하지만 현재 API에 없는 것을 정리했습니다. (프론트는 아직 목 데이터를 사용 중)
기본 원칙은 **기존 응답은 그대로 두고 필드/엔드포인트를 추가**하는 것이라, 이미 연동한 화면이 깨지지 않습니다.

| 항목 | 종류 | 상태 |
|---|---|---|
| A. 가게 정보 확장 | 기존 응답에 필드 추가 | ✅ 구현됨 |
| B. 가게별 통계 + 대시보드 `todayOrders` | 신규 + 필드 추가 | ✅ 구현됨 |
| C. 현재 대기 목록 | 신규 | ✅ 구현됨 |
| D. 시간대별 이용 인원 | 기존 응답에 필드 추가 | ✅ 구현됨 (주문 기반, 인원 스냅샷의 영향을 받지 않음) |
| E. 알림 API | 신규 | ✅ 구현됨 |
| F. 진행 중 주문 1건 조회 | 신규 | ✅ 구현됨 |

**제외한 것**: 대기번호는 지금처럼 **날마다 1번부터 가게별로 초기화되는 숫자**(`waitingNumber: 13`)를 그대로 씁니다. `A-013` 같은 표기는 프론트에서 필요하면 포맷만 하면 됩니다 (예: `String(n).padStart(3, '0')`).

#### A. 가게 정보 확장 — `GET /api/cafeterias/{cafeteriaId}/stores`, `GET /api/stores/{storeId}`
홈 화면의 가게 카드가 가게 설명, 분류, 대표 메뉴, 최저가를 보여줍니다. 지금은 이를 얻으려면 가게마다 메뉴 API를 추가로 호출해야 합니다.

```json
{
  "id": 1,
  "cafeteriaId": 1,
  "name": "51장국밥",
  "description": "든든한 국밥과 한식 메뉴",
  "category": "한식",
  "avgWaitMinutes": 2,
  "imageUrl": null,
  "minPrice": 1000,
  "representativeMenuName": "고기만국밥"
}
```
| 필드 | 설명 |
|---|---|
| `description` | 가게 한 줄 소개. 없으면 `null` |
| `category` | 분류(한식, 아시안, 돈카츠, 분식, 일식, 덮밥 등). 없으면 `null` |
| `minPrice` | 판매 중(`available`)인 메뉴의 최저가. 판매 중인 메뉴가 없으면 `null` |
| `representativeMenuName` | 가게의 첫 번째 메뉴 이름(등록 순). 메뉴가 없으면 `null` |

DB: `stores.description VARCHAR(200) NULL`, `stores.category VARCHAR(30) NULL` 추가. `minPrice`와 `representativeMenuName`은 메뉴에서 계산합니다. 입력은 시드 데이터로 하며, 관리자 수정 API는 이번 범위에서 제외합니다.

#### B. 가게별 통계 — `GET /api/admin/cafeterias/{cafeteriaId}/store-stats` (ADMIN)
관리자 대시보드의 "가게별 주문 현황"과 통계의 "가게별 주문 비중"에 쓰입니다.
```json
[
  {
    "storeId": 1,
    "storeName": "51장국밥",
    "todayOrders": 23,
    "waitingCount": 4,
    "topMenus": [
      { "menuId": 12, "menuName": "순대국밥", "count": 11 },
      { "menuId": 14, "menuName": "얼큰고기만국밥", "count": 7 },
      { "menuId": 4, "menuName": "닭곰탕(밥 포함)", "count": 5 }
    ]
  }
]
```
- 학식당의 **모든 가게**가 가게 ID 순으로 포함됩니다 (주문이 없으면 `0`, `topMenus: []`).
- `todayOrders`: 오늘 주문한 수 (**취소 제외**). `waitingCount`: 현재 조리 중인 주문 수.
- `topMenus`: 오늘 주문 수 기준 상위 3개 (`count` = 그 메뉴가 포함된 주문 수, 취소 제외, 동률은 메뉴 ID 순).

`GET /api/admin/cafeterias/{cafeteriaId}/dashboard` 응답에 **`todayOrders`**(전 가게 합계, 취소 제외)를 추가합니다.
```json
{ "currentPeople": 87, "waitingPeople": 5, "seatCount": 120, "usageRate": 72.5, "congestionLevel": "CROWDED", "todayOrders": 124 }
```

#### C. 현재 대기 목록 — `GET /api/admin/cafeterias/{cafeteriaId}/waitings` (ADMIN)
관리자 "현재 대기 현황" 화면(대기번호, 가게·메뉴, 대기 시작 시각, 준비까지 남은 시간)에 쓰입니다.
```json
[
  {
    "orderId": 7,
    "waitingNumber": 13,
    "storeId": 1,
    "storeName": "51장국밥",
    "status": "COOKING",
    "items": [ { "menuId": 12, "menuName": "순대국밥", "quantity": 1 } ],
    "arrivedAt": "2026-10-08T12:32:10",
    "expectedReadyAt": "2026-10-08T12:38:34",
    "remainingSeconds": 384
  }
]
```
- 오늘 도착 인증을 했고 아직 `RECEIVED`로 정리되지 않은 주문(`COOKING`, 예상 시간이 지났지만 사용자가 조회하기 전이면 `READY`로 표시)이며, **도착 인증이 최근인 순**입니다.
- `READY`이면 `remainingSeconds`는 `0`입니다. 대시보드의 `waitingPeople`은 `COOKING`만 셉니다.
- `GET /api/admin/orders?status=COOKING`과 달리 DB에 `COOKING`으로 남아 있는 지난 주문이 섞이지 않고, 남은 시간이 서버 기준으로 계산됩니다.

#### D. 시간대별 이용 인원 — `GET /api/admin/cafeterias/{cafeteriaId}/usage/hourly` 확장
관리자 통계의 "시간대별 이용 인원" 막대그래프는 **그 시간에 식당에 있던 인원**을 보여줍니다. 기존 응답은 주문·도착·수령 **건수**라서 `peakPeople`, `avgPeople`을 추가합니다.
```json
{ "hour": 12, "orderCount": 31, "arrivalCount": 28, "receivedCount": 27, "peakPeople": 87, "avgPeople": 62.5 }
```
- `peakPeople`: 그 시간대의 최대 이용 인원 / `avgPeople`: 평균 이용 인원 (소수 첫째 자리).
- 계산 근거: **주문 기반**(수령 시각 ~ 이용 종료 시각이 겹치는 인원)입니다. `peakPeople`·`avgPeople`을 포함한 이 API의 모든 필드는 CLOVA·SIMULATION·MANUAL 인원 스냅샷의 영향을 받지 않습니다. 스냅샷 기록은 8-1의 `occupancy/history`로 조회합니다. 현재 인원 status·대시보드와 시간대별 이용 현황은 계산 근거가 다릅니다.
- 항상 0~23시 24개 항목이며, 화면에서는 필요한 시간대(예: 09~16시)만 사용합니다.

#### E. 알림 API — `/api/notifications` (로그인)
학생 화면의 알림 탭과 홈의 알림 점(●)에 쓰입니다. 서버가 주문·충전 이벤트가 발생할 때 알림을 **저장**하고, 프론트는 조회합니다. (푸시/SSE는 포함하지 않으며, 프론트가 주기적으로 조회합니다.)

알림 응답
```json
{
  "id": 12,
  "type": "FOOD_READY",
  "title": "음식을 수령하러 와주세요",
  "message": "예상 준비시간이 지났습니다. 51장국밥 수령대로 와주세요.",
  "orderId": 7,
  "read": false,
  "createdAt": "2026-10-08T12:38:34"
}
```

알림 종류 (`type`)
| type | 발생 시점 | title | message 예 |
|---|---|---|---|
| `ORDER_PAID` | 선주문·결제 완료 | 선주문이 완료되었습니다 | 51장국밥 순대국밥 결제가 완료됐어요. |
| `WAITING_NUMBER_ISSUED` | 도착 인증 완료 | 대기번호 13번이 발급되었습니다 | 도착 인증이 완료되어 대기번호가 발급됐어요. |
| `COOKING_STARTED` | 도착 인증 완료 | 음식 조리가 시작되었습니다 | 예상 준비시간은 약 6분입니다. |
| `FOOD_READY` | 예상 준비 시각 경과 (음식이 나옴) | 음식을 수령하러 와주세요 | 예상 준비시간이 지났습니다. 51장국밥 수령대로 와주세요. |
| `ORDER_CANCELLED` | 주문 취소 / 1시간 미인증 자동 취소 | 주문이 취소되었습니다 | 6,500P가 환불되었어요. |
| `MILEAGE_CHARGED` | 마일리지 충전 성공 | 마일리지가 충전되었습니다 | 5,000P가 충전됐어요. |

- 여러 메뉴가 있는 주문의 메시지는 `{첫 메뉴} 외 N개`로 표기합니다. `orderId`는 주문과 무관한 알림(`MILEAGE_CHARGED`)이면 `null`입니다.

##### GET `/api/notifications` — 내 알림 목록
Query: `unreadOnly`(선택, 기본 `false`), `limit`(선택, 기본 30, 최대 100). **최신순**의 알림 응답 배열입니다.

##### GET `/api/notifications/unread-count` — 읽지 않은 알림 수
```json
{ "count": 2 }
```
홈 화면의 알림 점 표시용으로, 가볍게 주기적(예: 10~30초)으로 호출합니다.

##### PATCH `/api/notifications/{notificationId}/read` — 읽음 처리
Request body 없음. Response — `200 OK` + 읽음 처리된 알림. 이미 읽은 알림에도 `200`입니다.

| 에러 | 상황 |
|---|---|
| `NOTIFICATION_NOT_FOUND` | 없는 알림 |
| `FORBIDDEN` | 다른 사람의 알림 |

##### PATCH `/api/notifications/read-all` — 모두 읽음
```json
{ "updated": 3 }
```

처리 규칙
- 알림은 해당 이벤트가 일어난 **같은 트랜잭션에서 저장**됩니다 (주문 생성, 도착 인증, 취소, 충전 승인).
- **`FOOD_READY`는 스케줄러 없이 조회 시점에 만들어집니다.** 예상 준비 시각이 지난 뒤 사용자가 알림 목록, 읽지 않은 수, 주문 API 중 하나를 처음 호출하면 생성되며 `createdAt`은 **예상 준비 시각**으로 기록됩니다. 주문당 1번만 생성됩니다. 그래서 프론트가 `unread-count`를 주기적으로 호출하면 준비 완료 알림이 자연스럽게 나타납니다.
- 자동 취소 주문의 `ORDER_CANCELLED`는 매분 만료 작업 또는 조회 시 동기화에서 생성됩니다.
- DB: `notifications(id, user_id, type, title, message, order_id NULL, is_read, created_at)`. `(order_id, type)` 유니크로 중복 생성을 막습니다.

#### F. 진행 중 주문 1건 — `GET /api/orders/me/current`
앱을 새로고침해도 "내 주문" 화면을 복원하기 위한 조회입니다. 진행 중(`PAID`/`COOKING`)인 주문이 있으면 `200` + 주문 응답 형식, **없으면 `204 No Content`**(본문 없음)입니다. 지금은 `GET /api/orders/me`로 전체를 받아 걸러야 합니다.

#### 변경 요약 (DB)
| 대상 | 변경 |
|---|---|
| `stores` | `description`, `category` 컬럼 추가 |
| `notifications` | 신규 테이블 |
| `usage_snapshots` | `source` 컬럼 추가 (8-1) |

### 8-1. 이미지 기반 인원 추정 및 시뮬레이션

ADMIN 세션 인증이 필요합니다. 분석 결과와 직접 입력 값은 같은 `usage_snapshots` 저장 경로를 사용하며 출처는 CLOVA / SIMULATION / MANUAL입니다.

- `POST /api/admin/cafeterias/{id}/occupancy/analyze`: multipart `image` (JPEG/PNG, 2MiB 이하). HCX-005로 식사 공간의 인원을 추정합니다. 긴 변 2240px 이하, 짧은 변 4px 이상, 가로세로 비율 5:1 이하. 이미지 원본은 DB/Object Storage에 보관하지 않습니다.
- `POST /api/admin/cafeterias/{id}/occupancy`: JSON `{"currentPeople":42,"recordedAt":"2026-10-09T12:00:00","source":"MANUAL"}`. source 생략 시 SIMULATION, CLOVA 직접 지정 불가. recordedAt 생략 시 현재 한국 시각, 미래 시각 불가. 인원은 정수 0~10000.
- `POST /api/admin/cafeterias/{id}/occupancy/simulation`: `{"snapshots":[{"currentPeople":10,"recordedAt":"2026-10-09T11:00:00"},{"currentPeople":40,"recordedAt":"2026-10-09T12:00:00"},{"currentPeople":15,"recordedAt":"2026-10-09T13:00:00"}]}`. 최대 500개, 모두 SIMULATION으로 저장, 오류가 있으면 전체 롤백. 과거 입장·퇴장 시나리오를 시각별 인원으로 입력합니다. 자동 주기 생성은 없습니다.
- `GET /api/admin/cafeterias/{id}/occupancy/history?date=2026-10-09`: 한국 날짜 기준 하루 기록을 시각·ID 순으로 조회. date 생략 시 오늘.

저장 응답: `{"snapshotId":31,"cafeteriaId":1,"peopleCount":42,"source":"CLOVA","recordedAt":"2026-10-09T12:00:00"}`. 시뮬레이션 일괄 응답과 history는 이 객체의 배열입니다.

`GET /api/cafeterias/{id}/status` 및 관리자 현재 인원 대시보드는 최신 스냅샷을 300초 동안 사용하고, 없거나 오래되면 기존 주문 기반 인원으로 대체합니다. 같은 시각이면 ID가 큰 기록을 사용하고 미래 기록은 사용하지 않습니다. 대기 인원은 기존 주문 기반을 유지합니다. 이용률과 혼잡도 계산·응답 형식은 유지됩니다. 과거 시뮬레이션은 현재 인원을 덮어쓰지 않습니다(유효기간 밖일 때). 기존 시간대별 주문 통계 API는 그대로이며, 스냅샷 기록은 history로 조회합니다.

설정: `.env`의 `CLOVA_API_KEY`에 발급받은 CLOVA Studio API 키를 입력하고 서버를 재시작합니다. 별도 플레이그라운드 작업 ID 없이 v3 HCX-005 API에 시스템 프롬프트와 Base64 이미지를 함께 전송합니다. `CLOVA_ENDPOINT`, `CLOVA_TIMEOUT_SECONDS`(기본 60초), `OCCUPANCY_SNAPSHOT_MAX_AGE_SECONDS`(기본 300초)로 조정합니다. 키·이미지·원시 응답은 로그에 기록하지 않습니다.

오류: 키 미설정 503 CLOVA_NOT_CONFIGURED, 호출 실패/타임아웃/잘못된 JSON/분석 불가능/잘린 응답 502 ANALYSIS_FAILED. 실패하면 스냅샷을 저장하지 않으며 인원을 0으로 덮어쓰지 않습니다. 잘못된 이미지 400 INVALID_IMAGE, 용량 초과 413 IMAGE_TOO_LARGE, 치수·인원·미래 시각·출처 위반 400 VALIDATION_FAILED, 없는 식당 404 CAFETERIA_NOT_FOUND.

운영: 전체 식사 공간이 보이는 사진을 사용해야 합니다. 부분 사진과 사각지대는 식당 전체 인원을 보장하지 않습니다. 실제 카메라 각도의 수동 집계와 비교해 정확도를 검증해야 합니다. 호출 주기는 수집기에서 관리하며, 재시도에 따른 중복 비용을 막기 위해 서버 자동 재시도는 없습니다. CCTV 전용 키 인증은 아직 제공하지 않습니다.

스키마: `usage_snapshots.source` varchar(20), NOT NULL, 기본값 MANUAL 추가. 로컬은 ddl-auto=update, 운영에서는 배포 전에 스키마 변경이 필요합니다.

공식 호출 규격: https://api.ncloud-docs.com/docs/clovastudio-chatcompletionsv3

## 9. 개발 참고

- 로컬 개발에서 인증 메일을 보내지 않으려면 `MAIL_ENABLED=false`로 실행합니다 (인증 코드가 서버 로그에 출력됨).
- 서버 기동 시 `ADMIN_STUDENT_NUMBER` / `ADMIN_PASSWORD` 환경변수의 관리자 계정이 없으면 자동 생성됩니다.
- 환경변수 목록은 `.env.example`을 참고하세요.
- **Redis 요구사항**: 로그인 세션은 "사용자별 조회가 가능한(indexed)" 형태로 저장됩니다 (`spring.session.data.redis.repository-type: indexed`). 비밀번호 변경·재설정 때 다른 기기의 세션을 종료하려는 용도입니다. 요청 제한 카운터와 인증 코드도 같은 Redis를 사용합니다. 세션 만료 정리를 위해 Redis의 keyspace 알림(`notify-keyspace-events`)이 켜져 있어야 하며, 관리형 Redis에서 `CONFIG` 명령이 막혀 있으면 인프라 설정으로 켜야 합니다.
- **배포(ALB) 시 IP 제한**: 로그인 IP 제한은 `request.getRemoteAddr()` 기준입니다. ALB 뒤에서는 클라이언트 IP가 `X-Forwarded-For`로 오므로 `server.forward-headers-strategy` 설정이 필요합니다 (설정 전에는 모든 사용자가 ALB의 IP 하나로 보입니다).
- **프론트 로컬 개발**: Vite 기본 포트는 `5173`인데 서버의 CORS 기본값은 `http://localhost:3000`입니다. `.env`의 `CORS_ALLOWED_ORIGINS`를 `http://localhost:5173`으로 바꾸거나 Vite 프록시(`/api` → 백엔드)를 사용하세요. 요청에는 항상 `credentials: "include"`(axios `withCredentials: true`)가 필요합니다.

## 관리자 이미지 업로드 및 가게 수정

ADMIN 세션 쿠키가 필요합니다.

- `POST /api/admin/menus/{menuId}/image`
- `POST /api/admin/stores/{storeId}/image`

`multipart/form-data`의 `file` 필드로 파일 한 개를 전송합니다. JPEG/PNG/WebP, 최대 2MiB(2,097,152바이트), 최대 2천만 픽셀입니다. 확장자와 요청 Content-Type 대신 실제 파일을 디코딩해 검사합니다. 성공 시 200과 수정된 메뉴/가게 정보(`imageUrl` 포함)를 반환합니다.

### 이미지 업로드 성공 응답 (200 OK)

메뉴 업로드는 `MenuAdminResponse`를 반환합니다.

```json
{
  "id": 10,
  "storeId": 2,
  "name": "김치찌개",
  "price": 5500,
  "imageUrl": "https://images.example.com/images/menus/uuid.jpg",
  "available": true
}
```

가게 업로드는 `StoreAdminResponse`를 반환합니다.

```json
{
  "id": 2,
  "cafeteriaId": 1,
  "name": "한식",
  "description": "오늘의 식사",
  "category": "한식",
  "avgWaitMinutes": 3,
  "imageUrl": "https://images.example.com/images/stores/uuid.png"
}
```

| 응답 | 필드 |
|---|---|
| 메뉴 이미지 업로드 | id, storeId, name, price, imageUrl, available |
| 가게 이미지 업로드·PUT 수정 | id, cafeteriaId, name, description, category, avgWaitMinutes, imageUrl |
| 가게 상세 조회·PATCH 수정 | 위 가게 필드 + minPrice, representativeMenuName |

가게 이미지 업로드 응답에는 `minPrice`·`representativeMenuName`이 없습니다. `description`·`category`는 null일 수 있습니다. 예시 이미지 URL은 설명용이며 실제 URL은 저장소 설정에 따라 달라집니다.

### 프론트 업로드 예시

```js
const body = new FormData();
body.append('file', selectedFile);
const response = await fetch(`/api/admin/menus/${menuId}/image`, {
  method: 'POST', credentials: 'include', body
}); // Content-Type은 브라우저가 boundary와 함께 설정
```

`PUT /api/admin/stores/{storeId}`는 아래 JSON으로 이름·설명·분류·평균 대기시간을 수정합니다. 이미지 값은 유지됩니다. name과 avgWaitMinutes는 필수이며 description/category 생략 또는 null은 해당 값을 비웁니다.

```json
{"name":"한식", "description":"오늘의 식사", "category":"한식", "avgWaitMinutes":3}
```

name 최대 100자(공백만 불가), description 최대 200자, category 최대 30자, avgWaitMinutes 0 이상. 응답: id, cafeteriaId, name, description, category, avgWaitMinutes, imageUrl.

오류: 400 INVALID_IMAGE/VALIDATION_FAILED, 413 IMAGE_TOO_LARGE, 404 MENU_NOT_FOUND/STORE_NOT_FOUND, 503 STORAGE_NOT_CONFIGURED, 502 STORAGE_FAILED. 로그인 없음 401, 일반 사용자 403.

### Object Storage 설정

`.env.example`의 OBJECT_STORAGE_* 값을 `.env`/배포 환경에 설정하고 OBJECT_STORAGE_ENABLED=true로 활성화합니다. 키는 서버에만 보관합니다. 한국 endpoint는 https://kr.object.ncloudstorage.com, region은 kr-standard입니다. 버킷은 사전에 생성하고 서버 키에 해당 버킷의 업로드/삭제 권한을 부여합니다. 이미지 조회는 공개 읽기 가능한 `images/` 경로 또는 CDN을 구성하고 OBJECT_STORAGE_PUBLIC_BASE_URL을 그 경로의 기본 URL로 지정합니다(예: https://kr.object.ncloudstorage.com/my-bucket). 서버는 객체 ACL을 변경하지 않습니다. 기본 URL 최대 400자.

저장소 미설정 시에도 서버와 기존 URL 방식은 동작합니다. 업로드 이미지는 UUID 기반 키와 URL을 DB에 저장합니다(image_object_key 컬럼 추가, 현재 ddl-auto=update 적용; 운영에서는 스키마 변경 필요). 외부 URL 이미지는 삭제하지 않습니다. 교체는 DB 커밋 후 이전 객체 삭제, 롤백 시 새 객체 삭제로 처리하며 같은 메뉴/가게의 수정을 DB 잠금으로 직렬화합니다. 기존 메뉴 수정 API에서 imageUrl을 바꾸거나 null로 비워도 이전 업로드 객체를 정리합니다.

저장소 삭제 실패는 DB 저장을 되돌리지 않고 객체 키를 ERROR 로그에 남깁니다. 실패한 삭제는 해당 키로 수동 재시도해야 하며 자동 재시도 작업은 포함하지 않습니다. 스토리지 키/버킷을 변경할 때 기존 객체를 먼저 이관해야 합니다.

## 확정 정책 및 테스트 충전 (2026-10-09)

최신 정책은 [서비스 결정 사항](service-decisions.md)을 따릅니다. 신규 가입 마일리지는 0P, 충전은 토스 테스트 결제창으로만 수행합니다. 먼저 `POST /api/mileage/charge/prepare`에 `{ "amount":10000 }`을 보내 clientKey, customerKey, orderId, amount를 받고 결제창을 호출합니다. 성공 후 기존 confirm API를 호출합니다. `/test-charge.html`에서 전체 흐름을 테스트할 수 있습니다.

도착 인증 전 직접 취소와 주문 후 1시간 미인증 자동 취소는 모두 100% 마일리지 환불입니다. 날짜가 바뀌어도 주문 후 1시간 기준을 적용합니다. 매분 만료 주문을 처리하고 조회 시에도 동기화합니다. 기존 혼잡도·식사 시간 정책은 유지합니다. 새 `mileage_charges` 테이블이 필요합니다(로컬 ddl-auto=update, 운영 스키마 변경 필요).
