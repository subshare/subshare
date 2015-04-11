package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.dto.UserRepoInvitationDto;

public class UserRepoInvitationDtoConverter {

	private final UserRepoKeyDtoConverter userRepoKeyDtoConverter = new UserRepoKeyDtoConverter();

	public UserRepoInvitationDto toUserRepoInvitationDto(final UserRepoInvitation userRepoInvitation) {
		assertNotNull("userRepoInvitation", userRepoInvitation);
		final UserRepoInvitationDto result = new UserRepoInvitationDto();
		result.setInvitationUserRepoKeyDto(userRepoKeyDtoConverter.toUserRepoKeyDto(userRepoInvitation.getInvitationUserRepoKey()));
		result.setServerUrl(userRepoInvitation.getServerUrl());
		return result;
	}

	public UserRepoInvitation fromUserRepoInvitationDto(final UserRepoInvitationDto userRepoInvitationDto) {
		assertNotNull("userRepoInvitationDto", userRepoInvitationDto);

		final UserRepoKey invitationUserRepoKey = userRepoKeyDtoConverter.fromUserRepoKeyDto(
				userRepoInvitationDto.getInvitationUserRepoKeyDto());

		return new UserRepoInvitation(userRepoInvitationDto.getServerUrl(), invitationUserRepoKey);
	}
}
