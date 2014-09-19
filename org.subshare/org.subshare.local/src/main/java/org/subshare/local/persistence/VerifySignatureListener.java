package org.subshare.local.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import org.subshare.core.sign.Signable;
import org.subshare.core.sign.SignableVerifier;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.repo.local.AbstractLocalRepoTransactionListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.ContextWithPersistenceManager;

public class VerifySignatureListener extends AbstractLocalRepoTransactionListener implements StoreLifecycleListener {

	private final Set<Signable> signables = new HashSet<Signable>();
	private SignableVerifier signableVerifier;

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
		assertSignablesOk();
	}

	private void assertSignablesOk() {
		for (final Signable signable : signables)
			assertSignableOk(signable);

		signables.clear();
	}

	private void assertSignableOk(final Signable signable) {
		// Signatures are optional - e.g. a SsRepoFile does not have a signature on the client-side (only on the server-side).
		// Whether it's required for the entity in question is decided in the entity itself - either by the annotation
		// @Persistent(nullValue=NullValue.EXCEPTION) or by code (e.g. a store-callback).
		// But *if* there is a signature, it *must* be correct!
		if (signable.getSignature() != null) {
			try {
				getSignableVerifier().verify(signable);
			} catch (final SignatureException x) {
				throw new SignatureException(signable.toString() + ": " + x.getMessage(), x);
			}
		}
	}

	private SignableVerifier getSignableVerifier() {
		if (signableVerifier == null)
			signableVerifier = new SignableVerifier(new UserRepoKeyPublicKeyLookupImpl(getTransactionOrFail()));

		return signableVerifier;
	}
}
