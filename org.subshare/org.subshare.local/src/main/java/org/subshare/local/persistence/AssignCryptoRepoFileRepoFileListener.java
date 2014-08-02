package org.subshare.local.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class AssignCryptoRepoFileRepoFileListener extends AbstractLocalRepoTransactionListener implements StoreLifecycleListener {

	private final Map<String, RepoFile> repoFileName2RepoFile = new HashMap<>();

	@Override
	public void onBegin() {
		final PersistenceManager pm = ((ContextWithPersistenceManager)getTransactionOrFail()).getPersistenceManager();
		pm.addInstanceLifecycleListener(this, RepoFile.class);
	}

	@Override
	public void preStore(final InstanceLifecycleEvent event) { }

	@Override
	public void postStore(final InstanceLifecycleEvent event) {
		final RepoFile repoFile = (RepoFile) event.getPersistentInstance();
		repoFileName2RepoFile.put(repoFile.getName(), repoFile);
	}

	@Override
	public void onCommit() {
		if (repoFileName2RepoFile.isEmpty())
			return;

		final CryptoRepoFileDao cryptoRepoFileDao = getTransactionOrFail().getDao(CryptoRepoFileDao.class);
		final Collection<CryptoRepoFile> cryptoRepoFiles = cryptoRepoFileDao.getCryptoRepoFilesWithoutRepoFile();
		for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFiles) {
			final RepoFile repoFile = repoFileName2RepoFile.get(cryptoRepoFile.getCryptoRepoFileId().toString());
			if (repoFile != null)
				cryptoRepoFile.setRepoFile(repoFile);
		}
	}

}
