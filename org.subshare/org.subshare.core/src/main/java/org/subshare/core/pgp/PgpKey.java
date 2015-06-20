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
			final boolean privateKeyAvailable,
			final List<String> userIds,
			final Set<PgpKeyFlag> pgpKeyFlags,
			final boolean revoked) {
		this.pgpKeyId = assertNotNull("pgpKeyId", pgpKeyId);
		this.fingerprint = assertNotNull("fingerprint", fingerprint);
		this.masterKey = masterKey == null ? this : masterKey;
		this.created = assertNotNull("created", created);
		this.validTo = validTo; // may be null - null means, it does *not* expire.
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

	public PgpKey getPgpKeyForEncryptionOrFail() {
		final PgpKey mk = getMasterKey();
		final Date now = new Date();

		PgpKey result = null;
		for (final PgpKey subKey : mk.getSubKeys()) {
			if (subKey.getPgpKeyFlags().contains(CAN_ENCRYPT_STORAGE) && ! subKey.isRevoked() && subKey.isValid(now))
				result = subKey;
		}

		if (result == null) {
			for (final PgpKey subKey : mk.getSubKeys()) {
				if (subKey.getPgpKeyFlags().contains(CAN_ENCRYPT_COMMS) && ! subKey.isRevoked() && subKey.isValid(now))
					result = subKey;
			}
		}

		if (result == null) {
			if (mk.isRevoked())
				throw new IllegalStateException(String.format("The master-key %s was revoked and thus cannot be used for encryption!", mk.getPgpKeyId()));

			if (! mk.isValid(now))
				throw new IllegalStateException(String.format("The master-key %s is not valid (it expired) and thus cannot be used for encryption!", mk.getPgpKeyId()));

			if (!mk.getPgpKeyFlags().contains(CAN_ENCRYPT_STORAGE) && !mk.getPgpKeyFlags().contains(CAN_ENCRYPT_COMMS))
				throw new IllegalStateException(String.format("Neither any sub-key nor the master-key %s are suitable for encryption!", mk.getPgpKeyId()));

			result = mk;
		}

		return result;
	}

	public PgpKey getPgpKeyForSignatureOrFail() {
		final PgpKey mk = getMasterKey();
		final Date now = new Date();

		PgpKey result = null;
		for (final PgpKey subKey : mk.getSubKeys()) {
			if (! mk.getPgpKeyFlags().contains(CAN_SIGN) && ! subKey.isRevoked() && subKey.isValid(now))
				result = subKey;
		}

		if (result == null) {
			// if it can certify, it technically can sign, too, hence we use it rather than throwing an exception.
			for (final PgpKey subKey : mk.getSubKeys()) {
				if (! mk.getPgpKeyFlags().contains(CAN_CERTIFY) && ! subKey.isRevoked() && subKey.isValid(now))
					result = subKey;
			}
		}

		if (result == null) {
			if (mk.isRevoked())
				throw new IllegalStateException(String.format("The master-key %s was revoked and thus cannot be used for signing!", mk.getPgpKeyId()));

			if (! mk.isValid(now))
				throw new IllegalStateException(String.format("The master-key %s is not valid (it expired) and thus cannot be used for signing!", mk.getPgpKeyId()));

			// The master-key *must* technically be able to sign - otherwise sub-keys would be impossible. hence we can use it without any further check.
			// ... maybe we still check to make sure, everything is consistent and correct.
			if (!mk.getPgpKeyFlags().contains(CAN_SIGN) && !mk.getPgpKeyFlags().contains(CAN_CERTIFY))
				throw new IllegalStateException(String.format("Neither any sub-key nor the master-key %s are suitable for signing!", mk.getPgpKeyId()));

			result = mk;
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
