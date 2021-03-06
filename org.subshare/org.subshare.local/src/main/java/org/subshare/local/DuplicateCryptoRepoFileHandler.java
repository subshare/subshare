package org.subshare.local;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;
import org.subshare.local.persistence.Collision;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.HistoCryptoRepoFile;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;
import org.subshare.local.persistence.ScheduledReuploadDao;
import org.subshare.local.persistence.SsRemoteRepository;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class DuplicateCryptoRepoFileHandler {

	private static final Logger logger = LoggerFactory.getLogger(DuplicateCryptoRepoFileHandler.class);

	private final LocalRepoTransaction transaction;
	private final Cryptree cryptree;
	private CryptoRepoFile cryptoRepoFileActive;
	private CryptoRepoFile cryptoRepoFileDead;

	public static DuplicateCryptoRepoFileHandler createInstance(final LocalRepoTransaction transaction) {
		requireNonNull(transaction, "transaction");
		return createObject(DuplicateCryptoRepoFileHandler.class, transaction);
	}

	protected DuplicateCryptoRepoFileHandler(LocalRepoTransaction transaction) {
		this.transaction = requireNonNull(transaction, "transaction");
		cryptree = _getCryptree();
	}

	public LocalRepoTransaction getTransaction() {
		return transaction;
	}

	public void associateCryptoRepoFileWithRepoFile(final RepoFile repoFile, CryptoRepoFile cryptoRepoFile) {
		requireNonNull(repoFile, "repoFile");
		requireNonNull(cryptoRepoFile, "cryptoRepoFile");

		final CryptoRepoFile cryptoRepoFile2 = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(repoFile);
		if (cryptoRepoFile2 != null && ! cryptoRepoFile2.equals(cryptoRepoFile)) {
//			if (cryptoRepoFile2.getDeleted() != null) { // TODO is this really correct?! No collision after deletion? Simply switch to new CryptoRepoFile?
//				logger.warn("associateCryptoRepoFileWithRepoFile: RepoFile is currently associated with another CryptoRepoFile, which is already marked as deleted. Dissociating it without collision. {}, {}, {}",
//						repoFile, cryptoRepoFile, cryptoRepoFile2);
//				cryptoRepoFile2.setRepoFile(null);
//				transaction.flush();
//			}
//			else
			cryptoRepoFile = deduplicate(cryptoRepoFile, cryptoRepoFile2);
		}

		if (cryptoRepoFile != null)
			cryptoRepoFile.setRepoFile(repoFile);
	}

	protected CryptoRepoFile deduplicate(final CryptoRepoFile cryptoRepoFile1, final CryptoRepoFile cryptoRepoFile2) {
		assignCryptoRepoFiles(cryptoRepoFile1, cryptoRepoFile2);
		final CryptoRepoFile result = deduplicate();
		return result;
	}

	protected void assignCryptoRepoFiles(final CryptoRepoFile cryptoRepoFile1, final CryptoRepoFile cryptoRepoFile2) {
		requireNonNull(cryptoRepoFile1, "cryptoRepoFile1");
		requireNonNull(cryptoRepoFile2, "cryptoRepoFile2");

		assignCryptoRepoFileCreatedIfNeeded(cryptoRepoFile1);
		assignCryptoRepoFileCreatedIfNeeded(cryptoRepoFile2);

		// we keep the older one - and delete the newer one (we abort (if currently ongoing) and re-start (always) its upload)
		final int compared = compare(cryptoRepoFile1, cryptoRepoFile2);
		if (compared < 0) {
			// file1 was created before file2 (i.e. file1 is older than file2)
			cryptoRepoFileActive = cryptoRepoFile1;
			cryptoRepoFileDead = cryptoRepoFile2;
		}
		else if (compared > 0) {
			// file1 was created after file2 (i.e. file1 is newer than file2)
			cryptoRepoFileActive = cryptoRepoFile2;
			cryptoRepoFileDead = cryptoRepoFile1;
		}
		else
			throw new IllegalArgumentException("cryptoRepoFile1 == cryptoRepoFile2");

//		// If the older one was already deleted and the newer one was not, then we kill the deleted one.
//		if (cryptoRepoFileActive.getDeleted() != null && cryptoRepoFileDead.getDeleted() == null) {
//			final CryptoRepoFile tmp = cryptoRepoFileActive;
//			cryptoRepoFileActive = cryptoRepoFileDead;
//			cryptoRepoFileDead = tmp;
//		}
	}

	protected CryptoRepoFile deduplicate() {
		requireNonNull(cryptoRepoFileActive, "cryptoRepoFileActive");
		requireNonNull(cryptoRepoFileDead, "cryptoRepoFileDead");

		logger.debug("deduplicate: cryptoRepoFileActive={} cryptoRepoFileDead={}",
				cryptoRepoFileActive, cryptoRepoFileDead);

		final HistoCryptoRepoFileDao hcrfDao = transaction.getDao(HistoCryptoRepoFileDao.class);
		final Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = hcrfDao.getHistoCryptoRepoFiles(cryptoRepoFileActive);
		if (histoCryptoRepoFiles.isEmpty()) {
			logger.warn("deduplicate: cryptoRepoFileActive={} does not yet have any HistoCryptoRepoFile associated! Must postpone this operation!",
					cryptoRepoFileActive);
			return null;
		}

		RepoFile repoFile = cryptoRepoFileActive.getRepoFile();
		if (repoFile == null)
			repoFile = cryptoRepoFileDead.getRepoFile();

		// Touch repoFile to prevent it from being overwritten + to force additional collision when down-syncing.
		requireNonNull(repoFile, "repoFile").setLocalRevision(transaction.getLocalRevision());

		cryptoRepoFileDead.setRepoFile(null);
//		cryptoRepoFileDead.setDeleted(now()); // we really delete the instance (controlled by the collision) - no need for a deletion marker.
//		cryptree.sign(cryptoRepoFileDead); // commented out the signing, because this is not needed if we don't modify the 'deleted' property, anymore.

		transaction.flush(); // must flush before creating collision, so that queries return newly associated HistoCryptoRepoFiles!

		Collision collision = ((CryptreeImpl)cryptree).createCollisionIfNeeded(cryptoRepoFileActive, cryptoRepoFileDead, null, true);
		deduplicateFromCollisionIfNeeded(collision);

		transaction.flush(); // must flush here to make sure the association to the repoFile is nulled, before we re-associate, later (outside this method).
		return requireNonNull(cryptoRepoFileActive, "cryptoRepoFileActive");
	}

	public void deduplicateFromCollisionIfNeeded(final Collision collision) { // this method is called on client *and* server!
		requireNonNull(collision, "collision");
		if (collision.getDuplicateCryptoRepoFileId() == null)
			return; // not a duplicate-CryptoRepoFile-collision => nothing to do

		final CryptoRepoFileDao crfDao = transaction.getDao(CryptoRepoFileDao.class);
		cryptoRepoFileActive = collision.getHistoCryptoRepoFile1().getCryptoRepoFile();
		cryptoRepoFileDead = crfDao.getCryptoRepoFile(collision.getDuplicateCryptoRepoFileId());
		if (cryptoRepoFileDead == null)
			return; // already deleted => nothing to do

		logger.debug("deduplicateFromCollisionIfNeeded: cryptoRepoFileActive={} cryptoRepoFileDead={}",
				cryptoRepoFileActive, cryptoRepoFileDead);

		if (cryptree.getUserRepoKeyRing() != null) { // only if running on client! on the server, we cannot sign!
			Collection<CryptoRepoFile> children = crfDao.getChildCryptoRepoFiles(cryptoRepoFileDead);
			for (CryptoRepoFile child : children) {
				child.setParent(cryptoRepoFileActive);
				cryptree.sign(child);
			}

			RepoFile repoFile = cryptoRepoFileDead.getRepoFile();
			if (repoFile != null)
				transaction.getDao(ScheduledReuploadDao.class).scheduleReupload(repoFile);
		}

		crfDao.deletePersistent(cryptoRepoFileDead);

		cryptoRepoFileDead = null;
		transaction.flush(); // force early failure
	}

	private void assignCryptoRepoFileCreatedIfNeeded(final CryptoRepoFile cryptoRepoFile) {
		requireNonNull(cryptoRepoFile, "cryptoRepoFile");
		if (cryptoRepoFile.getCryptoRepoFileCreated() != null)
			return;

		HistoCryptoRepoFileDao hcrfDao = getTransaction().getDao(HistoCryptoRepoFileDao.class);
		Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = hcrfDao.getHistoCryptoRepoFiles(cryptoRepoFile);
		Date cryptoRepoFileCreated = null;
		for (HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFiles) {
			if (cryptoRepoFileCreated == null || cryptoRepoFileCreated.after(histoCryptoRepoFile.getSignature().getSignatureCreated()))
				cryptoRepoFileCreated = histoCryptoRepoFile.getSignature().getSignatureCreated();
		}

		requireNonNull(cryptoRepoFileCreated, "cryptoRepoFileCreated");
		cryptoRepoFile.setCryptoRepoFileCreated(cryptoRepoFileCreated);
		_getCryptree().sign(cryptoRepoFile);
	}

	private static int compare(final CryptoRepoFile cryptoRepoFile1, final CryptoRepoFile cryptoRepoFile2) {
		requireNonNull(cryptoRepoFile1, "cryptoRepoFile1");
		requireNonNull(cryptoRepoFile2, "cryptoRepoFile2");

		final Date cryptoRepoFileCreated1 = requireNonNull(
				cryptoRepoFile1.getCryptoRepoFileCreated(),
				"cryptoRepoFile1.cryptoRepoFileCreated [cryptoRepoFileId=" +cryptoRepoFile1.getCryptoRepoFileId()+ "]");

		final Date cryptoRepoFileCreated2 = requireNonNull(
				cryptoRepoFile2.getCryptoRepoFileCreated(),
				"cryptoRepoFile2.cryptoRepoFileCreated [cryptoRepoFileId=" +cryptoRepoFile2.getCryptoRepoFileId()+ "]");

		int result = cryptoRepoFileCreated1.compareTo(cryptoRepoFileCreated2);
		if (result != 0)
			return result;

		result = cryptoRepoFile1.getCryptoRepoFileId().compareTo(cryptoRepoFile2.getCryptoRepoFileId());
		return result;
	}

	private Cryptree _getCryptree() {
		Cryptree cryptree = transaction.getContextObject(Cryptree.class);
		if (cryptree == null) {
			final UUID localRepositoryId = transaction.getLocalRepoManager().getRepositoryId();

			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
			final Map<UUID, URL> remoteRepositoryId2RemoteRootMap = remoteRepositoryDao.getRemoteRepositoryId2RemoteRootMap();
			if (remoteRepositoryId2RemoteRootMap.size() != 1)
				throw new IllegalStateException("Not exactly one remote repository! size=" + remoteRepositoryId2RemoteRootMap.size());

			final UUID remoteRepositoryId = remoteRepositoryId2RemoteRootMap.keySet().iterator().next();
			final SsRemoteRepository remoteRepository = (SsRemoteRepository) remoteRepositoryDao.getRemoteRepositoryOrFail(remoteRepositoryId);
			final String remotePathPrefix = remoteRepository.getRemotePathPrefix();
			requireNonNull(remotePathPrefix, "remoteRepository.remotePathPrefix");

			final UserRepoKeyRing userRepoKeyRing = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup().getUserRepoKeyRing(
					new UserRepoKeyRingLookupContext(localRepositoryId, remoteRepositoryId));

			cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
					transaction, remoteRepositoryId, remotePathPrefix, userRepoKeyRing);
		}
		return cryptree;
	}
}
