package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.local.dto.CryptoRepoFileDtoConverter;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.LocalRepoMetaDataImpl;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class SsLocalRepoMetaDataImpl extends LocalRepoMetaDataImpl implements SsLocalRepoMetaData {

//	@Override
//	public CryptoRepoFileDto getCryptoRepoFileDto(final String localPath) {
//		assertNotNull("path", localPath);
//
//		final CryptoRepoFileDto result;
//		try (final LocalRepoTransaction tx = beginReadTransaction();) {
//			final CryptoRepoFileDtoConverter converter = CryptoRepoFileDtoConverter.create();
//			final CryptoRepoFile cryptoRepoFile = tx.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(localPath);
//
//			result = cryptoRepoFile == null ? null : converter.toCryptoRepoFileDto(cryptoRepoFile);
//
//			tx.commit();
//		}
//		return result;
//	}

	@Override
	public CryptoRepoFileDto getCryptoRepoFileDto(final long repoFileId) {
		final CryptoRepoFileDto result;
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final CryptoRepoFileDtoConverter converter = CryptoRepoFileDtoConverter.create();
			final RepoFile repoFile = tx.getDao(RepoFileDao.class).getObjectByIdOrNull(repoFileId);
			final CryptoRepoFile cryptoRepoFile =
					repoFile == null ? null : tx.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(repoFile);

			result = cryptoRepoFile == null ? null : converter.toCryptoRepoFileDto(cryptoRepoFile);

			tx.commit();
		}
		return result;
	}

	@Override
	public Map<Long, CryptoRepoFileDto> getCryptoRepoFileDtos(final Collection<Long> repoFileIds) {
		assertNotNull("repoFileIds", repoFileIds);
		final Map<Long, CryptoRepoFileDto> result = new LinkedHashMap<>();
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final RepoFileDao repoFileDao = tx.getDao(RepoFileDao.class);
			final CryptoRepoFileDao cryptoRepoFileDao = tx.getDao(CryptoRepoFileDao.class);
			final CryptoRepoFileDtoConverter converter = CryptoRepoFileDtoConverter.create();
			for (final Long repoFileId : repoFileIds) {
				assertNotNull("repoFileId", repoFileId);
				final RepoFile repoFile = repoFileDao.getObjectByIdOrNull(repoFileId);
				if (repoFile == null)
					continue;

				final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(repoFile);
				if (cryptoRepoFile == null)
					continue;

				result.put(repoFileId, converter.toCryptoRepoFileDto(cryptoRepoFile));
			}
			tx.commit();
		}
		return result;
	}

}
