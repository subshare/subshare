package org.subshare.core.repo.transport;

import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.HistoCryptoRepoFileDto;

public interface CryptreeServerFileRepoTransport extends CryptreeFileRepoTransport {
	void delete(SsDeleteModificationDto deleteModificationDto);

	void makeDirectory(String path, SsDirectoryDto directoryDto,
			HistoCryptoRepoFileDto histoCryptoRepoFileDto);

	void endPutFile(String path, SsNormalFileDto normalFileDto, HistoCryptoRepoFileDto histoCryptoRepoFileDto);
}
