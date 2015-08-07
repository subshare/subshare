package org.subshare.core.repo.listener;

import java.util.UUID;

public interface LocalRepoCommitEventManager {

	void addLocalRepoCommitEventListener(LocalRepoCommitEventListener listener);

	void removeLocalRepoCommitEventListener(LocalRepoCommitEventListener listener);

	void addLocalRepoCommitEventListener(UUID localRepositoryId, LocalRepoCommitEventListener listener);

	void removeLocalRepoCommitEventListener(UUID localRepositoryId, LocalRepoCommitEventListener listener);

}