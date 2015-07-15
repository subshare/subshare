package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.UserRepoInvitationDto;

public class UserRepoInvitationDtoConverter {

	private final UserRepoKeyDtoConverter userRepoKeyDtoConverter = new UserRepoKeyDtoConverter();
//	private final UserRepoKeyPublicKeyDtoWithSignatureConverter userRepoKeyPublicKeyDtoConverter = new UserRepoKeyPublicKeyDtoWithSignatureConverter();

	public UserRepoInvitationDto toUserRepoInvitationDto(final UserRepoInvitation userRepoInvitation) {
		assertNotNull("userRepoInvitation", userRepoInvitation);
		final UserRepoInvitationDto result = new UserRepoInvitationDto();
		result.setInvitationUserRepoKeyDto(userRepoKeyDtoConverter.toUserRepoKeyDto(userRepoInvitation.getInvitationUserRepoKey()));
		result.setServerUrl(userRepoInvitation.getServerUrl());
//		result.setSigningUserRepoKeyPublicKeyDto(
//				userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(userRepoInvitation.getSigningUserRepoKeyPublicKey()));
		return result;
	}

	public UserRepoInvitation fromUserRepoInvitationDto(final UserRepoInvitationDto userRepoInvitationDto) {
		assertNotNull("userRepoInvitationDto", userRepoInvitationDto);

		final UserRepoKey invitationUserRepoKey = userRepoKeyDtoConverter.fromUserRepoKeyDto(
				userRepoInvitationDto.getInvitationUserRepoKeyDto());

//		final UserRepoKey.PublicKey signingUserRepoKeyPublicKey = userRepoKeyPublicKeyDtoConverter.fromUserRepoKeyPublicKeyDto(
//				userRepoInvitationDto.getSigningUserRepoKeyPublicKeyDto());

		return new UserRepoInvitation(userRepoInvitationDto.getServerUrl(), invitationUserRepoKey); // , signingUserRepoKeyPublicKey);
	}
}
