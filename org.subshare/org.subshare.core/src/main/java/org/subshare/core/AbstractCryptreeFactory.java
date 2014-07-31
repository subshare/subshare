package org.subshare.core;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import org.subshare.core.user.UserRepoKey;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractCryptreeFactory implements CryptreeFactory {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public Cryptree createCryptree(final LocalRepoTransaction transaction, final UUID remoteRepositoryId) {
		assertNotNull("transaction", transaction);
		assertNotNull("remoteRepositoryId", remoteRepositoryId);
		final Cryptree cryptree = _createCryptree();
		cryptree.setCryptreeFactory(this);
		cryptree.setTransaction(transaction);
		cryptree.setRemoteRepositoryId(remoteRepositoryId);
		return cryptree;
	}

	@Override
	public Cryptree createCryptree(final LocalRepoTransaction transaction, final UUID remoteRepositoryId, final UserRepoKey userRepoKey) {
		final Cryptree cryptree = createCryptree(transaction, remoteRepositoryId);
		assertNotNull("userRepoKey", userRepoKey);
		cryptree.setUserRepoKey(userRepoKey);
		return cryptree;
	}

	protected abstract Cryptree _createCryptree();

}
