package org.subshare.core.repo.local;

import java.util.Collection;
import java.util.Map;

import org.subshare.core.dto.CryptoRepoFileDto;

import co.codewizards.cloudstore.core.repo.local.LocalRepoMetaData;

public interface SsLocalRepoMetaData extends LocalRepoMetaData {

	CryptoRepoFileDto getCryptoRepoFileDto(long repoFileId);

	Map<Long, CryptoRepoFileDto> getCryptoRepoFileDtos(Collection<Long> repoFileIds);

//	CryptoRepoFileDto getCryptoRepoFileDto(String localPath);
//

}
