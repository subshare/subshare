package org.subshare.core;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractLocalRepoStorageFactory implements LocalRepoStorageFactory {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public LocalRepoStorage getLocalRepoStorageOrCreate(final LocalRepoTransaction transaction) {
		assertNotNull("transaction", transaction);
		LocalRepoStorage lrs = transaction.getContextObject(LocalRepoStorage.class);
		if (lrs == null) {
			lrs = _createLocalRepoStorage();
			lrs.setLocalRepoStorageFactory(this);
			lrs.setTransaction(transaction);
			transaction.setContextObject(lrs);
		}
		else {
			// We allow only one single LocalRepoStorage instance, which is considered immutable, to be bound to the transaction.
			// Hence we check, if the parameters match this single instance.

			if (transaction != lrs.getTransaction())
				throw new IllegalStateException("transaction != cryptree.transaction");
		}
		return lrs;
	}

	@Override
	public LocalRepoStorage getLocalRepoStorageOrCreate(final LocalRepoTransaction transaction, final UUID remoteRepositoryId, final String remotePathPrefix) {
		assertNotNull("transaction", transaction);
		assertNotNull("remoteRepositoryId", remoteRepositoryId);
		assertNotNull("remotePathPrefix", remotePathPrefix);
		LocalRepoStorage cryptree = transaction.getContextObject(LocalRepoStorage.class);
		if (cryptree == null) {
			cryptree = _createLocalRepoStorage();
			cryptree.setLocalRepoStorageFactory(this);
			cryptree.setTransaction(transaction);
			cryptree.setRemoteRepositoryId(remoteRepositoryId);
			cryptree.setRemotePathPrefix(remotePathPrefix);
			transaction.setContextObject(cryptree);
		}
		else {
			// We allow only one single LocalRepoStorage instance, which is considered immutable, to be bound to the transaction.
			// Hence we check, if the parameters match this single instance.

			if (transaction != cryptree.getTransaction())
				throw new IllegalStateException("transaction != cryptree.transaction");

			if (! remoteRepositoryId.equals(cryptree.getRemoteRepositoryId()))
				throw new IllegalStateException("remoteRepositoryId != cryptree.remoteRepositoryId");

			if (! remotePathPrefix.equals(cryptree.getRemotePathPrefix()))
				throw new IllegalStateException("remotePathPrefix != cryptree.remotePathPrefix");
		}
		return cryptree;
	}

	protected abstract LocalRepoStorage _createLocalRepoStorage();

}
