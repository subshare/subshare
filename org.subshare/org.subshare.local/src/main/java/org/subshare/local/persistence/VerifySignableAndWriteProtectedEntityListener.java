package org.subshare.local.persistence;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import org.subshare.core.Cryptree;
import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.local.CryptreeImpl;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;

public class VerifySignableAndWriteProtectedEntityListener extends AbstractLocalRepoTransactionListener implements StoreLifecycleListener {

	private final Set<Signable> signables = new HashSet<Signable>();
	private SignableVerifier signableVerifier;
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
		final Signable signable = (Signable) event.getPersistentInstance();
		signables.add(signable);
	}

	@Override
	public void onCommit() {
		while (!signables.isEmpty()) {
			final Iterator<Signable> iterator = signables.iterator();
			final Signable signable = iterator.next();
			iterator.remove();
			assertSignableOk(signable);
		}
	}

	private void assertSignableOk(final Signable signable) {
		// Signatures are optional - e.g. a SsRepoFile does not have a signature on the client-side (only on the server-side).
		// Whether it's required for the entity in question is decided in the entity itself - either by the annotation
		// @Persistent(nullValue=NullValue.EXCEPTION) or by code (e.g. a store-callback).
		// But *if* there is a signature, it *must* be correct!
		if (signable.getSignature() == null) {
			if (LocalRepositoryType.SERVER == getLocalRepositoryType())
				throw new SignatureException(signable.toString() + ": Missing signature! On the server, every Signable must be signed!");
		}
		else {
			final Cryptree cryptree = getTransactionOrFail().getContextObject(Cryptree.class);
			try {

				if (cryptree != null && signable instanceof WriteProtectedEntity)
					((CryptreeImpl) cryptree).assertSignatureOk((WriteProtectedEntity) signable);
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

	public LocalRepositoryType getLocalRepositoryType() {
		if (localRepositoryType == null) {
			final LocalRepositoryDao localRepositoryDao = getTransactionOrFail().getDao(LocalRepositoryDao.class);
			final LocalRepository localRepository = localRepositoryDao.getLocalRepositoryOrFail();
			localRepositoryType = ((SsLocalRepository) localRepository).getLocalRepositoryType();
		}
		return localRepositoryType;
	}
}
