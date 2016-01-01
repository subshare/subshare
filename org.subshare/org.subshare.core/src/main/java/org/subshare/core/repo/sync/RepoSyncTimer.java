package org.subshare.core.repo.sync;

import java.util.UUID;

import co.codewizards.cloudstore.core.bean.Bean;
import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface RepoSyncTimer extends Bean<RepoSyncTimer.Property> {
	interface Property extends PropertyBase { }

	enum PropertyEnum implements Property {
		nextSyncTimestamps
	}

	String CONFIG_KEY_SYNC_PERIOD = "repo.syncPeriod";
	long DEFAULT_SYNC_PERIOD = 3600L * 1000L; // 1 hour

	long getNextSyncTimestamp(UUID localRepositoryId);

	void scheduleTimerTask();
}
