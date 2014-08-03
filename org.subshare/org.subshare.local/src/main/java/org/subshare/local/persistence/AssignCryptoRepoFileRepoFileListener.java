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
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class AssignCryptoRepoFileRepoFileListener extends AbstractLocalRepoTransactionListener implements StoreLifecycleListener {

	// used on server side
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
			final RepoFile repoFile;
			if (cryptoRepoFile.getLocalName() != null) // on client-side!
				repoFile = getRepoFileViaCryptoRepoFileLocalName(cryptoRepoFile);
			else // on server-side
				repoFile = repoFileName2RepoFile.get(cryptoRepoFile.getCryptoRepoFileId().toString());

			if (repoFile != null)
				cryptoRepoFile.setRepoFile(repoFile);
		}
	}

	private RepoFile getRepoFileViaCryptoRepoFileLocalName(final CryptoRepoFile cryptoRepoFile) {
		RepoFile repoFile = cryptoRepoFile.getRepoFile();
		if (repoFile == null) {
			final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
			if (parentCryptoRepoFile != null) {
				final RepoFile parentRepoFile = getRepoFileViaCryptoRepoFileLocalName(parentCryptoRepoFile);
				if (parentRepoFile == null)
					return null;

				final String localName = cryptoRepoFile.getLocalName();
				if (localName == null)
					throw new IllegalStateException("cryptoRepoFile.localName == null");

				repoFile = getTransactionOrFail().getDao(RepoFileDao.class).getChildRepoFile(parentRepoFile, localName);
			}
			else // TODO this needs to be changed when we allow checking out sub-directories.
				repoFile = getTransactionOrFail().getDao(RepoFileDao.class).getLocalRootDirectory();

			if (repoFile != null)
				cryptoRepoFile.setRepoFile(repoFile);
		}
		return repoFile;
	}
}
