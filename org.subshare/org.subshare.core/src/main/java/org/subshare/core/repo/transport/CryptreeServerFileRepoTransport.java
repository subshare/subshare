package org.subshare.core.repo.transport;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.core.dto.Uid;

public interface CryptreeServerFileRepoTransport extends CryptreeFileRepoTransport {
	void delete(SsDeleteModificationDto deleteModificationDto);

	void makeDirectory(String path, SsDirectoryDto directoryDto,
			CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto);

	void endPutFile(String path, SsNormalFileDto normalFileDto, CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto);

	byte[] getHistoFileData(Uid histoCryptoRepoFileId, long offset);
}
