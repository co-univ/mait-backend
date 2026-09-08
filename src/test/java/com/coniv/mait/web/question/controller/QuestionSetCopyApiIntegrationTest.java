package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionImageEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.enums.FileExtension;
import com.coniv.mait.global.s3.dto.FileType;
import com.coniv.mait.global.s3.service.S3FileUploader;
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

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Autowired
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Autowired
	private OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Autowired
	private QuestionImageEntityRepository questionImageEntityRepository;

	@MockitoBean
	private S3FileUploader s3FileUploader;

	@BeforeEach
	void clear() {
		multipleChoiceEntityRepository.deleteAll();
		shortAnswerEntityRepository.deleteAll();
		fillBlankAnswerEntityRepository.deleteAll();
		orderingOptionEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
		questionImageEntityRepository.deleteAll();
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

		CopyQuestionSetApiRequest request = new CopyQuestionSetApiRequest(targetTeam.getId(), "복제본 문제 셋",
			QuestionSetSolveMode.LIVE_TIME);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/copy", source.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.title").value("복제본 문제 셋"),
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
		assertThat(copied.getTitle()).isEqualTo("복제본 문제 셋");
		assertThat(copied.getSolveMode()).isEqualTo(QuestionSetSolveMode.LIVE_TIME);
		assertThat(copied.getDifficulty()).isEqualTo("보통");
		assertThat(copied.getCreationType()).isEqualTo(QuestionSetCreationType.MANUAL);
		assertThat(copied.getInstruction()).isNull();
		assertThat(copied.getStatus()).isEqualTo(QuestionSetStatus.MAKING);
		assertThat(copied.getStartTime()).isNull();
		assertThat(copied.getEndTime()).isNull();
		assertThat(copied.isAdvancementSelected()).isFalse();
	}


	@Test
	@DisplayName("문제 셋 복제 - 4가지 유형의 문제와 하위 엔티티가 모두 복제된다")
	void copyQuestionSet_success_copiesAllQuestionTypesAndSubEntities() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity sourceTeam = joinTeam(user, "원본 팀", TeamUserRole.MAKER);
		TeamEntity targetTeam = joinTeam(user, "대상 팀", TeamUserRole.MAKER);
		QuestionSetEntity source = saveQuestionSet(sourceTeam, user);

		MultipleQuestionEntity multiple = questionEntityRepository.save(MultipleQuestionEntity.builder()
			.content("객관식 문제").explanation("객관식 해설").number(1L).lexoRank("0|100000:")
			.questionSet(source).answerCount(4).build()); // 원본의 잘못된 저장값은 복제본에 전파하지 않는다.
		multipleChoiceEntityRepository.saveAll(List.of(
			MultipleChoiceEntity.builder().number(1).content("보기1").isCorrect(true).question(multiple).build(),
			MultipleChoiceEntity.builder().number(2).content("보기2").isCorrect(false).question(multiple).build()));

		ShortQuestionEntity shortQuestion = questionEntityRepository.save(ShortQuestionEntity.builder()
			.content("단답형 문제").number(2L).lexoRank("0|200000:")
			.questionSet(source).answerCount(1).build());
		shortAnswerEntityRepository.save(ShortAnswerEntity.builder()
			.answer("정답").isMain(true).number(1L).shortQuestionId(shortQuestion.getId()).build());

		FillBlankQuestionEntity fillBlank = questionEntityRepository.save(FillBlankQuestionEntity.builder()
			.content("빈칸 문제").number(3L).lexoRank("0|300000:").questionSet(source).build());
		fillBlankAnswerEntityRepository.save(FillBlankAnswerEntity.builder()
			.answer("빈칸정답").isMain(true).number(1L).fillBlankQuestionId(fillBlank.getId()).build());

		OrderingQuestionEntity ordering = questionEntityRepository.save(OrderingQuestionEntity.builder()
			.content("순서 문제").number(4L).lexoRank("0|400000:").questionSet(source).build());
		orderingOptionEntityRepository.saveAll(List.of(
			OrderingOptionEntity.builder().originOrder(1).content("가").answerOrder(2)
				.orderingQuestionId(ordering.getId()).build(),
			OrderingOptionEntity.builder().originOrder(2).content("나").answerOrder(1)
				.orderingQuestionId(ordering.getId()).build()));

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/copy", source.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CopyQuestionSetApiRequest(targetTeam.getId(), "복제본 문제 셋",
					QuestionSetSolveMode.LIVE_TIME))))
			.andExpect(status().isOk());

		// then
		QuestionSetEntity copiedSet = questionSetEntityRepository.findAll().stream()
			.filter(questionSet -> !questionSet.getId().equals(source.getId()))
			.findFirst()
			.orElseThrow();

		List<QuestionEntity> copiedQuestions =
			questionEntityRepository.findAllByQuestionSetIdOrderByLexoRankAsc(copiedSet.getId());
		assertThat(copiedQuestions).hasSize(4);
		assertThat(copiedQuestions).extracting(QuestionEntity::getContent)
			.containsExactly("객관식 문제", "단답형 문제", "빈칸 문제", "순서 문제");
		assertThat(copiedQuestions).extracting(QuestionEntity::getType)
			.containsExactly(QuestionType.MULTIPLE, QuestionType.SHORT, QuestionType.FILL_BLANK,
				QuestionType.ORDERING);
		assertThat(copiedQuestions).extracting(QuestionEntity::getLexoRank)
			.containsExactly("0|100000:", "0|200000:", "0|300000:", "0|400000:");
		assertThat(copiedQuestions).extracting(QuestionEntity::getId).doesNotContain(multiple.getId(),
			shortQuestion.getId(), fillBlank.getId(), ordering.getId());

		MultipleQuestionEntity copiedMultiple = (MultipleQuestionEntity)copiedQuestions.get(0);
		assertThat(copiedMultiple.getExplanation()).isEqualTo("객관식 해설");
		assertThat(copiedMultiple.getAnswerCount()).isEqualTo(1);
		assertThat(multiple.getAnswerCount()).isEqualTo(4);
		assertThat(multipleChoiceEntityRepository.findAllByQuestionId(copiedMultiple.getId()))
			.extracting(MultipleChoiceEntity::getContent, MultipleChoiceEntity::isCorrect)
			.containsExactlyInAnyOrder(tuple("보기1", true), tuple("보기2", false));

		assertThat(shortAnswerEntityRepository.findAllByShortQuestionId(copiedQuestions.get(1).getId()))
			.extracting(ShortAnswerEntity::getAnswer, ShortAnswerEntity::isMain)
			.containsExactly(tuple("정답", true));

		assertThat(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(copiedQuestions.get(2).getId()))
			.extracting(FillBlankAnswerEntity::getAnswer)
			.containsExactly("빈칸정답");

		assertThat(orderingOptionEntityRepository.findAllByOrderingQuestionId(copiedQuestions.get(3).getId()))
			.extracting(OrderingOptionEntity::getContent, OrderingOptionEntity::getAnswerOrder)
			.containsExactlyInAnyOrder(tuple("가", 2), tuple("나", 1));

		assertThat(multipleChoiceEntityRepository.findAllByQuestionId(multiple.getId())).hasSize(2);
	}


	@Test
	@DisplayName("문제 셋 복제 - 문제 이미지가 S3 복사를 거쳐 별도 이미지로 분리된다")
	void copyQuestionSet_success_copiesQuestionImage() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity sourceTeam = joinTeam(user, "원본 팀", TeamUserRole.MAKER);
		TeamEntity targetTeam = joinTeam(user, "대상 팀", TeamUserRole.MAKER);
		QuestionSetEntity source = saveQuestionSet(sourceTeam, user);

		QuestionImageEntity sourceImage = questionImageEntityRepository.save(QuestionImageEntity.builder()
			.fileKey("questions/origin.png")
			.url("https://bucket/questions/origin.png")
			.bucket("mait-bucket")
			.build());

		questionEntityRepository.save(MultipleQuestionEntity.builder()
			.content("이미지 문제").number(1L).lexoRank("0|100000:")
			.imageId(sourceImage.getId()).imageUrl(sourceImage.getUrl())
			.questionSet(source).answerCount(1).build());

		questionEntityRepository.save(MultipleQuestionEntity.builder()
			.content("이미지 없는 문제").number(2L).lexoRank("0|200000:")
			.questionSet(source).answerCount(1).build());

		doReturn(FileInfo.builder()
			.key("questions/copied.png")
			.url("https://bucket/questions/copied.png")
			.bucket("mait-bucket")
			.extension(FileExtension.PNG)
			.build())
			.when(s3FileUploader).copyFile("questions/origin.png", FileType.QUESTION_IMAGE);

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/copy", source.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CopyQuestionSetApiRequest(targetTeam.getId(), "복제본 문제 셋",
					QuestionSetSolveMode.LIVE_TIME))))
			.andExpect(status().isOk());

		// then
		verify(s3FileUploader, times(1)).copyFile("questions/origin.png", FileType.QUESTION_IMAGE);

		QuestionSetEntity copiedSet = questionSetEntityRepository.findAll().stream()
			.filter(questionSet -> !questionSet.getId().equals(source.getId()))
			.findFirst()
			.orElseThrow();

		List<QuestionEntity> copiedQuestions =
			questionEntityRepository.findAllByQuestionSetIdOrderByLexoRankAsc(copiedSet.getId());
		assertThat(copiedQuestions).hasSize(2);

		QuestionEntity copiedWithImage = copiedQuestions.get(0);
		assertThat(copiedWithImage.getImageId()).isNotNull();
		assertThat(copiedWithImage.getImageId()).isNotEqualTo(sourceImage.getId());
		assertThat(copiedWithImage.getImageUrl()).isEqualTo("https://bucket/questions/copied.png");

		QuestionImageEntity copiedImage = questionImageEntityRepository
			.findById(copiedWithImage.getImageId()).orElseThrow();
		assertThat(copiedImage.getFileKey()).isEqualTo("questions/copied.png");
		assertThat(copiedImage.isUsed()).isTrue();

		assertThat(copiedQuestions.get(1).getImageId()).isNull();

		QuestionImageEntity unchangedSource = questionImageEntityRepository
			.findById(sourceImage.getId()).orElseThrow();
		assertThat(unchangedSource.getFileKey()).isEqualTo("questions/origin.png");
		assertThat(unchangedSource.isUsed()).isTrue();
	}

	private QuestionSetEntity saveQuestionSet(final TeamEntity team, final UserEntity user) {
		return questionSetEntityRepository.save(QuestionSetEntity.builder()
			.title("원본 문제 셋")
			.creationType(QuestionSetCreationType.MANUAL)
			.solveMode(QuestionSetSolveMode.STUDY)
			.teamId(team.getId())
			.creatorId(user.getId())
			.status(QuestionSetStatus.BEFORE)
			.build());
	}

	private TeamEntity joinTeam(final UserEntity user, final String teamName, final TeamUserRole role) {
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup(teamName, user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, role));
		return team;
	}
}
