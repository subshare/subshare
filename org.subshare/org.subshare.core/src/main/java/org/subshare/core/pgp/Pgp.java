package org.subshare.core.pgp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

public interface Pgp {

	int getPriority();

	boolean isSupported();

	Collection<PgpKey> getMasterKeys();

	PgpKey getPgpKey(PgpKeyId pgpKeyId);

	PgpEncoder createEncoder(InputStream in, OutputStream out);

	PgpDecoder createDecoder(InputStream in, OutputStream out);

	Collection<PgpSignature> getSignatures(PgpKey pgpKey);

	Collection<PgpKey> getMasterKeysWithPrivateKey();

	boolean isTrusted(PgpKey pgpKey);

	PgpKeyTrustLevel getKeyTrustLevel(PgpKey pgpKey);

	void exportPublicKeys(Set<PgpKey> pgpKeys, OutputStream out);

	void exportPublicKeysWithPrivateKeys(Set<PgpKey> pgpKeys, OutputStream out);

	void importKeys(InputStream in);
}
