package org.subshare.core.repo.transport;

import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.SsNormalFileDto;

public interface CryptreeClientFileRepoTransport extends CryptreeFileRepoTransport {

	void endPutFile(String path, SsNormalFileDto fromNormalFileDto);

//	// Invoked during syncUp, only.
//	boolean setLastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced(long revision);

	CryptoChangeSetDto getCryptoChangeSetDto(Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);

}
