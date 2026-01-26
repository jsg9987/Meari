### 공통 응답 구조
```
{ "success": true, "data": null, "error": null }
```

# 인증 및 계정 관리
1.1 일반 회원가입
자체 서비스 계정으로 회원가입을 진행합니다.
Method: POST
URL: /api/v1/members/signup
Request Body:
```
{
  "email": "user@gmail.com",
  "password": "password123",
  "nickname": "김씨",
  "native_language": "베트남어"
}
```
참고: 초기 가입 시 프로필 사진 등록 기능은 제외되었으며, 마이페이지에서 별도로 수정해야 합니다.

## 1.2 로그인
이메일과 비밀번호를 통해 인증 토큰을 발급받습니다.
Method: POST
URL: /api/v1/members/login
Request Body: email, password
Response Header: refresh token
Response Body: access token 포함
Error: * 404 USER NOT FOUND: 존재하지 않는 이메일
401 PASSWORD MISMATCH: 비밀번호 불일치

## 1.3 로그아웃 및 회원탈퇴
로그아웃 (POST /api/v1/members/logout): 서버의 인증 상태를 해제합니다.
회원탈퇴 (DELETE /api/v1/members/delete): 계정을 삭제하며, 기존 데이터는 복구되지 않습니다.

## 2. 정보 조회 및 확인
### 2.1 이메일 중복확인
회원가입 전 이메일 사용 가능 여부를 확인합니다.
Method: GET
URL: /api/v1/members/email/check
Query Parameter: email=test@ssafy.com
Response Body: { "has_email": true/false }

### 2.2 내 정보 불러오기 (헤더용)
현재 로그인한 사용자의 프로필 정보를 조회합니다.
Method: GET
URL: /api/v1/members/me
Response Body:
```
{
  "success": true,
  "data": {
    "email": "user@gmail.com",
    "nickname": "닉네임",
    "profile_url": "http://..."
  }
}
```



# 컨텐츠 API 명세서

모든 API는 Bearer Token 인증이 필요

## 1. 테마 및 영상 목록 관리
1.1 테마 목록 조회
서비스에서 제공하는 학습 테마(식당, 공항, 비즈니스 등)의 전체 목록을 조회합니다.
Method: GET
URL: /api/v1/contents/themes
Response Body:
```
{
  "success": true,
  "data": [
    {
      "theme_id": 1,
      "name": "식당",
      "description": "식당에서 사용하는 표현 학습",
      "theme_url": "https://.../images/theme1.png"
    },
    {
      "theme_id": 2,
      "name": "비즈니스",
      "description": "업무 관련 회화 학습",
      "theme_url": "https://.../images/theme2.png"
    }
  ],
  "error": null
}
```

## 1.2 테마별 영상 목록 조회
특정 테마에 속한 영상 리스트를 조회합니다.
Method: GET
URL: /api/v1/contents/{theme_id}
Response Body:
```
{
  "success": true,
  "data": [
    {
      "content_id": 101,
      "title": "카페에서 아메리카노 주문하기",
      "thumbnail_url": "https://cdn.../thumb/cafe1.jpg",
      "max_people": 2
    }
  ],
  "error": null
}
```

## 2. 영상 상세 및 메타데이터
### 2.1 동영상 상세 정보 조회
영상 재생에 필요한 URL과 기본 메타데이터를 조회합니다.
Method: GET
URL: /api/v1/contents/{content_id}/video
Response Body:
```
{
  "success": true,
  "data": {
    "content_id": 101,
    "title": "카페에서 주문하기",
    "video_url": "https://d1234567.cloudfront.net/videos/cafe_101.mp4",
    "thumbnail_url": "https://d1234567.cloudfront.net/thumbs/cafe_101.jpg",
    "max_people": 2,
    "total_duration": 125.5
  },
  "error": null
}
- Error: 404 NOT_FOUND_CONTENT (존재하지 않는 콘텐츠 ID)
```

