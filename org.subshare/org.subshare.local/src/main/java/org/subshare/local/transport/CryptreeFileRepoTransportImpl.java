package org.subshare.local.transport;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.repo.transport.CryptreeFileRepoTransport;
import org.subshare.local.persistence.SsDeleteModification;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;

public class CryptreeFileRepoTransportImpl extends FileRepoTransport implements CryptreeFileRepoTransport {

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
}
