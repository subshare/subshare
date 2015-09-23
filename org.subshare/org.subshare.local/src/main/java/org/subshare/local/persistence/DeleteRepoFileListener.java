package org.subshare.local.persistence;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;

import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class DeleteRepoFileListener extends AbstractLocalRepoTransactionListener implements DeleteLifecycleListener {

	@Override
	public void onBegin() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PersistenceManager pm = ((ContextWithPersistenceManager)tx).getPersistenceManager();
		pm.addInstanceLifecycleListener(this, RepoFile.class);
	}

	@Override
	public void preDelete(final InstanceLifecycleEvent event) {
		final RepoFile repoFile = (RepoFile) event.getPersistentInstance();
		final LocalRepoTransaction tx = getTransactionOrFail();

		if (repoFile instanceof NormalFile) {
			final NormalFile normalFile = (NormalFile) repoFile;
			final TempFileChunkDao tempFileChunkDao = tx.getDao(TempFileChunkDao.class);
			final Collection<TempFileChunk> tempFileChunks = tempFileChunkDao.getTempFileChunks(normalFile);
			tempFileChunkDao.deletePersistentAll(tempFileChunks);
			tx.flush();
		}

		final CryptoRepoFileDao cryptoRepoFileDao = tx.getDao(CryptoRepoFileDao.class);
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(repoFile);
		if (cryptoRepoFile != null) {
			cryptoRepoFileDao.deletePersistent(cryptoRepoFile);
			tx.flush();
		}
	}

	@Override
	public void postDelete(final InstanceLifecycleEvent event) { }
}
