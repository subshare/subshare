package org.subshare.local.transport;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.context.RepoFileContext;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeFileRepoTransport;
import org.subshare.local.persistence.SsDeleteModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDao;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;
import co.codewizards.cloudstore.local.transport.ParentFileLastModifiedManager;
import co.codewizards.cloudstore.local.transport.TempChunkFileManager;

public class CryptreeFileRepoTransportImpl extends FileRepoTransport implements CryptreeFileRepoTransport {

	private static final Logger logger = LoggerFactory.getLogger(CryptreeFileRepoTransportImpl.class);

	@Override
	public void delete(final SsDeleteModificationDto deleteModificationDto) {
		assertNotNull("deleteModificationDto", deleteModificationDto);
		final UUID clientRepositoryId = assertNotNull("clientRepositoryId", getClientRepositoryId());

		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final String path = deleteModificationDto.getServerPath();
			final File file = getFile(path); // we *must* *not* prefix this path! It is absolute inside the repository (= relative to the repository's root - not the connection point)!
			if (!file.exists())
				return; // already deleted (probably by other client) => no need to do anything (and a DB rollback is fine)

			// We check first, if the file exists, because we cannot check the validity of the signature (more precisely the
			// permissions), if the parent was deleted.

			final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(transaction, clientRepositoryId);
			cryptree.assertSignatureOk(deleteModificationDto);

			final boolean collision = detectFileCollisionRecursively(transaction, clientRepositoryId, file);
			if (collision) // TODO find out what exactly was modified and return these more precise infos to the client!
				throw new DeleteModificationCollisionException(String.format("Collision in repository %s: The file/directory '%s' cannot be deleted, because it was modified by someone else in the meantime.", localRepoManager.getRepositoryId(), path));

			createAndPersistDeleteModifications(transaction, deleteModificationDto);

			final LocalRepoSync localRepoSync = LocalRepoSync.create(transaction);
			final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
			if (repoFile != null)
				localRepoSync.deleteRepoFile(repoFile, false);

			if (!IOUtil.deleteDirectoryRecursively(file))
				throw new IllegalStateException("Deleting file or directory failed: " + file);

			transaction.commit();
		}
	}

	private void createAndPersistDeleteModifications(final LocalRepoTransaction transaction, final SsDeleteModificationDto deleteModificationDto) {
		assertNotNull("transaction", transaction);
		assertNotNull("deleteModificationDto", deleteModificationDto);
		final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
		final DeleteModificationDao deleteModificationDao = transaction.getDao(DeleteModificationDao.class);

		for (final RemoteRepository remoteRepository : remoteRepositoryDao.getObjects()) {
			if (!getClientRepositoryIdOrFail().equals(remoteRepository.getRepositoryId())) {
				SsDeleteModification deleteModification = toDeleteModification(transaction, deleteModificationDto, remoteRepository);
				deleteModification = deleteModificationDao.makePersistent(deleteModification);
			}
		}
	}

	private SsDeleteModification toDeleteModification(final LocalRepoTransaction transaction, final SsDeleteModificationDto deleteModificationDto, final RemoteRepository remoteRepository) {
		assertNotNull("transaction", transaction);
		assertNotNull("deleteModificationDto", deleteModificationDto);
		assertNotNull("remoteRepository", remoteRepository);

		final SsDeleteModification deleteModification = new SsDeleteModification();
		deleteModification.setRemoteRepository(remoteRepository);
		deleteModification.setServerPath(deleteModificationDto.getServerPath());
		deleteModification.setPath(deleteModificationDto.getServerPath()); // the field does not allow null => must set it to a non-null value.
		deleteModification.setCryptoRepoFileIdControllingPermissions(deleteModificationDto.getCryptoRepoFileIdControllingPermissions());
		deleteModification.setLength(-1);
		deleteModification.setSignature(deleteModificationDto.getSignature());
		return deleteModification;
	}

	@Override
	public void beginPutFile(final String path) {
		final RepoFileContext context = RepoFileContext.getContext();
		boolean isOnServer = context != null;
		if (isOnServer) {
			final SsNormalFileDto normalFileDto = (SsNormalFileDto) context.getSsRepoFileDto();
			beginPutFile(path, normalFileDto);
		}
		else
			super.beginPutFile(path);
	}

	private final TempChunkFileManager tempChunkFileManager = TempChunkFileManager.getInstance();

	protected void beginPutFile(String path, final SsNormalFileDto normalFileDto) {
		assertNotNull("normalFileDto", normalFileDto);

		path = prefixPath(path);
		final File file = getFile(path); // null-check already inside getFile(...) - no need for another check here
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final File parentFile = file.getParentFile();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				if (file.isSymbolicLink() || (file.exists() && !file.isFile())) { // exists() and isFile() both resolve symlinks! Their result depends on where the symlink points to.
					logger.info("beginPutFile: Collision: Destination file already exists and is a symlink or a directory! file='{}'", file.getAbsolutePath());
//					final File collisionFile = IOUtil.createCollisionFile(file);
//					file.renameTo(collisionFile);
//					LocalRepoSync.create(transaction).sync(collisionFile, new NullProgressMonitor(), true); // recursiveChildren==true, because the colliding thing might be a directory.
					// Must not sync! On the server, we cannot handle collisions! Must throw proper exception!
					// *Temporarily* throwing this exception:
					throw new UnsupportedOperationException("Collision handling not yet implemented!");
				}

				if (file.isSymbolicLink() || (file.exists() && !file.isFile()))
					throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

				final File localRoot = getLocalRepoManager().getLocalRoot();
				assertNoDeleteModificationCollision(transaction, clientRepositoryId, path);

				boolean newFile = false;
				if (!file.isFile()) {
					newFile = true;
					try {
						file.createNewFile();
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
				}

				if (!file.isFile())
					throw new IllegalStateException("Could not create file (permissions?!): " + file);

				// A complete sync run might take very long. Therefore, we better update our local meta-data
				// *immediately* before beginning the sync of this file and before detecting a collision.
				// Furthermore, maybe the file is new and there's no meta-data, yet, hence we must do this anyway.
				final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);

				// Must not sync, if it already exists! This causes a collision! And it's actually is unnecessary,
				// because changes can only happen through this transport.
				RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);
				if (repoFile == null)
					LocalRepoSync.create(transaction).sync(file, new NullProgressMonitor(), false); // recursiveChildren has no effect on simple files, anyway (it's no directory).

				tempChunkFileManager.deleteTempChunkFilesWithoutDtoFile(tempChunkFileManager.getOffset2TempChunkFileWithDtoFile(file).values());

				repoFile = repoFileDao.getRepoFile(localRoot, file);
				if (repoFile == null)
					throw new IllegalStateException("LocalRepoSync.sync(...) did not create the RepoFile for file: " + file);

				if (!(repoFile instanceof NormalFile))
					throw new IllegalStateException("LocalRepoSync.sync(...) created an instance of " + repoFile.getClass().getName() + " instead  of a NormalFile for file: " + file);

				final NormalFile normalFile = (NormalFile) repoFile;

				if (!newFile && !normalFile.isInProgress())
					detectAndHandleFileCollision(transaction, clientRepositoryId, file, normalFile);

				normalFile.setLastSyncFromRepositoryId(clientRepositoryId);
				normalFile.setInProgress(true);
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		}
	}

	@Override
	protected void detectAndHandleFileCollision(
			LocalRepoTransaction transaction, UUID fromRepositoryId, File file,
			RepoFile normalFileOrSymlink) {
		super.detectAndHandleFileCollision(transaction, fromRepositoryId, file, normalFileOrSymlink);

		// TODO must not invoke super method! Must throw exception instead! We must not handle collisions in the server! We must throw
		// a detailed exception instead.
	}
}
