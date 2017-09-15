package org.subshare.core.repo.transport;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsSymlinkDto;

import co.codewizards.cloudstore.core.Uid;

public interface CryptreeServerFileRepoTransport extends CryptreeFileRepoTransport {
	void delete(SsDeleteModificationDto deleteModificationDto);

	void makeDirectory(String path, SsDirectoryDto directoryDto,
			CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto);

	void makeSymlink(String path, SsSymlinkDto symlinkDto,
			CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto);

	void endPutFile(String path, SsNormalFileDto normalFileDto, CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto);

	byte[] getHistoFileData(Uid histoCryptoRepoFileId, long offset);

	Long getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced();
}
