package org.subshare.core.user;

import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.crypto.CryptoRegistry;

public class UserRepoKeyPublicKeyDtoConverter {

	public UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKey.PublicKey publicKey) {
		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto = new UserRepoKeyPublicKeyDto();
		userRepoKeyPublicKeyDto.setUserRepoKeyId(publicKey.getUserRepoKeyId());
		userRepoKeyPublicKeyDto.setRepositoryId(publicKey.getRepositoryId());
		userRepoKeyPublicKeyDto.setPublicKeyData(CryptoRegistry.getInstance().encodePublicKey(publicKey.getPublicKey()));
		return userRepoKeyPublicKeyDto;
	}

}
