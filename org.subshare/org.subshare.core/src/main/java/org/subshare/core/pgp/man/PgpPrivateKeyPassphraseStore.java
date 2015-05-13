package org.subshare.core.pgp.man;

import java.util.Set;

import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKeyId;

public interface PgpPrivateKeyPassphraseStore {

	PgpAuthenticationCallback getPgpAuthenticationCallback();

	boolean hasPassphrase(PgpKeyId pgpKeyId);

	void putPassphrase(PgpKeyId pgpKeyId, char[] passphrase);

	Set<PgpKeyId> getPgpKeyIdsHavingPassphrase();
}
