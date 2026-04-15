package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.UserStudyStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.solve.service.StudyModeService;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.web.question.dto.QuestionSetGroupBy;
import com.coniv.mait.web.question.dto.QuestionSetView;
import com.coniv.mait.web.question.dto.QuestionSetViewApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetViewItem;
import com.coniv.mait.web.question.dto.QuestionSetViewSection;
import com.coniv.mait.web.question.dto.QuestionSetViewType;
import com.coniv.mait.web.question.dto.StudyQuestionSetDto;
import com.coniv.mait.web.question.dto.StudyQuestionSetGroup;
import com.coniv.mait.web.question.dto.UserParticipationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetViewQueryService {

	private static final List<QuestionSetStatus> ACTIVE_QUESTION_SET_STATUSES =
		List.of(QuestionSetStatus.BEFORE, QuestionSetStatus.ONGOING, QuestionSetStatus.AFTER);

	private static final List<QuestionSetStatus> LIVE_SOLVING_DISPLAY_STATUSES =
		List.of(QuestionSetStatus.ONGOING, QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW);

	private static final String ALL_SECTION_KEY = "ALL";
	private static final String ALL_SECTION_TITLE = "전체";

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionSetParticipantRepository questionSetParticipantRepository;
	private final TeamRoleValidator teamRoleValidator;
	private final StudyModeService studyModeService;

	@Transactional(readOnly = true)
	public QuestionSetViewApiResponse getLiveSolvingQuestionSets(final Long teamId, final MaitUser user) {
		teamRoleValidator.checkIsTeamMember(teamId, user.id());

		List<QuestionSetEntity> questionSets = questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
				teamId,
				QuestionSetSolveMode.LIVE_TIME,
				LIVE_SOLVING_DISPLAY_STATUSES)
			.stream()
			.sorted(latestFirst())
			.toList();
		Map<Long, QuestionSetParticipantEntity> participantByQuestionSetId =
			questionSetParticipantRepository.findAllByUserIdAndQuestionSetTeamId(user.id(), teamId).stream()
				.collect(Collectors.toMap(
					participant -> participant.getQuestionSet().getId(),
					Function.identity(),
					(left, right) -> left));

		Map<UserParticipationStatus, List<QuestionSetViewItem>> itemsByStatus = questionSets.stream()
			.map(questionSet -> liveSolvingItem(questionSet, participantByQuestionSetId.get(questionSet.getId())))
			.collect(Collectors.groupingBy(
				QuestionSetViewItem::userParticipationStatus,
				() -> new EnumMap<>(UserParticipationStatus.class),
				Collectors.toList()));

		List<QuestionSetViewSection> sections = List.of(
			participationSection(UserParticipationStatus.NOT_PARTICIPATED, itemsByStatus),
			participationSection(UserParticipationStatus.PARTICIPATING, itemsByStatus),
			participationSection(UserParticipationStatus.ELIMINATED, itemsByStatus),
			participationSection(UserParticipationStatus.FINISHED, itemsByStatus));

		return QuestionSetViewApiResponse.of(
			QuestionSetView.SOLVING,
			QuestionSetViewType.LIVE_TIME,
			QuestionSetGroupBy.USER_PARTICIPATION_STATUS,
			sections);
	}

	private QuestionSetViewItem liveSolvingItem(final QuestionSetEntity questionSet,
		final QuestionSetParticipantEntity participant) {
		UserParticipationStatus userParticipationStatus = questionSet.getStatus() == QuestionSetStatus.ONGOING
			? UserParticipationStatus.fromOngoingQuestionSet(participant)
			: UserParticipationStatus.FINISHED;

		return QuestionSetViewItem.of(questionSet, participant, userParticipationStatus);
	}

	@Transactional(readOnly = true)
	public QuestionSetViewApiResponse getStudySolvingQuestionSets(final Long teamId, final MaitUser user) {
		StudyQuestionSetGroup studyGroup = studyModeService.getStudyQuestionSets(teamId, user);
		Map<UserStudyStatus, List<StudyQuestionSetDto>> questionSets = studyGroup.questionSets();

		List<QuestionSetViewSection> sections = List.of(
			studySection(UserStudyStatus.BEFORE, questionSets),
			studySection(UserStudyStatus.ONGOING, questionSets),
			studySection(UserStudyStatus.AFTER, questionSets));

		return QuestionSetViewApiResponse.of(
			QuestionSetView.SOLVING,
			QuestionSetViewType.STUDY,
			QuestionSetGroupBy.USER_STUDY_STATUS,
			sections);
	}

	@Transactional(readOnly = true)
	public QuestionSetViewApiResponse getReviewSolvingQuestionSets(final Long teamId, final MaitUser user) {
		teamRoleValidator.checkIsTeamMember(teamId, user.id());
		return ungrouped(QuestionSetView.SOLVING, QuestionSetViewType.REVIEW, findReviewQuestionSets(teamId));
	}

	@Transactional(readOnly = true)
	public QuestionSetViewApiResponse getLiveManagementQuestionSets(final Long teamId, final MaitUser user) {
		checkManagementAuthority(teamId, user);
		return groupedByQuestionSetStatus(
			QuestionSetView.MANAGEMENT,
			QuestionSetViewType.LIVE_TIME,
			findBySolveMode(teamId, QuestionSetSolveMode.LIVE_TIME));
	}

	@Transactional(readOnly = true)
	public QuestionSetViewApiResponse getStudyManagementQuestionSets(final Long teamId, final MaitUser user) {
		checkManagementAuthority(teamId, user);
		return groupedByQuestionSetStatus(
			QuestionSetView.MANAGEMENT,
			QuestionSetViewType.STUDY,
			findBySolveMode(teamId, QuestionSetSolveMode.STUDY));
	}

	@Transactional(readOnly = true)
	public QuestionSetViewApiResponse getReviewManagementQuestionSets(final Long teamId, final MaitUser user) {
		checkManagementAuthority(teamId, user);
		return ungrouped(QuestionSetView.MANAGEMENT, QuestionSetViewType.REVIEW, findReviewQuestionSets(teamId));
	}

	@Transactional(readOnly = true)
	public QuestionSetViewApiResponse getMakingManagementQuestionSets(final Long teamId, final MaitUser user) {
		checkManagementAuthority(teamId, user);
		return ungrouped(QuestionSetView.MANAGEMENT, QuestionSetViewType.MAKING, findMakingQuestionSets(teamId));
	}

	private void checkManagementAuthority(final Long teamId, final MaitUser user) {
		teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, user.id());
	}

	private QuestionSetViewApiResponse groupedByQuestionSetStatus(final QuestionSetView view,
		final QuestionSetViewType type, final List<QuestionSetDto> questionSets) {
		Map<QuestionSetStatus, List<QuestionSetViewItem>> itemsByStatus = questionSets.stream()
			.map(QuestionSetViewItem::from)
			.collect(Collectors.groupingBy(
				QuestionSetViewItem::status,
				() -> new EnumMap<>(QuestionSetStatus.class),
				Collectors.toList()));

		List<QuestionSetViewSection> sections = ACTIVE_QUESTION_SET_STATUSES.stream()
			.map(status -> questionSetStatusSection(status, itemsByStatus))
			.toList();

		return QuestionSetViewApiResponse.of(view, type, QuestionSetGroupBy.QUESTION_SET_STATUS, sections);
	}

	private QuestionSetViewApiResponse ungrouped(final QuestionSetView view, final QuestionSetViewType type,
		final List<QuestionSetDto> questionSets) {
		List<QuestionSetViewItem> items = questionSets.stream()
			.map(QuestionSetViewItem::from)
			.toList();
		List<QuestionSetViewSection> sections = List.of(
			QuestionSetViewSection.of(ALL_SECTION_KEY, ALL_SECTION_TITLE, items));

		return QuestionSetViewApiResponse.of(view, type, QuestionSetGroupBy.NONE, sections);
	}

	private QuestionSetViewSection questionSetStatusSection(final QuestionSetStatus status,
		final Map<QuestionSetStatus, List<QuestionSetViewItem>> itemsByStatus) {
		return QuestionSetViewSection.of(
			status.name(),
			status.getDescription(),
			itemsByStatus.getOrDefault(status, List.of()));
	}

	private QuestionSetViewSection participationSection(final UserParticipationStatus status,
		final Map<UserParticipationStatus, List<QuestionSetViewItem>> itemsByStatus) {
		return QuestionSetViewSection.of(
			status.name(),
			status.getDescription(),
			itemsByStatus.getOrDefault(status, List.of()));
	}

	private QuestionSetViewSection studySection(final UserStudyStatus status,
		final Map<UserStudyStatus, List<StudyQuestionSetDto>> questionSets) {
		List<QuestionSetViewItem> items = questionSets.getOrDefault(status, List.of()).stream()
			.map(QuestionSetViewItem::from)
			.toList();
		return QuestionSetViewSection.of(status.name(), status.getDescription(), items);
	}

	private List<QuestionSetDto> findBySolveMode(final Long teamId, final QuestionSetSolveMode solveMode) {
		return findEntitiesBySolveMode(teamId, solveMode).stream()
			.map(QuestionSetDto::from)
			.toList();
	}

	private List<QuestionSetEntity> findEntitiesBySolveMode(final Long teamId, final QuestionSetSolveMode solveMode) {
		return questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
				teamId,
				solveMode,
				ACTIVE_QUESTION_SET_STATUSES)
			.stream()
			.sorted(latestFirst())
			.toList();
	}

	private List<QuestionSetDto> findReviewQuestionSets(final Long teamId) {
		return questionSetEntityRepository.findAllByTeamIdAndStatus(teamId, QuestionSetStatus.REVIEW).stream()
			.sorted(latestFirst())
			.map(QuestionSetDto::from)
			.toList();
	}

	private List<QuestionSetDto> findMakingQuestionSets(final Long teamId) {
		return questionSetEntityRepository.findAllByTeamId(teamId).stream()
			.filter(questionSet -> questionSet.getSolveMode() == null)
			.sorted(latestFirst())
			.map(QuestionSetDto::from)
			.toList();
	}

	private Comparator<QuestionSetEntity> latestFirst() {
		return Comparator.comparing(
			QuestionSetEntity::getModifiedAt,
			Comparator.nullsLast(Comparator.naturalOrder())).reversed();
	}
}
