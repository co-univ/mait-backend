package com.coniv.mait.domain.onboarding.entity;

import java.util.Set;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "onboarding_screens")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingScreenEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	private String code;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private boolean exposed;

	@Enumerated(EnumType.STRING)
	private TeamUserRole targetTeamRole;

	@Builder
	private OnboardingScreenEntity(String code, String title, boolean exposed,
		TeamUserRole targetTeamRole) {
		this.code = code;
		this.title = title;
		this.exposed = exposed;
		this.targetTeamRole = targetTeamRole;
	}

	public boolean isTargetedToAllUsers() {
		return this.targetTeamRole == null;
	}

	public boolean isVisibleTo(final Set<TeamUserRole> userRoles) {
		return isTargetedToAllUsers()
			|| userRoles.stream().anyMatch(role -> role.covers(this.targetTeamRole));
	}

	public void update(final String title, final TeamUserRole targetTeamRole) {
		this.title = title;
		this.targetTeamRole = targetTeamRole;
	}
}
