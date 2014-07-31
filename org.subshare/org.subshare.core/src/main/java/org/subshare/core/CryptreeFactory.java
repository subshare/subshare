package org.subshare.core;

import java.util.UUID;

import org.subshare.core.user.UserRepoKey;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public interface CryptreeFactory {

	int getPriority();

	Cryptree createCryptree(LocalRepoTransaction transaction, UUID remoteRepositoryId, UserRepoKey userRepoKey);

	Cryptree createCryptree(LocalRepoTransaction transaction, UUID remoteRepositoryId);

}
