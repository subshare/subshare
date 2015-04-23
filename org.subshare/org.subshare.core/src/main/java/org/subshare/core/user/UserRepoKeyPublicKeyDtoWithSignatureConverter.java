package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.dto.InvitationUserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;

public class UserRepoKeyPublicKeyDtoWithSignatureConverter {

//	public UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKey.PublicKey publicKey) {
//		if (publicKey instanceof UserRepoKey.PublicKeyWithSignature)
//			return toUserRepoKeyPublicKeyDto((UserRepoKey.PublicKeyWithSignature) publicKey);
//
//		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto = new UserRepoKeyPublicKeyDto();
//
//		userRepoKeyPublicKeyDto.setUserRepoKeyId(publicKey.getUserRepoKeyId());
//		userRepoKeyPublicKeyDto.setRepositoryId(publicKey.getServerRepositoryId());
//
//		return userRepoKeyPublicKeyDto;
//	}

	public UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKey.PublicKeyWithSignature publicKey) {
		assertNotNull("publicKey", publicKey);

		final InvitationUserRepoKeyPublicKeyDto invitationUserRepoKeyPublicKeyDto = publicKey.isInvitation() ? new InvitationUserRepoKeyPublicKeyDto() : null;

		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto =
				invitationUserRepoKeyPublicKeyDto != null ? invitationUserRepoKeyPublicKeyDto : new UserRepoKeyPublicKeyDto();

		userRepoKeyPublicKeyDto.setUserRepoKeyId(publicKey.getUserRepoKeyId());
		userRepoKeyPublicKeyDto.setRepositoryId(publicKey.getServerRepositoryId());
		userRepoKeyPublicKeyDto.setSignedPublicKeyData(publicKey.getSignedPublicKeyData());

		if (invitationUserRepoKeyPublicKeyDto != null) {
			invitationUserRepoKeyPublicKeyDto.setValidTo(publicKey.getValidTo());
			invitationUserRepoKeyPublicKeyDto.setSignature(publicKey.getSignature());
		}

		return userRepoKeyPublicKeyDto;
	}

//	public UserRepoKey.PublicKey fromUserRepoKeyPublicKeyDtoNoSig(final UserRepoKeyPublicKeyDto publicKeyDto) {
//		assertNotNull("publicKeyDto", publicKeyDto);
//
//		CryptoRegistry.
//
//		final UserRepoKey.PublicKey publicKey = new UserRepoKey.PublicKey(
//				publicKeyDto.getUserRepoKeyId(), publicKeyDto.getRepositoryId(),
//				null, false);
//
//		if (invPublicKeyDto != null)
//			publicKey.setSignature(invPublicKeyDto.getSignature());
//
//		return publicKey;
//	}

	public UserRepoKey.PublicKeyWithSignature fromUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKeyDto publicKeyDto) {
		assertNotNull("publicKeyDto", publicKeyDto);
		assertNotNull("publicKeyDto.signedPublicKeyData", publicKeyDto.getSignedPublicKeyData());

		InvitationUserRepoKeyPublicKeyDto invPublicKeyDto = (InvitationUserRepoKeyPublicKeyDto)
				(publicKeyDto instanceof InvitationUserRepoKeyPublicKeyDto ? publicKeyDto : null);

		final UserRepoKey.PublicKeyWithSignature publicKey = new UserRepoKey.PublicKeyWithSignature(
				publicKeyDto.getUserRepoKeyId(), publicKeyDto.getRepositoryId(), publicKeyDto.getSignedPublicKeyData(),
				(invPublicKeyDto != null ? invPublicKeyDto.getValidTo() : null),
				invPublicKeyDto != null);

		if (invPublicKeyDto != null)
			publicKey.setSignature(invPublicKeyDto.getSignature());

		return publicKey;
	}
}
