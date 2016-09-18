package org.subshare.local;

import java.util.HashSet;
import java.util.Set;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class UpdatePlainHistoCryptoRepoFilesMarker {
	private Set<Uid> histoCryptoRepoFileIds = new HashSet<>();

	public static UpdatePlainHistoCryptoRepoFilesMarker getInstance(LocalRepoTransaction tx) {
		UpdatePlainHistoCryptoRepoFilesMarker instance = tx.getContextObject(UpdatePlainHistoCryptoRepoFilesMarker.class);
		if (instance == null) {
			instance = new UpdatePlainHistoCryptoRepoFilesMarker();
			tx.setContextObject(instance);
		}
		return instance;
	}

	protected UpdatePlainHistoCryptoRepoFilesMarker() {
	}

	public Set<Uid> getHistoCryptoRepoFileIds() {
		return histoCryptoRepoFileIds;
	}
}
