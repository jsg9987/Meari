# 사전 정보

`**인증**: JWT 토큰 필요 (연결 시 쿼리 파라미터 또는 헤더로 전달)`

# 구독

`## 구독 (Subscribe) 엔드포인트`

`클라이언트는 다음 토픽을 구독하여 실시간 업데이트를 받을 수 있습니다.`

## 방 상태 관련 구독 엔드포인트

/topic/rooms/{roomId}
`방의 모든 실시간 이벤트를 수신합니다.`

## 방 상태 관련 변경 엔드포인트

## /app/room/{roomId}

{
  "type": "MEMBER_JOIN",
  "memberId": 12345,
  "ready": true,
  "roleId": 1,
  "phase": "WAITING",
  "nickname": "길동이",
  "newOwnerId": null,
  "contentId": null
}

# 준비 상태 변경(Toggle Ready)

### Publish Path: /app/room/{roomId}/ready

- Request Payload:

```
{
    "member_id": 1
}
```

⇒ 응답은 Sub로 옴.

### Subscribe Path: /topic/room/{roomId}/state

- Response Payload (Broadcast):

```
{
// 여기선 type READY로 준비 상태 완료를 보내줌
   {
	   "type": "READY",
	   "member_id": 123,
	   "ready": true  // or false
   }

// 전체 결과는 이렇게 됨
{
		"type":"READY",
		"member_id":2,
		"ready":true,
		"role_id":null,
		"phase":null,
		"nickname":null,
		"new_owner_id":null,
		"content_id":null
}
```

## 역할 선점

`/app/room/{roomId}/role`

- Request Payload:
```
{
    "role_id": 1,
    "member_id": 1
}
```

응답 타입
type: `ROLE_ASSIGNED`

## 역할 해제

`/app/rooms/{roomId}/roles/release`
```
{
    "role_id": 1,
    "member_id": 1
}
```

응답 타입

type: `ROLE_RELEASED`

게임 시작 알림

"/topic/room/{roomId}/state"

```
{
		"type":"GAME_START",
		"member_id":2,
		"ready":true,
		"role_id":null,
		"phase":WATCHING, // 영상 시청 페이즈로 넘어감
		"nickname":null,
		"new_owner_id":null,
		"content_id":null
}
```

