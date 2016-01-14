package org.subshare.core.repo.transport;

import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.core.dto.Uid;

public interface CryptreeServerFileRepoTransport extends CryptreeFileRepoTransport {
	void delete(SsDeleteModificationDto deleteModificationDto);

	void makeDirectory(String path, SsDirectoryDto directoryDto,
			HistoCryptoRepoFileDto histoCryptoRepoFileDto);

	void endPutFile(String path, SsNormalFileDto normalFileDto, HistoCryptoRepoFileDto histoCryptoRepoFileDto);

	byte[] getHistoFileData(Uid histoCryptoRepoFileId, long offset);
}
