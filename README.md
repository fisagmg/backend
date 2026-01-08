# CVEXPERT - Backend 💻

> CVEXPERT 서비스의 백엔드 애플리케이션입니다.

## 1. 주요 기능

- 🔐 **Keycloak 기반 인증/인가**: OAuth2 Resource Server를 통한 JWT 토큰 기반 인증 및 사용자 관리
- 🖥️ **자동화된 Lab 환경 생성**: Terraform Runner를 통한 AWS EC2 인스턴스 자동 프로비저닝
- 🌐 **웹 기반 원격 접속**: Apache Guacamole을 통한 브라우저 기반 SSH 접속 (별도 클라이언트 불필요)
- ⏱️ **실습 시간 관리**: 초기 TTL, 최대 TTL, 시간 연장 기능 및 만료된 세션 자동 종료
- 📊 **CVE 정보 조회**: 도메인, 연도, OS별 필터링 및 CVSS Score 기반 정렬
- 📰 **CVE 뉴스 크롤링**: 네이버 뉴스 API를 통한 CVE 관련 뉴스 자동 수집 및 CVE 자동 매핑
- 📝 **보고서 관리**: S3 기반 보고서 업로드/다운로드 및 버전 관리
- 👤 **마이페이지**: 완료한 CVE 목록, 프로필 관리
- 👨‍💼 **관리자 기능**: Lab 세션 모니터링, AWS CloudWatch 메트릭 조회

---

## 3. 기술 스택 및 선정 이유

## ⚙️ Backend

