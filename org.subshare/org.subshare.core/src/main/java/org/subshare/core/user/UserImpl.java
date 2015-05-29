package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
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
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.sign.SignableSigner;

import co.codewizards.cloudstore.core.dto.Uid;

public class UserImpl implements User {

	private /*final*cloned*/ PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public UserImpl() { }

	private Uid userId;

	private String firstName;

	private String lastName;

	private ObservableList<String> emails;

	private UserRepoKeyRing userRepoKeyRing;

	private ObservableList<PgpKeyId> pgpKeyIds;

	private ObservableList<UserRepoKey.PublicKeyWithSignature> userRepoKeyPublicKeys;

	private final UserRepoKeyRingChangeListener userRepoKeyRingChangeListener = new UserRepoKeyRingChangeListener();

	private Date changed = new Date();

	private class PostModificationListener implements StandardPostModificationListener {
		private final Property property;

		public PostModificationListener(Property property) {
			this.property = assertNotNull("property", property);
		}

		@Override
		public void modificationOccurred(StandardPostModificationEvent event) {
			firePropertyChange(property, null, event.getObservedCollection());
			updateChanged();
		}
	};

	private class UserRepoKeyRingChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			firePropertyChange(PropertyEnum.userRepoKeyRing, null, userRepoKeyRing);
		}
	}

	@Override
	public synchronized Uid getUserId() {
		return userId;
	}
	@Override
	public synchronized void setUserId(Uid userId) {
		if (this.userId != null && !this.userId.equals(userId))
			throw new IllegalStateException("this.userId is already assigned! Cannot modify afterwards!");

		this.userId = userId;
		updateChanged();
	}

	@Override
	public synchronized String getFirstName() {
		return firstName;
	}

	@Override
	public synchronized void setFirstName(final String firstName) {
		final String old = this.firstName;
		this.firstName = firstName;
		firePropertyChange(PropertyEnum.firstName, old, firstName);
		updateChanged();
	}

	@Override
	public synchronized String getLastName() {
		return lastName;
	}

	@Override
	public synchronized void setLastName(final String lastName) {
		final String old = this.lastName;
		this.lastName = lastName;
		firePropertyChange(PropertyEnum.lastName, old, lastName);
		updateChanged();
	}

	@Override
	public synchronized List<String> getEmails() {
		if (emails == null) {
			emails = ObservableList.decorate(new CopyOnWriteArrayList<String>());
			emails.getHandler().addPostModificationListener(new PostModificationListener(PropertyEnum.emails));
		}
		return emails;
	}

	@Override
	public synchronized List<PgpKeyId> getPgpKeyIds() {
		if (pgpKeyIds == null) {
			pgpKeyIds = ObservableList.decorate(new CopyOnWriteArrayList<PgpKeyId>());
			pgpKeyIds.getHandler().addPostModificationListener(new PostModificationListener(PropertyEnum.pgpKeyIds));
		}
		return pgpKeyIds;
	}

	@Override
	public synchronized UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}
	@Override
	public synchronized void setUserRepoKeyRing(final UserRepoKeyRing userRepoKeyRing) {
		final UserRepoKeyRing old = this.userRepoKeyRing;
		this.userRepoKeyRing = userRepoKeyRing;

		if (old != null)
			old.removePropertyChangeListener(userRepoKeyRingChangeListener);

		if (userRepoKeyRing != null)
			userRepoKeyRing.addPropertyChangeListener(userRepoKeyRingChangeListener);

		firePropertyChange(PropertyEnum.userRepoKeyRing, old, userRepoKeyRing);
		updateChanged();
	}
	@Override
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

	@Override
	public UserRepoKey createUserRepoKey(final UUID serverRepositoryId) {
		assertNotNull("serverRepositoryId", serverRepositoryId);

		final PgpKey pgpKey = getPgpKeyContainingPrivateKeyOrFail();

		final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
		final UserRepoKey userRepoKey = new UserRepoKey(serverRepositoryId, keyPair, Collections.singleton(pgpKey), pgpKey, null);

		final UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRingOrCreate();
		userRepoKeyRing.addUserRepoKey(userRepoKey);
		return userRepoKey;
	}

	@Override
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
		UserRepoKey signingUserRepoKey = getUserRepoKeyRing().getPermanentUserRepoKeys(serverRepositoryId).get(0);
		new SignableSigner(signingUserRepoKey).sign(userRepoKey.getPublicKey());
		return userRepoKey;
	}

	@Override
	public Set<PgpKey> getPgpKeys() {
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		final Set<PgpKey> pgpKeys = new HashSet<PgpKey>(getPgpKeyIds().size());
		for (final PgpKeyId pgpKeyId : getPgpKeyIds()) {
			final PgpKey k = pgp.getPgpKey(pgpKeyId);
			// TODO we should exclude disabled/expired keys here (or already earlier and make sure they're not in User.pgpKeyIds).
			if (k != null)
				pgpKeys.add(k);
		}
		return pgpKeys;
	}

	@Override
	public PgpKey getPgpKeyContainingPrivateKeyOrFail() {
		final PgpKey pgpKey = getPgpKeyContainingPrivateKey();

		if (pgpKey == null)
			throw new IllegalStateException(String.format("None of the PGP keys associated with %s has a private key available!", this));

		return pgpKey;
	}

	@Override
	public PgpKey getPgpKeyContainingPrivateKey() {
		final List<PgpKeyId> pgpKeyIds = getPgpKeyIds();

		if (pgpKeyIds.isEmpty())
			throw new IllegalStateException(String.format("There is no PGP key associated with %s!", this));

		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		PgpKey pgpKey = null;
		for (final PgpKeyId pgpKeyId : pgpKeyIds) {
			final PgpKey k = pgp.getPgpKey(pgpKeyId);
			if (k != null && k.isPrivateKeyAvailable()) {
				pgpKey = k;
				break;
			}
		}

		return pgpKey;
	}

	@Override
	public List<UserRepoKey.PublicKeyWithSignature> getUserRepoKeyPublicKeys() {
		if (userRepoKeyPublicKeys == null) {
			userRepoKeyPublicKeys = ObservableList.decorate(new CopyOnWriteArrayList<UserRepoKey.PublicKeyWithSignature>());
			userRepoKeyPublicKeys.getHandler().addPostModificationListener(new PostModificationListener(PropertyEnum.userRepoKeyPublicKeys));
		}
		return userRepoKeyPublicKeys;
	}

	@Override
	public Date getChanged() {
		return changed;
	}
	@Override
	public void setChanged(final Date changed) {
		assertNotNull("changed", changed);
		final Date old = this.changed;
		this.changed = changed;
		firePropertyChange(PropertyEnum.changed, old, changed);
	}

	protected void updateChanged() {
		setChanged(new Date());
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
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

	@Override
	public UserImpl clone() {
		final UserImpl clone;
		try {
			clone = (UserImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		clone.propertyChangeSupport = new PropertyChangeSupport(clone);

		if (clone.emails != null) {
			clone.emails = null;
			clone.getEmails().addAll(this.getEmails());
		}

		if (clone.userRepoKeyRing != null)
			clone.userRepoKeyRing = this.userRepoKeyRing.clone();

		if (clone.pgpKeyIds != null) {
			clone.pgpKeyIds = null;
			clone.getPgpKeyIds().addAll(this.getPgpKeyIds());
		}

		if (clone.userRepoKeyPublicKeys != null) {
			clone.userRepoKeyPublicKeys = null;
			clone.getUserRepoKeyPublicKeys().addAll(this.getUserRepoKeyPublicKeys());
		}
		return clone;
	}
}
