package org.subshare.core;

import static java.util.Objects.*;

import java.util.UUID;

import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractCryptree implements Cryptree {

	private CryptreeFactory cryptreeFactory;
	private LocalRepoTransaction transaction;
	private UserRepoKeyRing userRepoKeyRing;
	private UUID remoteRepositoryId;
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
		return requireNonNull(getCryptreeFactory(), "getCryptreeFactory()");
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
		return requireNonNull(getTransaction(), "getTransaction()");
	}

	@Override
	public UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}
	@Override
	public void setUserRepoKeyRing(final UserRepoKeyRing userRepoKeyRing) {
		this.userRepoKeyRing = userRepoKeyRing;
	}
//	protected UserRepoKey getUserRepoKeyOrFail() {
//		return requireNonNull("getUserRepoKey()", getUserRepoKeyRing());
//	}

	protected UserRepoKeyRing getUserRepoKeyRingOrFail() {
		return requireNonNull(getUserRepoKeyRing(), "getUserRepoKeyRing()");
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
		return requireNonNull(getRemoteRepositoryId(), "getRemoteRepositoryId()");
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
		return requireNonNull(getRemotePathPrefix(), "getRemotePathPrefix()");
	}
}
