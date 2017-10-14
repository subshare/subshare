package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@SuppressWarnings("serial") // used for LocalServer-communication, only - and they (LocalServer-server & -client) always use the very same JARs.
@XmlRootElement
public class DeletedCollisionDto implements Signable, Serializable {
	public static final String SIGNED_DATA_TYPE = "DeletedCollision";

	private Uid collisionId;

	@XmlElement
	private SignatureDto signatureDto;

	public DeletedCollisionDto() {
	}

	public Uid getCollisionId() {
		return collisionId;
	}
	public void setCollisionId(Uid collisionId) {
		this.collisionId = collisionId;
	}

	@Override
	public String getSignedDataType() {
		return SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code DeletedCollision} must exactly match the one in {@code DeletedCollisionDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
//			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(collisionId)
					);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	@XmlTransient
	@Override
	public Signature getSignature() {
		return signatureDto;
	}
	@Override
	public void setSignature(final Signature signature) {
		this.signatureDto = SignatureDto.copyIfNeeded(signature);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "collisionId=" + collisionId;
	}
}
