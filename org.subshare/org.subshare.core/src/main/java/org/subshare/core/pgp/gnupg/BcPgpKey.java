package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

	// A sub-key may be added twice, because we enlist from both the secret *and* public key ring
	// collection. Therefore, we now use a LinkedHashSet (instead of an ArrayList).
	private Set<BcPgpKey> subKeys = new LinkedHashSet<BcPgpKey>();

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

	public Set<BcPgpKey> getSubKeys() {
		return subKeys;
	}

	public PgpKey getPgpKey() {
		if (pgpKey == null) {
			final byte[] fingerprint = assertNotNull("publicKey", publicKey).getFingerprint();
			final boolean privateKeyAvailable = secretKey != null && ! secretKey.isPrivateKeyEmpty();

			final List<String> userIds = new ArrayList<String>();
			for (final Iterator<?> itUserIDs = publicKey.getUserIDs(); itUserIDs.hasNext(); )
				userIds.add((String) itUserIDs.next());

			this.subKeys = Collections.unmodifiableSet(new LinkedHashSet<>(this.subKeys)); // turn read-only
			final List<PgpKey> subKeys = new ArrayList<PgpKey>(this.subKeys.size());
			for (final BcPgpKey bcPgpKey : this.subKeys)
				subKeys.add(bcPgpKey.getPgpKey());

			final long validSeconds = publicKey.getValidSeconds();
			final Date created = publicKey.getCreationTime();
			final Date validTo = validSeconds < 1 ? null : new Date(created.getTime() + (validSeconds * 1000));
			this.pgpKey = new PgpKey(pgpKeyId, fingerprint, created, validTo, privateKeyAvailable, userIds, subKeys);
		}
		return pgpKey;
	}

}
