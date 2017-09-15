package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;

public class LastCryptoKeySyncToRemoteRepoDao extends Dao<LastCryptoKeySyncToRemoteRepo, LastCryptoKeySyncToRemoteRepoDao> {

	public LastCryptoKeySyncToRemoteRepo getLastCryptoKeySyncToRemoteRepo(final RemoteRepository remoteRepository) {
		assertNotNull(remoteRepository, "remoteRepository");
		final Query query = pm().newNamedQuery(getEntityClass(), "getLastCryptoKeySyncToRemoteRepo_remoteRepository");
		try {
			final LastCryptoKeySyncToRemoteRepo result = (LastCryptoKeySyncToRemoteRepo) query.execute(remoteRepository);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public LastCryptoKeySyncToRemoteRepo getLastCryptoKeySyncToRemoteRepoOrFail(final RemoteRepository remoteRepository) {
		final LastCryptoKeySyncToRemoteRepo result = getLastCryptoKeySyncToRemoteRepo(remoteRepository);
		if (result == null)
			throw new IllegalStateException("There is no LastCryptoKeySyncToRemoteRepo for the RemoteRepository with repositoryId=" + remoteRepository.getRepositoryId());

		return result;
	}
}
