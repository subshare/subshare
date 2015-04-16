package org.subshare.core.dto;

import java.net.URL;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserRepoInvitationDto {

	private URL serverUrl;

	private UserRepoKeyDto invitationUserRepoKeyDto;

//	private UserRepoKeyPublicKeyDto signingUserRepoKeyPublicKeyDto;

	public URL getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(final URL serverUrl) {
		this.serverUrl = serverUrl;
	}

	public UserRepoKeyDto getInvitationUserRepoKeyDto() {
		return invitationUserRepoKeyDto;
	}

	public void setInvitationUserRepoKeyDto(final UserRepoKeyDto invitationUserRepoKey) {
		this.invitationUserRepoKeyDto = invitationUserRepoKey;
	}

//	public UserRepoKeyPublicKeyDto getSigningUserRepoKeyPublicKeyDto() {
//		return signingUserRepoKeyPublicKeyDto;
//	}
//	public void setSigningUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKeyDto signingUserRepoKeyPublicKeyDto) {
//		this.signingUserRepoKeyPublicKeyDto = signingUserRepoKeyPublicKeyDto;
//	}
}
