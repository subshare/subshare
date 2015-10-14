package org.subshare.local.persistence;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;

import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;

public class DeleteTempFileChunkListener extends AbstractLocalRepoTransactionListener implements DeleteLifecycleListener {

	@Override
	public void onBegin() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PersistenceManager pm = ((ContextWithPersistenceManager)tx).getPersistenceManager();
		pm.addInstanceLifecycleListener(this, TempFileChunk.class);
	}

	@Override
	public void preDelete(final InstanceLifecycleEvent event) {
		final TempFileChunk tempFileChunk = (TempFileChunk) event.getPersistentInstance();
		final LocalRepoTransaction tx = getTransactionOrFail();
		final FileChunkPayloadDao fileChunkPayloadDao = tx.getDao(FileChunkPayloadDao.class);

		final FileChunkPayload fileChunkPayload = fileChunkPayloadDao.getFileChunkPayload(tempFileChunk);
		if (fileChunkPayload != null) {
			// check, if it's still referenced by a HistoFileChunk!
			final long histoFileChunkCount = tx.getDao(HistoFileChunkDao.class).getHistoFileChunkCount(fileChunkPayload);
			if (histoFileChunkCount == 0)
				fileChunkPayloadDao.deletePersistent(fileChunkPayload);
			else
				fileChunkPayload.setTempFileChunk(null);
		}

		tx.flush();
	}

	@Override
	public void postDelete(final InstanceLifecycleEvent event) { }
}
