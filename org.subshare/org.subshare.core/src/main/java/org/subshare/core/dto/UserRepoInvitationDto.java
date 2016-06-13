package org.subshare.core.dto;

import java.net.URL;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserRepoInvitationDto {

	private URL serverUrl;

	private String serverPath;

	private UserRepoKeyDto invitationUserRepoKeyDto;

	public URL getServerUrl() {
		return serverUrl;
	}
	public void setServerUrl(final URL serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getServerPath() {
		return serverPath;
	}
	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}

	public UserRepoKeyDto getInvitationUserRepoKeyDto() {
		return invitationUserRepoKeyDto;
	}
	public void setInvitationUserRepoKeyDto(final UserRepoKeyDto invitationUserRepoKey) {
		this.invitationUserRepoKeyDto = invitationUserRepoKey;
	}
}
