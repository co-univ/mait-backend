package com.coniv.mait.domain.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.admin.service.dto.AdminQuestionSetDto;
import com.coniv.mait.domain.admin.service.dto.AdminTeamDto;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.enums.TeamType;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;

@ExtendWith(MockitoExtension.class)
class AdminLookupServiceTest {

	private static final Long TEAM_ID = 10L;
	private static final Long QUESTION_SET_ID = 100L;

	@InjectMocks
	private AdminLookupService adminLookupService;

	@Mock
	private TeamEntityRepository teamEntityRepository;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionService questionService;

	@Test
	@DisplayName("getAllTeams - 삭제되지 않은 팀 목록을 조회해 DTO로 변환한다")
	void getAllTeams() {
		// given
		when(teamEntityRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(
			TeamEntity.ofGroup("팀A", 100L),
			TeamEntity.ofPersonal("개인", 200L)));

		// when
		List<AdminTeamDto> result = adminLookupService.getAllTeams();

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("팀A");
		assertThat(result.get(0).getType()).isEqualTo(TeamType.GROUP);
		assertThat(result.get(0).getCreatorId()).isEqualTo(100L);
		assertThat(result.get(1).getType()).isEqualTo(TeamType.PERSONAL);
	}

	@Test
	@DisplayName("getQuestionSets - 팀의 문제 셋 목록을 조회해 DTO로 변환한다")
	void getQuestionSets() {
		// given
		when(questionSetEntityRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(
			QuestionSetEntity.builder()
				.title("셋1")
				.subject("주제1")
				.status(QuestionSetStatus.AFTER)
				.solveMode(QuestionSetSolveMode.LIVE_TIME)
				.teamId(TEAM_ID)
				.build()));

		// when
		List<AdminQuestionSetDto> result = adminLookupService.getQuestionSets(TEAM_ID);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getTitle()).isEqualTo("셋1");
		assertThat(result.get(0).getSubject()).isEqualTo("주제1");
		assertThat(result.get(0).getStatus()).isEqualTo(QuestionSetStatus.AFTER);
		assertThat(result.get(0).getSolveMode()).isEqualTo(QuestionSetSolveMode.LIVE_TIME);
		assertThat(result.get(0).getTeamId()).isEqualTo(TEAM_ID);
	}

	@Test
	@DisplayName("getQuestions - 정답이 노출되는 MANAGING 모드로 문제 목록을 위임 조회한다")
	void getQuestions() {
		// given
		List<QuestionDto> questions = List.of(MultipleQuestionDto.builder().id(1L).choices(List.of()).build());
		when(questionService.getQuestions(QUESTION_SET_ID, DeliveryMode.MANAGING)).thenReturn(questions);

		// when
		List<QuestionDto> result = adminLookupService.getQuestions(QUESTION_SET_ID);

		// then
		assertThat(result).isEqualTo(questions);
		verify(questionService).getQuestions(QUESTION_SET_ID, DeliveryMode.MANAGING);
	}
}
