package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.Uid;

public class UserRepoKeyRing {

//	private static SecureRandom random = new SecureRandom();

	private final Map<Uid, UserRepoKey> userRepoKeyId2UserRepoKey = new HashMap<>();
	private final Map<UUID, List<UserRepoKey>> repositoryId2userRepoKeyList = new HashMap<>();

	public Collection<UserRepoKey> getUserRepoKeys() {
		return Collections.unmodifiableCollection(userRepoKeyId2UserRepoKey.values());
	}

	public List<UserRepoKey> getPermanentUserRepoKeys(final UUID serverRepositoryId) {
		return getPermanentUserRepoKeyList(serverRepositoryId);
	}

	protected synchronized List<UserRepoKey> getPermanentUserRepoKeyList(final UUID serverRepositoryId) {
		assertNotNull("repositoryId", serverRepositoryId);
		List<UserRepoKey> userRepoKeyList = repositoryId2userRepoKeyList.get(serverRepositoryId);

		if (userRepoKeyList == null) {
			List<UserRepoKey> l = filterByServerRepositoryId(userRepoKeyId2UserRepoKey.values(), serverRepositoryId);
			l = removeTemporaryUserRepoKeys(l);
			Collections.shuffle(l);
			userRepoKeyList = Collections.unmodifiableList(l);
			repositoryId2userRepoKeyList.put(serverRepositoryId, userRepoKeyList);
		}

		return userRepoKeyList;
	}

	protected List<UserRepoKey> filterByServerRepositoryId(Collection<UserRepoKey> userRepoKeys, final UUID serverRepositoryId) {
		final ArrayList<UserRepoKey> result = new ArrayList<UserRepoKey>(userRepoKeys.size());
		for (final UserRepoKey userRepoKey : userRepoKeys) {
			if (serverRepositoryId.equals(userRepoKey.getServerRepositoryId()))
				result.add(userRepoKey);
		}
//		result.trimToSize(); // filtered again, anyway => no need for this ;-)
		return result;
	}

	protected List<UserRepoKey> removeTemporaryUserRepoKeys(Collection<UserRepoKey> userRepoKeys) {
		final ArrayList<UserRepoKey> result = new ArrayList<UserRepoKey>(userRepoKeys.size());
		for (final UserRepoKey userRepoKey : userRepoKeys) {
			if (userRepoKey.getValidTo() == null)
				result.add(userRepoKey);
		}
		result.trimToSize();
		return result;
	}

	protected synchronized void shuffleUserRepoKeys(final UUID serverRepositoryId) {
		// The entries are shuffled in getUserRepoKeyList(...) - we thus simply clear this cache here.
		repositoryId2userRepoKeyList.remove(serverRepositoryId);
	}

//	public UserRepoKey getRandomUserRepoKey(final UUID serverRepositoryId) {
//		final List<UserRepoKey> list = getUserRepoKeyList(serverRepositoryId);
//		if (list.isEmpty())
//			return null;
//
//		final UserRepoKey userRepoKey = list.get(random.nextInt(list.size()));
//		return userRepoKey;
//	}
//
//	public UserRepoKey getRandomUserRepoKeyOrFail(final UUID serverRepositoryId) {
//		final UserRepoKey userRepoKey = getRandomUserRepoKey(serverRepositoryId);
//		if (userRepoKey == null)
//			throw new IllegalStateException(String.format("This UserRepoKeyRing does not contain any entry for repositoryId=%s!", serverRepositoryId));
//
//		return userRepoKey;
//	}

	public synchronized void addUserRepoKey(final UserRepoKey userRepoKey) {
		assertNotNull("userRepoKey", userRepoKey);
		userRepoKeyId2UserRepoKey.put(userRepoKey.getUserRepoKeyId(), userRepoKey);
		repositoryId2userRepoKeyList.remove(userRepoKey.getServerRepositoryId());
	}

	public void removeUserRepoKey(final UserRepoKey userRepoKey) {
		removeUserRepoKey(assertNotNull("userRepoKey", userRepoKey).getUserRepoKeyId());
	}

	public synchronized void removeUserRepoKey(final Uid userRepoKeyId) {
		final UserRepoKey userRepoKey = userRepoKeyId2UserRepoKey.remove(assertNotNull("userRepoKeyId", userRepoKeyId));
		if (userRepoKey != null)
			repositoryId2userRepoKeyList.remove(userRepoKey.getServerRepositoryId());
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
