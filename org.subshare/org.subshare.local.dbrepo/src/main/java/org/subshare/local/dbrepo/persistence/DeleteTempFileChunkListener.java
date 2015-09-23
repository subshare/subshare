package org.subshare.local.dbrepo.persistence;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;

import org.subshare.local.persistence.TempFileChunk;

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
		if (fileChunkPayload != null)
			fileChunkPayloadDao.deletePersistent(fileChunkPayload);

		tx.flush();
	}

	@Override
	public void postDelete(final InstanceLifecycleEvent event) { }
}
