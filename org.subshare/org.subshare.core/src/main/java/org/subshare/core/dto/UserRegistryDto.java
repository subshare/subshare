package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserRegistryDto {

	private List<UserDto> userDtos;

	private List<DeletedUid> deletedUserIds;

	private List<DeletedUid> deletedUserRepoKeyIds;

	public List<UserDto> getUserDtos() {
		if (userDtos == null)
			userDtos = new ArrayList<UserDto>();

		return userDtos;
	}
	public void setUserDtos(final List<UserDto> userDtos) {
		this.userDtos = userDtos;
	}

	public List<DeletedUid> getDeletedUserIds() {
		if (deletedUserIds == null)
			deletedUserIds = new ArrayList<>();

		return deletedUserIds;
	}
	public void setDeletedUserIds(List<DeletedUid> deletedUserIds) {
		this.deletedUserIds = deletedUserIds;
	}

	public List<DeletedUid> getDeletedUserRepoKeyIds() {
		if (deletedUserRepoKeyIds == null)
			deletedUserRepoKeyIds = new ArrayList<>();

		return deletedUserRepoKeyIds;
	}
	public void setDeletedUserRepoKeyIds(List<DeletedUid> deletedUserRepoKeyIds) {
		this.deletedUserRepoKeyIds = deletedUserRepoKeyIds;
	}
}
