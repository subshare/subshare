package org.subshare.core;

import static java.util.Objects.*;

import java.util.UUID;

import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractCryptreeFactory implements CryptreeFactory {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public Cryptree getCryptreeOrCreate(final LocalRepoTransaction transaction, final UUID remoteRepositoryId) {
		requireNonNull(transaction, "transaction");
		requireNonNull(remoteRepositoryId, "remoteRepositoryId");
		Cryptree cryptree = transaction.getContextObject(Cryptree.class);
		if (cryptree == null) {
			cryptree = _createCryptree();
			cryptree.setCryptreeFactory(this);
			cryptree.setTransaction(transaction);
			cryptree.setRemoteRepositoryId(remoteRepositoryId);
			transaction.setContextObject(cryptree);
		}
		else {
			// We allow only one single Cryptree instance, which is considered immutable, to be bound to the transaction.
			// Hence we check, if the parameters match this single instance.

			if (transaction != cryptree.getTransaction())
				throw new IllegalStateException("transaction != cryptree.transaction");

			if (!remoteRepositoryId.equals(cryptree.getRemoteRepositoryId()))
				throw new IllegalStateException("remoteRepositoryId != cryptree.remoteRepositoryId");
		}
		return cryptree;
	}

	@Override
	public Cryptree getCryptreeOrCreate(final LocalRepoTransaction transaction, final UUID remoteRepositoryId, final String remotePathPrefix, final UserRepoKeyRing userRepoKeyRing) {
		requireNonNull(transaction, "transaction");
		requireNonNull(remoteRepositoryId, "remoteRepositoryId");
		requireNonNull(remotePathPrefix, "remotePathPrefix");
		requireNonNull(userRepoKeyRing, "userRepoKeyRing");
		Cryptree cryptree = transaction.getContextObject(Cryptree.class);
		if (cryptree == null) {
			cryptree = _createCryptree();
			cryptree.setCryptreeFactory(this);
			cryptree.setTransaction(transaction);
			cryptree.setRemoteRepositoryId(remoteRepositoryId);
			cryptree.setUserRepoKeyRing(userRepoKeyRing);
			cryptree.setRemotePathPrefix(remotePathPrefix);
			transaction.setContextObject(cryptree);
		}
		else {
			// We allow only one single Cryptree instance, which is considered immutable, to be bound to the transaction.
			// Hence we check, if the parameters match this single instance.

			if (transaction != cryptree.getTransaction())
				throw new IllegalStateException("transaction != cryptree.transaction");

			if (! remoteRepositoryId.equals(cryptree.getRemoteRepositoryId()))
				throw new IllegalStateException("remoteRepositoryId != cryptree.remoteRepositoryId");

			if (userRepoKeyRing != cryptree.getUserRepoKeyRing())
				throw new IllegalStateException("userRepoKeyRing != cryptree.userRepoKeyRing");

			if (! remotePathPrefix.equals(cryptree.getRemotePathPrefix()))
				throw new IllegalStateException("remotePathPrefix != cryptree.remotePathPrefix");
		}
		return cryptree;
	}

	protected abstract Cryptree _createCryptree();

}
