package org.subshare.local.persistence;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;

public class SsRemoteRepositoryDao extends RemoteRepositoryDao {

	public SsRemoteRepository getUniqueRemoteRepository() {
		final RemoteRepositoryDao rrDao = getDao(RemoteRepositoryDao.class);
		final Map<UUID, URL> remoteRepositoryId2RemoteRootMap = rrDao.getRemoteRepositoryId2RemoteRootMap();

		if (remoteRepositoryId2RemoteRootMap.isEmpty())
			return null;

		if (remoteRepositoryId2RemoteRootMap.size() != 1)
			throw new IllegalStateException("There is more than one remote repository!");

		final RemoteRepository remoteRepository = rrDao.getRemoteRepositoryOrFail(remoteRepositoryId2RemoteRootMap.keySet().iterator().next());
		return (SsRemoteRepository) remoteRepository;
	}
}
