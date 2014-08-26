package org.subshare.core.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class User {

	public User() { }

	private String firstName;

	private String lastName;

	private List<String> emails;

	private UserRepoKeyRing userRepoKeyRing;

	private List<Long> pgpKeyIds;

	private Collection<UserRepoKey.PublicKey> userRepoKeyPublicKeys;

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

	public List<String> getEmails() {
		if (emails == null)
			emails = new ArrayList<String>();

		return emails;
	}

	public List<Long> getPgpKeyIds() {
		if (pgpKeyIds == null)
			pgpKeyIds = new ArrayList<Long>();

		return pgpKeyIds;
	}

	public UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}

	public Collection<UserRepoKey.PublicKey> getUserRepoKeyPublicKeys() {
		if (userRepoKeyPublicKeys == null)
			userRepoKeyPublicKeys = new ArrayList<UserRepoKey.PublicKey>();

		return userRepoKeyPublicKeys;
	}

}
