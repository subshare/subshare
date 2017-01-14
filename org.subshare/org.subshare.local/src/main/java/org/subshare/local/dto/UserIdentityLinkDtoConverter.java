package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.UserIdentityLinkDto;
import org.subshare.local.persistence.UserIdentityLink;

public class UserIdentityLinkDtoConverter {

	public UserIdentityLinkDto toUserIdentityLinkDto(final UserIdentityLink userIdentityLink) {
		assertNotNull(userIdentityLink, "userIdentity");
		final UserIdentityLinkDto userIdentityLinkDto = new UserIdentityLinkDto();
		userIdentityLinkDto.setUserIdentityLinkId(userIdentityLink.getUserIdentityLinkId());
		userIdentityLinkDto.setUserIdentityId(userIdentityLink.getUserIdentity().getUserIdentityId());
		userIdentityLinkDto.setForUserRepoKeyId(userIdentityLink.getForUserRepoKeyPublicKey().getUserRepoKeyId());
		userIdentityLinkDto.setEncryptedUserIdentityKeyData(userIdentityLink.getEncryptedUserIdentityKeyData());
		userIdentityLinkDto.setSignature(userIdentityLink.getSignature());
		return userIdentityLinkDto;
	}

}
