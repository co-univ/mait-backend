package com.coniv.mait.domain.solve.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.service.component.FillBlankQuestionValidator;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.web.solve.dto.FillBlankUpdateAnswerPayload;

@ExtendWith(MockitoExtension.class)
class FillBlankQuestionAnswerUpdaterTest {

	@Mock
	private FillBlankAnswerEntityRepository repository;

	@Mock
	private FillBlankQuestionValidator validator;

	@InjectMocks
	private FillBlankQuestionAnswerUpdater updater;

	@Captor
	private ArgumentCaptor<List<FillBlankAnswerEntity>> answersCaptor;

	@Test
	@DisplayName("검증 후 기존 그룹에 인정답안만 저장한다")
	void updateAnswer_validatesBeforeSaving() {
		// given
		FillBlankQuestionEntity question = FillBlankQuestionEntity.builder().id(1L).build();
		FillBlankUpdateAnswerPayload payload = new FillBlankUpdateAnswerPayload(List.of(
			FillBlankAnswerDto.builder().number(1L).answer(" gamma ").main(false).build(),
			FillBlankAnswerDto.builder().number(2L).answer("epsilon").main(false).build()
		));

		// when
		updater.updateAnswer(question, payload);

		// then
		verify(validator).validateAcceptedAnswerAddition(1L, payload.answers());
		verify(repository).saveAll(answersCaptor.capture());
		assertThat(answersCaptor.getValue())
			.extracting(FillBlankAnswerEntity::getNumber, FillBlankAnswerEntity::getAnswer,
				FillBlankAnswerEntity::isMain)
			.containsExactly(tuple(1L, " gamma ", false), tuple(2L, "epsilon", false));
		assertThat(answersCaptor.getValue()).allSatisfy(answer -> {
			assertThat(answer.getId()).isNull();
			assertThat(answer.getFillBlankQuestionId()).isEqualTo(1L);
		});
	}

	@Test
	@DisplayName("검증 실패 시 유효한 항목도 일부 저장하지 않는다")
	void updateAnswer_validationFailureDoesNotSave() {
		// given
		FillBlankQuestionEntity question = FillBlankQuestionEntity.builder().id(1L).build();
		FillBlankUpdateAnswerPayload payload = new FillBlankUpdateAnswerPayload(List.of(
			FillBlankAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
			FillBlankAnswerDto.builder().number(3L).answer("epsilon").main(false).build()
		));
		UserParameterException exception = new UserParameterException("존재하지 않는 번호입니다.");
		doThrow(exception).when(validator).validateAcceptedAnswerAddition(1L, payload.answers());

		// when & then
		assertThatThrownBy(() -> updater.updateAnswer(question, payload)).isSameAs(exception);
		verifyNoInteractions(repository);
	}
}
