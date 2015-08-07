package org.subshare.core.repo.listener;

import java.util.EventListener;

@FunctionalInterface
public interface LocalRepoCommitEventListener extends EventListener {

	void postCommit(LocalRepoCommitEvent event);

}
