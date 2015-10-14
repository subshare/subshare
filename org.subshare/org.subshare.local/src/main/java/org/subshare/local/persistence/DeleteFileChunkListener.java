package org.subshare.local.persistence;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;

import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.FileChunk;

public class DeleteFileChunkListener extends AbstractLocalRepoTransactionListener implements DeleteLifecycleListener {

	@Override
	public void onBegin() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PersistenceManager pm = ((ContextWithPersistenceManager)tx).getPersistenceManager();
		pm.addInstanceLifecycleListener(this, FileChunk.class);
	}

	@Override
	public void preDelete(final InstanceLifecycleEvent event) {
		final FileChunk fileChunk = (FileChunk) event.getPersistentInstance();
		final LocalRepoTransaction tx = getTransactionOrFail();
		final FileChunkPayloadDao fileChunkPayloadDao = tx.getDao(FileChunkPayloadDao.class);

		final FileChunkPayload fileChunkPayload = fileChunkPayloadDao.getFileChunkPayload(fileChunk);
		if (fileChunkPayload != null) {
			// check, if it's still referenced by a HistoFileChunk!
			final long histoFileChunkCount = tx.getDao(HistoFileChunkDao.class).getHistoFileChunkCount(fileChunkPayload);
			if (histoFileChunkCount == 0)
				fileChunkPayloadDao.deletePersistent(fileChunkPayload);
			else
				fileChunkPayload.setFileChunk(null);
		}

		tx.flush();
	}

	@Override
	public void postDelete(final InstanceLifecycleEvent event) { }
}
