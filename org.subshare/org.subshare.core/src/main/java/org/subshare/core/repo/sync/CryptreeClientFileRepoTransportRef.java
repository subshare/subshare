package org.subshare.core.repo.sync;

import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeClientFileRepoTransport;

import co.codewizards.cloudstore.core.repo.sync.LocalRepoTransportRef;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public class CryptreeClientFileRepoTransportRef extends LocalRepoTransportRef implements CryptreeClientFileRepoTransport {

	@Override
	public CryptreeClientFileRepoTransport getDelegate() {
		return (CryptreeClientFileRepoTransport) super.getDelegate();
	}

	@Override
	public CryptreeClientFileRepoTransport getDelegateOrFail() {
		return (CryptreeClientFileRepoTransport) super.getDelegateOrFail();
	}

	@Override
	public void setDelegate(RepoTransport delegate) {
		if (delegate != null && ! (delegate instanceof CryptreeClientFileRepoTransport))
			throw new IllegalArgumentException("! (delegate instanceof CryptreeClientFileRepoTransport)");

		super.setDelegate(delegate);
	}

	@Override
	public void beginPutFile(String path, SsNormalFileDto normalFileDto) {
		getDelegateOrFail().beginPutFile(path, normalFileDto);
	}

	@Override
	public void endPutFile(String path, SsNormalFileDto fromNormalFileDto) {
		getDelegateOrFail().endPutFile(path, fromNormalFileDto);
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDto(Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced) {
		return getDelegateOrFail().getCryptoChangeSetDto(lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);
	}
}
