package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;

public class BcPgpKey {

	private final PgpKeyId pgpKeyId;

	private PGPPublicKeyRing publicKeyRing;

	private PGPSecretKeyRing secretKeyRing;

	private PGPPublicKey publicKey;

	private PGPSecretKey secretKey;

	private final List<BcPgpKey> subKeys = new ArrayList<BcPgpKey>();

	private PgpKey pgpKey;

	public BcPgpKey(final PgpKeyId pgpKeyId) {
		this.pgpKeyId = assertNotNull("pgpKeyId", pgpKeyId);
	}

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}

	public PGPPublicKeyRing getPublicKeyRing() {
		return publicKeyRing;
	}
	public void setPublicKeyRing(PGPPublicKeyRing publicKeyRing) {
		this.publicKeyRing = publicKeyRing;
	}

	public PGPSecretKeyRing getSecretKeyRing() {
		return secretKeyRing;
	}
	public void setSecretKeyRing(PGPSecretKeyRing secretKeyRing) {
		this.secretKeyRing = secretKeyRing;
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
