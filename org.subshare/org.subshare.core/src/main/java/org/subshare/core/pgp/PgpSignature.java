package org.subshare.core.pgp;

import java.util.Date;

public class PgpSignature {

	private long pgpKeyId;
	private Date created;
	private PgpSignatureType signatureType;

	public long getPgpKeyId() {
		return pgpKeyId;
	}
	public void setPgpKeyId(final long pgpKeyId) {
		this.pgpKeyId = pgpKeyId;
	}

	public Date getCreated() {
		return created;
	}
	public void setCreated(final Date created) {
		this.created = created;
	}

	public PgpSignatureType getSignatureType() {
		return signatureType;
	}
	public void setSignatureType(final PgpSignatureType signatureType) {
		this.signatureType = signatureType;
	}
}
