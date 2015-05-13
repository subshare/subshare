package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PgpKey implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final PgpKeyId TEST_DUMMY_PGP_KEY_ID = new PgpKeyId(0);

	public static final PgpKey TEST_DUMMY_PGP_KEY = new PgpKey(
			TEST_DUMMY_PGP_KEY_ID,
			new byte[0],
			true,
			Collections.<String>emptyList(),
			Collections.<PgpKey>emptyList()
			);

	private final PgpKeyId pgpKeyId;

	private final byte[] fingerprint;

	private final boolean privateKeyAvailable;

	private final List<String> userIds;

	private final List<PgpKey> subKeys;

	public PgpKey(final PgpKeyId pgpKeyId, final byte[] fingerprint, final boolean privateKeyAvailable, final List<String> userIds, final List<PgpKey> subKeys) {
		this.pgpKeyId = assertNotNull("pgpKeyId", pgpKeyId);
		this.fingerprint = assertNotNull("fingerprint", fingerprint);
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
