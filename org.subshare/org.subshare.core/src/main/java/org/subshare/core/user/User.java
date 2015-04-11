package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.observable.ObservableList;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.Uid;

public class User {

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

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

	public User() { }

	private Uid userId;

	private String firstName;

	private String lastName;

	private ObservableList<String> emails;

	private UserRepoKeyRing userRepoKeyRing;

	private ObservableList<Long> pgpKeyIds;

	private ObservableList<UserRepoKey.PublicKeyWithSignature> userRepoKeyPublicKeys;

	private class PostModificationListener implements StandardPostModificationListener {
		private final Property property;

		public PostModificationListener(Property property) {
			this.property = assertNotNull("property", property);
		}

		@Override
		public void modificationOccurred(StandardPostModificationEvent event) {
			firePropertyChange(property, null, event.getObservedCollection());
		}
	};

	public synchronized Uid getUserId() {
		return userId;
	}
	public synchronized void setUserId(Uid userId) {
		if (this.userId != null && !this.userId.equals(userId))
			throw new IllegalStateException("this.userId is already assigned! Cannot modify afterwards!");

		this.userId = userId;
	}

	public synchronized String getFirstName() {
		return firstName;
	}

	public synchronized void setFirstName(final String firstName) {
		final String old = this.firstName;
		this.firstName = firstName;
		firePropertyChange(PropertyEnum.firstName, old, firstName);
	}

	public synchronized String getLastName() {
		return lastName;
	}

	public synchronized void setLastName(final String lastName) {
		final String old = this.lastName;
		this.lastName = lastName;
		firePropertyChange(PropertyEnum.lastName, old, lastName);
	}

	public synchronized List<String> getEmails() {
		if (emails == null) {
			emails = ObservableList.decorate(new CopyOnWriteArrayList<String>());
			emails.getHandler().addPostModificationListener(new PostModificationListener(PropertyEnum.emails));
		}
		return emails;
	}

	public synchronized List<Long> getPgpKeyIds() {
		if (pgpKeyIds == null) {
			pgpKeyIds = ObservableList.decorate(new CopyOnWriteArrayList<Long>());
			pgpKeyIds.getHandler().addPostModificationListener(new PostModificationListener(PropertyEnum.pgpKeyIds));
		}
		return pgpKeyIds;
	}

	public synchronized UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}
	public synchronized void setUserRepoKeyRing(final UserRepoKeyRing userRepoKeyRing) {
		final UserRepoKeyRing old = this.userRepoKeyRing;
		this.userRepoKeyRing = userRepoKeyRing;
		firePropertyChange(PropertyEnum.userRepoKeyRing, old, userRepoKeyRing);
	}
	public synchronized UserRepoKeyRing getUserRepoKeyRingOrCreate() {
		if (! getUserRepoKeyPublicKeys().isEmpty())
			throw new IllegalStateException("There are public keys! Either there is a userRepoKeyRing or there are public keys! There cannot be both!");

		UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRing();
		if (userRepoKeyRing == null) {
			userRepoKeyRing = new UserRepoKeyRing();
			setUserRepoKeyRing(userRepoKeyRing);
		}
		return userRepoKeyRing;
	}

	public UserRepoKey createUserRepoKey(final UUID serverRepositoryId) {
		assertNotNull("serverRepositoryId", serverRepositoryId);

		final PgpKey pgpKey = getPgpKeyContainingPrivateKeyOrFail();

		final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
		final UserRepoKey userRepoKey = new UserRepoKey(serverRepositoryId, keyPair, Collections.singleton(pgpKey), pgpKey, null);

		final UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRingOrCreate();
		userRepoKeyRing.addUserRepoKey(userRepoKey);
		return userRepoKey;
	}

	public UserRepoKey createInvitationUserRepoKey(final User invitedUser, final UUID serverRepositoryId, final long validityDurationMillis) {
		assertNotNull("invitedUser", invitedUser);
		assertNotNull("serverRepositoryId", serverRepositoryId);

		final PgpKey ownPgpKey = getPgpKeyContainingPrivateKeyOrFail();

		if (invitedUser.getPgpKeyIds().isEmpty())
			throw new IllegalStateException("There is no PGP key associated with the invitedUser!");

		final Set<PgpKey> invitedUserPgpKeys = invitedUser.getPgpKeys();
		if (invitedUserPgpKeys.isEmpty())
			throw new IllegalStateException("None of the PGP keys associated with the invitedUser is available in our PGP key ring!");

		final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
		final UserRepoKey userRepoKey = new UserRepoKey(serverRepositoryId, keyPair, invitedUserPgpKeys, ownPgpKey, new Date(System.currentTimeMillis() + validityDurationMillis));
		return userRepoKey;
	}

	public Set<PgpKey> getPgpKeys() {
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		final Set<PgpKey> pgpKeys = new HashSet<PgpKey>(getPgpKeyIds().size());
		for (final Long pgpKeyId : getPgpKeyIds()) {
			final PgpKey k = pgp.getPgpKey(pgpKeyId);
			// TODO we should exclude disabled/expired keys here (or already earlier and make sure they're not in User.pgpKeyIds).
			if (k != null)
				pgpKeys.add(k);
		}
		return pgpKeys;
	}

	public PgpKey getPgpKeyContainingPrivateKeyOrFail() {
		final List<Long> pgpKeyIds = getPgpKeyIds();

		if (pgpKeyIds.isEmpty())
			throw new IllegalStateException(String.format("There is no PGP key associated with %s!", this));

		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		PgpKey pgpKey = null;
		for (final Long pgpKeyId : pgpKeyIds) {
			final PgpKey k = pgp.getPgpKey(pgpKeyId);
			if (k != null && k.isPrivateKeyAvailable()) {
				pgpKey = k;
				break;
			}
		}

		if (pgpKey == null)
			throw new IllegalStateException(String.format("None of the PGP keys associated with %s has a private key available!", this));

		return pgpKey;
	}

	public List<UserRepoKey.PublicKeyWithSignature> getUserRepoKeyPublicKeys() {
		if (userRepoKeyPublicKeys == null) {
			userRepoKeyPublicKeys = ObservableList.decorate(new CopyOnWriteArrayList<UserRepoKey.PublicKeyWithSignature>());
			userRepoKeyPublicKeys.getHandler().addPostModificationListener(new PostModificationListener(PropertyEnum.userRepoKeyPublicKeys));
		}
		return userRepoKeyPublicKeys;
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
	public String toString() {
		return String.format("%s[%s, %s, %s, %s]", getClass().getSimpleName(), userId, firstName, lastName, emails);
	}
}
