package com.coniv.mait.domain.category.entity;

import java.time.LocalDateTime;

import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "team_categories",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_team_categories_teamid_name", columnNames = {"team_id", "name"})
	},
	indexes = {
		@Index(name = "idx_team_categories_teamid_deletedat", columnList = "team_id, deleted_at")
	}
)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamCategoryEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "team_id", nullable = false)
	private Long teamId;

	@Column(nullable = false, length = 40)
	private String name;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private TeamCategoryEntity(final Long teamId, final String name) {
		this.teamId = teamId;
		this.name = name;
	}

	public static TeamCategoryEntity of(final Long teamId, final String name) {
		return new TeamCategoryEntity(teamId, name);
	}

	public void updateDeletedAt(final LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	public void markDeleted() {
		this.deletedAt = LocalDateTime.now();
	}

	public void restore() {
		this.deletedAt = null;
	}

	public boolean deleted() {
		return deletedAt != null;
	}
}
