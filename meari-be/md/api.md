### 공통 응답 구조


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