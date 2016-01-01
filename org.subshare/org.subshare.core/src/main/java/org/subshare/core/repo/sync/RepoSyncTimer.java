package org.subshare.core.repo.sync;

public interface RepoSyncTimer {
	String CONFIG_KEY_SYNC_PERIOD = "syncPeriod";
	long DEFAULT_SYNC_PERIOD = 3600L * 1000L; // 1 hour
}
