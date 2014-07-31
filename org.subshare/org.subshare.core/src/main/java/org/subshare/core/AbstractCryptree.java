package org.subshare.core;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractCryptree implements Cryptree {

	private CryptreeFactory cryptreeFactory;
	private LocalRepoTransaction transaction;
	private UserRepoKey userRepoKey;
	private UUID remoteRepositoryId;

	@Override
	public CryptreeFactory getCryptreeFactory() {
		return cryptreeFactory;
	}
	@Override
	public void setCryptreeFactory(final CryptreeFactory cryptreeFactory) {
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

	@Override
	public UUID getRemoteRepositoryId() {
		return remoteRepositoryId;
	}
	@Override
	public void setRemoteRepositoryId(final UUID remoteRepositoryId) {
		this.remoteRepositoryId = remoteRepositoryId;
	}
	protected UUID getRemoteRepositoryIdOrFail() {
		return assertNotNull("getRemoteRepositoryId()", getRemoteRepositoryId());
	}

	@Override
	public void close() {
		// nothing to close - yet.
	}
}
