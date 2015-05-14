package org.subshare.core.pgp.man;

import java.util.Set;

import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKeyId;

/**
 * Store holding passphrases to PGP private keys <i>in memory</i>.
 * <p>
 * Note: <i>AllowSubShareInvocationFilter</i> denies reading passphrases from this object.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface PgpPrivateKeyPassphraseStore {

	PgpAuthenticationCallback getPgpAuthenticationCallback();

	boolean hasPassphrase(PgpKeyId pgpKeyId);

	void putPassphrase(PgpKeyId pgpKeyId, char[] passphrase);

	Set<PgpKeyId> getPgpKeyIdsHavingPassphrase();
}
