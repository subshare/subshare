package org.subshare.core.user;

import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.bean.CloneableBean;
import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface User extends CloneableBean<User.Property> {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		userId,
		firstName,
		lastName,
		emails,
		userRepoKeyRing,
		pgpKeyIds,
		userRepoKeyPublicKeys,
		changed
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

	/**
	 * Gets the PGP keys associated with this user.
	 * @return the PGP keys associated with this user. Never <code>null</code>, but maybe empty.
	 * @see #getValidPgpKeys()
	 */
	Set<PgpKey> getPgpKeys();

	/**
	 * Gets the valid (neither revoked nor expired) PGP keys of this user.
	 * @return the valid (neither revoked nor expired) PGP keys of this user. Never <code>null</code>, but maybe empty.
	 * @see #getPgpKeys()
	 */
	Set<PgpKey> getValidPgpKeys();

	PgpKey getPgpKeyContainingSecretKeyOrFail();
	PgpKey getPgpKeyContainingSecretKey();
	Set<UserRepoKey.PublicKeyWithSignature> getUserRepoKeyPublicKeys();

	@Override
	void addPropertyChangeListener(PropertyChangeListener listener);
	@Override
	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(PropertyChangeListener listener);
	@Override
	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	@Override
	User clone();
	Date getChanged();
	void setChanged(Date changed);

	List<UserRepoKey.PublicKeyWithSignature> getUserRepoKeyPublicKeys(UUID serverRepositoryId);
}
