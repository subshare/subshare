package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.dto.InvitationUserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

public class UserRepoKeyPublicKeyDtoConverter {

	public UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);

		final InvitationUserRepoKeyPublicKey invUserRepoKeyPublicKey = (InvitationUserRepoKeyPublicKey)
				(userRepoKeyPublicKey instanceof InvitationUserRepoKeyPublicKey ? userRepoKeyPublicKey : null);

		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto =
				invUserRepoKeyPublicKey != null ? new InvitationUserRepoKeyPublicKeyDto() : new UserRepoKeyPublicKeyDto();

		userRepoKeyPublicKeyDto.setLocalRevision(userRepoKeyPublicKey.getLocalRevision());
		userRepoKeyPublicKeyDto.setPublicKeyData(userRepoKeyPublicKey.getPublicKeyData());
		userRepoKeyPublicKeyDto.setRepositoryId(userRepoKeyPublicKey.getServerRepositoryId());
		userRepoKeyPublicKeyDto.setUserRepoKeyId(userRepoKeyPublicKey.getUserRepoKeyId());

		if (invUserRepoKeyPublicKey != null) {
			final InvitationUserRepoKeyPublicKeyDto invUserRepoKeyPublicKeyDto = (InvitationUserRepoKeyPublicKeyDto) userRepoKeyPublicKeyDto;
			invUserRepoKeyPublicKeyDto.setValidTo(invUserRepoKeyPublicKey.getValidTo());
			invUserRepoKeyPublicKeyDto.setSignature(invUserRepoKeyPublicKey.getSignature());
		}
		return userRepoKeyPublicKeyDto;
	}

}
