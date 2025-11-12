package com.coniv.mait.domain.question.enums;

import lombok.Getter;

@Getter
public enum AiRequestStatus {
	/**
	 * 대기 중 (요청 생성됨, 아직 처리 시작 안함)
	 */
	PENDING,

	/**
	 * 처리 중 (AI 서버 호출 중, 1-2분 소요)
	 */
	PROCESSING,

	/**
	 * 완료 (문제 생성 및 저장 완료)
	 */
	COMPLETED,

	NOT_FOUND,
	/**
	 * 실패 (AI 서버 에러, 타임아웃 등)
	 */
	FAILED
}

