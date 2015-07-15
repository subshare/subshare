package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.Uid;

public class UserRepoKeyRing implements Cloneable {

	private /*final*cloned*/ PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		userRepoKeys
	}

	private /*final*cloned*/ Map<Uid, UserRepoKey> userRepoKeyId2UserRepoKey = new HashMap<>();
	private /*final*cloned*/ Map<UUID, List<UserRepoKey>> repositoryId2InvitationUserRepoKeyList = new HashMap<>();
	private /*final*cloned*/ Map<UUID, List<UserRepoKey>> repositoryId2PermanentUserRepoKeyList = new HashMap<>();

	public Collection<UserRepoKey> getUserRepoKeys() {
		return Collections.unmodifiableCollection(userRepoKeyId2UserRepoKey.values());
	}

	public List<UserRepoKey> getInvitationUserRepoKeys(final UUID serverRepositoryId) {
		return getUserRepoKeyList(repositoryId2InvitationUserRepoKeyList, serverRepositoryId);
	}

	public List<UserRepoKey> getPermanentUserRepoKeys(final UUID serverRepositoryId) {
		return getUserRepoKeyList(repositoryId2PermanentUserRepoKeyList, serverRepositoryId);
	}

	protected synchronized List<UserRepoKey> getUserRepoKeyList(final Map<UUID, List<UserRepoKey>> repositoryId2UserRepoKeyList, final UUID serverRepositoryId) {
		assertNotNull("repositoryId", serverRepositoryId);
		List<UserRepoKey> userRepoKeyList = repositoryId2UserRepoKeyList.get(serverRepositoryId);

		if (userRepoKeyList == null) {
			final boolean invitation;
			if (repositoryId2PermanentUserRepoKeyList == repositoryId2UserRepoKeyList)
				invitation = false;
			else if (repositoryId2InvitationUserRepoKeyList == repositoryId2UserRepoKeyList)
				invitation = true;
			else
				throw new IllegalArgumentException("repositoryId2UserRepoKeyList unexpected!");

			List<UserRepoKey> l = filterByServerRepositoryId(userRepoKeyId2UserRepoKey.values(), serverRepositoryId);
			l = filterInvitationUserRepoKeys(l, invitation);
			Collections.shuffle(l);
			userRepoKeyList = Collections.unmodifiableList(l);
			repositoryId2UserRepoKeyList.put(serverRepositoryId, userRepoKeyList);
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

	protected List<UserRepoKey> filterInvitationUserRepoKeys(Collection<UserRepoKey> userRepoKeys, boolean invitation) {
		final ArrayList<UserRepoKey> result = new ArrayList<UserRepoKey>(userRepoKeys.size());
		for (final UserRepoKey userRepoKey : userRepoKeys) {
			if (invitation == userRepoKey.isInvitation())
				result.add(userRepoKey);
		}
		result.trimToSize();
		return result;
	}

//	protected synchronized void shuffleUserRepoKeys(final UUID serverRepositoryId) {
//		// The entries are shuffled in getUserRepoKeyList(...) - we thus simply clear this cache here.
//		repositoryId2PermanentUserRepoKeyList.remove(serverRepositoryId);
//	}

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
		clearCache(userRepoKey.getServerRepositoryId());
		firePropertyChange(PropertyEnum.userRepoKeys, null, getUserRepoKeys());
	}

	public void removeUserRepoKey(final UserRepoKey userRepoKey) {
		removeUserRepoKey(assertNotNull("userRepoKey", userRepoKey).getUserRepoKeyId());
	}

	public synchronized void removeUserRepoKey(final Uid userRepoKeyId) {
		final UserRepoKey userRepoKey = userRepoKeyId2UserRepoKey.remove(assertNotNull("userRepoKeyId", userRepoKeyId));
		if (userRepoKey != null) {
			clearCache(userRepoKey.getServerRepositoryId());
			firePropertyChange(PropertyEnum.userRepoKeys, null, getUserRepoKeys());
		}
	}

	private void clearCache(final UUID serverRepositoryId) {
		assertNotNull("serverRepositoryId", serverRepositoryId);
		repositoryId2PermanentUserRepoKeyList.remove(serverRepositoryId);
		repositoryId2InvitationUserRepoKeyList.remove(serverRepositoryId);
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

	public synchronized List<UserRepoKey> getUserRepoKeys(UUID serverRepositoryId) {
		// no need to cache - very rarely used (currently only in tests AFAIK)
		final List<UserRepoKey> l = filterByServerRepositoryId(userRepoKeyId2UserRepoKey.values(), serverRepositoryId);
		Collections.shuffle(l);
		return Collections.unmodifiableList(l);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}

	@Override
	public UserRepoKeyRing clone() {
		final UserRepoKeyRing clone;
		try {
			clone = (UserRepoKeyRing) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		clone.propertyChangeSupport = new PropertyChangeSupport(clone);

		clone.userRepoKeyId2UserRepoKey = new HashMap<>();
		clone.userRepoKeyId2UserRepoKey.putAll(this.userRepoKeyId2UserRepoKey); // content is immutable => no need to clone even deeper

		clone.repositoryId2InvitationUserRepoKeyList = new HashMap<>(); // only a cache
		clone.repositoryId2PermanentUserRepoKeyList = new HashMap<>(); // only a cache
		return clone;
	}
}