| 기술 | 선정 이유 |
| :--- | :--- |
| ![Java](https://img.shields.io/badge/Java_17-007396?logo=openjdk&logoColor=white) | CVE 실습 환경 관리, 세션 제어, 외부 인프라 연동 등 복잡한 비즈니스 로직을 안정적으로 처리하기 위해 LTS 기반의 Java 17을 선택했습니다. |
| ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3.5-6DB33F?logo=springboot&logoColor=white) | 인증, 인프라 연동, 스케줄러 등 다양한 기능을 빠르게 구성해야 하는 프로젝트 특성상 자동 설정과 풍부한 생태계를 제공하는 Spring Boot가 적합했습니다. |
| ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white) | Keycloak과 연동되는 OAuth2 Resource Server 구조를 통해 인증 책임을 분리하고, 백엔드는 토큰 검증과 권한 기반 접근 제어에 집중하도록 설계하기 위해 사용했습니다. |
| ![Keycloak](https://img.shields.io/badge/Keycloak-4D4D4D?logo=keycloak&logoColor=white) | 사용자 인증과 권한 관리를 애플리케이션 로직과 분리하여 중앙 집중식으로 관리하기 위해 도입했습니다. 실습 서비스 특성상 사용자 식별과 접근 제어가 중요해 검증된 오픈소스 IDM 솔루션을 선택했습니다. |
| ![JPA](https://img.shields.io/badge/Spring_Data_JPA-59666C?logo=hibernate&logoColor=white) | Lab, CVE, Report 등 도메인 중심의 데이터 모델을 빠르게 설계하고 비즈니스 로직에 집중하기 위해 사용했습니다. |

---

## 🗄️ Database & Storage

| 기술 | 선정 이유 |
| :--- | :--- |
| ![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white) | 사용자 정보, 실습 세션, 보고서 메타데이터 등 관계형 데이터의 정합성이 중요한 서비스 특성상 안정적인 트랜잭션 처리가 가능한 RDBMS를 선택했습니다. |
| ![Amazon S3](https://img.shields.io/badge/Amazon_S3-232F3E?logo=amazonaws&logoColor=white) | 실습 결과 보고서 파일을 안전하게 저장하고, Presigned URL을 통해 인증된 사용자만 접근하도록 하기 위해 사용했습니다. |

---

## 🔗 Infrastructure & Integration

| 기술 | 선정 이유 |
| :--- | :--- |
| ![Apache Guacamole](https://img.shields.io/badge/Apache_Guacamole-4A7C59) | 사용자가 별도 클라이언트 설치 없이 웹 브라우저만으로 실습 환경에 접근할 수 있도록 하기 위해 도입했습니다. |
| ![Terraform](https://img.shields.io/badge/Terraform-7B42BC?logo=terraform&logoColor=white) | CVE별 실습 환경을 수동으로 관리하는 것은 운영상 불가능하다고 판단하여 Infrastructure as Code 방식으로 실습 환경 생성·삭제를 자동화하기 위해 사용했습니다. |
| ![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?logo=amazonaws&logoColor=white) | 실습 요청 시점에 따라 인프라가 동적으로 생성·삭제되어야 하는 특성상 확장성과 자동화에 유리한 클라우드 환경을 확보하였습니다. |
| ![CloudWatch](https://img.shields.io/badge/AWS_CloudWatch-FF4F8B?logo=amazoncloudwatch&logoColor=white) | 실습 환경과 서비스 인스턴스의 상태를 운영자가 실시간으로 파악할 수 있도록 메트릭 기반 모니터링을 구성했습니다. |

---

## 🧪 Test & API Tools

| 기술 | 선정 이유 |
| :--- | :--- |
| ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=black) | 프론트엔드 및 Terraform Runner와의 협업을 위해 API 명세를 명확히 공유할 필요가 있어 자동 문서화를 도입했습니다. |
| ![Postman](https://img.shields.io/badge/Postman-FF6C37?logo=postman&logoColor=white) | 초기 API 테스트 및 시나리오 검증에 사용하여 실제 동작을 빠르게 확인했습니다. |

<br>

---

## 4. 프로젝트 구조

본 프로젝트는 **Domain-Oriented Feature-Based Structure**(도메인 중심 기능 구조)를 채택했습니다.

```
cve-labhub-backend/
├── build.gradle
└── src
    ├── main
    │   └── java/com/labhub/CveLabhubBack
    │       ├── auth              # 인증/인가 (Keycloak 연동, 회원가입, 로그인, OTP)
    │       ├── cve                # CVE 정보 조회 및 필터링
    │       ├── cve_lab            # Lab 세션 관리 (생성, 삭제, 시간 관리, Guacamole 연동)
    │       ├── config             # 전역 설정 (RestClient, CORS 등)
    │       ├── configForSwagger  # Swagger 설정
    │       ├── lab_admin         # 관리자용 Lab 모니터링 (CloudWatch 메트릭)
    │       ├── mypage            # 마이페이지 (프로필, 완료한 CVE 목록)
    │       ├── news              # CVE 뉴스 크롤링 (네이버 뉴스 API)
    │       └── report           # 보고서 관리 (S3 업로드/다운로드)
    └── test                     # 단위·통합 테스트
```

> 각 도메인 패키지는 공통적으로 controller, service, repository, entity, dto 하위 구조를 따릅니다.

---

## 5. 주요 API 엔드포인트

#### 🔐 인증

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/otp/send` | 이메일 인증번호 발송 |
| POST | `/api/v1/auth/otp/verify` | 이메일 인증번호 검증 |
| DELETE | `/api/v1/auth/withdraw` | 회원 탈퇴 |

#### 🔍 CVE

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/v1/cve` | CVE 목록 조회 |

#### 🖥️ Lab Session

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| POST | `/api/v1/labs/create` | Lab 실습 환경 생성 |
| POST | `/api/v1/labs/destroy` | Lab 실습 환경 삭제 |
| POST | `/api/v1/labs/{uuid}/complete` | 실습 완료 처리 |
| GET | `/api/v1/labs/{uuid}/remaining-time` | 실습 잔여시간 조회 |
| GET | `/api/v1/labs/{uuid}/extendable` | 시간 연장 가능 여부 확인 |
| POST | `/api/v1/labs/{uuid}/extend` | 실습 시간 연장 |

#### 📝 Report

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| POST | `/api/v1/reports` | 보고서 생성 |
| PUT | `/api/v1/reports/{id}/file` | 보고서 파일 업로드 |
| GET | `/api/v1/reports/me` | 내 보고서 목록 조회 |
| GET | `/api/v1/reports/{id}` | 보고서 상세 조회 |
| GET | `/api/v1/reports/{id}/download` | 보고서 다운로드 URL 생성 |
| GET | `/api/v1/reports/cve/{cveId}` | CVE ID로 보고서 조회 |
| DELETE | `/api/v1/reports/{id}` | 보고서 삭제 |

#### 📰 News

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/v1/news/top` | 최신 뉴스 조회 |
| GET | `/api/v1/news` | 전체 뉴스 조회 |
| POST | `/api/v1/news/crawl` | 뉴스 크롤링 |

#### 👤 Mypage

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/v1/mypage/me` | 사용자 정보 조회 |
| PUT | `/api/v1/mypage/me` | 사용자 정보 수정 |

#### 👨‍💼 Admin

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/v1/admin/labs` | Lab 목록 조회 |
| GET | `/api/v1/admin/labs/{labUuid}` | Lab 상세 조회 |
| GET | `/api/v1/admin/labs/{labUuid}/metrics` | Lab 메트릭 조회 |

---

## 6. ERD

<img width="1216" height="794" alt="image" src="https://github.com/user-attachments/assets/e4f24c77-6003-4240-a264-3e21b79bdbb6" />

### 주요 테이블

| 테이블명 | 설명 |
|:---|:---|
| `users` | 사용자 정보 |
| `cve` | CVE 정보 |
| `lab` | Lab 세션 정보 |
| `report` | 보고서 정보 |
| `news` | CVE 관련 뉴스 |
| `cve_news_mapping` | CVE와 뉴스 매핑 |
| `done_cve` | 사용자가 완료한 CVE 목록 |

### 데이터베이스 구성

- **MySQL**: 메인 데이터베이스
- **Guacamole MySQL**: Guacamole 전용 데이터베이스

---

## 7. 메인 로직

### 7-1. Lab 세션 생성 플로우

1. **사용자 요청**: CVE 이름을 포함한 Lab 생성 요청
2. **Terraform Runner 호출**: VM 프로비저닝 요청 (UUID, CVE 이름, 사용자 ID 전달)
3. **DB 저장**: Lab 엔티티 생성 및 저장 (ACTIVE 상태, 만료 시간 설정)
4. **Guacamole 연결 생성**: 
   - Guacamole Admin 토큰 획득
   - Connection 생성 (SSH 프로토콜, VM IP/포트 설정)
   - Connection ID를 Lab 엔티티에 저장
5. **Iframe URL 생성**: 사용자 토큰 기반 접속 URL 생성
6. **응답 반환**: Lab 정보 및 Guacamole 접속 URL 반환

### 7.2 실습 시간 관리

| 항목 | 내용 |
|:---|:---|
| **초기 TTL** | Lab 생성 시 기본 제공 시간 (기본값: 60분) |
| **최대 TTL** | 한 세션에서 사용 가능한 최대 시간 (기본값: 120분) |
| **연장 단위** | 한 번에 연장 가능한 시간 (기본값: 30분) |
| **자동 종료** | 만료된 세션을 주기적으로 조회하여 VM 삭제 및 상태 업데이트 |

### 7-3. 뉴스 크롤링 스케줄러

| 항목 | 내용 |
|:---|:---|
| **실행 주기** | 3시간마다 자동 실행 (`@Scheduled(cron = "0 0 */3 * * *")`) |
| **크롤링 키워드** | "CVE-2025", "CVE-2024", "CVE-2023" |
| **CVE 자동 매핑** | 뉴스 제목/내용에서 CVE ID 패턴을 추출하여 자동 매핑 |
| **중복 방지** | `external_url` 기준으로 중복 뉴스 저장 방지 |

### 7-4. 보고서 관리

| 항목 | 내용 |
|:---|:---|
| **템플릿 기반 생성** | S3에 저장된 템플릿 파일을 복제하여 새 보고서 생성 |
| **S3 업로드** | 사용자가 작성한 .docx 파일을 S3에 업로드 |
| **Presigned URL** | 다운로드 시 임시 URL 생성 (보안 강화) |
| **버전 관리** | 보고서 업로드 시 버전 자동 증가 |
| **Soft Delete** | 삭제 시 `status`를 "deleted"로 변경, `deleted_at` 설정 |

### 7-5. Guacamole 연동

| 항목 | 내용 |
|:---|:---|
| **연결 방식** | Guacamole REST API를 통한 Connection 생성/삭제 |
| **인증 방식** | Admin 계정 토큰 기반 인증 |
| **프로토콜** | SSH (추후 VNC, RDP 확장 가능) |
| **Iframe URL** | Base64 인코딩된 clientId와 토큰을 포함한 접속 URL 생성 |

---

## 8. 트러블슈팅

CVEXPERT 백엔드를 개발하며 마주한 주요 이슈와 해결 과정을 정리했습니다.  
**서비스 안정성과 운영에 직접적인 영향을 주었던 문제들만 선별**했습니다.

<br>

<details>
<summary><strong>1️⃣ JWT Subject(userId) 타입 혼용 문제</strong></summary>

**문제**

- 일부 토큰은 `JWT subject`가 문자열(userMadeId), 일부는 숫자(userId)로 생성됨
- 인증 필터 및 비즈니스 로직에서 `Long.valueOf()` 변환 오류 발생

**원인**

- 인증 구조 설계 초기에 subject 용도가 명확히 정의되지 않음
- 사용자 식별 값이 토큰마다 다르게 사용됨

**해결**

- JWT subject를 **Long 타입 userId로 고정**
- 문자열 식별자(userMadeId)는 claim으로 분리
- UserDetails 기반 인증 로직과 userId 기반 도메인 로직을 명확히 분리

**결과**

- 인증 관련 500 오류 완전 제거
- 토큰 구조 표준화로 인증 로직 안정성 확보

<br>
</details>

<details>
<summary><strong>2️⃣ Guacamole Connection 생성 실패 (500 Error)</strong></summary>

**문제**

- VM 생성 이후 Guacamole Connection 생성 단계에서 간헐적 500 오류 발생
- Connection ID가 정상 반환되지 않아 실습 접속 실패

**원인**

- Guacamole Admin AuthToken 발급 시점과 Connection 생성 요청 간 동기화 문제
- Connection 생성 요청 파라미터 검증 부족

**해결**

- Admin AuthToken 발급 로직을 동기화 처리
- Connection 생성 요청 파라미터 검증 강화
- 실패 시 원인 파악을 위한 상세 로깅 추가
- Connection ID 추출 로직 안정화

**결과**

- Guacamole Connection 생성 성공률 향상
- 실습 접속 실패 이슈 대폭 감소

<br>
</details>

<details>
<summary><strong>3️⃣ Guacamole iframe URL / clientId 생성 오류</strong></summary>

**문제**

- iframe 접속 시 인증 루프 발생
- Guacamole 화면이 정상 로드되지 않음

**원인**

- 잘못된 엔드포인트(`/api/tunnels`) 호출
- connectionId를 그대로 clientId로 사용
- Base64 clientId 규칙 미준수
- 만료된 토큰 재사용

**해결**

- connectionId + datasource 기반 **표준 Base64 clientId 생성**
- iframe URL을 `/guacamole/#/client/{clientId}?token={freshToken}` 구조로 통일
- iframe 로드 직전 항상 최신 토큰 발급
- attributes null 전달 방지 처리

**결과**

- iframe 접속 안정화
- 인증 루프 및 404 오류 완전 제거

<br>
</details>

<details>
<summary><strong>4️⃣ Terraform Runner 응답 타임아웃 문제</strong></summary>

**문제**

- VM 생성 요청 시 Terraform Runner 응답 대기 중 타임아웃 발생
- 대용량 인스턴스 생성 시 실습 환경 생성 실패

**원인**

- Spring `RestTemplate` 기본 타임아웃 값이 Terraform 실행 시간 대비 너무 짧음

**해결**

- Terraform Runner 전용 RestTemplate Bean 분리
- 연결 타임아웃 10초, 읽기 타임아웃 5분으로 확장
- 필요 시 재시도 가능한 구조로 설계

**결과**

- VM 생성 성공률 향상
- 실습 환경 생성 과정의 안정성 확보

<br>
</details>

<details>
<summary><strong>5️⃣ Lab 세션 만료 후 리소스 정리 누락</strong></summary>

**문제**

- 실습 제한 시간은 종료되었지만, 예외 상황에서 VM과 Guacamole 연결이 남는 케이스 발생
- 장애·에러 발생 시 리소스가 정상 정리되지 않을 가능성 존재

**원인**

- 종료 이벤트 기반 처리만 존재하고, 보정 로직이 없음

**해결**

- 스케줄러를 통해 만료 시간이 지난 Lab 세션을 주기적으로 점검
- 만료 세션에 대해 VM 삭제 및 Guacamole 연결 정리 자동 수행
- DB 상태를 TERMINATED로 업데이트하여 중복 처리 방지

**결과**

- 예외 상황에서도 리소스가 자동 회수되는 안정적인 구조 확보
- 운영자 개입 없이 비용 및 리소스 관리 가능

<br>
</details>

<details>
<summary><strong>6️⃣ CloudWatch 로그 조회 실패 (events=[] 반환)</strong></summary>

**문제**

- CloudWatch 콘솔에서는 로그가 정상 수집되지만
- 백엔드 API 조회 결과는 항상 빈 배열 반환

**원인**

- CloudWatch Agent 로그 그룹·스트림 구조와
  백엔드 조회 로직의 경로 규칙 불일치

**해결**

- Agent 로그 구조(`/aws/ec2/cve-lab/{instanceId}/{suffix}`)에 맞게
  백엔드 조회 로직 수정
- 로그 스트림 및 suffix 규칙 통일
- 로그 구조를 환경 변수 기반으로 관리

**결과**

- 관리자 화면에서 실습 로그 정상 조회
- 운영 가시성 및 장애 분석 효율 향상

<br>
</details>

