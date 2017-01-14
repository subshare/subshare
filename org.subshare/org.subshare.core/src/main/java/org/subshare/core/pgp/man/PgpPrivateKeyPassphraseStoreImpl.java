package org.subshare.core.pgp.man;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;

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
			final PgpKey masterKey = assertNotNull(pgpKey, "pgpKey").getMasterKey();
			final PgpKeyId pgpKeyId = masterKey.getPgpKeyId();
			assertNotNull(pgpKeyId, "pgpKey.pgpKeyId");
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
		assertNotNull(pgpKeyId, "pgpKeyId");
		final char[] passphrase = pgpKeyId2Passphrase.get(pgpKeyId);
		return passphrase;
	}

	@Override
	public synchronized boolean hasPassphrase(final PgpKeyId pgpKeyId) {
		return pgpKeyId2Passphrase.containsKey(pgpKeyId);
	}

	@Override
	public void putPassphrase(final PgpKeyId pgpKeyId, final char[] passphrase) throws SecurityException {
		assertNotNull(pgpKeyId, "pgpKeyId");
		assertNotNull(passphrase, "passphrase");

		assertPassphraseValid(pgpKeyId, passphrase);

		synchronized (this) {
			pgpKeyId2Passphrase.put(pgpKeyId, passphrase);
		}
	}

	private void assertPassphraseValid(final PgpKeyId pgpKeyId, final char[] passphrase) throws SecurityException {
		assertNotNull(pgpKeyId, "pgpKeyId");
		assertNotNull(passphrase, "passphrase"); // empty for no passphrase! never null!
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
		assertNotNull(pgpKey, "pgp.getPgpKey(" + pgpKeyId + ")");
		if (! pgp.testPassphrase(pgpKey, passphrase))
			throw new SecurityException("Wrong passphrase!");
	}

	@Override
	public synchronized Set<PgpKeyId> getPgpKeyIdsHavingPassphrase() {
		return Collections.unmodifiableSet(new HashSet<PgpKeyId>(pgpKeyId2Passphrase.keySet()));
	}
}
