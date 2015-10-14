package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class AssignCryptoRepoFileRepoFileListener extends AbstractLocalRepoTransactionListener implements StoreLifecycleListener {

	// Used primarily on server side (where the repoFileName is the unique cryptoRepoFileId)
	// On the client side, it only matters whether it's empty (nothing to do) or not (sth. to do).
	private final Map<String, RepoFile> repoFileName2RepoFile = new HashMap<>();

	@Override
	public void onBegin() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PersistenceManager pm = ((ContextWithPersistenceManager)tx).getPersistenceManager();
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
		final Collection<CryptoRepoFile> cryptoRepoFiles = cryptoRepoFileDao.getCryptoRepoFilesWithoutRepoFileAndNotDeleted();
		for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFiles) {
			final RepoFile repoFile;
			if (cryptoRepoFile.getLocalName() != null) // on client-side!
				repoFile = getRepoFileViaCryptoRepoFileLocalName(cryptoRepoFile);
			else // on server-side
				repoFile = repoFileName2RepoFile.get(cryptoRepoFile.getCryptoRepoFileId().toString());

			if (repoFile != null)
				cryptoRepoFile.setRepoFile(repoFile);
		}

		repoFileName2RepoFile.clear();
	}

	/**
	 * Gets the {@link RepoFile} via {@link CryptoRepoFile#getLocalName() cryptoRepoFile.localName}.
	 * <p>
	 * This method only works and should thus only be executed on the client-side!
	 * @param cryptoRepoFile the {@link CryptoRepoFile} for which to look up the {@link RepoFile}.
	 * @return
	 */
	private RepoFile getRepoFileViaCryptoRepoFileLocalName(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		RepoFile repoFile = cryptoRepoFile.getRepoFile();
		if (repoFile == null) {
			final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
			if (parentCryptoRepoFile != null) {
				final RepoFile parentRepoFile = getRepoFileViaCryptoRepoFileLocalName(parentCryptoRepoFile);
				if (parentRepoFile == null)
					return null;

				// Please note: cryptoRepoFile.localName is null, if the user has no read-access to it.
				// We currently synchronise all CryptoRepoFile instances of the entire server repository,
				// even if the client checked-out a sub-directory only (and is allowed to read only this sub-dir).
				final String localName = cryptoRepoFile.getLocalName();
				if (localName != null)
					repoFile = getTransactionOrFail().getDao(RepoFileDao.class).getChildRepoFile(parentRepoFile, localName);
			}
			if (repoFile != null)
				cryptoRepoFile.setRepoFile(repoFile);
		}
		return repoFile;
	}
}
