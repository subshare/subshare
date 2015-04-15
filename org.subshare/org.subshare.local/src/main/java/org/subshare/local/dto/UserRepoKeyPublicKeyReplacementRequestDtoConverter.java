package org.subshare.local.dto;

import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDto;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequest;

public class UserRepoKeyPublicKeyReplacementRequestDtoConverter {

//	private final UserRepoKeyPublicKeyDtoConverter userRepoKeyPublicKeyDtoConverter = new UserRepoKeyPublicKeyDtoConverter();

	public UserRepoKeyPublicKeyReplacementRequestDto toUserRepoKeyPublicKeyReplacementRequestDto(UserRepoKeyPublicKeyReplacementRequest request) {
		final UserRepoKeyPublicKeyReplacementRequestDto requestDto = new UserRepoKeyPublicKeyReplacementRequestDto();
		requestDto.setRequestId(request.getRequestId());
		requestDto.setLocalRevision(request.getLocalRevision());
		requestDto.setSignature(request.getSignature());
//		requestDto.setOldKey(userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(request.getOldKey()));
//		requestDto.setNewKey(userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(request.getNewKey()));
		requestDto.setOldKeyId(request.getOldKey().getUserRepoKeyId());
		requestDto.setNewKeyId(request.getNewKey().getUserRepoKeyId());
		return requestDto;
	}

}
