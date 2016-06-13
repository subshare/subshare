package org.subshare.core.dto;

import java.io.Serializable;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
@XmlRootElement
public class DebugUserRepoKeyDto implements Serializable {

	private Uid userRepoKeyId;
	private UUID serverRepositoryId;
	private boolean owner;
	private boolean inDatabase;
	private boolean inKeyRing;
	private int userIdentityCount;
	private boolean invitation;
	private UserIdentityPayloadDto userIdentityPayloadDto;

	public DebugUserRepoKeyDto() {

	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}
	public void setUserRepoKeyId(Uid userRepoKeyId) {
		this.userRepoKeyId = userRepoKeyId;
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
	public void setServerRepositoryId(UUID serverRepositoryId) {
		this.serverRepositoryId = serverRepositoryId;
	}

	public boolean isOwner() {
		return owner;
	}
	public void setOwner(boolean owner) {
		this.owner = owner;
	}

	public boolean isInDatabase() {
		return inDatabase;
	}
	public void setInDatabase(boolean inDatabase) {
		this.inDatabase = inDatabase;
	}

	public boolean isInKeyRing() {
		return inKeyRing;
	}
	public void setInKeyRing(boolean inKeyRing) {
		this.inKeyRing = inKeyRing;
	}

	public int getUserIdentityCount() {
		return userIdentityCount;
	}
	public void setUserIdentityCount(int userIdentityCount) {
		this.userIdentityCount = userIdentityCount;
	}

	public boolean isInvitation() {
		return invitation;
	}
	public void setInvitation(boolean invitation) {
		this.invitation = invitation;
	}

	public UserIdentityPayloadDto getUserIdentityPayloadDto() {
		return userIdentityPayloadDto;
	}
	public void setUserIdentityPayloadDto(UserIdentityPayloadDto userIdentityPayloadDto) {
		this.userIdentityPayloadDto = userIdentityPayloadDto;
	}
}
