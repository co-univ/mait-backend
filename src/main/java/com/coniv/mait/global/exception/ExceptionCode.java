package com.coniv.mait.global.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExceptionCode {
	USER_INPUT_EXCEPTION(HttpStatus.BAD_REQUEST, "C-001", "사용자 입력 오류입니다."),
	ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C-002", "특정 엔티티를 조회할 수 없습니다."),
	USER_PARAMETER_EXCEPTION(HttpStatus.BAD_REQUEST, "C-003", "사용자 파라미터 오류입니다."),
	RESOURCE_NOT_BELONG_EXCEPTION(HttpStatus.BAD_REQUEST, "C-004", "리소스가 소속되지 않았습니다."),

	PROCESSING(HttpStatus.CONFLICT, "C-005", "이전 요청이 처리 중입니다."),
	Question_SET_LIVE_EXCEPTION(HttpStatus.BAD_REQUEST, "C-006", "실시간 문제 세트에서 오류가 발생했습니다."),

	TEAM_INVITE_FAIL_EXCEPTION(HttpStatus.BAD_REQUEST, "C-007", "팀 초대 생성에서 오류가 발생했습니다."),
	USER_ROLE_EXCEPTION(HttpStatus.FORBIDDEN, "C-008", "사용자 권한이 부족합니다."),

	LOGIN_FAIL_EXCEPTION(HttpStatus.UNAUTHORIZED, "A-001", "로그인 실패"),
	JWT_AUTH_EXCEPTION(HttpStatus.UNAUTHORIZED, "A-002", "JWT 인증 오류"),

	S3_FILE_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "F-001", "S3 파일 처리 중 오류가 발생했습니다."),

	UNEXPECTED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "S-000", "예기치 못한 오류가 발생했습니다."),
	DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S-001", "데이터베이스 처리 중 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
