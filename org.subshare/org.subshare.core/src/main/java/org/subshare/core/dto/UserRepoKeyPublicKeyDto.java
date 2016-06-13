package org.subshare.core.dto;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
@XmlRootElement
public class UserRepoKeyPublicKeyDto implements Serializable {
	private Uid userRepoKeyId;
	private UUID repositoryId;
	private byte[] publicKeyData; // for storage in repo DB
	private byte[] signedPublicKeyData; // for storage in UserRegistry - signed with OpenPGP
	private long localRevision;

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}
	public void setUserRepoKeyId(final Uid userRepoKeyId) {
		this.userRepoKeyId = userRepoKeyId;
	}

	public UUID getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(final UUID repositoryId) {
		this.repositoryId = repositoryId;
	}

	public byte[] getPublicKeyData() {
		return publicKeyData;
	}
	public void setPublicKeyData(final byte[] publicKeyData) {
		this.publicKeyData = publicKeyData;
	}

	public byte[] getSignedPublicKeyData() {
		return signedPublicKeyData;
	}
	public void setSignedPublicKeyData(byte[] signedPublicKeyData) {
		this.signedPublicKeyData = signedPublicKeyData;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

	@Override
	public String toString() {
		return "UserRepoKeyPublicKeyDto[userRepoKeyId=" + userRepoKeyId
				+ ", repositoryId=" + repositoryId + ", publicKeyData="
				+ Arrays.toString(publicKeyData) + ", localRevision="
				+ localRevision + "]";
	}
}
