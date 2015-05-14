package org.subshare.test;

import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;

public class StaticUserRepoKeyRingLookup implements UserRepoKeyRingLookup {

	private UserRepoKeyRing userRepoKeyRing;

	public StaticUserRepoKeyRingLookup() {
	}

	public StaticUserRepoKeyRingLookup(UserRepoKeyRing userRepoKeyRing) {
		this.userRepoKeyRing = userRepoKeyRing;
	}

	public UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}
	public void setUserRepoKeyRing(UserRepoKeyRing userRepoKeyRing) {
		this.userRepoKeyRing = userRepoKeyRing;
	}
	@Override
	public UserRepoKeyRing getUserRepoKeyRing(UserRepoKeyRingLookupContext context) {
		return userRepoKeyRing;
	}
}
