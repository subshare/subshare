package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class RepositoryOwnerDto implements Signable {

	public static final String SIGNED_DATA_TYPE = "RepositoryOwner";

	private UUID serverRepositoryId;

	private Uid userRepoKeyId;

	@XmlElement
	private SignatureDto signatureDto;

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}

	public void setServerRepositoryId(final UUID serverRepositoryId) {
		this.serverRepositoryId = serverRepositoryId;
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	public void setUserRepoKeyId(final Uid userRepoKeyId) {
		this.userRepoKeyId = userRepoKeyId;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code RepositoryOwner} must exactly match the one in {@link RepositoryOwnerDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		byte separatorIndex = 0;
		try {
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(RepositoryOwnerDto.SIGNED_DATA_TYPE),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(serverRepositoryId),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(userRepoKeyId)
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
}
