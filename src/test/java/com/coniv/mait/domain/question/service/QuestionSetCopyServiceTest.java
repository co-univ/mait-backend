package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionSetCopyServiceTest {

	private static final Long QUESTION_SET_ID = 10L;
	private static final Long SOURCE_TEAM_ID = 1L;
	private static final Long TARGET_TEAM_ID = 2L;
	private static final Long USER_ID = 100L;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@InjectMocks
	private QuestionSetCopyService questionSetCopyService;

	@Test
	@DisplayName("문제 셋 복제 - 대상 팀과 요청자로 재설정된 복제본을 저장한다")
	void copyQuestionSet_success_savesCopyInTargetTeam() {
		// given
		givenSource(sourceBuilder().build());

		// when
		QuestionSetDto result = questionSetCopyService.copyQuestionSet(QUESTION_SET_ID, TARGET_TEAM_ID, USER_ID);

		// then
		QuestionSetEntity saved = captureSaved();
		assertThat(saved.getTitle()).isEqualTo("원본 제목");
		assertThat(saved.getSolveMode()).isEqualTo(QuestionSetSolveMode.STUDY);
		assertThat(saved.getDifficulty()).isEqualTo("보통");
		assertThat(saved.getTeamId()).isEqualTo(TARGET_TEAM_ID);
		assertThat(saved.getCreatorId()).isEqualTo(USER_ID);
		assertThat(saved.getSourceQuestionSetId()).isEqualTo(QUESTION_SET_ID);
		assertThat(result.getTeamId()).isEqualTo(TARGET_TEAM_ID);
	}

	@Test
	@DisplayName("문제 셋 복제 - AI 생성 문제 셋을 복제하면 MANUAL 로 저장하고 instruction 을 승계하지 않는다")
	void copyQuestionSet_aiGeneratedSource_copiesAsManualWithoutInstruction() {
		// given
		givenSource(sourceBuilder()
			.creationType(QuestionSetCreationType.AI_GENERATED)
			.instruction("AI 생성 시 사용한 보충 설명")
			.build());

		// when
		questionSetCopyService.copyQuestionSet(QUESTION_SET_ID, TARGET_TEAM_ID, USER_ID);

		// then
		QuestionSetEntity saved = captureSaved();
		assertThat(saved.getCreationType()).isEqualTo(QuestionSetCreationType.MANUAL);
		assertThat(saved.getInstruction()).isNull();
	}

	@Test
	@DisplayName("문제 셋 복제 - 원본의 진행 상태는 승계하지 않고 제작 시작 시점으로 되돌린다")
	void copyQuestionSet_progressStateNotCopied() {
		// given
		givenSource(sourceBuilder()
			.status(QuestionSetStatus.ONGOING)
			.startTime(LocalDateTime.of(2026, 1, 1, 10, 0))
			.endTime(LocalDateTime.of(2026, 1, 1, 12, 0))
			.advancementSelected(true)
			.build());

		// when
		questionSetCopyService.copyQuestionSet(QUESTION_SET_ID, TARGET_TEAM_ID, USER_ID);

		// then
		QuestionSetEntity saved = captureSaved();
		assertThat(saved.getStatus()).isEqualTo(QuestionSetStatus.MAKING);
		assertThat(saved.getStartTime()).isNull();
		assertThat(saved.getEndTime()).isNull();
		assertThat(saved.isAdvancementSelected()).isFalse();
	}

	@Test
	@DisplayName("문제 셋 복제 - 복제본을 다시 복제하면 직전 원본을 sourceQuestionSetId 로 남긴다")
	void copyQuestionSet_copyOfCopy_pointsToImmediateSource() {
		// given
		givenSource(sourceBuilder().sourceQuestionSetId(1L).build());

		// when
		questionSetCopyService.copyQuestionSet(QUESTION_SET_ID, TARGET_TEAM_ID, USER_ID);

		// then
		assertThat(captureSaved().getSourceQuestionSetId()).isEqualTo(QUESTION_SET_ID);
	}

	@Test
	@DisplayName("문제 셋 복제 - 존재하지 않는 문제 셋이면 예외가 발생하고 저장하지 않는다")
	void copyQuestionSet_sourceNotFound_throws() {
		// given
		doThrow(new EntityNotFoundException(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다."))
			.when(questionSetReader).getQuestionSet(QUESTION_SET_ID);

		// when & then
		assertThatThrownBy(() -> questionSetCopyService.copyQuestionSet(QUESTION_SET_ID, TARGET_TEAM_ID, USER_ID))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다.");

		verify(questionSetEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("문제 셋 복제 - 원본 팀의 멤버가 아니면 예외가 발생하고 저장하지 않는다")
	void copyQuestionSet_notMemberOfSourceTeam_throws() {
		// given
		doReturn(sourceBuilder().build()).when(questionSetReader).getQuestionSet(QUESTION_SET_ID);
		doThrow(new UserRoleException("해당 팀의 멤버가 아닙니다."))
			.when(teamRoleValidator).checkIsTeamMember(SOURCE_TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionSetCopyService.copyQuestionSet(QUESTION_SET_ID, TARGET_TEAM_ID, USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");

		verify(teamRoleValidator, never()).checkHasCreateQuestionSetAuthority(anyLong(), anyLong());
		verify(questionSetEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("문제 셋 복제 - 대상 팀에 문제 셋 생성 권한이 없으면 예외가 발생하고 저장하지 않는다")
	void copyQuestionSet_noAuthorityInTargetTeam_throws() {
		// given
		doReturn(sourceBuilder().build()).when(questionSetReader).getQuestionSet(QUESTION_SET_ID);
		doThrow(new UserRoleException("문제 세트 생성 권한이 없습니다."))
			.when(teamRoleValidator).checkHasCreateQuestionSetAuthority(TARGET_TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionSetCopyService.copyQuestionSet(QUESTION_SET_ID, TARGET_TEAM_ID, USER_ID))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("문제 세트 생성 권한이 없습니다.");

		verify(questionSetEntityRepository, never()).save(any());
	}

	private QuestionSetEntity.QuestionSetEntityBuilder sourceBuilder() {
		return QuestionSetEntity.builder()
			.id(QUESTION_SET_ID)
			.title("원본 제목")
			.creationType(QuestionSetCreationType.MANUAL)
			.solveMode(QuestionSetSolveMode.STUDY)
			.difficulty("보통")
			.teamId(SOURCE_TEAM_ID)
			.creatorId(999L)
			.status(QuestionSetStatus.BEFORE);
	}

	private void givenSource(final QuestionSetEntity source) {
		doReturn(source).when(questionSetReader).getQuestionSet(QUESTION_SET_ID);
		doAnswer(invocation -> invocation.getArgument(0)).when(questionSetEntityRepository)
			.save(any(QuestionSetEntity.class));
	}

	private QuestionSetEntity captureSaved() {
		ArgumentCaptor<QuestionSetEntity> captor = ArgumentCaptor.forClass(QuestionSetEntity.class);
		verify(questionSetEntityRepository).save(captor.capture());
		return captor.getValue();
	}
}
