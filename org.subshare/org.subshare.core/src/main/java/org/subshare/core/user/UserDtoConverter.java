package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;

import org.subshare.core.dto.UserDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyRingDto;
import org.subshare.core.pgp.PgpKeyId;

public class UserDtoConverter {
	private final UserRepoKeyPublicKeyDtoWithSignatureConverter userRepoKeyPublicKeyDtoWithSignatureConverter = new UserRepoKeyPublicKeyDtoWithSignatureConverter();
	private final UserRepoKeyRingDtoConverter userRepoKeyRingDtoConverter = new UserRepoKeyRingDtoConverter();

	public UserDto toUserDto(final User user) {
		assertNotNull("user", user);

		final UserDto userDto = new UserDto();
		userDto.setUserId(user.getUserId());
		userDto.setFirstName(user.getFirstName());
		userDto.setLastName(user.getLastName());
		userDto.setPgpKeyIds(new ArrayList<PgpKeyId>(user.getPgpKeyIds()));
		userDto.setEmails(new ArrayList<String>(user.getEmails()));

		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
		if (userRepoKeyRing != null)
			userDto.setUserRepoKeyRingDto(userRepoKeyRingDtoConverter.toUserRepoKeyRingDto(userRepoKeyRing));
		else {
			for (final UserRepoKey.PublicKeyWithSignature publicKey : user.getUserRepoKeyPublicKeys())
				userDto.getUserRepoKeyPublicKeyDtos().add(userRepoKeyPublicKeyDtoWithSignatureConverter.toUserRepoKeyPublicKeyDto(publicKey));
		}

		userDto.setChanged(user.getChanged());
		return userDto;
	}

	public User fromUserDto(UserDto userDto) {
		assertNotNull("userDto", userDto);

		final User user = new UserImpl();
		user.setUserId(userDto.getUserId());
		user.setFirstName(userDto.getFirstName());
		user.setLastName(userDto.getLastName());
		user.getPgpKeyIds().addAll(userDto.getPgpKeyIds());
		user.getEmails().addAll(userDto.getEmails());

		final UserRepoKeyRingDto userRepoKeyRingDto = userDto.getUserRepoKeyRingDto();
		if (userRepoKeyRingDto != null)
			user.setUserRepoKeyRing(userRepoKeyRingDtoConverter.fromUserRepoKeyRingDto(userRepoKeyRingDto));
		else {
			for (final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto : userDto.getUserRepoKeyPublicKeyDtos())
				user.getUserRepoKeyPublicKeys().add(userRepoKeyPublicKeyDtoWithSignatureConverter.fromUserRepoKeyPublicKeyDto(userRepoKeyPublicKeyDto));
		}

		user.setChanged(userDto.getChanged());
		return user;
	}
}
