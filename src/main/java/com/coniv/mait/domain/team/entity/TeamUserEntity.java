package com.coniv.mait.domain.team.entity;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "team_users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamUserEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private TeamEntity team;

	@Enumerated(EnumType.STRING)
	private TeamUserRole userRole;

	private TeamUserEntity(UserEntity user, TeamEntity team, TeamUserRole userRole) {
		this.user = user;
		this.team = team;
		this.userRole = userRole;
	}

	public static TeamUserEntity createPlayerUser(UserEntity user, TeamEntity team) {
		return new TeamUserEntity(user, team, TeamUserRole.PLAYER);
	}

	public static TeamUserEntity createOwnerUser(UserEntity user, TeamEntity team) {
		return new TeamUserEntity(user, team, TeamUserRole.OWNER);
	}

	public static TeamUserEntity createTeamUser(UserEntity user, TeamEntity team, TeamUserRole role) {
		return new TeamUserEntity(user, team, role);
	}

	public boolean canInvite() {
		return this.userRole == TeamUserRole.OWNER || this.userRole == TeamUserRole.MAKER;
	}

	public boolean canApproveApplications() {
		return this.userRole == TeamUserRole.OWNER || this.userRole == TeamUserRole.MAKER;
	}
}
