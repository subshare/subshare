package org.subshare.core.repo.transport;

import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.CryptoRepoFileOnServerDto;

public interface CryptreeServerFileRepoTransport extends CryptreeFileRepoTransport {
	void delete(SsDeleteModificationDto deleteModificationDto);

	void makeDirectory(String path, SsDirectoryDto directoryDto,
			CryptoRepoFileOnServerDto cryptoRepoFileOnServerDto);

	void endPutFile(String path, SsNormalFileDto normalFileDto, CryptoRepoFileOnServerDto cryptoRepoFileOnServerDto);
}
