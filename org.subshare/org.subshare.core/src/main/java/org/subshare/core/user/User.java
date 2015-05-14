package org.subshare.core.user;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.Uid;

public interface User extends Cloneable {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		userId,
		firstName,
		lastName,
		emails,
		userRepoKeyRing,
		pgpKeyIds,
		userRepoKeyPublicKeys
	}

	Uid getUserId();
	void setUserId(Uid userId);

	String getFirstName();
	void setFirstName(final String firstName);

	String getLastName();
	void setLastName(final String lastName);

	List<String> getEmails();

	List<PgpKeyId> getPgpKeyIds();

	UserRepoKeyRing getUserRepoKeyRing();
	void setUserRepoKeyRing(final UserRepoKeyRing userRepoKeyRing);
	UserRepoKeyRing getUserRepoKeyRingOrCreate();
	UserRepoKey createUserRepoKey(final UUID serverRepositoryId);

	UserRepoKey createInvitationUserRepoKey(final User invitedUser, final UUID serverRepositoryId, final long validityDurationMillis);

	Set<PgpKey> getPgpKeys();
	PgpKey getPgpKeyContainingPrivateKeyOrFail();
	PgpKey getPgpKeyContainingPrivateKey();
	List<UserRepoKey.PublicKeyWithSignature> getUserRepoKeyPublicKeys();

	void addPropertyChangeListener(PropertyChangeListener listener);
	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);
	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	User clone();
}
