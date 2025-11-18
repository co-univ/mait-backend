package com.coniv.mait.domain.team.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.entity.BaseTimeEntity;
import com.coniv.mait.global.enums.InviteTokenDuration;

import jakarta.persistence.Column;
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
	private UserEntity invitor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private TeamEntity team;

	@Column(nullable = false, unique = true)
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InviteTokenDuration tokenDuration;

	@Column
	private boolean requiresApproval;

	@Column
	private LocalDateTime expiredAt;

	public static TeamInviteEntity createInvite(UserEntity invitor, TeamEntity team, String token,
		InviteTokenDuration duration, boolean requiresApproval) {
		return TeamInviteEntity.builder()
			.invitor(invitor)
			.team(team)
			.token(token)
			.tokenDuration(duration)
			.requiresApproval(requiresApproval)
			.expiredAt(duration.calculateExpirationTime())
			.build();
	}

	public boolean isExpired() {
		return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
	}
}
