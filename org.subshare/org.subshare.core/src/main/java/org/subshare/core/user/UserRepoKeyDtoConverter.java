package org.subshare.core.user;

import org.subshare.core.dto.UserRepoKeyDto;
import org.subshare.core.dto.UserRepoKeyPrivateKeyDto;

public class UserRepoKeyDtoConverter {
	private final UserRepoKeyPublicKeyDtoConverter userRepoKeyPublicKeyDtoConverter = new UserRepoKeyPublicKeyDtoConverter();

	public UserRepoKeyDto toUserRepoKeyDto(final UserRepoKey userRepoKey) {
		final UserRepoKeyDto userRepoKeyDto = new UserRepoKeyDto();
		final UserRepoKeyPrivateKeyDto privateKeyDto = new UserRepoKeyPrivateKeyDto();
		privateKeyDto.setUserRepoKeyId(userRepoKey.getUserRepoKeyId());
		privateKeyDto.setRepositoryId(userRepoKey.getRepositoryId());
		privateKeyDto.setEncryptedSignedPrivateKeyData(userRepoKey.getEncryptedSignedPrivateKeyData());
		userRepoKeyDto.setPrivateKeyDto(privateKeyDto);
		userRepoKeyDto.setPublicKeyDto(userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(userRepoKey.getPublicKey()));
		return userRepoKeyDto;
	}


}
