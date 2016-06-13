package org.subshare.core.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.subshare.core.pgp.PgpKeyId;

@SuppressWarnings("serial")
@XmlRootElement
public class UserIdentityPayloadDto implements Serializable {

	private String firstName;

	private String lastName;

	private List<String> emails;

	private List<PgpKeyId> pgpKeyIds;

	private UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto;

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public List<String> getEmails() {
		if (emails == null)
			emails = new ArrayList<>();

		return emails;
	}
	public void setEmails(List<String> emails) {
		this.emails = emails;
	}

	public List<PgpKeyId> getPgpKeyIds() {
		if (pgpKeyIds == null)
			pgpKeyIds = new ArrayList<>();

		return pgpKeyIds;
	}
	public void setPgpKeyIds(List<PgpKeyId> pgpKeyIds) {
		this.pgpKeyIds = pgpKeyIds;
	}

	/**
	 * Gets the public-key-DTO with signature.
	 * @return the public-key-DTO with signature.
	 */
	public UserRepoKeyPublicKeyDto getUserRepoKeyPublicKeyDto() {
		return userRepoKeyPublicKeyDto;
	}
	public void setUserRepoKeyPublicKeyDto(UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto) {
		this.userRepoKeyPublicKeyDto = userRepoKeyPublicKeyDto;
	}

	@Override
	public String toString() {
		return String.format("%s['%s', '%s', %s, %s]", getClass().getSimpleName(), firstName, lastName, emails, pgpKeyIds);
	}
}
