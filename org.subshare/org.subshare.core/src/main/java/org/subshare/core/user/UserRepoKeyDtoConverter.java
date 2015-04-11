package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.dto.UserRepoKeyDto;

public class UserRepoKeyDtoConverter {
//	private final UserRepoKeyPublicKeyDtoConverter userRepoKeyPublicKeyDtoConverter = new UserRepoKeyPublicKeyDtoConverter();

	public UserRepoKeyDto toUserRepoKeyDto(final UserRepoKey userRepoKey) {
		assertNotNull("userRepoKey", userRepoKey);

		final UserRepoKeyDto userRepoKeyDto = new UserRepoKeyDto();
//		final UserRepoKeyPrivateKeyDto privateKeyDto = new UserRepoKeyPrivateKeyDto();
//		privateKeyDto.setUserRepoKeyId(userRepoKey.getUserRepoKeyId());
//		privateKeyDto.setServerRepositoryId(userRepoKey.getServerRepositoryId());
//		privateKeyDto.setEncryptedSignedPrivateKeyData(userRepoKey.getEncryptedSignedPrivateKeyData());
//		userRepoKeyDto.setPrivateKeyDto(privateKeyDto);
//		userRepoKeyDto.setPublicKeyDto(userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(userRepoKey.getPublicKey()));
		userRepoKeyDto.setUserRepoKeyId(userRepoKey.getUserRepoKeyId());
		userRepoKeyDto.setServerRepositoryId(userRepoKey.getServerRepositoryId());
		userRepoKeyDto.setEncryptedSignedPrivateKeyData(userRepoKey.getEncryptedSignedPrivateKeyData());
		userRepoKeyDto.setSignedPublicKeyData(userRepoKey.getSignedPublicKeyData());
		userRepoKeyDto.setValidTo(userRepoKey.getValidTo());
		return userRepoKeyDto;
	}

	public UserRepoKey fromUserRepoKeyDto(final UserRepoKeyDto userRepoKeyDto) {
		assertNotNull("userRepoKeyDto", userRepoKeyDto);

//		final UserRepoKeyPrivateKeyDto privateKeyDto = assertNotNull("userRepoKeyDto.privateKeyDto", userRepoKeyDto.getPrivateKeyDto());
//		final UserRepoKeyPublicKeyDto publicKeyDto = assertNotNull("userRepoKeyDto.publicKeyDto", userRepoKeyDto.getPublicKeyDto());

		final UserRepoKey userRepoKey = new UserRepoKey(
				userRepoKeyDto.getUserRepoKeyId(),
				userRepoKeyDto.getServerRepositoryId(),
				userRepoKeyDto.getEncryptedSignedPrivateKeyData(),
				userRepoKeyDto.getSignedPublicKeyData(),
				userRepoKeyDto.getValidTo());

//		if (!privateKeyDto.getUserRepoKeyId().equals(publicKeyDto.getUserRepoKeyId()))
//			throw new IllegalArgumentException(String.format("privateKeyDto.userRepoKeyId != publicKeyDto.userRepoKeyId :: %s != %s",
//					privateKeyDto.getUserRepoKeyId(), publicKeyDto.getUserRepoKeyId()));

		return userRepoKey;
	}
}
