package org.subshare.core.dto;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserRepoKeyPublicKeyDto {
	private Uid userRepoKeyId;
	private UUID repositoryId;
	private Date validTo;
	private byte[] publicKeyData;
	private byte[] signedPublicKeyData;
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

	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
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
