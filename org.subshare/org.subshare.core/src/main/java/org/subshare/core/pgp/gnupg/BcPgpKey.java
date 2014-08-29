package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.subshare.core.pgp.PgpKey;

public class BcPgpKey {

	private final long pgpKeyId;

	private PGPPublicKey publicKey;

	private PGPSecretKey secretKey;

	private final List<BcPgpKey> subKeys = new ArrayList<BcPgpKey>();

	private PgpKey pgpKey;

	public BcPgpKey(final long pgpKeyId) {
		this.pgpKeyId = pgpKeyId;
	}

	public long getPgpKeyId() {
		return pgpKeyId;
	}

	public PGPPublicKey getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(final PGPPublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public PGPSecretKey getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(final PGPSecretKey secretKey) {
		this.secretKey = secretKey;
	}

	public List<BcPgpKey> getSubKeys() {
		return subKeys;
	}

	public PgpKey getPgpKey() {
		if (pgpKey == null) {
			final PgpKey pgpKey = new PgpKey();
			pgpKey.setPgpKeyId(pgpKeyId);
			pgpKey.setFingerprint(assertNotNull("publicKey", publicKey).getFingerprint());
			pgpKey.setPrivateKeyAvailable(secretKey != null && ! secretKey.isPrivateKeyEmpty());

			for (final Iterator<?> itUserIDs = publicKey.getUserIDs(); itUserIDs.hasNext(); )
				pgpKey.getUserIds().add((String) itUserIDs.next());

			this.pgpKey = pgpKey;
		}
		return pgpKey;
	}

}
