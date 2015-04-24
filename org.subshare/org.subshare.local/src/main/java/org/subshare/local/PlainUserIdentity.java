package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.local.persistence.UserIdentity;

public class PlainUserIdentity {

	private final UserIdentity userIdentity;

	private final KeyParameter sharedSecret;

	public PlainUserIdentity(final UserIdentity userIdentity, final KeyParameter sharedSecret) {
		this.userIdentity = assertNotNull("userIdentity", userIdentity);
		this.sharedSecret = assertNotNull("sharedSecret", sharedSecret);
	}

	public UserIdentity getUserIdentity() {
		return userIdentity;
	}

	public KeyParameter getSharedSecret() {
		return sharedSecret;
	}
}
