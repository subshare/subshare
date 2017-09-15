package org.subshare.core.repo.transport;

import java.util.UUID;

import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.pgp.PgpKey;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public interface CryptreeRestRepoTransport extends RepoTransport, CryptreeRepoTransport {

	void delete(SsDeleteModificationDto deleteModificationDto);

	void createRepository(UUID serverRepositoryId, PgpKey pgpKey);

//	void beginPutFile(String path, NormalFileDto fromNormalFileDto); // inherited

	void endPutFile(String path, NormalFileDto fromNormalFileDto);

	byte[] getHistoFileData(Uid histoCryptoRepoFileId, long offset);

	// Invoked during syncUp, only.
	Long getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced();

	void putCryptoChangeSetDto(CryptoChangeSetDto cryptoChangeSetDto);
}
