package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserIdentityPayloadDto {

	private String firstName;

	private String lastName;

	private List<String> emails;

	private List<Long> pgpKeyIds;

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

	public List<Long> getPgpKeyIds() {
		if (pgpKeyIds == null)
			pgpKeyIds = new ArrayList<>();

		return pgpKeyIds;
	}
	public void setPgpKeyIds(List<Long> pgpKeyIds) {
		this.pgpKeyIds = pgpKeyIds;
	}

	@Override
	public String toString() {
		return String.format("%s['%s', '%s', %s, %s]", getClass().getSimpleName(), firstName, lastName, emails, pgpKeyIds);
	}
}
