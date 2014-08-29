package org.subshare.core.user;

import java.util.ArrayList;

import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserRepoKeyDto;

public class UserDtoConverter {
	private final UserRepoKeyPublicKeyDtoConverter userRepoKeyPublicKeyDtoConverter = new UserRepoKeyPublicKeyDtoConverter();
	private final UserRepoKeyDtoConverter userRepoKeyDtoConverter = new UserRepoKeyDtoConverter();

	public UserDto toUserDto(final User user) {
		final UserDto userDto = new UserDto();
		userDto.setFirstName(user.getFirstName());
		userDto.setLastName(user.getLastName());
		userDto.setPgpKeyIds(new ArrayList<Long>(user.getPgpKeyIds()));
		userDto.setEmails(new ArrayList<String>(user.getEmails()));

		for (final UserRepoKey.PublicKey publicKey : user.getUserRepoKeyPublicKeys())
			userDto.getUserRepoKeyPublicKeyDtos().add(userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(publicKey));

		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
		if (userRepoKeyRing != null) {
			for (final UserRepoKey userRepoKey : userRepoKeyRing.getUserRepoKeys()) {
				final UserRepoKeyDto userRepoKeyDto = userRepoKeyDtoConverter.toUserRepoKeyDto(userRepoKey);

//				userDto.getUserRepoKeyDtos().addAll(c)
				throw new UnsupportedOperationException("NYI");
			}
		}

		return userDto;
	}

}
