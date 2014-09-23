package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.subshare.core.dto.SignatureDto;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.junit.Test;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.local.LocalRepoTransactionImpl;
import co.codewizards.cloudstore.rest.client.RemoteException;

public class BrokenSignatureIT extends AbstractRepoToRepoSyncIT {

	@Test
	public void uploadBrokenSignature() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();

		final int caseRandom = random.nextInt(300);

		if (caseRandom < 100)
			breakRandomCryptoKeySignature();
		else if (caseRandom < 200)
			breakRandomCryptoLinkSignature();
		else
			breakRandomCryptoRepoFileSignature();

		// TODO (probably here) break signature in random uploaded SsRepoFileDto
		// TODO (probably here) break signature in uploaded file data

		try {
			syncFromLocalSrcToRemote();
			fail("The broken signature was not detected by the server!");
		} catch (final SignatureException x) {
			final RemoteException remoteException = ExceptionUtil.getCause(x, RemoteException.class);
			if (remoteException == null)
				fail("The broken signature was detected by the client; it should have been sent to the server and detected there! We have to change our code or this test!");
		}
	}

	//@Test // TODO implement this!
	public void downloadBrokenSignature() throws Exception {

	}

	private void breakRandomCryptoRepoFileSignature() {
		final PersistenceManager pm = getTransactionalPersistenceManager(localSrcRoot);
		try {
			final CryptoRepoFileDao cryptoRepoFileDao = new CryptoRepoFileDao().persistenceManager(pm);
			final List<CryptoRepoFile> cryptoRepoFiles = (List<CryptoRepoFile>) cryptoRepoFileDao.getObjects(); // we know that it is a list ;-)
			Collections.shuffle(cryptoRepoFiles);
			final int index = random.nextInt(cryptoRepoFiles.size());
			final CryptoRepoFile cryptoRepoFile1 = cryptoRepoFiles.get(index);
			CryptoRepoFile cryptoRepoFile2 = null;
			for (final CryptoRepoFile crf : cryptoRepoFiles) {
				if (cryptoRepoFile1 != crf) {
					cryptoRepoFile2 = crf;
					break;
				}
			}

			assertThat(cryptoRepoFile2).isNotNull();
			cryptoRepoFile1.setSignature(SignatureDto.copyIfNeeded(cryptoRepoFile2.getSignature()));
			cryptoRepoFile1.setLocalRevision(Long.MAX_VALUE);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();
		}
	}

	private void breakRandomCryptoLinkSignature() {
		final PersistenceManager pm = getTransactionalPersistenceManager(localSrcRoot);
		try {
			final CryptoLinkDao cryptoLinkDao = new CryptoLinkDao().persistenceManager(pm);
			final List<CryptoLink> cryptoLinks = (List<CryptoLink>) cryptoLinkDao.getObjects(); // we know that it is a list ;-)
			Collections.shuffle(cryptoLinks);
			final int index = random.nextInt(cryptoLinks.size());
			final CryptoLink cryptoLink1 = cryptoLinks.get(index);
			CryptoLink cryptoLink2 = null;
			for (final CryptoLink cl : cryptoLinks) {
				if (cryptoLink1 != cl) {
					cryptoLink2 = cl;
					break;
				}
			}

			assertThat(cryptoLink2).isNotNull();
			cryptoLink1.setSignature(SignatureDto.copyIfNeeded(cryptoLink2.getSignature()));
			cryptoLink1.setLocalRevision(Long.MAX_VALUE);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();
		}
	}

	private void breakRandomCryptoKeySignature() {
		final PersistenceManager pm = getTransactionalPersistenceManager(localSrcRoot);
		try {
			final CryptoKeyDao cryptoKeyDao = new CryptoKeyDao().persistenceManager(pm);
			final List<CryptoKey> cryptoKeys = (List<CryptoKey>) cryptoKeyDao.getObjects(); // we know that it is a list ;-)
			Collections.shuffle(cryptoKeys);
			final int index = random.nextInt(cryptoKeys.size());
			final CryptoKey cryptoKey1 = cryptoKeys.get(index);
			CryptoKey cryptoKey2 = null;
			for (final CryptoKey ck : cryptoKeys) {
				if (cryptoKey1 != ck
						&& cryptoKey1.getCryptoKeyRole().equals(ck.getCryptoKeyRole())
						&& cryptoKey1.getCryptoKeyType().equals(ck.getCryptoKeyType())) {
					cryptoKey2 = ck;
					break;
				}
			}

			assertThat(cryptoKey2).isNotNull();
			cryptoKey1.setSignature(SignatureDto.copyIfNeeded(cryptoKey2.getSignature()));
			cryptoKey1.setLocalRevision(Long.MAX_VALUE);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();
		}
	}

	private PersistenceManager getTransactionalPersistenceManager(final File localRoot) {
		final PersistenceManagerFactory pmf;
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				pmf = ((LocalRepoTransactionImpl)transaction).getPersistenceManager().getPersistenceManagerFactory();
			}
		}
		final PersistenceManager pm = pmf.getPersistenceManager();
		pm.currentTransaction().begin();
		return pm;
	}
}
