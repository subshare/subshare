package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.core.pgp.PgpKeyFlag.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PgpKey implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final PgpKeyId TEST_DUMMY_PGP_KEY_ID = new PgpKeyId(0);

	public static final PgpKey TEST_DUMMY_PGP_KEY = new PgpKey(
			TEST_DUMMY_PGP_KEY_ID,
			new byte[0],
			null,
			new Date(), // created
			null, // validTo
			PgpKeyAlgorithm.RSA,
			4096,
			true,
			Collections.<String>emptyList(),
			EnumSet.of(PgpKeyFlag.CAN_AUTHENTICATE, PgpKeyFlag.CAN_CERTIFY, PgpKeyFlag.CAN_SIGN, PgpKeyFlag.CAN_ENCRYPT_COMMS, PgpKeyFlag.CAN_ENCRYPT_STORAGE),
			false
			);
	static {
		TEST_DUMMY_PGP_KEY.setSubKeys(Collections.<PgpKey>emptyList());
	}

	private final PgpKeyId pgpKeyId;

	private final byte[] fingerprint;

	private final Date created;

	private final Date validTo;

	private final PgpKeyAlgorithm algorithm;

	private final int strength;

	private final boolean privateKeyAvailable;

	private final List<String> userIds;

	private final Set<PgpKeyFlag> pgpKeyFlags;

	private final boolean revoked;

	private final PgpKey masterKey;

	private List<PgpKey> subKeys;


	public PgpKey(
			final PgpKeyId pgpKeyId,
			final byte[] fingerprint,
			final PgpKey masterKey,
			final Date created,
			final Date validTo,
			final PgpKeyAlgorithm algorithm,
			final int strength,
			final boolean privateKeyAvailable,
			final List<String> userIds,
			final Set<PgpKeyFlag> pgpKeyFlags,
			final boolean revoked) {
		this.pgpKeyId = assertNotNull("pgpKeyId", pgpKeyId);
		this.fingerprint = assertNotNull("fingerprint", fingerprint);
		this.masterKey = masterKey == null ? this : masterKey;
		this.created = assertNotNull("created", created);
		this.validTo = validTo; // may be null - null means, it does *not* expire.
		this.algorithm = assertNotNull("algorithm", algorithm);
		this.strength = strength;
		this.privateKeyAvailable = privateKeyAvailable;
		this.userIds = Collections.unmodifiableList(new ArrayList<String>(assertNotNull("userIds", userIds)));

		final Set<PgpKeyFlag> tmpPgpKeyFlags = EnumSet.noneOf(PgpKeyFlag.class);
		tmpPgpKeyFlags.addAll(assertNotNull("pgpKeyFlags", pgpKeyFlags));
		this.pgpKeyFlags = Collections.unmodifiableSet(tmpPgpKeyFlags);

		this.revoked = revoked;
	}

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}

	public byte[] getFingerprint() {
		return fingerprint;
	}

	public Date getCreated() {
		return created;
	}

	/**
	 * Gets the date this PGP key expires. The exact timestamp denoted by this date is excluded. It is valid until the
	 * millisecond before this timestamp.
	 *
	 * @return the date this PGP key expires. May be <code>null</code>, which means, it never expires.
	 */
	public Date getValidTo() {
		return validTo;
	}

	public boolean isValid(Date date) {
		if (date == null)
			date = new Date();

		if (date.before(created))
			return false;

		if (validTo == null || date.before(validTo))
			return true;

		return false;
	}

	public PgpKeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public int getStrength() {
		return strength;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public boolean isPrivateKeyAvailable() {
		return privateKeyAvailable;
	}

	public List<String> getUserIds() {
		return userIds;
	}

	public Set<PgpKeyFlag> getPgpKeyFlags() {
		return pgpKeyFlags;
	}

	/**
	 * Gets the master-key of this key. If this key is already the master-key, it returns <code>this</code> instead.
	 * @return the master-key of this key or <code>this</code>, if this is already the master-key.
	 */
	public PgpKey getMasterKey() {
		return assertNotNull("masterKey", masterKey);
	}

	public void setSubKeys(List<PgpKey> subKeys) {
		if (this.subKeys != null)
			throw new IllegalStateException("this.subKeys already assigned!");

		this.subKeys = Collections.unmodifiableList(new ArrayList<PgpKey>(assertNotNull("subKeys", subKeys)));
	}

	private List<PgpKey> getSubKeysAndMasterKey() {
		final PgpKey mk = getMasterKey();
		final List<PgpKey> subKeys = mk.getSubKeys();
		final List<PgpKey> result = new ArrayList<PgpKey>(subKeys.size() + 1);
		result.addAll(subKeys);
		result.add(mk);
		return result;
	}

	public PgpKey getPgpKeyForEncryptionOrFail() {
		final Date now = new Date();
		final List<PgpKey> allKeys = getSubKeysAndMasterKey();

		int keysSupportingEncryptStorage = 0;
		int keysSupportingEncryptComms = 0;

		PgpKey result = null;
		for (final PgpKey key : allKeys) {
			if (key.getPgpKeyFlags().contains(CAN_ENCRYPT_STORAGE)) {
				++keysSupportingEncryptStorage;
				if (! key.isRevoked() && key.isValid(now))
					result = key;
			}
		}

		if (result == null) {
			for (final PgpKey key : allKeys) {
				if (key.getPgpKeyFlags().contains(CAN_ENCRYPT_COMMS)) {
					++keysSupportingEncryptComms;
					if (! key.isRevoked() && key.isValid(now))
						result = key;
				}
			}
		}

		if (result == null) {
			final PgpKey mk = getMasterKey();
			throw new IllegalStateException(String.format(
					"Neither any sub-key nor the master-key %s are suitable for encryption! There are %s keys with flag 'CAN_ENCRYPT_STORAGE' and %s keys with flag 'CAN_ENCRYPT_COMMS' (all of them revoked or expired).",
					mk.getPgpKeyId(), keysSupportingEncryptStorage, keysSupportingEncryptComms));
		}
		return result;
	}

	public PgpKey getPgpKeyForSignatureOrFail() {
		final Date now = new Date();
		final List<PgpKey> allKeys = getSubKeysAndMasterKey();

		int keysSupportingSign = 0;
		int keysSupportingCertify = 0;

		PgpKey result = null;
		for (final PgpKey key : allKeys) {
			if (key.getPgpKeyFlags().contains(CAN_SIGN)) {
				++keysSupportingSign;
				if (! key.isRevoked() && key.isValid(now))
					result = key;
			}
		}

		if (result == null) {
			// if it can certify, it technically can sign, too, hence we use it rather than throwing an exception.
			for (final PgpKey key : allKeys) {
				if (key.getPgpKeyFlags().contains(CAN_CERTIFY)) {
					++keysSupportingCertify;
					if (! key.isRevoked() && key.isValid(now))
						result = key;
				}
			}
		}

		if (result == null) {
			final PgpKey mk = getMasterKey();
			throw new IllegalStateException(String.format(
					"Neither any sub-key nor the master-key %s are suitable for signing! There are %s keys with flag 'CAN_SIGN' and %s keys with flag 'CAN_CERTIFY' (all of them revoked or expired).",
					mk.getPgpKeyId(), keysSupportingSign, keysSupportingCertify));
		}

		return result;
	}

	public List<PgpKey> getSubKeys() {
		return subKeys;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (pgpKeyId == null ? 0 : pgpKeyId.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PgpKey other = (PgpKey) obj;
		return equal(this.pgpKeyId, other.pgpKeyId);
	}

	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), pgpKeyId);
	}
}
