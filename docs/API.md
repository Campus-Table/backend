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

### 에러 코드 목록

| code | HTTP | message |
|---|---|---|
| `INVALID_REQUEST` | 400 | 잘못된 요청입니다. |
| `VALIDATION_FAILED` | 400 | (입력 항목별 안내 문구) |
| `UNAUTHORIZED` | 401 | 로그인이 필요합니다. |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. |
| `INTERNAL_ERROR` | 500 | 서버 오류가 발생했습니다. |
| `INVALID_CREDENTIALS` | 401 | 학번 또는 비밀번호가 올바르지 않습니다. |
| `DUPLICATE_STUDENT_NUMBER` | 409 | 이미 가입된 학번입니다. |
| `MAIL_SEND_FAILED` | 500 | 인증 메일 발송에 실패했습니다. |
| `EMAIL_NOT_VERIFIED` | 400 | 이메일 인증이 필요합니다. |
| `INVALID_VERIFICATION_CODE` | 400 | 인증 코드가 올바르지 않거나 만료되었습니다. |
| `USER_NOT_FOUND` | 404 | 해당 사용자를 찾을 수 없습니다. |
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

## 2. API 목록

| 분류 | Method | URL | 권한 | 설명 |
|---|---|---|---|---|
| Auth | POST | `/api/auth/email/send` | Public | 학번 학교 메일로 인증 코드 발송 |
| Auth | POST | `/api/auth/email/verify` | Public | 인증 코드 확인 |
| Auth | POST | `/api/auth/signup` | Public | 회원가입 |
| Auth | POST | `/api/auth/login` | Public | 로그인 |
| Auth | POST | `/api/auth/logout` | Public | 로그아웃 |
| User | GET | `/api/users/me` | 로그인 | 내 정보 |
| Cafeteria | GET | `/api/cafeterias` | 로그인 | 학식당 목록 |
| Cafeteria | GET | `/api/cafeterias/{cafeteriaId}` | 로그인 | 학식당 상세 |
| Cafeteria | GET | `/api/cafeterias/{cafeteriaId}/stores` | 로그인 | 학식당의 가게 목록 |
| Cafeteria | GET | `/api/cafeterias/{cafeteriaId}/status` | 로그인 | 혼잡도(이용/대기 인원) |
| Store | GET | `/api/stores/{storeId}` | 로그인 | 가게 상세 |
| Store | GET | `/api/stores/{storeId}/menus` | 로그인 | 가게 메뉴 목록 |
| Mileage | GET | `/api/mileage` | 로그인 | 마일리지 잔액 |
| Mileage | GET | `/api/mileage/transactions` | 로그인 | 마일리지 내역 |
| Mileage | POST | `/api/mileage/charge/confirm` | 로그인 | 결제 승인 후 충전 |
| Order | POST | `/api/orders` | 로그인 | 선주문 + 마일리지 결제 |
| Order | GET | `/api/orders/me` | 로그인 | 내 주문 목록 |
| Order | GET | `/api/orders/{orderId}` | 로그인 | 내 주문 상세 |
| Order | POST | `/api/orders/{orderId}/arrival` | 로그인 | 현장 번호 도착 인증 |
| Order | POST | `/api/orders/{orderId}/receive` | 로그인 | 음식 수령 |
| Order | POST | `/api/orders/{orderId}/cancel` | 로그인 | 주문 취소(도착 인증 전) |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/arrival-code` | ADMIN | 오늘의 현장 번호 |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/dashboard` | ADMIN | 이용·대기 인원, 이용률, 혼잡도 |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/queues` | ADMIN | 가게별 현재 대기번호 |
| Admin | GET | `/api/admin/cafeterias/{cafeteriaId}/usage/hourly` | ADMIN | 시간대별 이용 현황 |
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
- `diningMinutes`: 음식 수령 후 이용 종료로 간주하는 시간(분)
- `openingTime`, `closingTime`은 값이 없으면 `null`

### GET `/api/cafeterias/{cafeteriaId}` — 학식당 상세
응답 형식은 목록의 항목과 같습니다. 없으면 `404 CAFETERIA_NOT_FOUND`.

### GET `/api/cafeterias/{cafeteriaId}/stores` — 가게 목록
```json
[
  { "id": 1, "cafeteriaId": 1, "name": "Korean", "avgWaitMinutes": 2, "imageUrl": null },
  { "id": 2, "cafeteriaId": 1, "name": "Noodle", "avgWaitMinutes": 3, "imageUrl": null }
]
```
- `avgWaitMinutes`: 가게의 1인분 평균 대기시간(분). 예상 대기시간 계산에 사용됩니다.
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
계산 규칙 (주문 데이터에서 요청 시점에 실시간 계산)
- `currentPeople`: 음식을 수령했고 아직 이용 종료 시각(`수령 시각 + diningMinutes`) 전인 주문 수
- `waitingPeople`: 도착 인증 후 조리 중인(예상 완료 시각 전) 주문 수
- `usageRate`: `currentPeople / seatCount × 100` (소수 첫째 자리 반올림)
- `congestionLevel`: `RELAXED`(30% 미만) / `NORMAL`(60% 미만) / `CROWDED`(85% 미만) / `VERY_CROWDED`
  - ⚠️ 구간 기준은 **임시값**이며 확정 시 변경됩니다.
- `recordedAt`: 응답 생성 시각

### GET `/api/stores/{storeId}` — 가게 상세
```json
{ "id": 1, "cafeteriaId": 1, "name": "Korean", "avgWaitMinutes": 2, "imageUrl": null }
```
없으면 `404 STORE_NOT_FOUND`.

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
1) 프론트: 토스페이먼츠 결제창 호출 (클라이언트 키 사용, orderId는 프론트가 생성 — 6~64자)
2) 결제 완료 후 successUrl로 paymentKey, orderId, amount 전달됨
3) 프론트 → 이 API 호출 → 서버가 토스에 승인 요청 → 성공 시 마일리지 적립
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
- 토스가 승인을 거절하면 `400 PAYMENT_FAILED` (이미 처리된 결제를 다시 보내는 경우 포함)
- ⚠️ 승인은 토스 **테스트 키** 기준입니다. 실제 결제는 일어나지 않습니다.

---

## 6. Order

### 주문 상태

```text
PAID ──(현장 번호 인증)──▶ COOKING ──(예상 시간 경과)──▶ READY ──(수령)──▶ RECEIVED
 │
 └──(취소 / 다음 날 미인증)──▶ CANCELLED
