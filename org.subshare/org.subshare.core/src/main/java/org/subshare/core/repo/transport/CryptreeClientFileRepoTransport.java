package org.subshare.core.repo.transport;

import org.subshare.core.dto.SsNormalFileDto;

public interface CryptreeClientFileRepoTransport extends CryptreeFileRepoTransport {

	void endPutFile(String path, SsNormalFileDto fromNormalFileDto);

}
