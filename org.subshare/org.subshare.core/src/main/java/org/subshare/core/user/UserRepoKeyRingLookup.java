package org.subshare.core.user;

import static java.util.Objects.*;

public interface UserRepoKeyRingLookup {

	static class Helper {
		private static UserRepoKeyRingLookup instance = new UserRepoKeyRingLookupImpl();

		public static UserRepoKeyRingLookup getUserRepoKeyRingLookup() {
			return instance;
		}

		public static void setUserRepoKeyRingLookup(final UserRepoKeyRingLookup lookup) {
			instance = requireNonNull(lookup, "lookup");
		}
	}

	UserRepoKeyRing getUserRepoKeyRing(UserRepoKeyRingLookupContext context);
}
