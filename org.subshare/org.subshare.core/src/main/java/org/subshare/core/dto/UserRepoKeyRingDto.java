package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

public class UserRepoKeyRingDto {

	private List<UserRepoKeyDto> userRepoKeyDtos;

	public List<UserRepoKeyDto> getUserRepoKeyDtos() {
		if (userRepoKeyDtos == null)
			userRepoKeyDtos = new ArrayList<UserRepoKeyDto>();

		return userRepoKeyDtos;
	}
	public void setUserRepoKeyDtos(final List<UserRepoKeyDto> userRepoKeyDtos) {
		this.userRepoKeyDtos = userRepoKeyDtos;
	}
}
