package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.local.DuplicateCryptoRepoFileHandler;

import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class AssignCryptoRepoFileRepoFileListener extends AbstractLocalRepoTransactionListener implements StoreLifecycleListener {
	private static final Logger logger = LoggerFactory.getLogger(AssignCryptoRepoFileRepoFileListener.class);

	// Used primarily on server side (where the repoFileName is the unique cryptoRepoFileId)
	// On the client side, it only matters whether it's empty (nothing to do) or not (sth. to do).
	private final Map<String, RepoFile> repoFileName2RepoFile = new HashMap<>();
	private boolean cryptoRepoFilePersisted;
	private boolean histoCryptoRepoFilePersisted;

	@Override
	public void onBegin() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PersistenceManager pm = ((ContextWithPersistenceManager)tx).getPersistenceManager();
		pm.addInstanceLifecycleListener(this, RepoFile.class, CryptoRepoFile.class, HistoCryptoRepoFile.class);
	}

	@Override
	public void preStore(final InstanceLifecycleEvent event) { }

	@Override
	public void postStore(final InstanceLifecycleEvent event) {
		final Object persistable = assertNotNull(event.getPersistentInstance(), "event.persistentInstance");
		if (persistable instanceof RepoFile) {
			final RepoFile repoFile = (RepoFile) persistable;
			repoFileName2RepoFile.put(assertNotNull(repoFile.getName(), "repoFile.name"), repoFile);
		}
		else if (persistable instanceof CryptoRepoFile)
			cryptoRepoFilePersisted = true;
		else if (persistable instanceof HistoCryptoRepoFile)
			histoCryptoRepoFilePersisted = true;
	}

	@Override
	public void onCommit() {
		if (repoFileName2RepoFile.isEmpty() && ! cryptoRepoFilePersisted && ! histoCryptoRepoFilePersisted)
			return;

		final LocalRepoTransaction tx = getTransactionOrFail();
		final CryptoRepoFileDao cryptoRepoFileDao = tx.getDao(CryptoRepoFileDao.class);
		final Collection<CryptoRepoFile> cryptoRepoFiles = cryptoRepoFileDao.getCryptoRepoFilesWithoutRepoFileAndNotDeleted();
		for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFiles) {
			final RepoFile repoFile;
			if (cryptoRepoFile.getLocalName() != null) // on client-side!
				repoFile = associateRepoFileViaCryptoRepoFileLocalName(cryptoRepoFile);
			else { // on server-side
				repoFile = repoFileName2RepoFile.get(cryptoRepoFile.getCryptoRepoFileId().toString());
				if (repoFile != null) {
					cryptoRepoFile.setRepoFile(repoFile);
					tx.flush(); // we want an early failure!
				}
			}
		}

		repoFileName2RepoFile.clear();
		cryptoRepoFilePersisted = false;
		histoCryptoRepoFilePersisted = false;
	}

	/**
	 * Associates and returns the {@link RepoFile} via {@link CryptoRepoFile#getLocalName() cryptoRepoFile.localName}.
	 * <p>
	 * This method only works and should thus only be executed on the client-side!
	 * @param cryptoRepoFile the {@link CryptoRepoFile} for which to look up the {@link RepoFile}. Must not be <code>null</code>.
	 * @return the {@link RepoFile} associated with the given {@code cryptoRepoFile}. May be <code>null</code>.
	 */
	private RepoFile associateRepoFileViaCryptoRepoFileLocalName(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");

//		if (cryptoRepoFile.getDeleted() != null) { // Yes we MUST associate, because we otherwise don't have a unique parent-child-localName-relation anymore! See CryptreeNode.getCryptoRepoFile() and CryptoRepoFileDao.getChildCryptoRepoFile(CryptoRepoFile parent, String localName)
//			logger.info("associateRepoFileViaCryptoRepoFileLocalName: NOT associating deleted cryptoRepoFile! {}", cryptoRepoFile);
//			return null;
//		}

		RepoFile repoFile = cryptoRepoFile.getRepoFile();
		if (repoFile == null) {
			final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
			final LocalRepoTransaction tx = getTransactionOrFail();
			if (parentCryptoRepoFile != null) {
				// Please note: cryptoRepoFile.localName is null, if the user has no read-access to it.
				// We currently synchronise all CryptoRepoFile instances of the entire server repository,
				// even if the client checked-out a sub-directory only (and is allowed to read only this sub-dir).
				final String localName = cryptoRepoFile.getLocalName();
				if (localName != null) {
					final RepoFile parentRepoFile = associateRepoFileViaCryptoRepoFileLocalName(parentCryptoRepoFile);
//					assertNotNull("parentRepoFile", parentRepoFile); // IMHO, parents should always be readable + available, if the child is readable. TODO check why this is not the case. my test just failed because of this :-(
					if (parentRepoFile == null)
						return null;

					repoFile = tx.getDao(RepoFileDao.class).getChildRepoFile(parentRepoFile, localName);
				}
			}
			if (repoFile != null) {
				DuplicateCryptoRepoFileHandler.createInstance(tx).associateCryptoRepoFileWithRepoFile(repoFile, cryptoRepoFile);
				tx.flush(); // we want an early failure!
			}
		}
		return repoFile;
	}

//	private void associateCryptoRepoFileWithRepoFile(final RepoFile repoFile, CryptoRepoFile cryptoRepoFile) {
//		assertNotNull("repoFile", repoFile);
//		assertNotNull("cryptoRepoFile", cryptoRepoFile);
//
//		final LocalRepoTransaction tx = getTransactionOrFail();
//		final CryptoRepoFile cryptoRepoFile2 = tx.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(repoFile);
//		if (cryptoRepoFile2 != null && !cryptoRepoFile2.equals(cryptoRepoFile))
//			cryptoRepoFile = handleCollision(cryptoRepoFile, cryptoRepoFile2);
//
//		cryptoRepoFile.setRepoFile(repoFile);
//	}
//
//	private CryptoRepoFile handleCollision(CryptoRepoFile cryptoRepoFile, CryptoRepoFile cryptoRepoFile2) {
//
//
//		return null;
//	}
//
//	protected Cryptree getCryptree() {
//		if (cryptree == null) {
//			final LocalRepoTransaction tx = getTransactionOrFail();
//			final UUID localRepositoryId = tx.getLocalRepoManager().getRepositoryId();
//
//			final RemoteRepositoryDao remoteRepositoryDao = tx.getDao(RemoteRepositoryDao.class);
//			final Map<UUID, URL> remoteRepositoryId2RemoteRootMap = remoteRepositoryDao.getRemoteRepositoryId2RemoteRootMap();
//			if (remoteRepositoryId2RemoteRootMap.size() != 1)
//				throw new IllegalStateException("Not exactly one remote repository!");
//
//			final UUID remoteRepositoryId = remoteRepositoryId2RemoteRootMap.keySet().iterator().next();
//			final SsRemoteRepository remoteRepository = (SsRemoteRepository) remoteRepositoryDao.getRemoteRepositoryOrFail(remoteRepositoryId);
//			final String remotePathPrefix = remoteRepository.getRemotePathPrefix();
//			assertNotNull("remoteRepository.remotePathPrefix", remotePathPrefix);
//
//			final UserRepoKeyRing userRepoKeyRing = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup().getUserRepoKeyRing(
//					new UserRepoKeyRingLookupContext(localRepositoryId, remoteRepositoryId));
//
//			cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
//					tx, remoteRepositoryId, remotePathPrefix, userRepoKeyRing);
//		}
//		return cryptree;
//	}
}
