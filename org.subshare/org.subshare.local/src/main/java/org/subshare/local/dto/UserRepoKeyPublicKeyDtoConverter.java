package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

public class UserRepoKeyPublicKeyDtoConverter {

	public UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);
		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto = new UserRepoKeyPublicKeyDto();
		userRepoKeyPublicKeyDto.setLocalRevision(userRepoKeyPublicKey.getLocalRevision());
		userRepoKeyPublicKeyDto.setPublicKeyData(userRepoKeyPublicKey.getPublicKeyData());
		userRepoKeyPublicKeyDto.setRepositoryId(userRepoKeyPublicKey.getServerRepositoryId());
		userRepoKeyPublicKeyDto.setUserRepoKeyId(userRepoKeyPublicKey.getUserRepoKeyId());
		userRepoKeyPublicKeyDto.setValidTo(userRepoKeyPublicKey.getValidTo());
		return userRepoKeyPublicKeyDto;
	}

}
