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
    CSV_PARSE_ERROR(HttpStatus.BAD_REQUEST, "CSV 파일 파싱 중 오류가 발생했습니다."),
    CSV_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "CSV 파일 형식이 올바르지 않습니다."),
    CSV_INVALID_HEADER(HttpStatus.BAD_REQUEST, "CSV 헤더가 올바르지 않습니다."),
    CSV_INVALID_DATA_TYPE(HttpStatus.BAD_REQUEST, "CSV 데이터 타입이 올바르지 않습니다."),
    CSV_EMPTY_FILE(HttpStatus.BAD_REQUEST, "빈 CSV 파일입니다."),

    // Unauthorized Error: 401
    UNAUTHORIZED_MEMBER(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    FAILURE_LOGIN(HttpStatus.UNAUTHORIZED, "잘못된 아이디 또는 비밀번호입니다."),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
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

    // Access Denied Error(FORBIDDEN): 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_ROOM_PASSWORD(HttpStatus.FORBIDDEN, "방 비밀번호가 일치하지 않습니다."),
    NOT_ROOM_OWNER(HttpStatus.FORBIDDEN, "방장 권한이 필요합니다."),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "방 참여자가 아닙니다."),
    OWNER_CANNOT_READY(HttpStatus.BAD_REQUEST, "방장은 준비 상태를 변경할 수 없습니다."),

	// Not Found Error: 404
	NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found "),
	NOT_FOUND_END_POINT(HttpStatus.NOT_FOUND, "존재하지 않는 API 엔드포인트입니다."),
    NOT_FOUND_RESOURCE(HttpStatus.NOT_FOUND, "해당 리소스가 존재하지 않습니다."),
    NOT_FOUND_AUTHORIZATION_HEADER(HttpStatus.NOT_FOUND, "Authorization 헤더가 존재하지 않습니다."),
    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    NOT_FOUND_ROOM(HttpStatus.NOT_FOUND, "존재하지 않는 방입니다."),
    NOT_FOUND_THEME(HttpStatus.NOT_FOUND, "존재하지 않는 테마입니다."),
    NOT_FOUND_CONTENT(HttpStatus.NOT_FOUND, "존재하지 않는 콘텐츠입니다."),
    NOT_FOUND_ROLE(HttpStatus.NOT_FOUND, "존재하지 않는 역할입니다."),
    NOT_FOUND_MEMBER_ROOM(HttpStatus.NOT_FOUND, "방에 참여하지 않은 사용자입니다."),
    NOT_FOUND_KOPIC_SENTENCE(HttpStatus.NOT_FOUND, "해당 테마에 코픽 문장이 존재하지 않습니다."),
    NOT_FOUND_KOPIC_REPORT(HttpStatus.NOT_FOUND, "존재하지 않는 코픽 리포트입니다."),
    NOT_FOUND_KOPIC_TOTAL_REPORT(HttpStatus.NOT_FOUND, "존재하지 않는 코픽 통합 리포트입니다."),
    OPENVIDU_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "OpenVidu 세션을 찾을 수 없습니다."),

	// Method Not Allowed Error: 405
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메소드입니다."),
    
    // Conflict: 409
    DATA_ALREADY_EXISTS(HttpStatus.CONFLICT, "데이터가 이미 존재합니다."),
    ROOM_FULL(HttpStatus.CONFLICT, "방 정원이 가득 찼습니다."),
    ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 방입니다."),
    ROLE_ALREADY_TAKEN(HttpStatus.CONFLICT, "이미 선점된 역할입니다."),
    ROOM_NOT_JOINABLE(HttpStatus.CONFLICT, "입장할 수 없는 방입니다."),
    ROOM_NOT_WAITING(HttpStatus.CONFLICT, "대기 중인 방이 아닙니다."),
    ROOM_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "진행 중인 방이 아닙니다."),
    NOT_ALL_READY(HttpStatus.CONFLICT, "모든 참여자가 준비 완료되지 않았습니다."),
    INVALID_PHASE(HttpStatus.CONFLICT, "현재 단계에서 수행할 수 없는 작업입니다."),
    CONTENT_NOT_SELECTED(HttpStatus.CONFLICT, "동영상이 선택되지 않았습니다."),
    CONTENT_MISMATCH(HttpStatus.CONFLICT, "선택된 동영상과 일치하지 않습니다."),
    CONTENT_SELECT_ONLY_WAITING(HttpStatus.CONFLICT, "동영상은 대기 중일 때만 선택할 수 있습니다."),
    ROOM_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 학습이 시작된 방입니다."),
    ROOM_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 종료된 방입니다."),
    ROLE_NOT_SELECTED(HttpStatus.CONFLICT, "모든 참여자가 역할을 선택하지 않았습니다."),
    ROLES_NOT_CONFIRMED(HttpStatus.CONFLICT, "역할이 확정되지 않았습니다."),
    ROLES_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "역할이 이미 확정되었습니다."),
    ROLE_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "역할 개수가 참여자 수와 일치하지 않습니다."),
    DUPLICATE_MEMBER_ROLE(HttpStatus.BAD_REQUEST, "한 멤버에게 여러 역할을 할당할 수 없습니다."),
    DUPLICATE_ROLE_ASSIGNMENT(HttpStatus.CONFLICT, "같은 역할을 여러 멤버에게 할당할 수 없습니다."),

	
	// Unsupported Media Type: 415
	
	
    // Internal Server Error: 500
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 에러입니다."),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽어들이다 에러 발생"),
    OPENVIDU_SESSION_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OpenVidu 세션 생성에 실패했습니다."),
    OPENVIDU_CONNECTION_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OpenVidu 연결(토큰) 생성에 실패했습니다."),
    OPENVIDU_SESSION_CLOSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OpenVidu 세션 종료에 실패했습니다."),
    OPENVIDU_DISCONNECT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OpenVidu 연결 강제 종료에 실패했습니다."),
    OPENVIDU_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OpenVidu 세션 정보 조회에 실패했습니다."),
    GEMINI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Gemini AI 분석에 실패했습니다."),
	CHAT_SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "채팅 메시지 저장에 실패했습니다."),
	CHAT_BROADCAST_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "채팅 메시지 브로드캐스트에 실패했습니다."),

	// External Server Error(BAD_GATEWAY): 502
	EXTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "서버 외부 에러입니다.");
	
	private final HttpStatus httpStatus;
	private final String message;
}
