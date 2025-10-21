package com.coniv.mait.migration;

public interface MigrationJob {

	void migrate();

	default String getName() {
		return getClass().getSimpleName();
	}
}
