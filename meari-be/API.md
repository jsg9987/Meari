# MEARI Backend API

MEARI 백엔드 REST API 및 WebSocket(STOMP) 엔드포인트 요약입니다.

## Base URL
- `/api/v1`

## 인증
- HTTP 요청 헤더: `Authorization: Bearer <ACCESS_TOKEN>`
- WebSocket(STOMP) CONNECT 헤더: `Authorization: Bearer <ACCESS_TOKEN>`

## 권한 구분
- 일반 사용자: `Auth`가 "필요"인 엔드포인트 호출 가능
- 관리자: `Admin` 섹션 엔드포인트 호출 가능

## 공통 응답 형식
```json
{
  "success": true,
  "data": {},
  "error": null
}
```

## 에러 응답 형식
```json
{
  "success": false,
  "data": null,
  "error": {
    "message": "<ERROR_MESSAGE>",
    "errorFields": {
      "field": "message"
    }
  }
}
```

## 에러 응답 규칙
- `error.message`는 표준화된 메시지 문자열
- `error.errorFields`는 유효성 검증 실패 시에만 포함

## 페이지네이션 규칙 (Cursor)
- `cursor`: 마지막 아이템의 기준값(예: `created_at`의 epoch ms)
- `size`: 페이지 크기 (기본값 10)
- 응답은 `CursorPageResponse<T>` 형태 (상세 필드는 구현체 기준)

## 멀티파트 요청 예시

### KOPIC 발화 분석 (`/kopic/evaluate`)
```http
POST /api/v1/kopic/evaluate
Content-Type: multipart/form-data
Authorization: Bearer <ACCESS_TOKEN>

request: {"kopicSentenceId": 1}
audio: <BINARY_AUDIO_FILE>
```

### 관리자 이미지 업로드 (`/admin/kopic-pictures/upload`)
```http
POST /api/v1/admin/kopic-pictures/upload
Content-Type: multipart/form-data
Authorization: Bearer <ACCESS_TOKEN>

file: <BINARY_IMAGE_FILE>
```

## REST API

### Auth
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/auth/refresh` | Access Token 재발급 | 없음 |
| POST | `/auth/logout` | 로그아웃 | 필요 |
| POST | `/auth/signup` | 회원가입 | 없음 |
| GET | `/auth/email/check` | 이메일 중복 확인 | 없음 |
| GET | `/auth/nickname/check` | 닉네임 중복 확인 | 없음 |
| DELETE | `/auth/delete` | 회원 탈퇴 | 필요 |

### Member
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/members/me` | 내 정보 조회 | 필요 |

### MyPage
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/mypage` | 마이페이지 조회 | 필요 |
| PATCH | `/mypage/change` | 마이페이지 수정 | 필요 |
| PATCH | `/mypage/change/pw` | 비밀번호 변경 | 필요 |
| POST | `/mypage/check-password` | 비밀번호 확인 | 필요 |

### Content
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/contents/themes` | 테마 목록 조회 | 없음 |
| GET | `/contents/{themeId}` | 테마별 콘텐츠 목록 | 없음 |
| GET | `/contents/{contentId}/roles` | 콘텐츠 역할 목록 | 없음 |
| GET | `/contents/quiz` | 문장 순서 맞추기 퀴즈 | 없음 |
| GET | `/contents/words/random` | 랜덤 단어 10개 | 없음 |

### KOPIC
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/contents` | KOPIC 문장 랜덤 조회 (`theme_id` 쿼리 필요) | 없음 |
| POST | `/kopic/total-report` | KOPIC 통합 리포트 생성 | 필요 |
| POST | `/kopic/evaluate` | KOPIC 발화 분석 요청 (multipart) | 필요 |
| GET | `/kopic/report/{reportId}` | KOPIC 리포트 조회 | 없음 |
| GET | `/kopic/total-report/{totalReportId}` | KOPIC 통합 리포트 조회 | 없음 |

### Room
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/rooms` | 방 생성 | 필요 |
| POST | `/rooms/quick` | 빠른 방 생성 | 필요 |
| GET | `/rooms` | 방 목록 조회 (cursor paging) | 없음 |
| GET | `/rooms/{roomId}` | 방 상세 조회 | 없음 |
| POST | `/rooms/{roomId}/enter` | 방 입장 | 필요 |
| DELETE | `/rooms/{roomId}/leave` | 방 퇴장 | 필요 |
| DELETE | `/rooms/{roomId}/members/{memberId}` | 멤버 강퇴 | 필요 |
| POST | `/rooms/{roomId}/content` | 콘텐츠 선택 | 필요 |
| POST | `/rooms/{roomId}/start` | 게임 시작 | 필요 |
| POST | `/rooms/{roomId}/watching/finish` | 영상 시청 완료 | 필요 |
| POST | `/rooms/{roomId}/roles/confirm` | 역할 확정 | 필요 |
| POST | `/rooms/{roomId}/rounds/start` | 라운드 시작 | 필요 |
| POST | `/rooms/{roomId}/rounds/{round}/finish` | 라운드 종료 | 필요 |
| POST | `/rooms/{roomId}/finish` | 게임 종료 | 필요 |

