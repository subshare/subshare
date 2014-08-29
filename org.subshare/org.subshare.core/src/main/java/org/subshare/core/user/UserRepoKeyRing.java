package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.Uid;

public class UserRepoKeyRing {

	private static SecureRandom random = new SecureRandom();

//	private final List<Long> pgpKeyIds = new ArrayList<Long>();
//	private List<Long> _pgpKeyIds;

	private final Map<Uid, UserRepoKey> userRepoKeyId2UserRepoKey = new HashMap<>();
	private final Map<UUID, List<UserRepoKey>> repositoryId2userRepoKeyList = new HashMap<>();

	public Collection<UserRepoKey> getUserRepoKeys() {
		return Collections.unmodifiableCollection(userRepoKeyId2UserRepoKey.values());
	}

	public Collection<UserRepoKey> getUserRepoKeys(final UUID repositoryId) {
		return getUserRepoKeyList(repositoryId);
	}

	protected synchronized List<UserRepoKey> getUserRepoKeyList(final UUID repositoryId) {
		assertNotNull("repositoryId", repositoryId);
		List<UserRepoKey> userRepoKeyList = repositoryId2userRepoKeyList.get(repositoryId);

		if (userRepoKeyList == null) {
			userRepoKeyList = Collections.unmodifiableList(new ArrayList<UserRepoKey>(userRepoKeyId2UserRepoKey.values()));
			repositoryId2userRepoKeyList.put(repositoryId, userRepoKeyList);
		}

		return userRepoKeyList;
	}

//	public synchronized Collection<Long> getPgpKeyIds() {
//		if (_pgpKeyIds == null)
//			_pgpKeyIds = Collections.unmodifiableList(new ArrayList<Long>(pgpKeyIds));
//
//		return _pgpKeyIds;
//	}
//
//	public synchronized void addPgpKeyId(final long pgpKeyId) {
//		if (!pgpKeyIds.contains(pgpKeyId)) {
//			pgpKeyIds.add(pgpKeyId);
//			_pgpKeyIds = null;
//		}
//	}
//
//	public synchronized void removePgpKeyId(final long pgpKeyId) {
//		pgpKeyIds.remove(Long.valueOf(pgpKeyId));
//		_pgpKeyIds = null;
//	}

	public UserRepoKey getRandomUserRepoKey(final UUID repositoryId) {
		final List<UserRepoKey> list = getUserRepoKeyList(repositoryId);
		if (list.isEmpty())
			return null;

		final UserRepoKey userRepoKey = list.get(random.nextInt(list.size()));
		return userRepoKey;
	}

	public UserRepoKey getRandomUserRepoKeyOrFail(final UUID repositoryId) {
		final UserRepoKey userRepoKey = getRandomUserRepoKey(repositoryId);
		if (userRepoKey == null)
			throw new IllegalStateException(String.format("This UserRepoKeyRing does not contain any entry for repositoryId=%s!", repositoryId));

		return userRepoKey;
	}

	public synchronized void addUserRepoKey(final UserRepoKey userRepoKey) {
		assertNotNull("userRepoKey", userRepoKey);
		userRepoKeyId2UserRepoKey.put(userRepoKey.getUserRepoKeyId(), userRepoKey);
		repositoryId2userRepoKeyList.remove(userRepoKey.getRepositoryId());
	}

	public void removeUserRepoKey(final UserRepoKey userRepoKey) {
		removeUserRepoKey(assertNotNull("userRepoKey", userRepoKey).getUserRepoKeyId());
	}

	public synchronized void removeUserRepoKey(final Uid userRepoKeyId) {
		final UserRepoKey userRepoKey = userRepoKeyId2UserRepoKey.remove(assertNotNull("userRepoKeyId", userRepoKeyId));
		if (userRepoKey != null)
			repositoryId2userRepoKeyList.remove(userRepoKey.getRepositoryId());
	}

	public synchronized UserRepoKey getUserRepoKey(final Uid userRepoKeyId) {
		return userRepoKeyId2UserRepoKey.get(assertNotNull("userRepoKeyId", userRepoKeyId));
	}

	public UserRepoKey getUserRepoKeyOrFail(final Uid userRepoKeyId) {
		final UserRepoKey userRepoKey = getUserRepoKey(userRepoKeyId);
		if (userRepoKey == null)
			throw new IllegalStateException(String.format("There is no UserRepoKey with userRepoKeyId='%s'!", userRepoKeyId));

		return userRepoKey;
	}
}
