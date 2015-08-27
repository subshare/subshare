package org.subshare.local.transport;

import java.util.Iterator;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;

public class CryptreeFileRepoTransportImpl extends FileRepoTransport {

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
			final Iterator<UUID> repoIdIt = getLocalRepoManager().getRemoteRepositoryId2RemoteRootMap().keySet().iterator();
			if (! repoIdIt.hasNext())
				throw new IllegalStateException("There is no remote-repository!");

			final UUID serverRepositoryId = repoIdIt.next();
			try (final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction();) {
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(transaction, serverRepositoryId);
				metaOnly = cryptree.isMetaOnly();
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
}
