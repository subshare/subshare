package org.subshare.local.dto;

import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDeletionDto;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDeletion;

public class UserRepoKeyPublicKeyReplacementRequestDeletionDtoConverter {

	public UserRepoKeyPublicKeyReplacementRequestDeletionDto toUserRepoKeyPublicKeyReplacementRequestDeletionDto(UserRepoKeyPublicKeyReplacementRequestDeletion requestDeletion) {
		final UserRepoKeyPublicKeyReplacementRequestDeletionDto requestDeletionDto = new UserRepoKeyPublicKeyReplacementRequestDeletionDto();
		requestDeletionDto.setRequestId(requestDeletion.getRequestId());
		requestDeletionDto.setOldUserRepoKeyId(requestDeletion.getOldUserRepoKeyId());
		requestDeletionDto.setSignature(requestDeletion.getSignature());
		return requestDeletionDto;
	}
}
