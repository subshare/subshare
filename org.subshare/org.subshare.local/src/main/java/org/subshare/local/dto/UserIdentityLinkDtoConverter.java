package org.subshare.local.dto;

import static java.util.Objects.*;

import org.subshare.core.dto.UserIdentityLinkDto;
import org.subshare.local.persistence.UserIdentityLink;

public class UserIdentityLinkDtoConverter {

	public UserIdentityLinkDto toUserIdentityLinkDto(final UserIdentityLink userIdentityLink) {
		requireNonNull(userIdentityLink, "userIdentity");
		final UserIdentityLinkDto userIdentityLinkDto = new UserIdentityLinkDto();
		userIdentityLinkDto.setUserIdentityLinkId(userIdentityLink.getUserIdentityLinkId());
		userIdentityLinkDto.setUserIdentityId(userIdentityLink.getUserIdentity().getUserIdentityId());
		userIdentityLinkDto.setForUserRepoKeyId(userIdentityLink.getForUserRepoKeyPublicKey().getUserRepoKeyId());
		userIdentityLinkDto.setEncryptedUserIdentityKeyData(userIdentityLink.getEncryptedUserIdentityKeyData());
		userIdentityLinkDto.setSignature(userIdentityLink.getSignature());
		return userIdentityLinkDto;
	}

}
