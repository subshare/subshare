package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial") // used for LocalServer-communication, only - and they (LocalServer-server & -client) always use the very same JARs.
@XmlRootElement
public class CollisionDto implements Signable, Serializable {
	public static final String SIGNED_DATA_TYPE = "Collision";

	private Uid collisionId;

	@XmlElement
	private SignatureDto signatureDto;

	private Uid histoCryptoRepoFileId1;

	private Uid histoCryptoRepoFileId2;

	private Uid duplicateCryptoRepoFileId;

	private Date resolved;

	private String comment;

	public CollisionDto() {
	}

	public Uid getCollisionId() {
		return collisionId;
	}
	public void setCollisionId(Uid collisionId) {
		this.collisionId = collisionId;
	}

	public Uid getHistoCryptoRepoFileId1() {
		return histoCryptoRepoFileId1;
	}
	public void setHistoCryptoRepoFileId1(Uid histoCryptoRepoFileId1) {
		this.histoCryptoRepoFileId1 = histoCryptoRepoFileId1;
	}
	public Uid getHistoCryptoRepoFileId2() {
		return histoCryptoRepoFileId2;
	}
	public void setHistoCryptoRepoFileId2(Uid histoCryptoRepoFileId2) {
		this.histoCryptoRepoFileId2 = histoCryptoRepoFileId2;
	}

	public Uid getDuplicateCryptoRepoFileId() {
		return duplicateCryptoRepoFileId;
	}
	public void setDuplicateCryptoRepoFileId(Uid duplicateCryptoRepoFileId) {
		this.duplicateCryptoRepoFileId = duplicateCryptoRepoFileId;
	}

	public Date getResolved() {
		return resolved;
	}
	public void setResolved(Date resolved) {
		this.resolved = resolved;
	}

	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
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
	 * <b>Important:</b> The implementation in {@code Collision} must exactly match the one in {@code CollisionDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(collisionId),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFileId1),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFileId2),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(duplicateCryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(resolved),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(comment)
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
		return this.getClass().getSimpleName() + "[collisionId=" + collisionId
				+ ", histoCryptoRepoFileId1=" + histoCryptoRepoFileId1
				+ ", histoCryptoRepoFileId2=" + histoCryptoRepoFileId2
				+ ", duplicateCryptoRepoFileId=" + duplicateCryptoRepoFileId
				+ "]";
	}
}
