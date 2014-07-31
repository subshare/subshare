package org.subshare.core;

import java.util.UUID;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.user.UserRepoKey;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public interface Cryptree extends AutoCloseable {

	CryptreeFactory getCryptreeFactory();
	void setCryptreeFactory(CryptreeFactory cryptreeFactory);

	LocalRepoTransaction getTransaction();
	void setTransaction(LocalRepoTransaction transaction);

	UserRepoKey getUserRepoKey();
	void setUserRepoKey(UserRepoKey userRepoKey);

	UUID getRemoteRepositoryId();
	void setRemoteRepositoryId(UUID remoteRepositoryId);

	@Override
	void close();

	CryptoChangeSetDto createOrUpdateCryptoRepoFile(String path);
	CryptoChangeSetDto getCryptoChangeSetDtoWithCryptoRepoFiles();
	void updateLastCryptoKeySyncToRemoteRepo();
	KeyParameter getDataKey(String path);

}
