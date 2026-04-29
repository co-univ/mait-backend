package com.coniv.mait.domain.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.user.entity.UserEntity;

public interface TeamUserEntityRepository extends JpaRepository<TeamUserEntity, Long> {
	List<TeamUserEntity> findAllByTeamId(Long teamId);

	Optional<TeamUserEntity> findByTeamAndUser(TeamEntity team, UserEntity user);

	boolean existsByTeamAndUser(TeamEntity team, UserEntity user);

	@Query("select tu from TeamUserEntity tu join fetch tu.team t where tu.user = :user and t.deletedAt is null")
	List<TeamUserEntity> findAllByUserFetchJoinActiveTeam(@Param("user") final UserEntity user);

	@Query("select tu from TeamUserEntity tu join fetch tu.user where tu.team.id = :teamId")
	List<TeamUserEntity> findAllByTeamIdFetchJoinUser(@Param("teamId") Long teamId);

	Optional<TeamUserEntity> findByTeamIdAndUserRole(Long teamId, TeamUserRole userRole);

	@Query("""
		SELECT count(tu)
		FROM TeamUserEntity tu
		LEFT JOIN SolvingSessionEntity ss
			ON ss.user.id = tu.user.id
			AND ss.questionSet.id = :questionSetId
			AND ss.solveMode = com.coniv.mait.domain.question.enums.QuestionSetSolveMode.STUDY
			AND ss.status = com.coniv.mait.domain.solve.enums.SolvingStatus.COMPLETE
		WHERE tu.team.id = :teamId
			AND ss.id IS NULL
		""")
	long countTeamMembersWithoutCompletedStudySession(
		@Param("teamId") Long teamId,
		@Param("questionSetId") Long questionSetId);
}
