# 방 입장
post
/api/v1/rooms/{roomId}/enter

req body
{
	"email": "user@gmail.com",
	"password": "1234",
	"nickname": "김싸피"
}

res body
{
    "success": true,
    "data": null,
    "error": null
}

# 세션 생성
post
/api/v1/openvidu/sessions

req body
{
    "custom_session_id": "room_123",
    "room_id": 1
}

{
    "success": true,
    "data": {
        "session_id": "ses_ABC123xyz"
    },
    "error": null
}

# 방 퇴장
post
/api/v1/rooms/{roomId}/webrtc/leave

req body
{
    "custom_session_id": "room_123",
    "room_id": 1
}

{
    "success": true,
    "data": {
        "session_id": "ses_ABC123xyz"
    },
    "error": null
}

req body
없음

res body
{
    "success": true,
    "data": null,
    "error": null
}

# 연결 토큰 생성
post
/api/v1/openvidu/sessions/{sessionId}/connections

req body
{
    "member_id": 1,
    "nickname": "김철수",
    "role_id": 2
}


{
    "success": true,
    "data": {
        "session_id": "ses_ABC123xyz",
        "token": "wss://your-openvidu-server:443?sessionId=ses_ABC123xyz&token=tok_...",
        "connection_id": "con_XYZ789abc"
    },
    "error": null
}

# 세션 종료
delete
/api/v1/openvidu/sessions/{sessionId} 

req body
없음

res body
{
    "success": true,
    "data": null,
    "error": null
}
