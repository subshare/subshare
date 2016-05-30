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
//			PreliminaryDeletion preliminaryDeletion = tx.getDao(PreliminaryDeletionDao.class).getPreliminaryDeletion(cryptoRepoFile);
//			if (preliminaryDeletion == null && cryptoRepoFile.getDeleted() == null)
//				throw new IllegalStateException(String.format("%s not marked as deleted!", cryptoRepoFile));
			// The above is a legal state when switching from one type of RepoFile to another (e.g. from NormalFile to Directory).

			cryptoRepoFile.setRepoFile(null); // this must be done, now, because we cannot delete the RepoFile otherwise due to a constraint-violation
		}
	}

	@Override
	public void postDelete(final InstanceLifecycleEvent event) { }
}
