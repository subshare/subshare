package org.subshare.core.dto;

import java.util.Arrays;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserRepoKeyPrivateKeyDto {
	private Uid userRepoKeyId;
	private UUID serverRepositoryId;
	private byte[] encryptedSignedPrivateKeyData;

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}
	public void setUserRepoKeyId(final Uid userRepoKeyId) {
		this.userRepoKeyId = userRepoKeyId;
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
	public void setServerRepositoryId(final UUID repositoryId) {
		this.serverRepositoryId = repositoryId;
	}

	/**
	 * Gets the encrypted (OpenPGP) private key.
	 * @return the encrypted (OpenPGP) private key.
	 */
	public byte[] getEncryptedSignedPrivateKeyData() {
		return encryptedSignedPrivateKeyData;
	}
	public void setEncryptedSignedPrivateKeyData(final byte[] privateKeyData) {
		this.encryptedSignedPrivateKeyData = privateKeyData;
	}

	@Override
	public String toString() {
		return "UserRepoKeyPrivateKeyDto[userRepoKeyId=" + userRepoKeyId
				+ ", serverRepositoryId=" + serverRepositoryId + ", encryptedSignedPrivateKeyData="
				+ Arrays.toString(encryptedSignedPrivateKeyData) + "]";
	}
}
