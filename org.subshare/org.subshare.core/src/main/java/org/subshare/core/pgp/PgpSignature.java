package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.Date;

public class PgpSignature implements Serializable {
	private static final long serialVersionUID = 1L;

	private PgpKeyId pgpKeyId;
	private Date created;
	private PgpSignatureType signatureType;
	private String userId;

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}
	public void setPgpKeyId(final PgpKeyId pgpKeyId) {
		this.pgpKeyId = assertNotNull("pgpKeyId", pgpKeyId);
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

	/**
	 * Gets the user-id that was certified by this signature or <code>null</code>, if this signature is not a {@linkplain PgpSignatureType#isCertification() certification}.
	 * @return the user-id that was certified by this signature; <code>null</code>, if this signature is not a {@linkplain PgpSignatureType#isCertification() certification}.
	 * @see Pgp#getCertifications(PgpKey)
	 * @see PgpSignatureType#isCertification()
	 */
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
}
