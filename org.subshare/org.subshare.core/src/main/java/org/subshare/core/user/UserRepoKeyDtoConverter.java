package org.subshare.core.user;

import static java.util.Objects.*;

import org.subshare.core.dto.UserRepoKeyDto;
import org.subshare.core.sign.Signature;

public class UserRepoKeyDtoConverter {

	public UserRepoKeyDto toUserRepoKeyDto(final UserRepoKey userRepoKey) {
		requireNonNull(userRepoKey, "userRepoKey");

		final UserRepoKeyDto userRepoKeyDto = new UserRepoKeyDto();
		userRepoKeyDto.setUserRepoKeyId(userRepoKey.getUserRepoKeyId());
		userRepoKeyDto.setServerRepositoryId(userRepoKey.getServerRepositoryId());
		userRepoKeyDto.setEncryptedSignedPrivateKeyData(userRepoKey.getEncryptedSignedPrivateKeyData());
		userRepoKeyDto.setSignedPublicKeyData(userRepoKey.getSignedPublicKeyData());
		userRepoKeyDto.setValidTo(userRepoKey.getValidTo());
		userRepoKeyDto.setInvitation(userRepoKey.isInvitation());
		userRepoKeyDto.setPublicKeySignature(userRepoKey.getPublicKey().getSignature());
		return userRepoKeyDto;
	}

	public UserRepoKey fromUserRepoKeyDto(final UserRepoKeyDto userRepoKeyDto) {
		requireNonNull(userRepoKeyDto, "userRepoKeyDto");

		final UserRepoKey userRepoKey = new UserRepoKeyImpl(
				userRepoKeyDto.getUserRepoKeyId(),
				userRepoKeyDto.getServerRepositoryId(),
				userRepoKeyDto.getEncryptedSignedPrivateKeyData(),
				userRepoKeyDto.getSignedPublicKeyData(),
				userRepoKeyDto.getValidTo(),
				userRepoKeyDto.isInvitation());

		final Signature publicKeySignature = userRepoKeyDto.getPublicKeySignature();
		if (publicKeySignature != null)
			userRepoKey.getPublicKey().setSignature(publicKeySignature);

		return userRepoKey;
	}
}
