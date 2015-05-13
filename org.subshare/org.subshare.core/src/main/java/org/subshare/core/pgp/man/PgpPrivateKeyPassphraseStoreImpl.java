package org.subshare.core.pgp.man;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;

/**
 * In-memory store holding the passwords for the private OpenPGP keys.
 * <p>
 * TODO even though only the current user is able to access the LocalServer, we might still try to make
 * this a one-way street, i.e. only putting passphrases via the Ls-protocoal should be possible, reading
 * them should be impossible. Low priority, though, because the current user probably can
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class PgpPrivateKeyPassphraseStoreImpl implements PgpPrivateKeyPassphraseStore {

	private final Map<PgpKeyId, char[]> pgpKeyId2Passphrase = new HashMap<>();

	private final PgpAuthenticationCallback pgpAuthenticationCallback = new PgpAuthenticationCallback() {
		@Override
		public char[] getPassphrase(final PgpKey pgpKey) {
			final PgpKeyId pgpKeyId = assertNotNull("pgpKey", pgpKey).getPgpKeyId();
			assertNotNull("pgpKey.pgpKeyId", pgpKeyId);
			return PgpPrivateKeyPassphraseStoreImpl.this.getPassphrase(pgpKeyId);
		}
	};

	private static final class Holder {
		public static final PgpPrivateKeyPassphraseStoreImpl instance = new PgpPrivateKeyPassphraseStoreImpl();
	}

	private PgpPrivateKeyPassphraseStoreImpl() {
	}

	public static PgpPrivateKeyPassphraseStore getInstance() {
		return Holder.instance;
	}

	@Override
	public PgpAuthenticationCallback getPgpAuthenticationCallback() {
		return pgpAuthenticationCallback;
	}

	protected synchronized char[] getPassphrase(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
		final char[] passphrase = pgpKeyId2Passphrase.get(pgpKeyId);
		return passphrase;
	}

	@Override
	public synchronized boolean hasPassphrase(final PgpKeyId pgpKeyId) {
		return pgpKeyId2Passphrase.containsKey(pgpKeyId);
	}

	@Override
	public synchronized void putPassphrase(final PgpKeyId pgpKeyId, final char[] passphrase) {
		assertNotNull("pgpKeyId", pgpKeyId);
		assertNotNull("passphrase", passphrase);
		pgpKeyId2Passphrase.put(pgpKeyId, passphrase);
	}

	@Override
	public synchronized Set<PgpKeyId> getPgpKeyIdsHavingPassphrase() {
		return Collections.unmodifiableSet(new HashSet<PgpKeyId>(pgpKeyId2Passphrase.keySet()));
	}
}
