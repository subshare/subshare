package org.subshare.local.persistence;

import static java.util.Objects.*;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;

public class LastCryptoKeySyncFromRemoteRepoDao extends Dao<LastCryptoKeySyncFromRemoteRepo, LastCryptoKeySyncFromRemoteRepoDao> {

	public LastCryptoKeySyncFromRemoteRepo getLastCryptoKeySyncFromRemoteRepo(final RemoteRepository remoteRepository) {
		requireNonNull(remoteRepository, "remoteRepository");
		final Query query = pm().newNamedQuery(getEntityClass(), "getLastCryptoKeySyncFromRemoteRepo_remoteRepository");
		try {
			final LastCryptoKeySyncFromRemoteRepo result = (LastCryptoKeySyncFromRemoteRepo) query.execute(remoteRepository);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public LastCryptoKeySyncFromRemoteRepo getLastCryptoKeySyncFromRemoteRepoOrFail(final RemoteRepository remoteRepository) {
		final LastCryptoKeySyncFromRemoteRepo result = getLastCryptoKeySyncFromRemoteRepo(remoteRepository);
		if (result == null)
			throw new IllegalStateException("There is no LastCryptoKeySyncFromRemoteRepo for the RemoteRepository with repositoryId=" + remoteRepository.getRepositoryId());

		return result;
	}
}
