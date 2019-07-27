package org.subshare.local;

import static java.util.Objects.*;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.local.persistence.UserIdentity;

public class PlainUserIdentity {

	private final UserIdentity userIdentity;

	private final KeyParameter sharedSecret;

	public PlainUserIdentity(final UserIdentity userIdentity, final KeyParameter sharedSecret) {
		this.userIdentity = requireNonNull(userIdentity, "userIdentity");
		this.sharedSecret = requireNonNull(sharedSecret, "sharedSecret");
	}

	public UserIdentity getUserIdentity() {
		return userIdentity;
	}

	public KeyParameter getSharedSecret() {
		return sharedSecret;
	}
}
