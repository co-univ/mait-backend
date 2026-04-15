package com.coniv.mait.domain.solve.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftId;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.component.StudyAnswerDraftFactory;
import com.coniv.mait.domain.solve.service.dto.SolvingSessionDto;
import com.coniv.mait.domain.solve.service.dto.StudyAnswerDraftDto;
import com.coniv.mait.domain.solve.service.dto.StudyGradeResultDto;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.web.question.dto.StudyQuestionSetDto;
import com.coniv.mait.web.question.dto.StudyQuestionSetGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyModeService {

	private static final List<QuestionSetStatus> STUDY_DISPLAY_STATUSES =
		List.of(QuestionSetStatus.BEFORE, QuestionSetStatus.ONGOING, QuestionSetStatus.AFTER);

	private final ObjectMapper objectMapper;
	private final UserReader userReader;
	private final TeamRoleValidator teamRoleValidator;
	private final StudyAnswerDraftFactory studyAnswerDraftFactory;
	private final QuestionReader questionReader;
	private final AnswerGrader answerGrader;

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final SolvingSessionEntityRepository solvingSessionEntityRepository;
	private final StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Transactional(readOnly = true)
	public StudyQuestionSetGroup getStudyQuestionSets(final Long teamId, final MaitUser user) {
		teamRoleValidator.checkIsTeamMember(teamId, user.id());

		Map<Long, SolvingSessionEntity> sessionByQuestionSetId = solvingSessionEntityRepository.findAllByUserIdAndModeAndQuestionSetTeamId(
				user.id(), DeliveryMode.STUDY, teamId).stream()
			.collect(Collectors.toUnmodifiableMap(session ->
				session.getQuestionSet().getId(), Function.identity()));

		List<StudyQuestionSetDto> questionSets = questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
				teamId, QuestionSetSolveMode.STUDY, STUDY_DISPLAY_STATUSES).stream()
			.sorted(Comparator.comparing(
				QuestionSetEntity::getModifiedAt,
				Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.map(questionSet -> StudyQuestionSetDto.of(questionSet, sessionByQuestionSetId.get(questionSet.getId())))
			.toList();

		return StudyQuestionSetGroup.from(questionSets);
	}

	@Transactional
	public SolvingSessionDto startStudyMode(final MaitUser maitUser, final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문제 셋 입니다."));

		UserEntity user = userReader.getById(maitUser.id());

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), user.getId());

		Optional<SolvingSessionEntity> maybeSession = solvingSessionEntityRepository
			.findByUserIdAndQuestionSetIdAndMode(user.getId(), questionSet.getId(), DeliveryMode.STUDY);

		if (maybeSession.isPresent()) {
			return SolvingSessionDto.from(maybeSession.get());
		}

		SolvingSessionEntity solvingSession = solvingSessionEntityRepository.save(
			SolvingSessionEntity.studySession(user, questionSet));

		studyAnswerDraftFactory.createDrafts(solvingSession, questionSet.getId());

		return SolvingSessionDto.from(solvingSession);
	}

	public List<StudyAnswerDraftDto> getStudyAnswerDrafts(final MaitUser maitUser, final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문제 셋 입니다."));

		UserEntity user = userReader.getById(maitUser.id());

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), user.getId());

		SolvingSessionEntity solvingSession = solvingSessionEntityRepository
			.findByUserIdAndQuestionSetIdAndMode(user.getId(), questionSet.getId(), DeliveryMode.STUDY)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 학습 세션 입니다."));

		return studyAnswerDraftFactory.getDraftsBySolvingSessionId(solvingSession.getId()).stream()
			.map(StudyAnswerDraftDto::from)
			.toList();
	}

	@Transactional
	public StudyGradeResultDto gradeStudySession(final MaitUser maitUser, final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문제 셋 입니다."));

		UserEntity user = userReader.getById(maitUser.id());

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), user.getId());

		SolvingSessionEntity solvingSession = solvingSessionEntityRepository
			.findByUserIdAndQuestionSetIdAndMode(user.getId(), questionSet.getId(), DeliveryMode.STUDY)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 학습 세션 입니다."));

		if (solvingSession.getStatus() == SolvingStatus.COMPLETE) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.ALREADY_GRADED);
		}

		List<StudyAnswerDraftEntity> drafts = studyAnswerDraftFactory.getDraftsBySolvingSessionId(
			solvingSession.getId());

		List<AnswerSubmitRecordEntity> records = new ArrayList<>();

		for (StudyAnswerDraftEntity draft : drafts) {
			Long questionId = draft.getQuestionId();
			boolean isCorrect = false;
			String submittedAnswer = draft.getSubmittedAnswer();

			if (draft.isSubmitted() && submittedAnswer != null) {
				QuestionEntity question = questionReader.getQuestion(questionId);
				SubmitAnswerDto<?> submitAnswerDto = SubmitAnswerDto.fromJson(submittedAnswer);
				isCorrect = answerGrader.gradeAnswer(question, submitAnswerDto);
			}

			AnswerSubmitRecordEntity record = AnswerSubmitRecordEntity.builder()
				.userId(user.getId())
				.questionId(questionId)
				.submitOrder(null)
				.isCorrect(isCorrect)
				.submittedAnswer(submittedAnswer)
				.build();

			records.add(record);
		}

		answerSubmitRecordEntityRepository.saveAll(records);

		int totalCount = drafts.size();
		int correctCount = (int)records.stream()
			.filter(AnswerSubmitRecordEntity::isCorrect)
			.count();

		solvingSession.submit(totalCount, correctCount);

		return StudyGradeResultDto.of(solvingSession, records);
	}

	@Transactional
	public StudyAnswerDraftDto updateStudyAnswerDraft(final MaitUser maitUser, final Long questionSetId,
		final Long questionId, final SubmitAnswerDto<?> submittedAnswer) throws JsonProcessingException {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문제 셋 입니다."));

		UserEntity user = userReader.getById(maitUser.id());

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), user.getId());

		SolvingSessionEntity solvingSession = solvingSessionEntityRepository
			.findByUserIdAndQuestionSetIdAndMode(user.getId(), questionSet.getId(), DeliveryMode.STUDY)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 학습 세션 입니다."));

		StudyAnswerDraftId draftId = StudyAnswerDraftId.builder()
			.solvingSessionId(solvingSession.getId())
			.questionId(questionId)
			.build();

		StudyAnswerDraftEntity draft = studyAnswerDraftEntityRepository.findById(draftId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 답안 초안 입니다."));

		draft.updateSubmittedAnswer(objectMapper.writeValueAsString(submittedAnswer));

		return StudyAnswerDraftDto.from(draft);
	}
}
