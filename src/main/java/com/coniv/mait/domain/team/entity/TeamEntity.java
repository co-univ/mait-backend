package com.coniv.mait.domain.team.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.enums.TeamType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "teams")
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Long creatorId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TeamType type;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private TeamEntity(String name, Long creatorId, TeamType type) {
		this.name = name;
		this.creatorId = creatorId;
		this.type = type;
	}

	public static TeamEntity ofGroup(String name, Long creatorId) {
		return new TeamEntity(name, creatorId, TeamType.GROUP);
	}

	public static TeamEntity ofPersonal(String name, Long creatorId) {
		return new TeamEntity(name, creatorId, TeamType.PERSONAL);
	}

	public void updateDeletedAt(final LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	public void markDeleted() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean deleted() {
		return deletedAt != null;
	}
}
