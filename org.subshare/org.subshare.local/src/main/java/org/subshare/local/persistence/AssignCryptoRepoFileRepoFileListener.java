package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

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
		assignCryptoRepoFileRepoFile();
	}

	private void assignCryptoRepoFileRepoFile() {
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

	/**
	 * Gets the {@link RepoFile} via {@link CryptoRepoFile#getLocalName() cryptoRepoFile.localName}.
	 * <p>
	 * This method only works and should thus only be executed on the client-side!
	 * @param cryptoRepoFile
	 * @return
	 */
	private RepoFile getRepoFileViaCryptoRepoFileLocalName(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		// Early failure: Check cryptoRepoFile.localName even if we don't need it.
		final String localName = cryptoRepoFile.getLocalName();
		assertNotNull("cryptoRepoFile.localName", localName);

		RepoFile repoFile = cryptoRepoFile.getRepoFile();
		if (repoFile == null) {
			final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
			if (parentCryptoRepoFile != null) {
				final RepoFile parentRepoFile = getRepoFileViaCryptoRepoFileLocalName(parentCryptoRepoFile);
				if (parentRepoFile == null)
					return null;

				repoFile = getTransactionOrFail().getDao(RepoFileDao.class).getChildRepoFile(parentRepoFile, localName);
			}
//			else {
//				throw new IllegalStateException("The root was not initialised! It should have been, though!");
////				repoFile = getTransactionOrFail().getDao(RepoFileDao.class).getLocalRootDirectory(); // works only, if no sub-dir is checked out.
//			}

			if (repoFile != null)
				cryptoRepoFile.setRepoFile(repoFile);
		}
		return repoFile;
	}
}
