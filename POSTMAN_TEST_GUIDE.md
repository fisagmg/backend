# Postman API 테스트 가이드

## 1. API 엔드포인트 정보

- **URL**: `POST http://localhost:{SERVER_PORT}/labs`
- **인증**: JWT Bearer Token 필요 (Keycloak)
- **Content-Type**: `application/json`

## 2. JWT 토큰 발급 방법

### 방법 1: Keycloak에서 직접 토큰 발급

1. Keycloak 관리 콘솔 접속
2. Realm 선택 → Clients → 클라이언트 선택
3. **Service Account Roles** 탭에서 필요한 권한 부여
4. 다음 API로 토큰 발급:

```http
POST {KEYCLOAK_BASE_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id={KEYCLOAK_CLIENT_ID}
&client_secret={KEYCLOAK_CLIENT_SECRET}
```

**응답 예시:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer"
}
```

### 방법 2: 사용자 로그인으로 토큰 발급

```http
POST {KEYCLOAK_BASE_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
&client_id={KEYCLOAK_CLIENT_ID}
&client_secret={KEYCLOAK_CLIENT_SECRET}
&username={USERNAME}
&password={PASSWORD}
```

## 3. Postman 설정 방법

### Step 1: 새 Request 생성
1. Postman 열기
2. **New** → **HTTP Request** 클릭
3. Method를 **POST**로 선택
4. URL 입력: `http://localhost:{SERVER_PORT}/labs`
   - 예: `http://localhost:8080/labs`

### Step 2: Headers 설정
1. **Headers** 탭 클릭
2. 다음 헤더 추가:

| Key | Value |
|-----|-------|
| `Content-Type` | `application/json` |
| `Authorization` | `Bearer {JWT_TOKEN}` |

**참고**: `{JWT_TOKEN}` 부분을 위에서 발급받은 `access_token` 값으로 교체

### Step 3: Body 설정
1. **Body** 탭 클릭
2. **raw** 선택
3. **JSON** 형식 선택
4. 다음 JSON 입력:

```json
{
  "vmUuid": "test-uuid-123",
  "userId": "test-user-123",
  "cveId": "CVE-2021-44228"
}
```

### Step 4: 요청 전송
1. **Send** 버튼 클릭
2. 응답 확인

## 4. 예상 응답

### 성공 응답 (200 OK)
```json
{
  "vmUuid": "test-uuid-123",
  "userId": "test-user-123",
  "privateIp": "10.0.1.100",
  "hostname": "lab-test-user-123-CVE-2021-44228"
}
```

### 인증 실패 (401 Unauthorized)
```json
{
  "error": "unauthorized",
  "error_description": "Full authentication is required to access this resource"
}
```

### 잘못된 요청 (400 Bad Request)
```json
{
  "timestamp": "2024-01-01T00:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}
```

## 5. Postman Collection 예시

Postman에서 Collection을 만들어 재사용할 수 있습니다:

### Collection Variables 설정
1. Collection 우클릭 → **Edit**
2. **Variables** 탭에서 다음 변수 추가:

| Variable | Initial Value | Current Value |
|----------|---------------|---------------|
| `base_url` | `http://localhost:8080` | `http://localhost:8080` |
| `jwt_token` | (토큰 발급 후 수동 입력) | (토큰 발급 후 수동 입력) |

### Request 설정
- **URL**: `{{base_url}}/labs`
- **Authorization**: Type을 **Bearer Token**으로 선택하고, Token에 `{{jwt_token}}` 입력

## 6. 테스트 시 주의사항

1. **서버 실행 확인**: Spring Boot 애플리케이션이 실행 중인지 확인
2. **포트 확인**: `application.properties`의 `server.port` 값 확인
3. **Keycloak 설정 확인**: `.env` 파일에 Keycloak 설정이 올바른지 확인
4. **토큰 만료**: JWT 토큰은 만료 시간이 있으므로, 만료되면 새로 발급받아야 함
5. **Runner 서비스**: EC2 생성은 Runner 서비스가 실행 중이어야 함

## 7. 빠른 테스트를 위한 임시 설정 (개발용)

테스트를 쉽게 하려면 `SecurityConfig.java`에서 `/labs` 경로를 임시로 permitAll로 설정할 수 있습니다:

```java
.requestMatchers("/labs").permitAll()  // 임시: 테스트용
```

**주의**: 프로덕션에서는 반드시 제거해야 합니다!

## 8. cURL 예시

Postman 대신 cURL로도 테스트 가능:

```bash
curl -X POST http://localhost:8080/labs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{
    "vmUuid": "test-uuid-123",
    "userId": "test-user-123",
    "cveId": "CVE-2021-44228"
  }'
```


