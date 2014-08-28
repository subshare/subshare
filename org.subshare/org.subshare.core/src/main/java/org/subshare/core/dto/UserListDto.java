package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserListDto {

	private List<UserDto> userDtos;

	public List<UserDto> getUserDtos() {
		if (userDtos == null)
			userDtos = new ArrayList<UserDto>();

		return userDtos;
	}
	public void setUserDtos(final List<UserDto> userDtos) {
		this.userDtos = userDtos;
	}

}
