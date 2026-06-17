package com.coniv.mait.domain.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.admin.service.dto.AdminQuestionSetDto;
import com.coniv.mait.domain.admin.service.dto.AdminTeamDto;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminLookupService {

	private final TeamEntityRepository teamEntityRepository;
	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionService questionService;

	@Transactional(readOnly = true)
	public List<AdminTeamDto> getAllTeams() {
		return teamEntityRepository.findAllByDeletedAtIsNull().stream()
			.map(AdminTeamDto::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminQuestionSetDto> getQuestionSets(final Long teamId) {
		return questionSetEntityRepository.findAllByTeamId(teamId).stream()
			.map(AdminQuestionSetDto::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<QuestionDto> getQuestions(final Long questionSetId) {
		// 어드민은 실제 정답까지 확인해야 하므로 정답이 노출되는 MANAGING 모드로 조회한다.
		return questionService.getQuestions(questionSetId, DeliveryMode.MANAGING);
	}
}