```

| status | 의미 |
|---|---|
| `PAID` | 선주문 + 마일리지 결제 완료. 아직 조리 시작 전 |
| `COOKING` | 도착 인증 완료 → 대기번호 발급, 조리 시작 |
| `READY` | 예상 대기시간 경과. 수령 가능 (**시간이 지나면 자동으로 바뀝니다. 관리자가 변경하지 않음**) |
| `RECEIVED` | 음식 수령 완료 |
| `CANCELLED` | 취소됨 |

- 상태 전이는 서버가 **조회 시점에** 반영합니다. 프론트는 주문 상세를 주기적으로 조회(폴링)해서 `READY`가 되면 "수령 안내"를 띄우면 됩니다.
- **한 사용자는 진행 중인 주문(`PAID`/`COOKING`/`READY`)을 1개만** 가질 수 있습니다.
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
| `leaveAt` | 이용 종료 예정 시각 (수령 시각 + `diningMinutes`). 수령 후에만 값이 있음 |
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

### POST `/api/orders/{orderId}/receive` — 음식 수령

Request body 없음. Response — `200 OK` (주문 응답 형식, `status: "RECEIVED"`)
- `READY` 상태일 때만 가능합니다. 수령하면 현재 이용 인원에 반영되고, `leaveAt` 시각이 지나면 자동으로 이용 종료로 처리됩니다 (사용자가 "식사 완료"를 누르지 않음).

| 에러 | 상황 |
|---|---|
| `INVALID_ORDER_STATUS` | 아직 조리 중이거나 이미 수령/취소됨 |

### POST `/api/orders/{orderId}/cancel` — 주문 취소

Request body 없음. Response — `200 OK` (주문 응답 형식, `status: "CANCELLED"`)
- **도착 인증 전(`PAID`)에만** 취소할 수 있습니다.
- 결제 금액의 **50%만 환불**됩니다 (`REFUND` 내역 생성, 소수점 이하 버림).
- 그날 도착 인증을 하지 않은 주문은 **다음 날 조회 시 자동으로 `CANCELLED` 처리되고 50%가 환불**됩니다.

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

### GET `/api/admin/cafeterias/{cafeteriaId}/dashboard` — 현황
```json
{
  "currentPeople": 1,
  "waitingPeople": 1,
  "seatCount": 100,
  "usageRate": 1.0,
  "congestionLevel": "RELAXED"
}
```
계산 규칙은 `GET /api/cafeterias/{id}/status`와 같습니다.

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
  { "hour": 0, "orderCount": 0, "arrivalCount": 0, "receivedCount": 0 },
  { "hour": 12, "orderCount": 3, "arrivalCount": 2, "receivedCount": 1 }
]
```
- 항상 0~23시 24개 항목. 각 시간대에 주문한 수 / 도착 인증한 수 / 수령한 수입니다.

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
- 최신 주문이 먼저 옵니다. 조리 완료 예상 시각이 지난 주문은 `READY`로 표시됩니다.
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

회의록 기준으로 아직 구현되지 않았거나 확정되지 않은 항목입니다. 구현 전에 이 문서에 먼저 추가합니다.

- 예상 시간 경과 후 **수령 안내 알림** (현재는 프론트 폴링으로 `READY` 확인)
- 메뉴 이미지 업로드 (NAVER Cloud Object Storage)
- 시뮬레이션 이용 데이터(시간대별 이용 인원) 주입
- 혼잡도 구간 확정
- 식사 시간(`diningMinutes`)을 가게/메뉴별로 다르게 할지 여부
- 가게 정보(평균 대기시간 등) 관리자 수정
- 학식당 현장 번호의 관리자 재발급

## 9. 개발 참고

- 로컬 개발에서 인증 메일을 보내지 않으려면 `MAIL_ENABLED=false`로 실행합니다 (인증 코드가 서버 로그에 출력됨).
- 서버 기동 시 `ADMIN_STUDENT_NUMBER` / `ADMIN_PASSWORD` 환경변수의 관리자 계정이 없으면 자동 생성됩니다.
- 환경변수 목록은 `.env.example`을 참고하세요.
