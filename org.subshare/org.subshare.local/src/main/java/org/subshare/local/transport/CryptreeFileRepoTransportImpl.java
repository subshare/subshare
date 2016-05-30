package org.subshare.local.transport;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.dto.SsFileChunkDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeClientFileRepoTransport;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.PreliminaryCollision;
import org.subshare.local.persistence.PreliminaryCollisionDao;
import org.subshare.local.persistence.PreliminaryDeletion;
import org.subshare.local.persistence.PreliminaryDeletionDao;
import org.subshare.local.persistence.SsFileChunk;
import org.subshare.local.persistence.SsNormalFile;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseAdapter;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseEvent;
import co.codewizards.cloudstore.core.repo.transport.CollisionException;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepoDao;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;

public class CryptreeFileRepoTransportImpl extends FileRepoTransport implements CryptreeClientFileRepoTransport {

	private static final Logger logger = LoggerFactory.getLogger(CryptreeFileRepoTransportImpl.class);

	private Boolean metaOnly;
	private CryptreeFactory cryptreeFactory;
	private UserRepoKeyRing userRepoKeyRing;

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
		else {
			try {
				super.delete(path);
			} catch (CollisionException x) {
				clearCryptoRepoFileDeleted(path);
				throw x;
			}
		}
	}

	public void clearCryptoRepoFileDeleted(String path) {
		path = prefixPath(path);
		try (final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();) {
			getCryptree(transaction).clearCryptoRepoFileDeleted(path);

			transaction.commit();
		}
	}

	protected CryptreeFactory getCryptreeFactory() {
		if (cryptreeFactory == null)
			cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();

		return cryptreeFactory;
	}

	protected Cryptree getCryptree(final LocalRepoTransaction transaction) {
		return getCryptreeFactory().getCryptreeOrCreate(transaction, getClientRepositoryIdOrFail(), getPathPrefix(), getUserRepoKeyRing());
	}

	protected UserRepoKeyRing getUserRepoKeyRing() {
		if (userRepoKeyRing == null) {
			final UserRepoKeyRingLookup lookup = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup();
			final UserRepoKeyRingLookupContext context = new UserRepoKeyRingLookupContext(getRepositoryId(), getClientRepositoryIdOrFail());
			userRepoKeyRing = lookup.getUserRepoKeyRing(context);
			if (userRepoKeyRing == null)
				throw new IllegalStateException(String.format("UserRepoKeyRingLookup.getUserRepoKeyRing(context) returned null! lookup=%s context=%s", lookup, context));
		}
		return userRepoKeyRing;
	}

	private void deleteRepoFileRecursively(final LocalRepoTransaction transaction, final RepoFile repoFile) {
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		for (final RepoFile childRepoFile : repoFileDao.getChildRepoFiles(repoFile))
			deleteRepoFileRecursively(transaction, childRepoFile);

		repoFileDao.deletePersistent(repoFile);
	}

	@Override
	protected RepoFile syncRepoFile(final LocalRepoTransaction transaction, final File file) {
		assertNotNull("transaction", transaction);
		assertNotNull("file", file);

		final File localRoot = getLocalRepoManager().getLocalRoot();
		final RepoFileDao rfDao = transaction.getDao(RepoFileDao.class);
		RepoFile repoFile = rfDao.getRepoFile(localRoot, file);
		// If the type changed, we must delete the RepoFile here before invoking the super-method,
		// because this would otherwise cause an invocation of Cryptree.preDelete(...) causing
		// the actual file to be deleted.

		if (repoFile != null && ! LocalRepoSync.create(transaction).isRepoFileTypeCorrect(repoFile, file)) {
			rfDao.deletePersistent(repoFile);
			repoFile = null;
			transaction.flush();
		}
		repoFile = super.syncRepoFile(transaction, file);
		return repoFile;
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
	protected void mkDir(LocalRepoTransaction transaction, UUID clientRepositoryId, File file, Date lastModified) {
//		// CloudStore does not check for directory collisions, but silently ignores them (since the
//		// timestamp is the only thing that can collide and is assumed to be unimportant). Because of
//		// the different way Subshare handles collisions, Subshare wants to detect this type of collisions,
//		// too. Hence, we invoke the check here.
//
//		if (lastModified != null) { // this method is invoked recursively - we only want to react on the primary invocation
//			if (file.isDirectory() && ! file.isSymbolicLink()) { // it exists and is a directory
//				final File localRoot = getLocalRepoManager().getLocalRoot();
//				if (! localRoot.equals(file)) { // we ignore the local root, because we otherwise *always* have a collision on the first down-sync and the local root does not really matter. corner-case too rare and too unimportant to properly handle ;-)
//					RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, file);
//					if (repoFile == null) { // it was newly created between the last LocalSync and this mkDir invocation!
//						// currently ignored - really rare corner case ;-)
//					}
//					if (repoFile instanceof Directory) { // this is the only type of collision we have to check now.
//						detectAndHandleFileCollision(transaction, clientRepositoryId, file, repoFile);
//					}
//				}
//			}
//		}
//
//		// hmmm... IMHO this sucks. Too many collisions in situations probably no user ever cares about.
//		// I thus commented out the above check.

		super.mkDir(transaction, clientRepositoryId, file, lastModified);
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
				createAndPersistPreliminaryCollision(event.getLocalRepoManager(), file, null, null);
			}
			@Override
			public void postCommit(LocalRepoTransactionPostCloseEvent event) {
				throw new IllegalStateException("Commit is not allowed, anymore!");
			}
		});

		throw new CollisionException();
	}

	@Override
	protected void handleFileTypeCollision(LocalRepoTransaction transaction, UUID fromRepositoryId, File file, Class<? extends RepoFileDto> fromFileType) {
		// In contrast to CloudStore, Subshare does not rename the collision-file immediately. Therefore,
		// this method is invoked when a type-collision was already handled somewhere else and we're re-downloading
		// here.

		final RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(fromRepositoryId);
		final LastSyncToRemoteRepo lastSyncToRemoteRepo = transaction.getDao(LastSyncToRemoteRepoDao.class).getLastSyncToRemoteRepo(remoteRepository);

		final File localRoot = getLocalRepoManager().getLocalRoot();
		final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, file);

		if (lastSyncToRemoteRepo != null && repoFile.getLocalRevision() <= lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced()) {
			file.deleteRecursively();
			return;
		}
		super.handleFileTypeCollision(transaction, fromRepositoryId, file, fromFileType);
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

	protected void createAndPersistPreliminaryCollision(final LocalRepoManager localRepoManager, final File file, String localPath, Uid cryptoRepoFileId) {
		assertNotNull("localRepoManager", localRepoManager);
		if (localPath == null)
			assertNotNull("localPath/file", file);

		logger.debug("createAndPersistPreliminaryCollision: localRoot='{}' localRepositoryId={} file='{}' localPath='{}' cryptoRepoFileId={}",
				localRepoManager.getLocalRoot(), getRepositoryId(), (file == null ? "" : file.getAbsolutePath()),
						(localPath == null ? "" : localPath), cryptoRepoFileId);

		try (final LocalRepoTransaction tx = localRepoManager.beginWriteTransaction();) {
			if (localPath == null)
				localPath = '/' + localRepoManager.getLocalRoot().relativize(file).replace(FILE_SEPARATOR_CHAR, '/');

			final PreliminaryCollisionDao pcDao = tx.getDao(PreliminaryCollisionDao.class);

			PreliminaryCollision preliminaryCollision = pcDao.getPreliminaryCollision(localPath);
			if (preliminaryCollision == null) {
				preliminaryCollision = new PreliminaryCollision();
				preliminaryCollision.setPath(localPath);
				preliminaryCollision = pcDao.makePersistent(preliminaryCollision);
			}

			final CryptoRepoFileDao crfDao = tx.getDao(CryptoRepoFileDao.class);
			if (cryptoRepoFileId != null) {
				CryptoRepoFile cryptoRepoFile = crfDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
				preliminaryCollision.setCryptoRepoFile(cryptoRepoFile);
			}
			else if (file != null) {
				final RepoFileDao rfDao = tx.getDao(RepoFileDao.class);
				final RepoFile repoFile = rfDao.getRepoFile(localRepoManager.getLocalRoot(), file);
				if (repoFile != null) {
					final CryptoRepoFile cryptoRepoFile = crfDao.getCryptoRepoFileOrFail(repoFile);
					preliminaryCollision.setCryptoRepoFile(cryptoRepoFile);
				}
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

	@Override
	protected void assertNoDeleteModificationCollision(LocalRepoTransaction transaction, UUID fromRepositoryId, final String path) throws CollisionException {
		// super.assertNoDeleteModificationCollision(transaction, fromRepositoryId, path); // DeleteModification is *NOT* used by Subshare!

		final Uid cryptoRepoFileId = getCryptree(transaction).getCryptoRepoFileId(path);
		if (cryptoRepoFileId == null)
			return;

		final PreliminaryDeletionDao pdDao = transaction.getDao(PreliminaryDeletionDao.class);
		final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFileOrFail(cryptoRepoFileId);
		CryptoRepoFile crf = cryptoRepoFile;
		while (crf != null) {
			final String candidateLocalPath = crf.getLocalPathOrFail();
			final PreliminaryDeletion preliminaryDeletion = pdDao.getPreliminaryDeletion(crf);
			if (crf.getDeleted() != null || preliminaryDeletion != null) {
				transaction.addPostCloseListener(new LocalRepoTransactionPostCloseAdapter() {
					@Override
					public void postRollback(LocalRepoTransactionPostCloseEvent event) {
						createAndPersistPreliminaryCollision(event.getLocalRepoManager(), null, path, cryptoRepoFileId);
					}
					@Override
					public void postCommit(LocalRepoTransactionPostCloseEvent event) {
						throw new IllegalStateException("Commit is not allowed, anymore!");
					}
				});

				throw new DeleteModificationCollisionException(
						String.format("The associated CryptoRepoFile or one of its parents is marked as deleted! repositoryId=%s path='%s' deletedPath='%s'", fromRepositoryId, path, candidateLocalPath));
			}

			crf = crf.getParent();
		}
	}
}
