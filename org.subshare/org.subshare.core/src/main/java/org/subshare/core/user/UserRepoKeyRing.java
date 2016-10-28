package org.subshare.core.user;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface UserRepoKeyRing extends Cloneable {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		userRepoKeys
	}

	Collection<UserRepoKey> getUserRepoKeys();

	List<UserRepoKey> getInvitationUserRepoKeys(final UUID serverRepositoryId);

	List<UserRepoKey> getPermanentUserRepoKeys(final UUID serverRepositoryId);

	void addUserRepoKey(final UserRepoKey userRepoKey);

	void removeUserRepoKey(final UserRepoKey userRepoKey);

	void removeUserRepoKey(final Uid userRepoKeyId);

	UserRepoKey getUserRepoKey(final Uid userRepoKeyId);

	UserRepoKey getUserRepoKeyOrFail(final Uid userRepoKeyId);

	List<UserRepoKey> getUserRepoKeys(UUID serverRepositoryId);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	UserRepoKeyRing clone();
}