### WebRTC
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/rooms/{roomId}/webrtc/enter` | 방 입장(WebRTC 토큰 발급) | 필요 |
| DELETE | `/rooms/{roomId}/webrtc/leave` | 방 퇴장(WebRTC) | 필요 |

### S3
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/s3/presigned-url` | 녹음 업로드용 Presigned URL 발급 | 없음 |
| GET | `/s3/contents/{contentId}/video-url` | 콘텐츠 영상 Presigned URL 발급 | 없음 |

### Dashboard
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/dashboard/me/daily-records` | 일일 학습 기록 조회 | 필요 |
| POST | `/dashboard/me/learning-completion` | 학습 완료 기록 | 필요 |
| GET | `/dashboard/me/activities` | 최근 활동 조회 | 필요 |
| GET | `/dashboard/me/shadowing` | 최근 섀도잉 기록 | 필요 |
| GET | `/dashboard/me/kopic` | KOPIC 요약 조회 | 필요 |

### Report
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/reports/shadowing` | 섀도잉 리포트 목록 | 필요 |
| GET | `/reports/shadowing/{reportId}` | 섀도잉 리포트 상세 | 필요 |
| GET | `/reports/kopic` | KOPIC 통합 리포트 목록 | 필요 |

### Solo Practice
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/solo-practice/start` | 혼자 연습 시작 | 없음(코드 기준) |

### Admin (관리자 전용)
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/admin/scripts/upload` | 스크립트 CSV 업로드 | 필요(관리자) |
| POST | `/admin/kopic-pictures/upload` | KOPIC 문장 이미지 업로드 | 필요(관리자) |
| POST | `/admin/kopic-pictures/upload-multi` | KOPIC 이미지 zip 업로드 | 필요(관리자) |
| POST | `/admin/content/{contentId}/preprocess-audio` | 콘텐츠 오디오 전처리 | 필요(관리자) |

### Dev/Test
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/analysis/test/request` | 분석 요청 테스트 | 없음 |
| GET | `/nlp/morpheme-analysis` | 형태소 분석 | 없음 |
| POST | `/nlp/morpheme-analysis/batch` | 형태소 분석(배치) | 없음 |
| GET | `/nlp/extract-nouns` | 명사 추출 | 없음 |
| GET | `/nlp/extract-by-pos` | 품사별 단어 추출 | 없음 |
| POST | `/openvidu/sessions` | OpenVidu 세션 생성 | 없음 |
| POST | `/openvidu/sessions/{sessionId}/connections` | OpenVidu 연결 토큰 발급 | 없음 |
| GET | `/openvidu/sessions/{sessionId}` | OpenVidu 세션 정보 | 없음 |
| GET | `/openvidu/sessions` | OpenVidu 활성 세션 목록 | 없음 |
| DELETE | `/openvidu/sessions/{sessionId}` | OpenVidu 세션 종료 | 없음 |
| DELETE | `/openvidu/sessions/{sessionId}/connections/{connectionId}` | OpenVidu 연결 종료 | 없음 |

## WebSocket (STOMP)
- Endpoint: `/ws` (SockJS 지원)
- App Prefix: `/app`
- Broker Prefix: `/topic`, `/queue`

### Client → Server
| Destination | Description |
|---|---|
| `/app/room/{roomId}/ready` | 준비 상태 토글 |
| `/app/room/{roomId}/role` | 역할 선택 |
| `/app/room/{roomId}/role/release` | 역할 해제 |
| `/app/room/{roomId}/chat` | 채팅 전송 |
| `/app/room/{roomId}/watching/complete` | 영상 시청 완료 |
| `/app/room/{roomId}/recording/complete` | 녹음 완료 |

### Server → Client
| Destination | Description |
|---|---|
| `/topic/room/{roomId}/state` | 방 상태 브로드캐스트 |
| `/topic/room/{roomId}/chat` | 채팅 브로드캐스트 |
