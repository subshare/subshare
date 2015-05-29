package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserDto {

	private Uid userId;

	private String firstName;

	private String lastName;

	private List<String> emails;

	private UserRepoKeyRingDto userRepoKeyRingDto;

	private List<PgpKeyId> pgpKeyIds;

	private List<UserRepoKeyPublicKeyDto> userRepoKeyPublicKeyDtos;

	private Date changed;

	public Uid getUserId() {
		return userId;
	}
	public void setUserId(Uid userId) {
		this.userId = userId;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public UserRepoKeyRingDto getUserRepoKeyRingDto() {
		return userRepoKeyRingDto;
	}
	public void setUserRepoKeyRingDto(final UserRepoKeyRingDto userRepoKeyRingDto) {
		this.userRepoKeyRingDto = userRepoKeyRingDto;
	}

	public List<String> getEmails() {
		if (emails == null)
			emails = new ArrayList<String>();

		return emails;
	}
	public void setEmails(final List<String> emails) {
		this.emails = emails;
	}

	public List<PgpKeyId> getPgpKeyIds() {
		if (pgpKeyIds == null)
			pgpKeyIds = new ArrayList<PgpKeyId>();

		return pgpKeyIds;
	}
	public void setPgpKeyIds(final List<PgpKeyId> pgpKeyIds) {
		this.pgpKeyIds = pgpKeyIds;
	}

	public List<UserRepoKeyPublicKeyDto> getUserRepoKeyPublicKeyDtos() {
		if (userRepoKeyPublicKeyDtos == null)
			userRepoKeyPublicKeyDtos = new ArrayList<UserRepoKeyPublicKeyDto>();

		return userRepoKeyPublicKeyDtos;
	}
	public void setUserRepoKeyPublicKeyDtos(final List<UserRepoKeyPublicKeyDto> userRepoKeyPublicKeyDtos) {
		this.userRepoKeyPublicKeyDtos = userRepoKeyPublicKeyDtos;
	}

	public Date getChanged() {
		return changed;
	}
	public void setChanged(Date changed) {
		this.changed = changed;
	}
}
