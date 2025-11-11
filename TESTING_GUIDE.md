# API 테스트 가이드

이 가이드는 `curl`을 사용하여 회원가입, 로그인, 로그아웃 등 주요 인증 API를 테스트하는 방법을 안내합니다.

**사전 준비:**
- 애플리케이션이 `localhost:8060`에서 실행 중이어야 합니다.
- `jq`가 설치되어 있으면 JSON 응답을 더 쉽게 파싱할 수 있습니다. (선택 사항)

---

### 1단계: 휴대폰 인증번호 발송

- **API:** `POST /api/v1/auth/signup/send-sms`
- **설명:** 본인 확인 정보를 담아 SMS 인증번호를 요청합니다. 성공 시 `verificationId`를 반환합니다.

**요청:**
```bash
curl -X POST http://localhost:8060/api/v1/auth/signup/send-sms \
-H "Content-Type: application/json" \
-d '{
  "name": "홍길동",
  "birth": "2000-01-01",
  "gender": "MALE",
  "phoneNum": "01012345678"
}' | jq
```

**성공 응답:**
```json
{
  "isSuccess": true,
  "code": 200,
  "message": "SUCCESS!",
  "result": {
    "verificationId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
  }
}
```
- **`verificationId` 값을 다음 단계에서 사용하기 위해 복사해두세요.**
- **실제 SMS 발송은 CoolSMS API 키가 설정되어 있어야 합니다. 테스트 중에는 SMS가 발송되지 않더라도 Redis에 인증 정보가 저장되므로 다음 단계를 진행할 수 있습니다. Redis에서 `GET <verificationId>`로 인증 코드를 확인할 수 있습니다.**

---

### 2단계: 휴대폰 인증번호 확인

- **API:** `POST /api/v1/auth/signup/check-code`
- **설명:** 발급받은 `verificationId`와 SMS로 받은 인증 코드를 검증합니다.

**요청:**
- `verificationId`를 1단계에서 받은 값으로 교체하세요.
- `authCode`는 Redis에서 확인한 값 또는 SMS로 받은 값을 입력하세요. (기본 6자리 숫자)

```bash
# 'YOUR_VERIFICATION_ID'와 '123456'을 실제 값으로 변경하세요.
curl -X POST http://localhost:8060/api/v1/auth/signup/check-code \
-H "Content-Type: application/json" \
-d '{
  "verificationId": "YOUR_VERIFICATION_ID",
  "authCode": "123456"
}' | jq
```

**성공 응답:**
```json
{
  "isSuccess": true,
  "code": 200,
  "message": "SUCCESS!",
  "result": "인증번호가 일치합니다."
}
```

---

### 3단계: 아이디 중복 검사

- **API:** `GET /api/v1/auth/signup/check-login-id`
- **설명:** 사용할 아이디가 중복되는지 확인합니다.

**요청 (사용 가능한 아이디):**
```bash
curl -X GET "http://localhost:8060/api/v1/auth/signup/check-login-id?loginId=newuser123" | jq
```

**성공 응답:**
```json
{
  "isSuccess": true,
  "code": 200,
  "message": "SUCCESS!",
  "result": "사용 가능한 아이디입니다."
}
```

---

### 4단계: 최종 회원가입

- **API:** `POST /api/v1/auth/signup/submit`
- **설명:** 모든 정보를 종합하여 최종 회원가입을 완료합니다.

**요청:**
- `verificationId`를 1단계에서 받은 값으로 교체하세요.

```bash
# 'YOUR_VERIFICATION_ID'를 실제 값으로 변경하세요.
curl -X POST http://localhost:8060/api/v1/auth/signup/submit \
-H "Content-Type: application/json" \
-d '{
  "verificationId": "YOUR_VERIFICATION_ID",
  "termAgreements": [
    {"termId": 1, "isAgreed": true},
    {"termId": 2, "isAgreed": true},
    {"termId": 3, "isAgreed": false}
  ],
  "loginId": "newuser123",
  "password": "mySecurePassword123!",
  "passwordConfirm": "mySecurePassword123!",
  "financialPropensity": "공격투자형",
  "keywordIds": [6, 11, 15]
}' | jq
```

**성공 응답:**
```json
{
  "isSuccess": true,
  "code": 201,
  "message": "SUCCESS!",
  "result": "회원가입이 완료되었습니다."
}
```

---

### 5단계: 로그인

- **API:** `POST /api/v1/auth/login`
- **설명:** 회원가입한 아이디와 비밀번호로 로그인하여 Access Token과 Refresh Token을 발급받습니다.

**요청:**
```bash
curl -X POST http://localhost:8060/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{
  "loginId": "newuser123",
  "password": "mySecurePassword123!"
}' | jq
```

**성공 응답:**
```json
{
  "isSuccess": true,
  "code": 200,
  "message": "SUCCESS!",
  "result": {
    "grantType": "Bearer",
    "accessToken": "ey...",
    "refreshToken": "ey..."
  }
}
```
- **`accessToken`과 `refreshToken` 값을 다음 단계에서 사용하기 위해 복사해두세요.**

---

### 6단계: 로그아웃

- **API:** `POST /api/v1/auth/logout`
- **설명:** Refresh Token을 사용하여 로그아웃을 요청하고, 해당 토큰을 블랙리스트에 추가합니다.

**요청:**
- `Authorization` 헤더에 5단계에서 발급받은 `refreshToken`을 `Bearer ` 접두사와 함께 넣어주세요.

```bash
# 'YOUR_REFRESH_TOKEN'을 실제 값으로 변경하세요.
curl -X POST http://localhost:8060/api/v1/auth/logout \
-H "Authorization: Bearer YOUR_REFRESH_TOKEN" | jq
```

**성공 응답:**
```json
{
  "isSuccess": true,
  "code": 200,
  "message": "SUCCESS!",
  "result": "로그아웃이 완료되었습니다."
}
```

**로그아웃 확인:**
- 동일한 `refreshToken`으로 다시 로그아웃을 시도하면 오류가 발생해야 합니다.

```bash
curl -X POST http://localhost:8060/api/v1/auth/logout \
-H "Authorization: Bearer YOUR_REFRESH_TOKEN" | jq
```

**오류 응답:**
```json
{
  "isSuccess": false,
  "code": 400,
  "message": "AUTH_011",
  "result": "이미 로그아웃된 토큰입니다."
}
```
