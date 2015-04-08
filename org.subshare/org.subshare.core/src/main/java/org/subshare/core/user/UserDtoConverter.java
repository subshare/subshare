package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.ArrayList;

import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyRingDto;

public class UserDtoConverter {
	private final UserRepoKeyPublicKeyDtoConverter userRepoKeyPublicKeyDtoConverter = new UserRepoKeyPublicKeyDtoConverter();
	private final UserRepoKeyRingDtoConverter userRepoKeyRingDtoConverter = new UserRepoKeyRingDtoConverter();

	public UserDto toUserDto(final User user) {
		assertNotNull("user", user);

		final UserDto userDto = new UserDto();
		userDto.setFirstName(user.getFirstName());
		userDto.setLastName(user.getLastName());
		userDto.setPgpKeyIds(new ArrayList<Long>(user.getPgpKeyIds()));
		userDto.setEmails(new ArrayList<String>(user.getEmails()));

		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
		if (userRepoKeyRing != null)
			userDto.setUserRepoKeyRingDto(userRepoKeyRingDtoConverter.toUserRepoKeyRingDto(userRepoKeyRing));
		else {
			for (final UserRepoKey.PublicKeyWithSignature publicKey : user.getUserRepoKeyPublicKeys())
				userDto.getUserRepoKeyPublicKeyDtos().add(userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(publicKey));
		}
		return userDto;
	}

	public User fromUserDto(UserDto userDto) {
		assertNotNull("userDto", userDto);

		final User user = new User();
		user.setFirstName(userDto.getFirstName());
		user.setLastName(userDto.getLastName());
		user.getPgpKeyIds().addAll(userDto.getPgpKeyIds());
		user.getEmails().addAll(userDto.getEmails());

		final UserRepoKeyRingDto userRepoKeyRingDto = userDto.getUserRepoKeyRingDto();
		if (userRepoKeyRingDto != null)
			user.setUserRepoKeyRing(userRepoKeyRingDtoConverter.fromUserRepoKeyRingDto(userRepoKeyRingDto));
		else {
			for (final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto : userDto.getUserRepoKeyPublicKeyDtos())
				user.getUserRepoKeyPublicKeys().add(userRepoKeyPublicKeyDtoConverter.fromUserRepoKeyPublicKeyDto(userRepoKeyPublicKeyDto));
		}
		return user;
	}
}
