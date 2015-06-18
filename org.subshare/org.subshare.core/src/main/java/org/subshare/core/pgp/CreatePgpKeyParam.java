package org.subshare.core.pgp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import co.codewizards.cloudstore.ls.core.invoke.NoObjectRef;

@NoObjectRef
public class CreatePgpKeyParam implements Serializable {
	private static final long serialVersionUID = 1L;

	private final List<String> userIds = new ArrayList<>();

	private char[] passphrase;

	public CreatePgpKeyParam() {
	}

	public List<String> getUserIds() {
		return userIds;
	}

	public char[] getPassphrase() {
		return passphrase;
	}
	public void setPassphrase(char[] passphrase) {
		this.passphrase = passphrase;
	}
}
