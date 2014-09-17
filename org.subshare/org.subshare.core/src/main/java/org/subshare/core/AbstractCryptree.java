package org.subshare.core;


import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;

import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractCryptree implements Cryptree {

	private CryptreeFactory cryptreeFactory;
	private LocalRepoTransaction transaction;
	private UserRepoKey userRepoKey;
	private UUID serverRepositoryId;
	private String remotePathPrefix;

	@Override
	public CryptreeFactory getCryptreeFactory() {
		return cryptreeFactory;
	}
	@Override
	public void setCryptreeFactory(final CryptreeFactory cryptreeFactory) {
		if (this.cryptreeFactory != null && this.cryptreeFactory != cryptreeFactory)
			throw new IllegalStateException("this.cryptreeFactory already assigned! Cannot modify after initial assignment!");

		this.cryptreeFactory = cryptreeFactory;
	}
	protected CryptreeFactory getCryptreeFactoryOrFail() {
		return assertNotNull("getCryptreeFactory()", getCryptreeFactory());
	}

	@Override
	public LocalRepoTransaction getTransaction() {
		return transaction;
	}
	@Override
	public void setTransaction(final LocalRepoTransaction transaction) {
		this.transaction = transaction;
	}
	protected LocalRepoTransaction getTransactionOrFail() {
		return assertNotNull("getTransaction()", getTransaction());
	}

	@Override
	public UserRepoKey getUserRepoKey() {
		return userRepoKey;
	}
	@Override
	public void setUserRepoKey(final UserRepoKey userRepoKey) {
		this.userRepoKey = userRepoKey;
	}
	protected UserRepoKey getUserRepoKeyOrFail() {
		return assertNotNull("getUserRepoKey()", getUserRepoKey());
	}

	protected UserRepoKeyRing getUserRepoKeyRingOrFail() {
		return assertNotNull("getUserRepoKeyOrFail().getUserRepoKeyRing()", getUserRepoKeyOrFail().getUserRepoKeyRing());
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
	public void setServerRepositoryId(final UUID serverRepositoryId) {
		if (this.serverRepositoryId != null && !this.serverRepositoryId.equals(serverRepositoryId))
			throw new IllegalStateException("this.remoteRepositoryId already assigned! Cannot modify after initial assignment!");

		this.serverRepositoryId = serverRepositoryId;
	}
	protected UUID getServerRepositoryIdOrFail() {
		return assertNotNull("getRemoteRepositoryId()", getServerRepositoryId());
	}

	@Override
	public String getServerPathPrefix() {
		return remotePathPrefix;
	}
	@Override
	public void setServerPathPrefix(final String remotePathPrefix) {
		if (this.remotePathPrefix != null && !this.remotePathPrefix.equals(remotePathPrefix))
			throw new IllegalStateException("this.remotePathPrefix already assigned! Cannot modify after initial assignment!");

		this.remotePathPrefix = remotePathPrefix;
	}
	protected String getRemotePathPrefixOrFail() {
		return assertNotNull("getRemotePathPrefix()", getServerPathPrefix());
	}

	@Override
	public void close() {
		// nothing to close - yet.
	}
}
