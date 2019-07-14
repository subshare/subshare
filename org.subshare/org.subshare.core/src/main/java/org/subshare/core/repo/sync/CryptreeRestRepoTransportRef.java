package org.subshare.core.repo.sync;

import java.util.UUID;

import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.repo.transport.CryptreeRestRepoTransport;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.repo.sync.RepoTransportRef;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public class CryptreeRestRepoTransportRef extends RepoTransportRef implements CryptreeRestRepoTransport {

	@Override
	public CryptreeRestRepoTransport getDelegate() {
		return (CryptreeRestRepoTransport) super.getDelegate();
	}

	@Override
	public CryptreeRestRepoTransport getDelegateOrFail() {
		return (CryptreeRestRepoTransport) super.getDelegateOrFail();
	}

	@Override
	public void setDelegate(RepoTransport delegate) {
		if (delegate != null && ! (delegate instanceof CryptreeRestRepoTransport))
			throw new IllegalArgumentException("! (delegate instanceof CryptreeRestRepoTransport)");

		super.setDelegate(delegate);
	}

	@Override
	public void beginPutFile(String path, SsNormalFileDto normalFileDto) {
		getDelegateOrFail().beginPutFile(path, normalFileDto);
	}

	@Override
	public void delete(SsDeleteModificationDto deleteModificationDto) {
		getDelegateOrFail().delete(deleteModificationDto);
	}

	@Override
	public void createRepository(UUID serverRepositoryId, PgpKey pgpKey) {
		getDelegateOrFail().createRepository(serverRepositoryId, pgpKey);
	}

	@Override
	public void endPutFile(String path, NormalFileDto fromNormalFileDto) {
		getDelegateOrFail().endPutFile(path, fromNormalFileDto);
	}

	@Override
	public byte[] getHistoFileData(Uid histoCryptoRepoFileId, long offset) {
		return getDelegateOrFail().getHistoFileData(histoCryptoRepoFileId, offset);
	}

	@Override
	public Long getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced() {
		return getDelegateOrFail().getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced();
	}

	@Override
	public void putCryptoChangeSetDto(CryptoChangeSetDto cryptoChangeSetDto) {
		getDelegateOrFail().putCryptoChangeSetDto(cryptoChangeSetDto);
	}

}
