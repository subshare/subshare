package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.List;

public class PgpKey {

	public static final PgpKeyId TEST_DUMMY_PGP_KEY_ID = new PgpKeyId(0);

	public static final PgpKey TEST_DUMMY_PGP_KEY = new PgpKey();
	static {
		TEST_DUMMY_PGP_KEY.setPrivateKeyAvailable(true);
	}

	private PgpKeyId pgpKeyId;

	private byte[] fingerprint;

	private boolean privateKeyAvailable;

	private final List<String> userIds = new ArrayList<String>(1);

	private final List<PgpKey> subKeys = new ArrayList<PgpKey>(1);

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}
	public void setPgpKeyId(final PgpKeyId pgpKeyId) {
		this.pgpKeyId = pgpKeyId;
	}

	public byte[] getFingerprint() {
		return fingerprint;
	}
	public void setFingerprint(final byte[] fingerprint) {
		this.fingerprint = fingerprint;
	}

	public boolean isPrivateKeyAvailable() {
		return privateKeyAvailable;
	}
	public void setPrivateKeyAvailable(final boolean privateKeyAvailable) {
		this.privateKeyAvailable = privateKeyAvailable;
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
