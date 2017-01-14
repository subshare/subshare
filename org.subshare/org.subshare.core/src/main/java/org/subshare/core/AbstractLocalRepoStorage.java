package org.subshare.core;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractLocalRepoStorage implements LocalRepoStorage {
	private LocalRepoStorageFactory localRepoStorageFactory;
	private LocalRepoTransaction transaction;
	private UUID remoteRepositoryId;
	private String remotePathPrefix;

	@Override
	public LocalRepoStorageFactory getLocalRepoStorageFactory() {
		return localRepoStorageFactory;
	}

	@Override
	public void setLocalRepoStorageFactory(LocalRepoStorageFactory localRepoStorageFactory) {
		if (this.localRepoStorageFactory != null && this.localRepoStorageFactory != localRepoStorageFactory)
			throw new IllegalStateException("this.localRepoStorageFactory already assigned! Cannot modify after initial assignment!");

		this.localRepoStorageFactory = localRepoStorageFactory;
	}

	protected LocalRepoStorageFactory getLocalRepoStorageFactoryOrFail() {
		return assertNotNull(getLocalRepoStorageFactoryOrFail(), "getLocalRepoStorageFactoryOrFail()");
	}

	@Override
	public LocalRepoTransaction getTransaction() {
		return transaction;
	}
	@Override
	public void setTransaction(final LocalRepoTransaction transaction) {
		if (this.transaction != null && this.transaction != transaction)
			throw new IllegalStateException("this.transaction already assigned! Cannot modify after initial assignment!");

		this.transaction = transaction;
	}
	protected LocalRepoTransaction getTransactionOrFail() {
		return assertNotNull(getTransaction(), "getTransaction()");
	}

	@Override
	public UUID getRemoteRepositoryId() {
		return remoteRepositoryId;
	}
	@Override
	public void setRemoteRepositoryId(final UUID serverRepositoryId) {
		if (this.remoteRepositoryId != null && !this.remoteRepositoryId.equals(serverRepositoryId))
			throw new IllegalStateException("this.remoteRepositoryId already assigned! Cannot modify after initial assignment!");

		this.remoteRepositoryId = serverRepositoryId;
	}
	protected UUID getRemoteRepositoryIdOrFail() {
		return assertNotNull(getRemoteRepositoryId(), "getRemoteRepositoryId()");
	}

	@Override
	public String getRemotePathPrefix() {
		return remotePathPrefix;
	}
	@Override
	public void setRemotePathPrefix(final String remotePathPrefix) {
		if (this.remotePathPrefix != null && !this.remotePathPrefix.equals(remotePathPrefix))
			throw new IllegalStateException("this.remotePathPrefix already assigned! Cannot modify after initial assignment!");

		this.remotePathPrefix = remotePathPrefix;
	}
	protected String getRemotePathPrefixOrFail() {
		return assertNotNull(getRemotePathPrefix(), "getRemotePathPrefix()");
	}
}
