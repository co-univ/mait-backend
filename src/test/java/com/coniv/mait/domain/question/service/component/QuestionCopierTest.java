package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;

@ExtendWith(MockitoExtension.class)
class QuestionCopierTest {

	private static final Long SOURCE_QUESTION_SET_ID = 1L;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private MultipleQuestionFactory multipleQuestionFactory;

	@Mock
	private ShortQuestionFactory shortQuestionFactory;

	@Mock
	private QuestionImageCopier questionImageCopier;

	@Mock
	private QuestionSetEntity targetQuestionSet;

	@Test
	@DisplayName("문제 복제 - 원본에 문제가 없으면 저장을 시도하지 않는다")
	void copyQuestions_noQuestions_doesNotSave() {
		// given
		doReturn(List.of()).when(questionEntityRepository).findAllByQuestionSetId(SOURCE_QUESTION_SET_ID);

		// when
		questionCopier().copyQuestions(SOURCE_QUESTION_SET_ID, targetQuestionSet);

		// then
		verify(questionEntityRepository, never()).saveAll(any());
		verify(multipleQuestionFactory, never()).copyQuestion(any(), any());
		verify(multipleQuestionFactory, never()).copySubEntities(any());
		verify(shortQuestionFactory, never()).copyQuestion(any(), any());
		verify(shortQuestionFactory, never()).copySubEntities(any());
	}

	@Test
	@DisplayName("문제 복제 - 유형별 팩토리에 위임해 문제를 복제하고 한 번에 저장한다")
	void copyQuestions_delegatesToFactoryByType() {
		// given
		MultipleQuestionEntity source = multipleQuestion(10L);
		doReturn(List.<QuestionEntity>of(source)).when(questionEntityRepository)
			.findAllByQuestionSetId(SOURCE_QUESTION_SET_ID);

		MultipleQuestionEntity copied = multipleQuestion(100L);
		doReturn(copied).when(multipleQuestionFactory).copyQuestion(source, targetQuestionSet);

		// when
		questionCopier().copyQuestions(SOURCE_QUESTION_SET_ID, targetQuestionSet);

		// then
		ArgumentCaptor<Collection<QuestionEntity>> savedCaptor = ArgumentCaptor.captor();
		verify(questionEntityRepository).saveAll(savedCaptor.capture());
		assertThat(savedCaptor.getValue()).containsExactly(copied);
	}

	@Test
	@DisplayName("문제 복제 - 하위 엔티티 복제는 유형별로 한 번씩만 호출해 N+1 을 피한다")
	void copyQuestions_callsSubEntityCopyOncePerType() {
		// given
		MultipleQuestionEntity firstMultiple = multipleQuestion(10L);
		MultipleQuestionEntity secondMultiple = multipleQuestion(11L);
		ShortQuestionEntity shortQuestion = shortQuestion(12L);
		doReturn(List.<QuestionEntity>of(firstMultiple, secondMultiple, shortQuestion))
			.when(questionEntityRepository).findAllByQuestionSetId(SOURCE_QUESTION_SET_ID);

		doReturn(multipleQuestion(100L)).when(multipleQuestionFactory).copyQuestion(firstMultiple, targetQuestionSet);
		doReturn(multipleQuestion(101L)).when(multipleQuestionFactory).copyQuestion(secondMultiple, targetQuestionSet);
		doReturn(shortQuestion(102L)).when(shortQuestionFactory).copyQuestion(shortQuestion, targetQuestionSet);

		// when
		questionCopier().copyQuestions(SOURCE_QUESTION_SET_ID, targetQuestionSet);

		// then
		ArgumentCaptor<Map<Long, QuestionEntity>> multipleCaptor = ArgumentCaptor.captor();
		verify(multipleQuestionFactory, times(1)).copySubEntities(multipleCaptor.capture());
		assertThat(multipleCaptor.getValue()).containsOnlyKeys(10L, 11L);

		ArgumentCaptor<Map<Long, QuestionEntity>> shortCaptor = ArgumentCaptor.captor();
		verify(shortQuestionFactory, times(1)).copySubEntities(shortCaptor.capture());
		assertThat(shortCaptor.getValue()).containsOnlyKeys(12L);
	}


