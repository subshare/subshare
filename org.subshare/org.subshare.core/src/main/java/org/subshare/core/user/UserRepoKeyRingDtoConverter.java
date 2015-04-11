package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.dto.UserRepoKeyDto;
import org.subshare.core.dto.UserRepoKeyRingDto;

public class UserRepoKeyRingDtoConverter {
	private final UserRepoKeyDtoConverter userRepoKeyDtoConverter = new UserRepoKeyDtoConverter();

	public UserRepoKeyRingDto toUserRepoKeyRingDto(final UserRepoKeyRing userRepoKeyRing) {
		assertNotNull("userRepoKeyRing", userRepoKeyRing);

		final UserRepoKeyRingDto userRepoKeyRingDto = new UserRepoKeyRingDto();
		for (final UserRepoKey userRepoKey : userRepoKeyRing.getUserRepoKeys()) {
			final UserRepoKeyDto userRepoKeyDto = userRepoKeyDtoConverter.toUserRepoKeyDto(userRepoKey);
			userRepoKeyRingDto.getUserRepoKeyDtos().add(userRepoKeyDto);
		}
		return userRepoKeyRingDto;
	}

	public UserRepoKeyRing fromUserRepoKeyRingDto(final UserRepoKeyRingDto userRepoKeyRingDto) {
		assertNotNull("userRepoKeyRingDto", userRepoKeyRingDto);

		final UserRepoKeyRing userRepoKeyRing = new UserRepoKeyRing();
		for (final UserRepoKeyDto userRepoKeyDto : userRepoKeyRingDto.getUserRepoKeyDtos()) {
			final UserRepoKey userRepoKey = userRepoKeyDtoConverter.fromUserRepoKeyDto(userRepoKeyDto);
			userRepoKeyRing.addUserRepoKey(userRepoKey);
		}
		return userRepoKeyRing;
	}
}
