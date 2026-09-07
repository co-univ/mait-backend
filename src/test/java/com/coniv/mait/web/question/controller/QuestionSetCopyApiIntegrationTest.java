package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.CopyQuestionSetApiRequest;

@WithCustomUser
public class QuestionSetCopyApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@BeforeEach
	void clear() {
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("문제 셋 복제 - 대상 팀에 제작 시작 상태의 복제본이 생성된다")
	void copyQuestionSet_success_createsCopyInTargetTeam() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity sourceTeam = joinTeam(user, "원본 팀", TeamUserRole.MAKER);
		TeamEntity targetTeam = joinTeam(user, "대상 팀", TeamUserRole.OWNER);

		QuestionSetEntity source = questionSetEntityRepository.save(QuestionSetEntity.builder()
			.title("원본 문제 셋")
			.creationType(QuestionSetCreationType.AI_GENERATED)
			.solveMode(QuestionSetSolveMode.STUDY)
			.difficulty("보통")
			.instruction("AI 생성 보충 설명")
			.teamId(sourceTeam.getId())
			.creatorId(user.getId())
			.status(QuestionSetStatus.ONGOING)
			.startTime(LocalDateTime.of(2026, 1, 1, 10, 0))
			.build());

		CopyQuestionSetApiRequest request = new CopyQuestionSetApiRequest(targetTeam.getId());

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/copy", source.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.title").value("원본 문제 셋"),
				jsonPath("$.data.teamId").value(targetTeam.getId())
			);

		List<QuestionSetEntity> questionSets = questionSetEntityRepository.findAll();
		assertThat(questionSets).hasSize(2);

		QuestionSetEntity copied = questionSets.stream()
			.filter(questionSet -> !questionSet.getId().equals(source.getId()))
			.findFirst()
			.orElseThrow();

		assertThat(copied.getTeamId()).isEqualTo(targetTeam.getId());
		assertThat(copied.getCreatorId()).isEqualTo(user.getId());
		assertThat(copied.getSourceQuestionSetId()).isEqualTo(source.getId());
		assertThat(copied.getTitle()).isEqualTo("원본 문제 셋");
		assertThat(copied.getSolveMode()).isEqualTo(QuestionSetSolveMode.STUDY);
		assertThat(copied.getDifficulty()).isEqualTo("보통");
		assertThat(copied.getCreationType()).isEqualTo(QuestionSetCreationType.MANUAL);
		assertThat(copied.getInstruction()).isNull();
		assertThat(copied.getStatus()).isEqualTo(QuestionSetStatus.MAKING);
		assertThat(copied.getStartTime()).isNull();
		assertThat(copied.getEndTime()).isNull();
		assertThat(copied.isAdvancementSelected()).isFalse();
	}

	private TeamEntity joinTeam(final UserEntity user, final String teamName, final TeamUserRole role) {
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup(teamName, user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, role));
		return team;
	}
}
