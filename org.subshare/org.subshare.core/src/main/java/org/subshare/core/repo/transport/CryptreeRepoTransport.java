package org.subshare.core.repo.transport;

import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public interface CryptreeRepoTransport extends RepoTransport {

	void beginPutFile(String path, SsNormalFileDto normalFileDto);

}
