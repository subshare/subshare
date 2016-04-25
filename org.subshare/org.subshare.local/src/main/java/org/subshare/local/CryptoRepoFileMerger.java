package org.subshare.local;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.HistoCryptoRepoFile;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;
import org.subshare.local.persistence.SsRemoteRepository;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class CryptoRepoFileMerger {

	private final LocalRepoTransaction transaction;
	private final Cryptree cryptree;
	private CryptoRepoFile cryptoRepoFileActive;
	private CryptoRepoFile cryptoRepoFileDead;

	public static CryptoRepoFileMerger createInstance(final LocalRepoTransaction transaction) {
		assertNotNull("transaction", transaction);
		return createObject(CryptoRepoFileMerger.class, transaction);
	}

	protected CryptoRepoFileMerger(LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
		cryptree = getCryptree();
	}

	protected Cryptree getCryptree() {
		final UUID localRepositoryId = transaction.getLocalRepoManager().getRepositoryId();

		final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
		final Map<UUID, URL> remoteRepositoryId2RemoteRootMap = remoteRepositoryDao.getRemoteRepositoryId2RemoteRootMap();
		if (remoteRepositoryId2RemoteRootMap.size() != 1)
			throw new IllegalStateException("Not exactly one remote repository!");

		final UUID remoteRepositoryId = remoteRepositoryId2RemoteRootMap.keySet().iterator().next();
		final SsRemoteRepository remoteRepository = (SsRemoteRepository) remoteRepositoryDao.getRemoteRepositoryOrFail(remoteRepositoryId);
		final String remotePathPrefix = remoteRepository.getRemotePathPrefix();
		assertNotNull("remoteRepository.remotePathPrefix", remotePathPrefix);

		final UserRepoKeyRing userRepoKeyRing = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup().getUserRepoKeyRing(
				new UserRepoKeyRingLookupContext(localRepositoryId, remoteRepositoryId));

		final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
				transaction, remoteRepositoryId, remotePathPrefix, userRepoKeyRing);

		return cryptree;
	}

	public LocalRepoTransaction getTransaction() {
		return transaction;
	}

	public void associateCryptoRepoFileWithRepoFile(final RepoFile repoFile, CryptoRepoFile cryptoRepoFile) {
		assertNotNull("repoFile", repoFile);
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		final CryptoRepoFile cryptoRepoFile2 = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(repoFile);
		if (cryptoRepoFile2 != null && !cryptoRepoFile2.equals(cryptoRepoFile))
			cryptoRepoFile = merge(cryptoRepoFile, cryptoRepoFile2);

		cryptoRepoFile.setRepoFile(repoFile);
	}

	public CryptoRepoFile merge(final CryptoRepoFile cryptoRepoFile1, final CryptoRepoFile cryptoRepoFile2) {
		assignCryptoRepoFiles(cryptoRepoFile1, cryptoRepoFile2);
		final CryptoRepoFile result = merge();
		return result;
	}

	protected void assignCryptoRepoFiles(final CryptoRepoFile cryptoRepoFile1, final CryptoRepoFile cryptoRepoFile2) {
		assertNotNull("cryptoRepoFile1", cryptoRepoFile1);
		assertNotNull("cryptoRepoFile2", cryptoRepoFile2);

		final int compared = compare(cryptoRepoFile1, cryptoRepoFile2);
		if (compared < 0) {
			// file1 was signed before file2 (i.e. file1 is older than file2)
			cryptoRepoFileActive = cryptoRepoFile2;
			cryptoRepoFileDead = cryptoRepoFile1;
		}
		else if (compared > 0) {
			// file1 was signed before file2 (i.e. file1 is older than file2)
			cryptoRepoFileActive = cryptoRepoFile1;
			cryptoRepoFileDead = cryptoRepoFile2;
		}
		else
			throw new IllegalArgumentException("cryptoRepoFile1 == cryptoRepoFile2");
	}

	protected CryptoRepoFile merge() {
		assertNotNull("cryptoRepoFileActive", cryptoRepoFileActive);
		assertNotNull("cryptoRepoFileDead", cryptoRepoFileDead);

		Collection<CryptoRepoFile> children = transaction.getDao(CryptoRepoFileDao.class).getChildCryptoRepoFiles(cryptoRepoFileDead);
		for (CryptoRepoFile child : children) {
			child.setParent(cryptoRepoFileActive);
			cryptree.sign(child);
		}

		Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = transaction.getDao(HistoCryptoRepoFileDao.class).getHistoCryptoRepoFiles(cryptoRepoFileDead);
		for (HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFiles) {
			histoCryptoRepoFile.setCryptoRepoFile(cryptoRepoFileActive);
			cryptree.sign(histoCryptoRepoFile);
		}

		// Do I need to update a CurrentHistoCryptoRepoFile?! I don't think so...
//		// What about FileChunks? Probably not either.
		// I'm not attempting to transfer permissions, either. This is a very rare situation anyway...

		cryptoRepoFileDead.setRepoFile(null);
		cryptoRepoFileDead.setDeleted(new Date()); // TODO we should better mark this type of 'deleted' in a different way?!
		cryptree.sign(cryptoRepoFileDead);

		transaction.flush(); // must flush before creating collision, so that queries return newly associated HistoCryptoRepoFiles!

		((CryptreeImpl)cryptree).createCollisionIfNeeded(cryptoRepoFileActive, null, true);

		transaction.flush(); // must flush here to make sure the association to the repoFile is nulled, before we re-associate, later (outside this method).
		return assertNotNull("cryptoRepoFileActive", cryptoRepoFileActive);
	}

	private static int compare(final CryptoRepoFile cryptoRepoFile1, final CryptoRepoFile cryptoRepoFile2) {
		assertNotNull("cryptoRepoFile1", cryptoRepoFile1);
		assertNotNull("cryptoRepoFile2", cryptoRepoFile2);

		final Date signatureCreated1 = cryptoRepoFile1.getSignature().getSignatureCreated();
		final Date signatureCreated2 = cryptoRepoFile2.getSignature().getSignatureCreated();

		int result = signatureCreated1.compareTo(signatureCreated2);
		if (result != 0)
			return result;

		result = cryptoRepoFile1.getCryptoRepoFileId().compareTo(cryptoRepoFile2.getCryptoRepoFileId());
		return result;
	}

}