### 2.2 캐릭터(역할) 목록 조회
특정 영상에 정의된 모든 캐릭터의 이름과 ID를 조회합니다.
Method: GET
URL: /api/v1/contents/{content_id}/roles
Response Body:
```
{
  "success": true,
  "data": [
    { "role_id": 1, "content_id": 101, "name": "점원" },
    { "role_id": 2, "content_id": 101, "name": "손님" }
  ],
  "error": null
}
```
Error: 404 NOT_FOUND_CONTENT (존재하지 않는 콘텐츠 ID)

## 3. 학습 스크립트 및 단어
### 3.1 스크립트 상세 조회
영상 전체 대사, 타임스탬프, 역할 정보를 포함한 전체 스크립트를 조회합니다.
Method: GET
URL: /api/v1/contents/{content_id}/sentences
Response Body:

```
{
  "success": true,
  "data": {
    "content_id": 101,
    "video_url": "https://cdn.../videos/lesson1.mp4",
    "roles": [
      { "role_id": 1, "role_name": "점원" },
      { "role_id": 2, "role_name": "손님" }
    ],
    "sentences": [
      {
        "sentence_id": 501,
        "role_id": 1,
        "sequence": 1,
        "text_ko": "어서오세요. 주문 도와드릴까요?",
        "text_vn": "Bạn muốn gọi món không?",
        "start_time": 1.5,
        "end_time": 4.2
      }
    ]
  },
  "error": null
}
```

### 3.2 단어 조회
특정 문장에 포함된 주요 단어 및 뜻풀이를 조회합니다.
Method: GET
URL: /api/v1/contents/{sentence_id}/words
Response Body:
```
{
  "success": true,
  "data": [
    {
      "sentence_id": 1,
      "sequence": 1,
      "speaker_role": "USER",
      "text": "주문하시겠어요?",
      "text_vn": "Bạn muốn gọi món không?"
    }
  ],
  "error": null
}
```

### 3.3 문장 순서 맞추기 퀴즈 조회
문장 복습을 위한 섞인 단어 배열과 정답 순서 정보를 조회합니다.
Method: GET
URL: /api/v1/contents/{sentence_id}/quiz
Response Body:
```
{
  "success": true,
  "data": [
    {
      "script_id": 501,
      "shuffled_words": ["까요", "어서오세요", "주문"],
      "correct_order": [1, 2, 3]
    }
  ],
  "error": null
}
```

# 마이페이지 API
## 1. 회원 정보 조회 및 수정

### 1.1 마이페이지 정보 조회
현재 로그인한 사용자의 닉네임, 이메일, 프로필 이미지, 모국어 정보를 조회합니다.
Method: GET
URL: /api/v1/mypage
Response Body:
```
{
  "success": true,
  "data": {
    "nickname": "사용자닉네임",
    "email": "user@example.com",
    "profile_image_url": "https://cdn.example.com/profile/default.png",
    "native_language": "베트남"
  },
  "error": null
}
```

### 1.2 회원 정보 수정
사용자의 닉네임 및 모국어 정보를 수정합니다.
Method: PATCH
URL: /api/v1/mypage/change
Request Body:
```
{
  "nickname": "새닉네임",
  "native_language": "수정할언어"
}
```

## 2. 보안 관리 (비밀번호)
### 2.1 비밀번호 확인
민감한 정보 수정 전, 사용자가 입력한 현재 비밀번호가 일치하는지 확인합니다.
Method: POST
URL: /api/v1/members/check-password
Request Body:
```
{
  "password": "사용자가 입력한 비밀번호"
}
```
Response Body: 
```
{ "success": true, "data": null, "error": null }
```

### 2.2 비밀번호 수정
기존 비밀번호를 새 비밀번호로 변경합니다.
Method: PATCH
URL: /api/v1/mypage/change/pw
Request Body:
```
{
  "new_password": "새비밀번호1234"
}
```

Response Body: 
```
{ "success": true, "data": null, "error": null }
```

# 쉐도잉 방 API 명세서
## 1. 방 관리 (Room Management)
### 1.1 방 목록 검색
테마별로 생성된 쉐도잉 방 목록을 무한 스크롤(커서 기반 페이징) 방식으로 조회합니다.
Method: GET
URL: /api/v1/rooms?theme_id={theme_id}&cursor={cursor_id}&size={size}
Response Body:

