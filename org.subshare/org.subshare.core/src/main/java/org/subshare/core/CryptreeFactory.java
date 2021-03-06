package org.subshare.core;

import java.util.UUID;

import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public interface CryptreeFactory {

	int getPriority();

	Cryptree getCryptreeOrCreate(LocalRepoTransaction transaction, UUID remoteRepositoryId, String remotePathPrefix, UserRepoKeyRing userRepoKeyRing);

	Cryptree getCryptreeOrCreate(LocalRepoTransaction transaction, UUID remoteRepositoryId);

}
