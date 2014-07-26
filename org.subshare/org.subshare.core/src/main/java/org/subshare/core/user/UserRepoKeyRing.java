package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.codewizards.cloudstore.core.dto.Uid;

public class UserRepoKeyRing {

	private static SecureRandom random = new SecureRandom();
	private final Map<Uid, UserRepoKey> userRepoKeyId2UserRepoKey = new HashMap<Uid, UserRepoKey>();
	private List<UserRepoKey> userRepoKeyList;

	public Collection<UserRepoKey> getUserRepoKeys() {
		return getUserRepoKeyList();
	}

	protected List<UserRepoKey> getUserRepoKeyList() {
		if (userRepoKeyList == null)
			userRepoKeyList = Collections.unmodifiableList(new ArrayList<UserRepoKey>(userRepoKeyId2UserRepoKey.values()));

		return userRepoKeyList;
	}

	public UserRepoKey getRandomUserRepoKey() {
		final List<UserRepoKey> list = getUserRepoKeyList();
		if (list.isEmpty())
			return null;

		final UserRepoKey userRepoKey = list.get(random.nextInt(list.size()));
		return userRepoKey;
	}

	public UserRepoKey getRandomUserRepoKeyOrFail() {
		final UserRepoKey userRepoKey = getRandomUserRepoKey();
		if (userRepoKey == null)
			throw new IllegalStateException("This UserRepoKeyRing is empty!");

		return userRepoKey;
	}

	public void addUserRepoKey(final UserRepoKey userRepoKey) {
		assertNotNull("userRepoKey", userRepoKey);
		userRepoKeyId2UserRepoKey.put(userRepoKey.getUserRepoKeyId(), userRepoKey);
		userRepoKeyList = null;
	}

	public void removeUserRepoKey(final UserRepoKey userRepoKey) {
		removeUserRepoKey(assertNotNull("userRepoKey", userRepoKey).getUserRepoKeyId());
	}

	public void removeUserRepoKey(final Uid userRepoKeyId) {
		userRepoKeyId2UserRepoKey.remove(assertNotNull("userRepoKeyId", userRepoKeyId));
		userRepoKeyList = null;
	}

	public UserRepoKey getUserRepoKey(final Uid userRepoKeyId) {
		return userRepoKeyId2UserRepoKey.get(assertNotNull("userRepoKeyId", userRepoKeyId));
	}

	public UserRepoKey getUserRepoKeyOrFail(final Uid userRepoKeyId) {
		final UserRepoKey userRepoKey = getUserRepoKey(userRepoKeyId);
		if (userRepoKey == null)
			throw new IllegalStateException(String.format("There is no UserRepoKey with userRepoKeyId='%s'!", userRepoKeyId));

		return userRepoKey;
	}
}
