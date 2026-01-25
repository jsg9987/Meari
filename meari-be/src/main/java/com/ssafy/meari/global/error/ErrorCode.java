package com.ssafy.meari.global.error;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
	

	// Invalid Argument Error(BAD_REQUEST): 400 
    MISSING_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "필수 경로 변수가 누락되었습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 인자입니다."),
    INVALID_PARAMETER_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 인자 형식입니다."),
    INVALID_HEADER_ERROR(HttpStatus.BAD_REQUEST, "유효하지 않은 헤더입니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "유효하지 않은 period입니다."),
    MISSING_REQUEST_HEADER(HttpStatus.BAD_REQUEST, "필수 요청 헤더가 누락되었습니다."),
    BAD_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 요청 파라미터입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 미디어 타입입니다."),
    BAD_REQUEST_JSON(HttpStatus.BAD_REQUEST, "잘못된 JSON 형식입니다."),
    EXCEEDED_MAX_SIZE(HttpStatus.BAD_REQUEST, "파일 크기가 3MB를 초과했습니다."),
    DUPLICATED_USER(HttpStatus.BAD_REQUEST, "중복된 사용자입니다."),
    DUPLICATED_USER_NICKNAME(HttpStatus.BAD_REQUEST, "중복된 사용자 닉네임입니다."),
    NO_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "파일에 확장자가 없습니다."),
    FRIEND_REQUEST_MYSELF(HttpStatus.BAD_REQUEST, "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    INVALID_TRIP_DATE(HttpStatus.BAD_REQUEST, "여행 종료일이 시작일보다 이전일 수 없습니다."),
    TRIP_INVITE_MYSELF(HttpStatus.BAD_REQUEST, "자기 자신을 여행에 초대할 수 없습니다."),
    INVALID_DAY_NUMBER(HttpStatus.BAD_REQUEST, "일차가 여행 기간을 초과합니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "종료 시간이 시작 시간보다 이전일 수 없습니다."),
    INVALID_VISIT_ORDER(HttpStatus.BAD_REQUEST, "유효하지 않은 방문 순서입니다."),

    // Unauthorized Error: 401
    UNAUTHORIZED_MEMBER(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    FAILURE_LOGIN(HttpStatus.UNAUTHORIZED, "잘못된 아이디 또는 비밀번호입니다."),
    EXPIRED_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_MALFORMED_ERROR(HttpStatus.UNAUTHORIZED, "토큰이 올바르지 않습니다."),
    TOKEN_TYPE_ERROR(HttpStatus.UNAUTHORIZED, "토큰 타입이 일치하지 않거나 비어있습니다."),
    TOKEN_UNSUPPORTED_ERROR(HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰입니다."),
    TOKEN_GENERATION_ERROR(HttpStatus.UNAUTHORIZED, "토큰 생성에 실패하였습니다."),
    TOKEN_UNKNOWN_ERROR(HttpStatus.UNAUTHORIZED, "알 수 없는 토큰입니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh Token을 찾을 수 없습니다."),
    SPOTIFY_NOT_CONNECTED(HttpStatus.UNAUTHORIZED, "Spotify 연동이 필요합니다."),
    INVALID_SPOTIFY_STATE(HttpStatus.UNAUTHORIZED, "유효하지 않은 Spotify 인증 state입니다."),
    SPOTIFY_TOKEN_REFRESH_FAILED(HttpStatus.UNAUTHORIZED, "Spotify 토큰 갱신에 실패했습니다."),
    SPOTIFY_API_ERROR(HttpStatus.BAD_GATEWAY, "Spotify API 호출에 실패했습니다."),

    // Access Denied Error(FORBIDDEN): 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    FORBIDDEN_FRIEND_ACCEPT(HttpStatus.FORBIDDEN, "친구 요청을 수락할 권한이 없습니다."),
    FORBIDDEN_FRIEND_REJECT(HttpStatus.FORBIDDEN, "친구 요청을 거절할 권한이 없습니다."),
    FORBIDDEN_TRIP_ACCESS(HttpStatus.FORBIDDEN, "해당 여행에 접근할 권한이 없습니다."),

	// Not Found Error: 404
	NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found "),
	NOT_FOUND_END_POINT(HttpStatus.NOT_FOUND, "존재하지 않는 API 엔드포인트입니다."),
    NOT_FOUND_RESOURCE(HttpStatus.NOT_FOUND, "해당 리소스가 존재하지 않습니다."),
    NOT_FOUND_AUTHORIZATION_HEADER(HttpStatus.NOT_FOUND, "Authorization 헤더가 존재하지 않습니다."),
    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    NOT_FOUND_FRIEND_REQUEST(HttpStatus.NOT_FOUND, "존재하지 않는 친구 요청입니다."),
    NOT_FOUND_SIDO(HttpStatus.NOT_FOUND, "존재하지 않는 시도 코드입니다."),
    NOT_FOUND_GUNGU(HttpStatus.NOT_FOUND, "존재하지 않는 구군 코드입니다."),
    NOT_FOUND_TRIP(HttpStatus.NOT_FOUND, "존재하지 않는 여행입니다."),
    NOT_FOUND_TRIP_MEMBER(HttpStatus.NOT_FOUND, "여행 초대를 받지 않았습니다."),
    NOT_FOUND_ATTRACTION(HttpStatus.NOT_FOUND, "존재하지 않는 관광지입니다."),
    NOT_FOUND_ITINERARY(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."),
    ATTRACTION_NOT_FOUND_IN_VECTOR_DB(HttpStatus.NOT_FOUND, "벡터 DB에 해당 관광지가 존재하지 않습니다."),
    NOT_FRIEND(HttpStatus.NOT_FOUND, "친구 관계가 아닙니다."),

	// Method Not Allowed Error: 405
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메소드입니다."),
    
    // Conflict: 409
    DATA_ALREADY_EXISTS(HttpStatus.CONFLICT, "데이터가 이미 존재합니다."),
    FRIEND_REQUEST_ALREADY_SENT(HttpStatus.CONFLICT, "이미 친구 요청을 보냈습니다."),
    FRIEND_REQUEST_ALREADY_RECEIVED(HttpStatus.CONFLICT, "상대방으로부터 이미 친구 요청을 받았습니다."),
    FRIEND_ALREADY_CONNECTED(HttpStatus.CONFLICT, "이미 친구 관계입니다."),
    TRIP_MEMBER_ALREADY_INVITED(HttpStatus.CONFLICT, "이미 해당 여행에 초대되었거나 참여 중입니다."),
    TRIP_INVITATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 여행 초대입니다."),
    
	
	// Unsupported Media Type: 415
	
	
    // Internal Server Error: 500
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 에러입니다."),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽어들이다 에러 발생"),
    MUSIC_RECOMMENDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "음악 추천에 실패했습니다."),
	
	
    // External Server Error(BAD_GATEWAY): 502
	EXTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "서버 외부 에러입니다.");
	
	private final HttpStatus httpStatus;
	private final String message;
}
