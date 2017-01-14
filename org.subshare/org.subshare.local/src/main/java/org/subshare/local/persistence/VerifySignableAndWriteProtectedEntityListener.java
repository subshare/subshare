package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import org.subshare.core.Cryptree;
import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.core.sign.WriteProtected;
import org.subshare.local.CryptreeImpl;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class VerifySignableAndWriteProtectedEntityListener extends AbstractLocalRepoTransactionListener implements StoreLifecycleListener {

	private final Set<Signable> signables = new HashSet<Signable>();
	private SignableVerifier signableVerifier;
	private LocalRepository localRepository;
	private LocalRepositoryType localRepositoryType;

	@Override
	public int getPriority() {
		// The lower the priority, the later this listener is triggered. The default priority is 0. We want
		// to make sure, the AssignCryptoRepoFileRepoFileListener is triggered *first*, before this here.
		return -100;
	}

	@Override
	public void onBegin() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PersistenceManager pm = ((ContextWithPersistenceManager)tx).getPersistenceManager();
		pm.addInstanceLifecycleListener(this, Signable.class);
	}

	@Override
	public void preStore(final InstanceLifecycleEvent event) { }

	@Override
	public void postStore(final InstanceLifecycleEvent event) {
		final Object persistentInstance = event.getPersistentInstance();
		final Signable signable = (Signable) persistentInstance;
		signables.add(signable);
	}

	/**
	 * Remove the given {@code signable} from the entities that are about to be checked.
	 * <p>
	 * <b>Important:</b> There is a good reason why this check is done generically: It must usually always be done!
	 * Only during bootstrapping a new repository, i.e. more precisely when inviting a new user, we need to skip
	 * this check, because our data is initially incomplete and we thus simply cannot verify.
	 * @param signable the entity that was written in the current transaction, but should <b>not</b> be verified.
	 */
	public void removeSignable(Signable signable) {
		assertNotNull(signable, "signable");
		signables.remove(signable);
	}

	@Override
	public void onCommit() {
		assertAllRepoFilesAreSignedOnServer();

		while (!signables.isEmpty()) {
			final Iterator<Signable> iterator = signables.iterator();
			final Signable signable = iterator.next();
			iterator.remove();
			assertSignableOk(signable);
		}
	}

	private void assertAllRepoFilesAreSignedOnServer() {
		if (LocalRepositoryType.SERVER != getLocalRepositoryType())
			return;

		final SsLocalRepository localRepository = (SsLocalRepository) getLocalRepository();
		if (localRepository.isAssertedAllRepoFilesAreSigned())
			return;

		// On the server, the root-Directory (RepoFile) cannot be signed when it is initially created.
		// That happens without a LocalRepoTransaction and therefore, this listener is not triggered for
		// this initial directory. However, it is required to be signed, too, when the client uploads
		// for the very first time. Therefore, as soon as we know that we are on the server and as soon
		// as there are at least 2 RepoFile instances, we must make sure that all RepoFile instances
		// are signed.

		final LocalRepoTransaction tx = getTransactionOrFail();
		final RepoFileDao repoFileDao = tx.getDao(RepoFileDao.class);
		if (repoFileDao.getObjectsCount() < 2)
			return;

		for (final RepoFile rf : repoFileDao.getObjects()) {
			final SsRepoFile ssrf = (SsRepoFile) rf;
			if (ssrf.getSignature() == null)
				throw new IllegalStateException("It seems, the root still has no signature! The root should be signed (i.e. uploaded *with* signature), before any other directory/file is uploaded to the server! repoFileMissingSignature: " + rf);
		}

		localRepository.setAssertedAllRepoFilesAreSigned(true);
	}

	private void assertSignableOk(final Signable signable) {
		// Signatures are not always required: SsRepoFiles do not have signatures on the client-side. Whether a
		// signature is required for the entity in question is usually decided by the entity, i.e. by the annotation
		// @Persistent(nullValue=NullValue.EXCEPTION).
		//
		// However, on the server, *every* Signable must be signed!
		//
		// And in all cases: *If* there is a signature, it *must* be correct!

		if (JDOHelper.isDeleted(signable))
			return; // skip deleted objects!

		if (signable.getSignature() == null) {
			if (LocalRepositoryType.SERVER == getLocalRepositoryType())
				throw new SignatureException(signable.toString() + ": Missing signature! On the server, every Signable must be signed!");
		}
		else {
			final Cryptree cryptree = getTransactionOrFail().getContextObject(Cryptree.class);
			try {

				if (cryptree != null && signable instanceof WriteProtected)
					((CryptreeImpl) cryptree).assertSignatureOk((WriteProtected) signable);
				else
					getSignableVerifier().verify(signable);

			} catch (final SignatureException x) {
				throw new SignatureException(signable.toString() + ": " + x.getMessage(), x);
			} catch (final GrantAccessDeniedException x) {
				throw new GrantAccessDeniedException(signable.toString() + ": " + x.getMessage(), x);
			} catch (final ReadAccessDeniedException x) {
				throw new ReadAccessDeniedException(signable.toString() + ": " + x.getMessage(), x);
			} catch (final WriteAccessDeniedException x) {
				throw new WriteAccessDeniedException(signable.toString() + ": " + x.getMessage(), x);
			}
		}
	}

	private SignableVerifier getSignableVerifier() {
		if (signableVerifier == null)
			signableVerifier = new SignableVerifier(new UserRepoKeyPublicKeyLookupImpl(getTransactionOrFail()));

		return signableVerifier;
	}

	private LocalRepository getLocalRepository() {
		if (localRepository == null) {
			final LocalRepositoryDao localRepositoryDao = getTransactionOrFail().getDao(LocalRepositoryDao.class);
			localRepository = localRepositoryDao.getLocalRepositoryOrFail();
		}
		return localRepository;
	}

	private LocalRepositoryType getLocalRepositoryType() {
		if (localRepositoryType == null)
			localRepositoryType = ((SsLocalRepository) getLocalRepository()).getLocalRepositoryType();

		return localRepositoryType;
	}
}
