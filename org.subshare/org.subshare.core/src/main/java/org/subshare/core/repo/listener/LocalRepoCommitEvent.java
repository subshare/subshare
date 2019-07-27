package org.subshare.core.repo.listener;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class LocalRepoCommitEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	private final LocalRepoManager localRepoManager;
	private final UUID localRepositoryId;
	private final List<EntityModification> modifications;

	protected LocalRepoCommitEvent(final LocalRepoCommitEventManagerImpl source, final LocalRepoManager localRepoManager, final List<EntityModification> modifications) {
		super(requireNonNull(source, "source"));
		requireNonNull(modifications, "modifications");
		this.localRepoManager = requireNonNull(localRepoManager, "localRepoManager");
		this.localRepositoryId = localRepoManager.getRepositoryId();
		this.modifications = Collections.unmodifiableList(new ArrayList<EntityModification>(modifications));
	}

	@Override
	public LocalRepoCommitEventManagerImpl getSource() {
		return (LocalRepoCommitEventManagerImpl) super.getSource();
	}

	public LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}

	public UUID getLocalRepositoryId() {
		return localRepositoryId;
	}

	public List<EntityModification> getModifications() {
		return modifications;
	}
}
