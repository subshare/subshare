package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

public interface UserRepoKeyRingLookup {

	static class Helper {
		private static UserRepoKeyRingLookup instance = new UserRepoKeyRingLookupImpl();

		public static UserRepoKeyRingLookup getUserRepoKeyRingLookup() {
			return instance;
		}

		public static void setUserRepoKeyRingLookup(final UserRepoKeyRingLookup lookup) {
			instance = assertNotNull(lookup, "lookup");
		}
	}

	UserRepoKeyRing getUserRepoKeyRing(UserRepoKeyRingLookupContext context);
}
