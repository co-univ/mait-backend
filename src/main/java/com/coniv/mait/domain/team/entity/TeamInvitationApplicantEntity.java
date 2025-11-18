package com.coniv.mait.domain.team.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.enums.InvitationApplicationStatus;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "team_invitation_applicants", indexes = {
	@Index(name = "idx_team_invitation_teamid_userid", columnList = "team_id, user_id")
})
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamInvitationApplicantEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long teamId;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long invitationLinkId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TeamUserRole role;

	@Column(nullable = false)
	private LocalDateTime appliedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InvitationApplicationStatus applicationStatus;
}
