package com.coniv.mait.web.admin.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
public class QuestionAnswerDistributionApiIntegrationTest extends BaseIntegrationTest {

	private static final String ANSWER_1 = "{\"type\":\"MULTIPLE\",\"submitAnswers\":[1]}";
	private static final String ANSWER_2 = "{\"type\":\"MULTIPLE\",\"submitAnswers\":[2]}";
	private static final String URL =
		"/api/v1/admin/question-sets/{questionSetId}/questions/{questionId}/statistics/answer-distribution";

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Test
	@DisplayName("전체 제출 기준으로 제출 답안을 그룹화하고, 문제 정보·실제 정답·정답률을 함께 조회한다")
	void getAnswerDistribution_allSubmits() throws Exception {
		// given
		MultipleQuestionEntity question = saveQuestionWithChoices();

		// user1: [2]오답(1) → [1]정답(2), user2: [2]오답, user3: [1]정답, user4: [2]오답
		saveRecord(1L, question.getId(), false, 1L, ANSWER_2);
		saveRecord(1L, question.getId(), true, 2L, ANSWER_1);
		saveRecord(2L, question.getId(), false, 1L, ANSWER_2);
		saveRecord(3L, question.getId(), true, 1L, ANSWER_1);
		saveRecord(4L, question.getId(), false, 1L, ANSWER_2);

		// when & then
		mockMvc.perform(get(URL, question.getQuestionSet().getId(), question.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.questionSetId").value(question.getQuestionSet().getId()),
				jsonPath("$.data.question.id").value(question.getId()),
				jsonPath("$.data.question.type").value("MULTIPLE"),
				jsonPath("$.data.question.content").value("문제 내용"),
				jsonPath("$.data.question.choices.length()").value(2),
				jsonPath("$.data.question.choices[*].isCorrect", hasItem(true)),
				jsonPath("$.data.totalSubmitCount").value(5),
				jsonPath("$.data.submittedUserCount").value(4),
				jsonPath("$.data.correctUserCount").value(1),
				jsonPath("$.data.correctRate").value(25.0),
				jsonPath("$.data.groups.length()").value(2),
				// 제출 수 내림차순: [2](3건, 오답) > [1](2건, 정답)
				jsonPath("$.data.groups[0].submitCount").value(3),
				jsonPath("$.data.groups[0].isCorrect").value(false),
				jsonPath("$.data.groups[0].submittedAnswer.submitAnswers[0]").value(2),
				jsonPath("$.data.groups[1].submitCount").value(2),
				jsonPath("$.data.groups[1].isCorrect").value(true),
				jsonPath("$.data.groups[1].submittedAnswer.submitAnswers[0]").value(1));
	}

	@Test
	@DisplayName("firstSubmitOnly=true 면 유저당 최초 제출만으로 분포를 구성한다")
	void getAnswerDistribution_firstSubmitOnly() throws Exception {
		// given
		MultipleQuestionEntity question = saveQuestionWithChoices();

		saveRecord(1L, question.getId(), false, 1L, ANSWER_2);
		saveRecord(1L, question.getId(), true, 2L, ANSWER_1);
		saveRecord(2L, question.getId(), false, 1L, ANSWER_2);
		saveRecord(3L, question.getId(), true, 1L, ANSWER_1);
		saveRecord(4L, question.getId(), false, 1L, ANSWER_2);

		// when & then
		mockMvc.perform(get(URL, question.getQuestionSet().getId(), question.getId())
				.param("firstSubmitOnly", "true"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				// 최초 제출만: [2] 3건(user1·2·4), [1] 1건(user3)
				jsonPath("$.data.totalSubmitCount").value(4),
				jsonPath("$.data.submittedUserCount").value(4),
				jsonPath("$.data.correctUserCount").value(1),
				jsonPath("$.data.correctRate").value(25.0),
				jsonPath("$.data.groups.length()").value(2),
				jsonPath("$.data.groups[0].submitCount").value(3),
				jsonPath("$.data.groups[1].submitCount").value(1));
	}

	private MultipleQuestionEntity saveQuestionWithChoices() {
		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.solveMode(QuestionSetSolveMode.LIVE_TIME)
				.status(QuestionSetStatus.AFTER)
				.build());

		MultipleQuestionEntity question = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet)
				.content("문제 내용")
				.explanation("해설")
				.number(1L)
				.lexoRank("a")
				.build());

		multipleChoiceEntityRepository.saveAll(List.of(
			MultipleChoiceEntity.builder().number(1).content("정답").isCorrect(true).question(question).build(),
			MultipleChoiceEntity.builder().number(2).content("오답").isCorrect(false).question(question).build()));

		return question;
	}

	private void saveRecord(final Long userId, final Long questionId, final boolean isCorrect, final Long submitOrder,
		final String submittedAnswer) {
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(userId)
			.questionId(questionId)
			.isCorrect(isCorrect)
			.submitOrder(submitOrder)
			.submittedAnswer(submittedAnswer)
			.build());
	}
}
