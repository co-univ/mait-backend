package com.coniv.mait.domain.team.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "team_invites")
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamInviteEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity inviter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private TeamEntity team;

	@Column(nullable = false, unique = true)
	private String token;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	public static TeamInviteEntity createInvite(UserEntity inviter, TeamEntity team, String token) {
		return TeamInviteEntity.builder()
			.inviter(inviter)
			.team(team)
			.token(token)
			.expiresAt(LocalDateTime.now())
			.build();
	}
}
