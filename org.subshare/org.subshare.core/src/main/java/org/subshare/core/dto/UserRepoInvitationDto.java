package org.subshare.core.dto;

import java.net.URL;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserRepoInvitationDto {

	private URL serverUrl;

	private UserRepoKeyDto invitationUserRepoKeyDto;

	public URL getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(URL serverUrl) {
		this.serverUrl = serverUrl;
	}

	public UserRepoKeyDto getInvitationUserRepoKeyDto() {
		return invitationUserRepoKeyDto;
	}

	public void setInvitationUserRepoKeyDto(UserRepoKeyDto invitationUserRepoKey) {
		this.invitationUserRepoKeyDto = invitationUserRepoKey;
	}
}
