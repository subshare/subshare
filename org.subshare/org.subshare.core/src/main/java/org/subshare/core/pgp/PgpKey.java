package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PgpKey implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final PgpKeyId TEST_DUMMY_PGP_KEY_ID = new PgpKeyId(0);

	public static final PgpKey TEST_DUMMY_PGP_KEY = new PgpKey(
			TEST_DUMMY_PGP_KEY_ID,
			new byte[0],
			new Date(), // created
			null, // validTo
			true,
			Collections.<String>emptyList(),
			Collections.<PgpKey>emptyList()
			);

	private final PgpKeyId pgpKeyId;

	private final byte[] fingerprint;

	private final Date created;

	private final Date validTo;

	private final boolean privateKeyAvailable;

	private final List<String> userIds;

	private final List<PgpKey> subKeys;

	public PgpKey(final PgpKeyId pgpKeyId, final byte[] fingerprint, final Date created, final Date validTo, final boolean privateKeyAvailable, final List<String> userIds, final List<PgpKey> subKeys) {
		this.pgpKeyId = assertNotNull("pgpKeyId", pgpKeyId);
		this.fingerprint = assertNotNull("fingerprint", fingerprint);
		this.created = assertNotNull("created", created);
		this.validTo = validTo; // may be null - null means, it does *not* expire.
		this.privateKeyAvailable = privateKeyAvailable;
		this.userIds = Collections.unmodifiableList(new ArrayList<String>(assertNotNull("userIds", userIds)));
		this.subKeys = Collections.unmodifiableList(new ArrayList<PgpKey>(assertNotNull("subKeys", subKeys)));
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

	public boolean isPrivateKeyAvailable() {
		return privateKeyAvailable;
	}

	public List<String> getUserIds() {
		return userIds;
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
}
