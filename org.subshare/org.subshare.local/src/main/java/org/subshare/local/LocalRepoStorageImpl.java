package org.subshare.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.AbstractLocalRepoStorage;
import org.subshare.local.dto.SsFileChunkDtoConverter;
import org.subshare.local.persistence.LocalRepositoryType;
import org.subshare.local.persistence.SsLocalRepository;
import org.subshare.local.persistence.TempFileChunk;
import org.subshare.local.persistence.TempFileChunkDao;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.dto.FileChunkDtoConverter;
import co.codewizards.cloudstore.local.dto.RepoFileDtoConverter;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class LocalRepoStorageImpl extends AbstractLocalRepoStorage {
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoStorageImpl.class);

	@Override
	public void makeMetaOnly() {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final SsLocalRepository localRepository = (SsLocalRepository) transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		if (localRepository.getLocalRepositoryType() != LocalRepositoryType.UNINITIALISED)
			throw new IllegalStateException("localRepositoryType is already initialised: " + localRepository.getLocalRepositoryType());

		localRepository.setLocalRepositoryType(LocalRepositoryType.CLIENT_META_ONLY);
	}

	@Override
	public boolean isMetaOnly() {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final SsLocalRepository localRepository = (SsLocalRepository) transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		return localRepository.getLocalRepositoryType() == LocalRepositoryType.CLIENT_META_ONLY;
	}

	@Override
	public Collection<? extends FileChunkDto> getTempFileChunkDtos(String localPath) {
		assertNotNull(localPath, "localPath");

		final UUID remoteRepositoryId = getRemoteRepositoryIdOrFail();
		final TempFileChunkDao tfcDao = getTransactionOrFail().getDao(TempFileChunkDao.class);
		final RepoFile repoFile = getRepoFile(localPath);
		if (! (repoFile instanceof NormalFile)) // it may be null - or an instance of SsDirectory or SsSymlink, if its type just changed.
			return Collections.emptyList();

		final NormalFile normalFile = (NormalFile) repoFile;
		final SsFileChunkDtoConverter fcDtoConverter = (SsFileChunkDtoConverter) FileChunkDtoConverter.create();

		final Collection<TempFileChunk> tempFileChunks = tfcDao.getTempFileChunks(normalFile, remoteRepositoryId);

		final Collection<FileChunkDto> result = new ArrayList<>(tempFileChunks.size());
		for (final TempFileChunk tempFileChunk : tempFileChunks) {
			if (TempFileChunk.Role.SENDING != tempFileChunk.getRole()) {
				logger.warn("getTempFileChunkDtos: tempFileChunk.role is not SENDING! localPath='{}' normalFile={} tempFileChunk={}",
						localPath, normalFile, tempFileChunk);
				continue;
			}

			final FileChunkDto fileChunkDto = fcDtoConverter.toFileChunkDto(tempFileChunk);
			result.add(fileChunkDto);
		}
		return result;
	}

	@Override
	public RepoFileDto getRepoFileDto(String localPath) {
		assertNotNull(localPath, "localPath");

		final RepoFile repoFile = getRepoFile(localPath);
		if (repoFile == null)
			return null;

		final LocalRepoTransaction transaction = getTransactionOrFail();
		final RepoFileDtoConverter converter = RepoFileDtoConverter.create(transaction);
		final RepoFileDto repoFileDto = converter.toRepoFileDto(repoFile, Integer.MAX_VALUE); // TODO pass depth as argument - or maybe leave it this way?
		return repoFileDto;
	}

	@Override
	public void putTempFileChunkDto(String localPath, long offset) {
		assertNotNull(localPath, "localPath");

		final UUID remoteRepositoryId = getRemoteRepositoryIdOrFail();
		final TempFileChunkDao tfcDao = getTransactionOrFail().getDao(TempFileChunkDao.class);
		final RepoFile repoFile = getRepoFile(localPath);
		if (repoFile == null)
			throw new IllegalStateException("RepoFile not found for localPath: " + localPath);

		final NormalFile normalFile = (NormalFile) repoFile;

		TempFileChunk tempFileChunk = tfcDao.getTempFileChunk(normalFile, remoteRepositoryId, offset);
		if (tempFileChunk == null) {
			tempFileChunk = new TempFileChunk();
			tempFileChunk.setNormalFile(normalFile);
			tempFileChunk.setOffset(offset);
			tempFileChunk.setRemoteRepositoryId(remoteRepositoryId);
		}
		else {
			if (TempFileChunk.Role.SENDING != tempFileChunk.getRole()) {
				logger.warn("getTempFileChunkDtos: tempFileChunk.role is not SENDING! localPath='{}' normalFile={} tempFileChunk={}",
						localPath, normalFile, tempFileChunk);
			}
		}
		tempFileChunk.setRole(TempFileChunk.Role.SENDING);

		final FileChunk fileChunk = getFileChunkOrFail(normalFile, offset);
		tempFileChunk.setLength(fileChunk.getLength());
		tempFileChunk.setSha1(fileChunk.getSha1());

		tfcDao.makePersistent(tempFileChunk);
	}

	private FileChunk getFileChunkOrFail(NormalFile normalFile, long offset) {
		for (FileChunk fileChunk : normalFile.getFileChunks()) {
			if (offset == fileChunk.getOffset())
				return fileChunk;
		}
		throw new IllegalArgumentException("No FileChunk found for: " + normalFile + " " + offset);
	}

	@Override
	public void clearTempFileChunkDtos(String localPath) {
		assertNotNull(localPath, "localPath");
		final UUID remoteRepositoryId = getRemoteRepositoryIdOrFail();
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final TempFileChunkDao tfcDao = transaction.getDao(TempFileChunkDao.class);
		final RepoFile repoFile = getRepoFile(localPath);
		if (repoFile == null) {
			logger.debug("clearTempFileChunkDtos: localPath='{}': Nothing to delete!", localPath);
			return; // nothing to delete ;-)
		}

		final NormalFile normalFile = (NormalFile) repoFile;
		final Collection<TempFileChunk> tempFileChunks = tfcDao.getTempFileChunks(normalFile, remoteRepositoryId);
		logger.debug("clearTempFileChunkDtos: localPath='{}': Deleting {} tempFileChunks!", localPath, tempFileChunks.size());
		tfcDao.deletePersistentAll(tempFileChunks);
		transaction.flush();
	}

	protected RepoFile getRepoFile(final String localPath) {
		assertNotNull(localPath, "localPath");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final RepoFile repoFile = repoFileDao.getRepoFile(localRepoManager.getLocalRoot(), createFile(localRepoManager.getLocalRoot(), localPath));
		return repoFile;
	}
}
