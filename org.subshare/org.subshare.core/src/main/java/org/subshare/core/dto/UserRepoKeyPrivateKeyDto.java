package org.subshare.core.dto;

import java.util.Arrays;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserRepoKeyPrivateKeyDto {
	private Uid userRepoKeyId;
	private UUID repositoryId;
	private byte[] privateKeyData;

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

	/**
	 * Gets the encrypted (OpenPGP) private key.
	 * @return the encrypted (OpenPGP) private key.
	 */
	public byte[] getPrivateKeyData() {
		return privateKeyData;
	}
	public void setPrivateKeyData(final byte[] privateKeyData) {
		this.privateKeyData = privateKeyData;
	}

	@Override
	public String toString() {
		return "UserRepoKeyPrivateKeyDto[userRepoKeyId=" + userRepoKeyId
				+ ", repositoryId=" + repositoryId + ", privateKeyData="
				+ Arrays.toString(privateKeyData) + "]";
	}
}