```
{
  "success": true,
  "data": {
    "contents": [
      {
        "room_id": 105,
        "title": "대화 해요",
        "current_people": 1,
        "max_people": 2,
        "content_title": "영상 제목",
        "has_password": true,
        "created_at": "2026-01-19T23:44:56"
      }
    ],
    "next_cursor": 105,
    "has_next": true
  }
}
```

### 1.2 방 생성하기
선택한 영상 콘텐츠를 기반으로 새로운 학습 방을 생성합니다. 생성한 유저는 자동으로 방장이 됩니다.

Method: POST
URL: /api/v1/rooms
Request Body: { "title": "방 제목", "password": "1234", "max_people": 2 }
Response Body: room_id, owner_id, content_id, is_active("ACTIVE") 등 반환

### 1.3 방 입장 권한 확인 및 퇴장
- 입장 권한 확인 (POST /api/v1/rooms/{room_id}/enter): 시그널링 연결 전 비밀번호 및 정원을 확인합니다.
Error: 403 INVALID PASSWORD (비밀번호 불일치), 409 ROOM FULL (정원 초과)

- 방 퇴장 (DELETE /api/v1/rooms/{room_id}/leave): 유저가 방을 나갈 때 정보를 정리합니다.

## 2. 방 내부 학습 액션
### 2.1 캐릭터(역할) 선택
학습에 참여한 유저가 자신의 캐릭터(role_id)를 선점형으로 선택합니다. 상태는 Redis에 반영됩니다.

Method: PATCH
Request Body: { "role_id": 1 }
Server Logic: Redis에서 선택된 역할을 확인하며, 이미 선점된 경우 409 Conflict를 반환합니다. 성공 시 WebSocket으로 브로드캐스팅됩니다.
Error: 409 ROLE ALREADY TAKEN

### 2.2 게임 시작
방장이 모든 준비가 완료된 후 학습(시청 및 녹음 세션)을 시작합니다.
Method: POST
URL: /api/v1/rooms/{room_id}/start
Response Body: 
```
{
    "success": true,
    "data": {
        [
            { "member_id": 1, "role_id": 1, "is_system": false }, // 유저A가 녹음
            { "member_id": null, "role_id": 2, "is_system": true } // 시스템이 재생
        ]
    },
    "error": null
}
```

### 3.1 동영상 시청
### 기본정보

| 메서드 | URL | 인증방식 |
| --- | --- | --- |
| POST | /api/v1/contents/{content_id}/video_info | 인증 O |

### Response Body

```json
{
    "success": true,
    "data": {
        "content_id": 101,
        "title": "카페에서 주문하기",
        "video_url": "https://s3.ap-northeast-2.amazonaws.com/your-bucket/videos/cafe_101.mp4",
        "total_duration": 125.500, // 초 단위 전체 길이 (밀리초 포함)
        "thumbnail_url": "https://s3.ap-northeast-2.amazonaws.com/your-bucket/thumbs/cafe_101.jpg"
    },
    "error": null
}
```

### 3.2 음성 업로드용 Presigned URL 발급

### 기본정보

| 메서드 | URL | 인증방식 |
| --- | --- | --- |
| POST | /api/v1/s3/presigned-url | 인증 인가 O |

# Request Header

| 헤더 이름 | 값 | 설명 | 필수 |
| --- | --- | --- | --- |
| Content-Type | application/json |  | O |
| Authorization | Bearer {Token} | Access Token | O |

# Request Body

```java
{
    "file_name": "shadowing_101_1.wav",
    "content_type": "audio/wav"
}
```

# Response Header

# Response Body

```json
{
    "success": true,
    "data": {
        "presigned_url": "https://s3.ap-northeast-2.amazonaws.com/...",
        "audio_url": "https://s3.ap-northeast-2.amazonaws.com/bucket/path/shadowing_101_1.wav"
    },
    "error": null
}
```


# 쉐도잉 API 
### 1. 개별 쉐도잉 분석 요청
### 기본정보

| 메서드 | URL | 인증방식 |
| --- | --- | --- |
| POST | /api/v1/shadowing/presigned-url | 인증 인가 O |

# Request Body

