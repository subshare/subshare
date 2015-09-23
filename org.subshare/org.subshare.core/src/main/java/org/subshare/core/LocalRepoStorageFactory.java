package org.subshare.core;

import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public interface LocalRepoStorageFactory {

	int getPriority();

	/**
	 * Gets the {@link LocalRepoStorage} bound to the given {@code transaction} or creates one.
	 * <p>
	 * The {@link LocalRepoStorage} is not associated with a remote repository (if it was
	 * created by this method - a previously created one might have the association).
	 *
	 * @param transaction the transaction. Must not be <code>null</code>.
	 * @return the {@link LocalRepoStorage} bound to the given {@code transaction}.
	 */
	LocalRepoStorage getLocalRepoStorageOrCreate(LocalRepoTransaction transaction);

	/**
	 * Gets the {@link LocalRepoStorage} bound to the given {@code transaction} or creates one.
	 * <p>
	 * The {@link LocalRepoStorage} is associated with the specified remote repository.
	 *
	 * @param transaction the transaction. Must not be <code>null</code>.
	 * @param remoteRepositoryId the remote repository's unique identifier. Must not be <code>null</code>.
	 * @param remotePathPrefix the remote repository's path-prefix. Must not be <code>null</code>. If there
	 * is no path-prefix, this is an empty string.
	 * @return the {@link LocalRepoStorage} bound to the given {@code transaction}.
	 */
	LocalRepoStorage getLocalRepoStorageOrCreate(LocalRepoTransaction transaction, UUID remoteRepositoryId, String remotePathPrefix);

}
