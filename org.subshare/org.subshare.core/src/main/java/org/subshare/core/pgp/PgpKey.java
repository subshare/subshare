package org.subshare.core.pgp;

import java.util.ArrayList;
import java.util.List;

public class PgpKey {

	private long pgpKeyId;

	private byte[] fingerprint;

	private boolean privateKeyAvailable;

	private final List<String> userIds = new ArrayList<String>(1);

	private final List<PgpKey> subKeys = new ArrayList<PgpKey>(1);

	private final List<Long> signaturePgpKeyIds = new ArrayList<Long>(3);

	public long getPgpKeyId() {
		return pgpKeyId;
	}
	public void setPgpKeyId(final long pgpKeyId) {
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

	public List<Long> getSignaturePgpKeyIds() {
		return signaturePgpKeyIds;
	}

}