	@Test
	@DisplayName("문제 복제 - 이미지가 있는 문제는 복제된 이미지로 교체한다")
	void copyQuestions_replacesImageWithCopiedOne() {
		// given
		MultipleQuestionEntity source = multipleQuestion(10L);
		doReturn(List.<QuestionEntity>of(source)).when(questionEntityRepository)
			.findAllByQuestionSetId(SOURCE_QUESTION_SET_ID);

		MultipleQuestionEntity copied = MultipleQuestionEntity.builder()
			.id(100L).lexoRank("a").imageId(7L).imageUrl("https://bucket/origin.png").build();
		doReturn(copied).when(multipleQuestionFactory).copyQuestion(source, targetQuestionSet);

		QuestionImageEntity copiedImage = QuestionImageEntity.builder()
			.id(77L).url("https://bucket/copied.png").build();
		doReturn(Map.of(7L, copiedImage)).when(questionImageCopier).copyAll(List.of(7L));

		// when
		questionCopier().copyQuestions(SOURCE_QUESTION_SET_ID, targetQuestionSet);

		// then
		assertThat(copied.getImageId()).isEqualTo(77L);
		assertThat(copied.getImageUrl()).isEqualTo("https://bucket/copied.png");
	}

	@Test
	@DisplayName("문제 복제 - 이미지가 없는 문제는 이미지 복제를 요청하지 않는다")
	void copyQuestions_noImage_skipsImageCopy() {
		// given
		MultipleQuestionEntity source = multipleQuestion(10L);
		doReturn(List.<QuestionEntity>of(source)).when(questionEntityRepository)
			.findAllByQuestionSetId(SOURCE_QUESTION_SET_ID);

		MultipleQuestionEntity copied = multipleQuestion(100L);
		doReturn(copied).when(multipleQuestionFactory).copyQuestion(source, targetQuestionSet);
		doReturn(Map.<Long, QuestionImageEntity>of()).when(questionImageCopier).copyAll(List.of());

		// when
		questionCopier().copyQuestions(SOURCE_QUESTION_SET_ID, targetQuestionSet);

		// then
		assertThat(copied.getImageId()).isNull();
		verify(questionImageCopier).copyAll(List.of());
	}

	@Test
	@DisplayName("문제 복제 - 여러 문제가 같은 이미지를 쓰면 한 번만 복제를 요청한다")
	void copyQuestions_sharedImage_copiedOnce() {
		// given
		MultipleQuestionEntity firstSource = multipleQuestion(10L);
		MultipleQuestionEntity secondSource = multipleQuestion(11L);
		doReturn(List.<QuestionEntity>of(firstSource, secondSource)).when(questionEntityRepository)
			.findAllByQuestionSetId(SOURCE_QUESTION_SET_ID);

		MultipleQuestionEntity firstCopied = MultipleQuestionEntity.builder()
			.id(100L).lexoRank("a").imageId(7L).build();
		MultipleQuestionEntity secondCopied = MultipleQuestionEntity.builder()
			.id(101L).lexoRank("a").imageId(7L).build();
		doReturn(firstCopied).when(multipleQuestionFactory).copyQuestion(firstSource, targetQuestionSet);
		doReturn(secondCopied).when(multipleQuestionFactory).copyQuestion(secondSource, targetQuestionSet);

		QuestionImageEntity copiedImage = QuestionImageEntity.builder()
			.id(77L).url("https://bucket/copied.png").build();
		doReturn(Map.of(7L, copiedImage)).when(questionImageCopier).copyAll(List.of(7L));

		// when
		questionCopier().copyQuestions(SOURCE_QUESTION_SET_ID, targetQuestionSet);

		// then
		verify(questionImageCopier).copyAll(List.of(7L));
		assertThat(firstCopied.getImageId()).isEqualTo(77L);
		assertThat(secondCopied.getImageId()).isEqualTo(77L);
	}

	private QuestionCopier questionCopier() {
		doReturn(QuestionType.MULTIPLE).when(multipleQuestionFactory).getQuestionType();
		doReturn(QuestionType.SHORT).when(shortQuestionFactory).getQuestionType();
		return new QuestionCopier(questionEntityRepository, questionImageCopier,
			List.of(multipleQuestionFactory, shortQuestionFactory));
	}

	private MultipleQuestionEntity multipleQuestion(final Long id) {
		return MultipleQuestionEntity.builder().id(id).lexoRank("a").build();
	}

	private ShortQuestionEntity shortQuestion(final Long id) {
		return ShortQuestionEntity.builder().id(id).lexoRank("a").build();
	}
}
