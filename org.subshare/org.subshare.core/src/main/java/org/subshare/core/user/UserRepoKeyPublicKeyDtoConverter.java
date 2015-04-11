package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.dto.UserRepoKeyPublicKeyDto;

public class UserRepoKeyPublicKeyDtoConverter {

	public UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKey.PublicKeyWithSignature publicKey) {
		assertNotNull("publicKey", publicKey);
		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto = new UserRepoKeyPublicKeyDto();
		userRepoKeyPublicKeyDto.setUserRepoKeyId(publicKey.getUserRepoKeyId());
		userRepoKeyPublicKeyDto.setRepositoryId(publicKey.getServerRepositoryId());
		userRepoKeyPublicKeyDto.setSignedPublicKeyData(publicKey.getSignedPublicKeyData());
		userRepoKeyPublicKeyDto.setValidTo(publicKey.getValidTo());
		return userRepoKeyPublicKeyDto;
	}

	public UserRepoKey.PublicKeyWithSignature fromUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKeyDto publicKeyDto) {
		assertNotNull("publicKeyDto", publicKeyDto);
		assertNotNull("publicKeyDto.signedPublicKeyData", publicKeyDto.getSignedPublicKeyData());
		final UserRepoKey.PublicKeyWithSignature publicKey = new UserRepoKey.PublicKeyWithSignature(
				publicKeyDto.getUserRepoKeyId(), publicKeyDto.getRepositoryId(), publicKeyDto.getSignedPublicKeyData(),
				publicKeyDto.getValidTo());
		return publicKey;
	}
}
