package org.subshare.local.transport;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.dto.SsFileChunkDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeClientFileRepoTransport;
import org.subshare.local.persistence.SsFileChunk;
import org.subshare.local.persistence.SsNormalFile;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.CollisionException;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;

public class CryptreeFileRepoTransportImpl extends FileRepoTransport implements CryptreeClientFileRepoTransport {

	private Boolean metaOnly;

	@Override
	public void delete(String path) {
		if (isMetaOnly()) {
			path = prefixPath(path);
			final File localRoot = getLocalRepoManager().getLocalRoot();
			try (final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();) {
				final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
				final File file = getFile(path);
				final RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);
				if (repoFile != null)
					deleteRepoFileRecursively(transaction, repoFile);

				transaction.commit();
			}
		}
		else
			super.delete(path);
	}

	private void deleteRepoFileRecursively(final LocalRepoTransaction transaction, final RepoFile repoFile) {
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		for (final RepoFile childRepoFile : repoFileDao.getChildRepoFiles(repoFile))
			deleteRepoFileRecursively(transaction, childRepoFile);

		repoFileDao.deletePersistent(repoFile);
	}

	private boolean isMetaOnly() {
		if (metaOnly == null) {
//			final Iterator<UUID> repoIdIt = getLocalRepoManager().getRemoteRepositoryId2RemoteRootMap().keySet().iterator();
//			if (! repoIdIt.hasNext())
//				throw new IllegalStateException("There is no remote-repository!");
//
//			final UUID serverRepositoryId = repoIdIt.next();
			try (final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction();) {
				final LocalRepoStorage lrs = LocalRepoStorageFactoryRegistry.getInstance().getLocalRepoStorageFactoryOrFail().getLocalRepoStorageOrCreate(transaction);

//				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(transaction, serverRepositoryId);
				metaOnly = lrs.isMetaOnly();
				transaction.commit();
			}
		}
		return metaOnly;
	}

	@Override
	protected void detectAndHandleFileCollision(
			LocalRepoTransaction transaction, UUID fromRepositoryId, File file,
			RepoFile normalFileOrSymlink) {
		super.detectAndHandleFileCollision(transaction, fromRepositoryId, file, normalFileOrSymlink);

		// TODO must not invoke super method! Must throw exception instead! We must not handle collisions in the server! We must throw
		// a detailed exception instead.
	}

	@Override
	public void beginPutFile(String path) {
		throw new UnsupportedOperationException("Should not be invoked on client-side!");
	}

	@Override
	public void beginPutFile(String path, SsNormalFileDto normalFileDto) {
		super.beginPutFile(path);
	}

	@Override
	public void endPutFile(String path, Date lastModified, long length, String sha1) {
		throw new UnsupportedOperationException("Should not be invoked on client-side!");
	}

	@Override
	public void endPutFile(String path, SsNormalFileDto fromNormalFileDto) {
		putPaddingMetaData(path, fromNormalFileDto);
		super.endPutFile(path, fromNormalFileDto.getLastModified(), fromNormalFileDto.getLength(), fromNormalFileDto.getSha1());
	}

	private void putPaddingMetaData(String path, SsNormalFileDto fromNormalFileDto) {
		path = prefixPath(path); // does a null-check
		assertNotNull("fromNormalFileDto", fromNormalFileDto);

		final File file = getFile(path);
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
			if (!(repoFile instanceof NormalFile)) {
				throw new IllegalStateException(String.format("RepoFile is not an instance of NormalFile! repoFile=%s file=%s",
						repoFile, file));
			}

			final SsNormalFile normalFile = (SsNormalFile) repoFile;
			normalFile.setLengthWithPadding(fromNormalFileDto.getLengthWithPadding());

			final Map<Long, SsFileChunk> offset2FileChunk = new HashMap<>(normalFile.getFileChunks().size());
			for (FileChunk fc : normalFile.getFileChunks())
				offset2FileChunk.put(fc.getOffset(), (SsFileChunk) fc);

			for (final FileChunkDto fcDto : fromNormalFileDto.getFileChunkDtos()) {
				SsFileChunkDto fileChunkDto = (SsFileChunkDto) fcDto;

				// If there is at least 1 byte of real data, the SHA1 (as well as the entire FileChunk object)
				// is created from it and we don't need to store the FileChunk we received from the other side.
				if (fileChunkDto.getLength() > 0)
					continue;

				boolean isNew = false;
				SsFileChunk fileChunk = offset2FileChunk.get(fileChunkDto.getOffset());
				if (fileChunk == null) {
					isNew = true;
					fileChunk = (SsFileChunk) createObject(FileChunk.class);
					fileChunk.setNormalFile(normalFile);
					fileChunk.setOffset(fileChunkDto.getOffset());
				}
				fileChunk.makeWritable();
				fileChunk.setLength(fileChunkDto.getLength());
				fileChunk.setLengthWithPadding(fileChunkDto.getLengthWithPadding());
				fileChunk.setSha1(fileChunkDto.getSha1());
				fileChunk.makeReadOnly();

				if (isNew)
					normalFile.getFileChunks().add(fileChunk);
			}

			transaction.commit();
		}
	}

	@Override
	protected File handleFileCollision(LocalRepoTransaction transaction, UUID fromRepositoryId, File file) {
		throw new CollisionException();
	}
}
