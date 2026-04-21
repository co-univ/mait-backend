package com.coniv.mait.domain.question.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.event.QuestionSetDeletedEvent;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.event.MaitEventPublisher;

@ExtendWith(MockitoExtension.class)
class QuestionSetDeleteServiceTest {

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;
	@Mock
	private QuestionEntityRepository questionEntityRepository;
	@Mock
	private QuestionSetParticipantRepository questionSetParticipantRepository;
	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;
	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;
	@Mock
	private QuestionScorerEntityRepository questionScorerEntityRepository;
	@Mock
	private StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;
	@Mock
	private TeamRoleValidator teamRoleValidator;
	@Mock
	private MaitEventPublisher maitEventPublisher;
	@Mock
	private QuestionSetReader questionSetReader;

	private QuestionFactory<?>  multipleFactory;
	private QuestionFactory<?> orderingFactory;
	private QuestionFactory<?> shortFactory;
	private QuestionFactory<?> fillBlankFactory;

	private QuestionSetDeleteService questionSetDeleteService;

	@BeforeEach
	void setUp() {
		multipleFactory = mock(QuestionFactory.class);
		orderingFactory = mock(QuestionFactory.class);
		shortFactory = mock(QuestionFactory.class);
		fillBlankFactory = mock(QuestionFactory.class);

		when(multipleFactory.getQuestionType()).thenReturn(QuestionType.MULTIPLE);
		when(orderingFactory.getQuestionType()).thenReturn(QuestionType.ORDERING);
		when(shortFactory.getQuestionType()).thenReturn(QuestionType.SHORT);
		when(fillBlankFactory.getQuestionType()).thenReturn(QuestionType.FILL_BLANK);

		questionSetDeleteService = new QuestionSetDeleteService(
			List.of(multipleFactory, orderingFactory, shortFactory, fillBlankFactory),
			questionSetEntityRepository,
			questionEntityRepository,
			questionSetParticipantRepository,
			solvingSessionEntityRepository,
			answerSubmitRecordEntityRepository,
			questionScorerEntityRepository,
			studyAnswerDraftEntityRepository,
			teamRoleValidator,
			maitEventPublisher,
			questionSetReader
		);
	}

	@Test
	void deleteQuestionSet_liveAfter_deletesParticipantsAndSubEntitiesViaFactory() {
		Long questionSetId = 10L;
		Long userId = 20L;
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.id(questionSetId)
			.teamId(30L)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.AFTER)
			.build();

		MultipleQuestionEntity multipleQuestion = MultipleQuestionEntity.builder()
			.id(1L).questionSet(questionSet).lexoRank("a").build();
		OrderingQuestionEntity orderingQuestion = OrderingQuestionEntity.builder()
			.id(2L).questionSet(questionSet).lexoRank("b").build();
		ShortQuestionEntity shortQuestion = ShortQuestionEntity.builder()
			.id(3L).questionSet(questionSet).lexoRank("c").build();
		FillBlankQuestionEntity fillBlankQuestion = FillBlankQuestionEntity.builder()
			.id(4L).questionSet(questionSet).lexoRank("d").build();
		List<QuestionEntity> questions = List.of(
			multipleQuestion, orderingQuestion, shortQuestion, fillBlankQuestion);

		when(questionSetReader.getActiveQuestionSet(questionSetId)).thenReturn(questionSet);
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);

		questionSetDeleteService.deleteQuestionSet(questionSetId, userId);

		verify(answerSubmitRecordEntityRepository).deleteAllByQuestionIdIn(List.of(1L, 2L, 3L, 4L));
		verify(questionSetParticipantRepository).deleteAllByQuestionSetId(questionSetId);
		verify(questionScorerEntityRepository).deleteAllByQuestionIdIn(List.of(1L, 2L, 3L, 4L));
		verify(multipleFactory).deleteSubEntities(multipleQuestion);
		verify(orderingFactory).deleteSubEntities(orderingQuestion);
		verify(shortFactory).deleteSubEntities(shortQuestion);
		verify(fillBlankFactory).deleteSubEntities(fillBlankQuestion);
		verify(questionEntityRepository).deleteAllByQuestionSetId(questionSetId);
		verify(questionSetEntityRepository).deleteById(questionSetId);
		verify(maitEventPublisher).publishEvent(QuestionSetDeletedEvent.builder()
			.questionSetId(questionSetId)
			.questionIds(List.of(1L, 2L, 3L, 4L))
			.imageIds(List.of())
			.build());
	}

	@Test
	void deleteQuestionSet_studyOngoing_deletesSessionsWithoutParticipantCleanup() {
		Long questionSetId = 11L;
		Long userId = 21L;
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.id(questionSetId)
			.teamId(31L)
			.solveMode(QuestionSetSolveMode.STUDY)
			.status(QuestionSetStatus.ONGOING)
			.build();
		ShortQuestionEntity shortQuestion = ShortQuestionEntity.builder()
			.id(5L).questionSet(questionSet).lexoRank("e").imageId(100L).build();
		List<QuestionEntity> questions = List.of(shortQuestion);

		when(questionSetReader.getActiveQuestionSet(questionSetId)).thenReturn(questionSet);
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId)).thenReturn(questions);
		when(solvingSessionEntityRepository.findSessionIdsByQuestionSetId(questionSetId))
			.thenReturn(List.of(101L, 102L));

		questionSetDeleteService.deleteQuestionSet(questionSetId, userId);

		verify(answerSubmitRecordEntityRepository).deleteAllByQuestionIdIn(List.of(5L));
		verify(studyAnswerDraftEntityRepository).deleteAllBySolvingSessionIdIn(List.of(101L, 102L));
		verify(solvingSessionEntityRepository).deleteAllByQuestionSetId(questionSetId);
		verify(questionSetParticipantRepository, never()).deleteAllByQuestionSetId(anyLong());
		verify(questionScorerEntityRepository, never()).deleteAllByQuestionIdIn(anyList());
		verify(shortFactory).deleteSubEntities(shortQuestion);
		verify(maitEventPublisher).publishEvent(QuestionSetDeletedEvent.builder()
			.questionSetId(questionSetId)
			.questionIds(List.of(5L))
			.imageIds(List.of(100L))
			.build());
	}
}
