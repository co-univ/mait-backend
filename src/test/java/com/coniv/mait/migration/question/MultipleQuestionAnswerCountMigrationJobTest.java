package com.coniv.mait.migration.question;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MultipleQuestionAnswerCountMigrationJobTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@InjectMocks
	private MultipleQuestionAnswerCountMigrationJob migration;

	@Test
	@DisplayName("불일치가 없으면 데이터를 갱신하지 않는다")
	void migrate_SkipsWhenConsistent() {
		// given
		when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);

		// when
		migration.migrate();

		// then
		verify(jdbcTemplate, never()).update(anyString());
	}

	@Test
	@DisplayName("보정 후 불일치가 남으면 예외를 던져 완료 처리를 막는다")
	void migrate_ThrowsWhenVerificationFails() {
		// given
		when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(2L, 1L);
		when(jdbcTemplate.update(anyString())).thenReturn(1);

		// when & then
		assertThatThrownBy(migration::migrate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("불일치가 남아 있습니다: 1");
	}
}
