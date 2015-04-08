package org.subshare.core.dto;

import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserRepoKeyDto {

	private Uid userRepoKeyId;
	private UUID serverRepositoryId;
	private byte[] encryptedSignedPrivateKeyData;
	private byte[] signedPublicKeyData;

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}
	public void setUserRepoKeyId(Uid userRepoKeyId) {
		this.userRepoKeyId = userRepoKeyId;
	}
	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
	public void setServerRepositoryId(UUID serverRepositoryId) {
		this.serverRepositoryId = serverRepositoryId;
	}
	public byte[] getEncryptedSignedPrivateKeyData() {
		return encryptedSignedPrivateKeyData;
	}
	public void setEncryptedSignedPrivateKeyData(byte[] encryptedSignedPrivateKeyData) {
		this.encryptedSignedPrivateKeyData = encryptedSignedPrivateKeyData;
	}
	public byte[] getSignedPublicKeyData() {
		return signedPublicKeyData;
	}
	public void setSignedPublicKeyData(byte[] signedPublicKeyData) {
		this.signedPublicKeyData = signedPublicKeyData;
	}

//	private UserRepoKeyPrivateKeyDto privateKeyDto;
//
//	private UserRepoKeyPublicKeyDto publicKeyDto;
//
//	public UserRepoKeyPrivateKeyDto getPrivateKeyDto() {
//		return privateKeyDto;
//	}
//	public void setPrivateKeyDto(final UserRepoKeyPrivateKeyDto privateKeyDto) {
//		this.privateKeyDto = privateKeyDto;
//	}
//
//	public UserRepoKeyPublicKeyDto getPublicKeyDto() {
//		return publicKeyDto;
//	}
//	public void setPublicKeyDto(final UserRepoKeyPublicKeyDto publicKeyDto) {
//		this.publicKeyDto = publicKeyDto;
//	}
}
