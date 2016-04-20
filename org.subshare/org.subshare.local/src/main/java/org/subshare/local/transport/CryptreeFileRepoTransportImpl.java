package org.subshare.local.transport;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.dto.SsFileChunkDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeClientFileRepoTransport;
import org.subshare.local.persistence.PreliminaryCollision;
import org.subshare.local.persistence.PreliminaryCollisionDao;
import org.subshare.local.persistence.SsFileChunk;
import org.subshare.local.persistence.SsNormalFile;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseAdapter;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseEvent;
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

		// TO DO must not invoke super method! Must throw exception instead! We must not handle collisions in the server! We must throw
		// a detailed exception instead.
		// ... seems to be fine, because the super-method calls handleFileCollision(...) which is overridden, below.
	}

	@Override
	protected File handleFileCollision(final LocalRepoTransaction transaction, final UUID fromRepositoryId, final File file) {
		transaction.addPostCloseListener(new LocalRepoTransactionPostCloseAdapter() {
			@Override
			public void postRollback(LocalRepoTransactionPostCloseEvent event) {
				createAndPersistPreliminaryCollision(event.getLocalRepoManager(), file);
			}
			@Override
			public void postCommit(LocalRepoTransactionPostCloseEvent event) {
				throw new IllegalStateException("Commit is not allowed, anymore!");
			}
		});

		throw new CollisionException();
	}

//	protected void createAndPersistCollision(final LocalRepoManager localRepoManager, final UUID fromRepositoryId, final File file) {
//		try (final LocalRepoTransaction tx = localRepoManager.beginWriteTransaction();) {
//			final String remotePathPrefix = ""; //$NON-NLS-1$ // TODO is this really fine?! If so, we should explain, why! And we should test!!!
//
//			final UserRepoKeyRing userRepoKeyRing = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup().getUserRepoKeyRing(
//					new UserRepoKeyRingLookupContext(localRepoManager.getRepositoryId(), fromRepositoryId));
//
//			Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail()
//					.getCryptreeOrCreate(tx, fromRepositoryId, remotePathPrefix, userRepoKeyRing);
//
//			final RepoFileDao repoFileDao = tx.getDao(RepoFileDao.class);
//			final RepoFile repoFile = repoFileDao.getRepoFile(localRepoManager.getLocalRoot(), file);
//			final String localPath = repoFile.getPath();
//
//			cryptree.createUnsealedHistoFrameIfNeeded();
//			LocalRepoSync.create(tx).sync(file, new NullProgressMonitor(), false);
//			cryptree.createCollisionIfNeeded(localPath);
//
//			tx.commit();
//		}
//	}

	protected void createAndPersistPreliminaryCollision(final LocalRepoManager localRepoManager, final File file) {
		try (final LocalRepoTransaction tx = localRepoManager.beginWriteTransaction();) {
			final String localPath = '/' + localRepoManager.getLocalRoot().relativize(file).replace(FILE_SEPARATOR_CHAR, '/');
			final PreliminaryCollisionDao pcDao = tx.getDao(PreliminaryCollisionDao.class);

			PreliminaryCollision preliminaryCollision = pcDao.getPreliminaryCollision(localPath);
			if (preliminaryCollision == null) {
				preliminaryCollision = new PreliminaryCollision();
				preliminaryCollision.setPath(localPath);
				pcDao.makePersistent(preliminaryCollision);
			}

			tx.commit();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
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

}
