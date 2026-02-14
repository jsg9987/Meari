
<br>
<br>

<div align="center">

![logo-dark-2.svg](logo-dark-2.svg)

<br>
<br>

> # MEARI (메아리)
> ### 숏폼 콘텐츠 기반 그룹 쉐도잉 종합 플랫폼
>**MEARI**는 이주민을 위한 한국어 말하기 연습 플랫폼입니다.  
> 생생한 표현이 담긴 컨텐츠를 친구들과 함께 연기하며 학습하는  
> 즐거운 학습경험을 제공합니다.


</div>

<br>
<br>

---
<div align="center">

## 📆 목차

<br><br>

**[프로젝트 개요](#프로젝트-개요)**

**[주요 기능](#주요-기능)**

**[기술 스택](#-기술-스택)**

**[시스템 아키텍처](#시스템-아키텍처)**

**[ERD](#ERD)**
</div>
<br>
<br><br>
<br>

---

<div align="center">

## 👥 팀 구성
<br>

| 이름  | 역할              | 담당 기능                               | GitHub                                              |
|-----|-----------------|-------------------------------------|-----------------------------------------------------|
| 김선엽 | Frontend        | 프로젝트 리드, 대시보드 및 메인 기능 기획 · 개발       | [@shipleaf](https://github.com/shipleaf)            |
| 국민혁 | Frontend        | 인증, 메인 기능 기획 · 개발                   | [@meanhg](https://github.com/meanhg)                |
| 전석균 | Backend / AI / Infra | CI/CD 및 인프라, AI 음성 분석 로직, WebSocket 기능 기획 · 개발 | [@jsg9987](https://github.com/jsg9987)              |
| 김희수 | Backend         | 인증/회원, WebSocket 기능 기획 · 개발         | [@kayaaaaaaaaaaa](https://github.com/kayaaaaaaaaaaa) |
| 하은지 | AI              | 데이터 관리, 음성 분석 모델 파인튜닝               | [@EunByu1](https://github.com/EunByu1)                     |
| 장상민 | Infra / Backend | CI/CD 및 인프라, WebRTC 기능 기획 · 개발            | [@thermoCat](https://github.com/thermoCat)          |

<br>
<br><br>
<br>

---

## 📄 프로젝트 개요
<br>

**MEARI**는 이주민을 위한 한국어 말하기 연습 플랫폼입니다.

**필요에 따라 선택하는 다양한 AI 숏폼 드라마**,  
**WebRTC 기반 실시간 그룹 쉐도잉**,  
**다양한 상황에 대처하는 KOPIC**,  
**AI 음성 분석을 통한 학습 리포트**,  
**지속적인 학습을 유도하는 일일학습** 

생생한 표현이 담긴 컨텐츠를 보고 친구들과 함께 연기하며 학습하는  
즐거운 학습경험을 제공합니다. 

</div>
<br>
<br>


![랜딩페이지 (1).png](%EB%9E%9C%EB%94%A9%ED%8E%98%EC%9D%B4%EC%A7%80%20%281%29.png)
![랜딩페이지 (2).png](%EB%9E%9C%EB%94%A9%ED%8E%98%EC%9D%B4%EC%A7%80%20%282%29.png)
![랜딩페이지 (4).png](%EB%9E%9C%EB%94%A9%ED%8E%98%EC%9D%B4%EC%A7%80%20%284%29.png)
![랜딩페이지 (3).png](%EB%9E%9C%EB%94%A9%ED%8E%98%EC%9D%B4%EC%A7%80%20%283%29.png)
![랜딩페이지 (5).png](%EB%9E%9C%EB%94%A9%ED%8E%98%EC%9D%B4%EC%A7%80%20%285%29.png)
![랜딩페이지 (6).png](%EB%9E%9C%EB%94%A9%ED%8E%98%EC%9D%B4%EC%A7%80%20%286%29.png)
![랜딩페이지 (7).png](%EB%9E%9C%EB%94%A9%ED%8E%98%EC%9D%B4%EC%A7%80%20%287%29.png)


<br>
<br>

### **1. 최대 4인 역할 기반 동시 쉐도잉으로 실제 상황 시뮬레이션**
  - 다인 간 대화 영상을 기반으로 상황과 맥락, 뉘앙스까지 학습
  - 실생활 기반 컨텐츠 구성으로, 나의 환경과 필요에 맞는 컨텐츠를 선택하여 학습

![쉐도잉_멤버참여.png](%EC%89%90%EB%8F%84%EC%9E%89_%EB%A9%A4%EB%B2%84%EC%B0%B8%EC%97%AC.png)
![쉐도잉_녹음중알림.png](%EC%89%90%EB%8F%84%EC%9E%89_%EB%85%B9%EC%9D%8C%EC%A4%91%EC%95%8C%EB%A6%BC.png)
<br><br><br>

### **2. 시간과 장소에 구애받지 않는 원격 그룹 학습**
  - 거주 국가, 지역, 시간에 관계 없이 원할 때 언제나 학습을 시작할 수 있는 실시간 스터디룸
  - 화상통화와 실시간 채팅으로,  원활한 의사소통 가능

![메인페이지.png](%EB%A9%94%EC%9D%B8%ED%8E%98%EC%9D%B4%EC%A7%80.png)
![쉐도잉_장치설정.png](%EC%89%90%EB%8F%84%EC%9E%89_%EC%9E%A5%EC%B9%98%EC%84%A4%EC%A0%95.png)
![쉐도잉_실시간채팅.png](%EC%89%90%EB%8F%84%EC%9E%89_%EC%8B%A4%EC%8B%9C%EA%B0%84%EC%B1%84%ED%8C%85.png)
<br><br><br>

### **3. 주어진 상황에 대한 언어적 대처능력 평가 및 학습 서비스 코픽**
- 상황에 따른 문맥과 뉘앙스, 표현적 적절성을 평가하고, 개선점을 도출

![코픽_녹음전.png](%EC%BD%94%ED%94%BD_%EB%85%B9%EC%9D%8C%EC%A0%84.png)
![코픽_녹음중.png](%EC%BD%94%ED%94%BD_%EB%85%B9%EC%9D%8C%EC%A4%91.png)
![코픽_문항별_피드백_생성중.png](%EC%BD%94%ED%94%BD_%EB%AC%B8%ED%95%AD%EB%B3%84_%ED%94%BC%EB%93%9C%EB%B0%B1_%EC%83%9D%EC%84%B1%EC%A4%91.png)
![코픽_리포트생성_완료.png](%EC%BD%94%ED%94%BD_%EB%A6%AC%ED%8F%AC%ED%8A%B8%EC%83%9D%EC%84%B1_%EC%99%84%EB%A3%8C.png)

<br><br><br>

### **4. AI 기반 발음, 억양, 정확도, 내용 적절성 피드백 리포트**
  - 기능적, 내용적 피드백을 제공하는 쉐도잉 리포트와 코픽 리포트

![리포트 생성 알림.png](%EB%A6%AC%ED%8F%AC%ED%8A%B8%20%EC%83%9D%EC%84%B1%20%EC%95%8C%EB%A6%BC.png)
![마이페이지_리포트목록.png](%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_%EB%A6%AC%ED%8F%AC%ED%8A%B8%EB%AA%A9%EB%A1%9D.png)
![마이페이지_리포트1_ 전체점수.png](%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_%EB%A6%AC%ED%8F%AC%ED%8A%B81_%20%EC%A0%84%EC%B2%B4%EC%A0%90%EC%88%98.png)
![마이페이지_리포트2_문장별점수.png](%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_%EB%A6%AC%ED%8F%AC%ED%8A%B82_%EB%AC%B8%EC%9E%A5%EB%B3%84%EC%A0%90%EC%88%98.png)
![마이페이지_리포트3_오류율.png](%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_%EB%A6%AC%ED%8F%AC%ED%8A%B83_%EC%98%A4%EB%A5%98%EC%9C%A8.png)
![마이페이지_리포트6_문장음절별지표.png](%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_%EB%A6%AC%ED%8F%AC%ED%8A%B86_%EB%AC%B8%EC%9E%A5%EC%9D%8C%EC%A0%88%EB%B3%84%EC%A7%80%ED%91%9C.png)
<br><br><br>


### **5. 어휘와 문법을 보충해주는 일일학습 과제**
  - 발화의 기반이 되는 문법과 어휘의 꾸준한 학습 독려

![단어학습.png](%EB%8B%A8%EC%96%B4%ED%95%99%EC%8A%B5.png)
![단어학습_완료.png](%EB%8B%A8%EC%96%B4%ED%95%99%EC%8A%B5_%EC%99%84%EB%A3%8C.png)
![문장순서맞추기.png](%EB%AC%B8%EC%9E%A5%EC%88%9C%EC%84%9C%EB%A7%9E%EC%B6%94%EA%B8%B0.png)
![문장순서맞추기_오답.png](%EB%AC%B8%EC%9E%A5%EC%88%9C%EC%84%9C%EB%A7%9E%EC%B6%94%EA%B8%B0_%EC%98%A4%EB%8B%B5.png)
![일일학습내역.png](%EC%9D%BC%EC%9D%BC%ED%95%99%EC%8A%B5%EB%82%B4%EC%97%AD.png)

<br><br><br>

### **6. 기간별 학습통계를 나타내는 대시보드**
  - 일별 학습 기록 통합 통합 조회로 성장 과정 확인


![마이페이지_대시보드1.png](%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_%EB%8C%80%EC%8B%9C%EB%B3%B4%EB%93%9C1.png)
![마이페이지_대시보드2.png](%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_%EB%8C%80%EC%8B%9C%EB%B3%B4%EB%93%9C2.png)


<br>
<br><br>
<br>

---

<div align="center">

## 🛠️ 기술 스택
<br>

### ⚙️ Backend

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![WebSocket](https://img.shields.io/badge/STOMP_WebSocket-010101?style=for-the-badge&logo=socketdotio&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

### 🗄️ Database & Infrastructure

![PostgreSQL](https://img.shields.io/badge/PostgreSQL_15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Cloudinary](https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white)

### 🔐 인증 & 보안

![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

### 🧠 AI & 미디어

![KOMORAN](https://img.shields.io/badge/KOMORAN_3.3.4-0078D4?style=for-the-badge&logoColor=white)
![WebRTC](https://img.shields.io/badge/OpenVidu_WebRTC-333333?style=for-the-badge&logo=webrtc&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-007808?style=for-the-badge&logo=ffmpeg&logoColor=white)

### 🖥️ Frontend

![React](https://img.shields.io/badge/React_19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript_5.9-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS_4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand_5-443E38?style=for-the-badge&logoColor=white)
![React Query](https://img.shields.io/badge/React_Query_5-FF4154?style=for-the-badge&logo=reactquery&logoColor=white)
![React Router](https://img.shields.io/badge/React_Router_7-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white)
![Axios](https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white)
![Recharts](https://img.shields.io/badge/Recharts-22B5BF?style=for-the-badge&logoColor=white)
![STOMP.js](https://img.shields.io/badge/STOMP.js-010101?style=for-the-badge&logo=socketdotio&logoColor=white)
![pnpm](https://img.shields.io/badge/pnpm-F69220?style=for-the-badge&logo=pnpm&logoColor=white)

### 🔁 테스트 & 문서화

![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![Mockito](https://img.shields.io/badge/Mockito-78A641?style=for-the-badge&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

### 🔧 Environment

![VS Code](https://img.shields.io/badge/VS_Code-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white)
![GitLab](https://img.shields.io/badge/GitLab-FC6D26?style=for-the-badge&logo=gitlab&logoColor=white)

### 💬 Communication

![Jira](https://img.shields.io/badge/Jira-0052CC?style=for-the-badge&logo=jira&logoColor=white)   ![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)

</div>

<br>
<br>

---

<div align="center">

## 💡 주요 기능
<br>
</div>





### 1. 회원 가입 및 인증
- JWT 기반 인증 시스템 (Access Token + Refresh Token)
- Spring Security를 통한 안전한 회원 관리
- Redis 세션 관리 및 PostgreSQL 데이터 저장

### 2. 실시간 협력 학습 (그룹 섀도잉)
- OpenVidu 2.30.0 기반 WebRTC 최대 4인 화상 통화
- STOMP WebSocket으로 실시간 역할 선택, 준비 상태, 채팅 동기화
- 라운드별 녹화 및 게임 진행 상태 관리 (WAITING → WATCHING → ROLE_PICK → ROUND_1 → ROUND_2)
- 테마별 방 생성 및 빠른 입장 (공개방/비밀방)
- FFmpeg를 이용한 동영상 처리 및 AWS S3 저장

### 3. 혼자 연습 모드
- 다른 사람 없이 혼자서 섀도잉 연습 가능
- 원하는 콘텐츠와 역할 자유 선택
- 라운드별 녹음 및 음성 분석 요청

### 4. 섀도잉 콘텐츠 관리
- 테마별 콘텐츠 관리 (드라마, 영화 등 실생활 기반)
- 문장 단위 학습: 한국어/베트남어 번역, 타이밍 정보, 역할(Role) 할당
- 최대 4개 역할 지원

### 5. AI 기반 음성 분석 (비동기)
- RabbitMQ 기반 비동기 처리 (Producer/Consumer 패턴)
- FastAPI + Wav2Vec2 기반 발음 정확도 분석 (0~100점)
- Parselmouth + DTW 기반 억양 유사도 분석 (0~100점)
- 음절 단위 상세 피드백 (오류 위치, 유형, 신뢰도)
- S3 자동 오디오 다운로드 및 결과 PostgreSQL 저장

### 6. KOPIC 언어 평가 시스템
- Gemini 2.5-flash 기반 상황별 언어 대처 능력 평가
- 5개 문항 랜덤 출제 (30초 타이머)
- 문맥, 뉘앙스, 표현 적절성 종합 평가
- 개별 문항 평가 + 통합 리포트 생성

### 7. 학습 리포트
- **섀도잉 리포트**: 라운드별 정확도, 억양, 음절별 상세 분석 (JSONB)
- **KOPIC 리포트**: 문항별 점수, AI 피드백, 종합 평가
- 커서 기반 페이징 및 날짜별 필터링
- Recharts 기반 차트 시각화

### 8. 학습 대시보드
- 일일 학습 기록 (GitHub 잔디 형식 히트맵)
- 최근 활동 피드 (섀도잉, KOPIC, 일일 학습 통합)
- 평균 정확도/억양 점수 추이 그래프
- Recharts + Framer Motion 애니메이션

### 9. 일일 학습
- **단어 학습**: 랜덤 단어 10문항 (다지선다형)
- **문장 순서 맞추기**: 드래그 앤 드롭 퀴즈
- 완료 시 대시보드 기록
<br>
<br><br>
<br>

---
<div align="center">


## 🗃️ 시스템 아키텍처


</div>
<br>

![meari_architecture.png](meari_architecture.png)

<br>

<br>
<br>

---
<div align="center">


## 🗺️ ERD

<br>
</div>

![meari_erd.png](meari_erd.png)
<br>
<br><br>
<br>

---

<div align="center">

<br>

### 라이센스

<br>
SSAFY(삼성 청년 소프트웨어 아카데미) 14기 공통 프로젝트

**C207 Meari**

</div>

<br>
<br>

---

**마지막 업데이트**: 2026-02-15