```java
{
    "sentence_id": 501,
    "audio_url": "https://s3.ap-northeast-2.amazonaws.com/..."
}
```

# Response Body

```json
{
    "success": true,
    "data": null,
    "error": null
}
```

# 리포트 API 

### 1. 개별 쉐도잉 리포트 조회
### 기본정보

| 메서드 | URL | 인증방식 |
| --- | --- | --- |
| POST | /api/v1/shadowing/reports/{shadowing_report_id} | 인증 O |


# Request Body

# Response Header

# Response Body

```json
[Request Header]
Authorization: Bearer {Token}

[Response Body]
{
    "success": true,
    "data": {
        "shadowing_report_id": 1001,
        "sentence_id": 501,
        "audio_url": "...",
        "accuracy": 85,
        "intonation": 90,
        "accuracy_detail": {
            "answer_phonemes": [...], // 정답 글자 배열
            "predict_phonemes": [...], // AI 예측 글자 배열(화자 발음)
            "predict_probability": [...] // 정답 예측 확률 배열
        },
        "intonation_detail": {
		        "answer_intonation": [...],
****		        "member_intonation": [...],
		        "alignment_path": [[0,0], [1,1], [2,1]...] // dtw로 매핑한 글자 간 위치
        }
    },
    "error": null
}
```

### 2. 개별 코픽 리포트 조회

응답 형식
```java
{
    "success": true,
    "data": {
        "shadowing_report_id": 1001,
        "member_id": 1,
        "sentence_id": 501,
        "text_ko": "오늘 점심 메뉴는 뭐예요?",
        "audio_url": "https://s3.../audio_1001.wav",
        "accuracy": 85,
        "intonation": 80,
        "status": "COMPLETED",
        "detailed_analysis": {
            "missed_point": "과거형(먹었어)과 희망사항(싶어요)이 합쳐져서 어색해요.",
            "correction": "비빔밥 먹고 싶어요.",
            "tip": "~고 싶다 앞에는 항상 동사의 기본형에서 '다'를 뺀 형태가 와야 해요!"
        }
    },
    "error": null
}
```

# 🌐 웹소켓 API 명세 요약

```
1. 방 상태 및 참여 관리
사용자의 접속 상태와 캐릭터 선점 현황을 실시간으로 모든 참여자에게 공유합니다.
2. 기능,관련 경로 (Send/Subscribe),주요 이벤트 (Type)

참여 및 레디 상태,Sub: /topic/room/{room_id}/state ,"USER_ENTER, USER_LEAVE, USER_READY "

역할 선점 알림,Sub: /topic/room/{room_id}/state ,ROLE_ASSIGNED
특이사항: 유저가 캐릭터를 선택하면 Redis 상태를 확인한 후 성공 시 ROLE_ASSIGNED 메시지를 브로드캐스팅합니다.
2. 영상 제어 및 학습 진행
방장이 영상 재생을 제어하면 모든 유저의 화면이 동기화되며, 시스템이 말하기 차례를 안내합니다. 


영상 재생 동기화 (Sync Video): 재생, 일시정지, 시점 이동 시 모든 플레이어의 시점을 강제로 맞춥니다.


경로: Send: /app/room/{room_id}/video-sync / Sub: /topic/room/{room_id}/video-sync 


데이터: current_time 및 재생 상태(PLAY, PAUSE 등) 


문장/턴 시작 알림: 다음 문장으로 넘어갈 때 누가 말할 차례인지 서버가 알립니다.


경로: Sub: /topic/room/{room_id}/turn 


데이터: sentence_id, role_id, 문장 시작/종료 시간 정보 

3. 통신 및 연결 (WebRTC & Chat)
실시간 미디어 연결을 위한 정보 교환과 유저 간 텍스트 소통을 지원합니다. 


WebRTC 시그널링: P2P 또는 미디어 서버 연동을 위한 SDP 및 ICE Candidate를 교환합니다.


경로: Send: /app/room/{room_id}/signal / Sub: /topic/room/{room_id}/signal 


실시간 채팅: 학습 중 유저 간의 자유로운 텍스트 메시지를 전달합니다.


경로: Send: /app/room/{room_id}/chat / Sub: /topic/room/{room_id}/chat
```
