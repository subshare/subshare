package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
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

import co.codewizards.cloudstore.core.bean.AbstractBean;
import co.codewizards.cloudstore.core.dto.Uid;

public class UserImpl extends AbstractBean<User.Property> implements User {

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
		if (userId == null)
			userId = new Uid();

		return userId;
	}
	@Override
	public void setUserId(Uid userId) {
		synchronized (this) {
			if (this.userId != null && !this.userId.equals(userId))
				throw new IllegalStateException("this.userId is already assigned! Cannot modify afterwards!");
		}
		setPropertyValue(PropertyEnum.userId, userId); // outside of synchronized block, because it fires events (and synchronizes itself)!
		updateChanged();
	}

	@Override
	public synchronized String getFirstName() {
		return firstName;
	}

	@Override
	public void setFirstName(final String firstName) {
		setPropertyValue(PropertyEnum.firstName, firstName);
		updateChanged();
	}

	@Override
	public synchronized String getLastName() {
		return lastName;
	}

	@Override
	public void setLastName(final String lastName) {
		setPropertyValue(PropertyEnum.lastName, lastName);
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
	public void setUserRepoKeyRing(final UserRepoKeyRing userRepoKeyRing) {
		final UserRepoKeyRing old = _setUserRepoKeyRing(userRepoKeyRing);
		firePropertyChange(PropertyEnum.userRepoKeyRing, old, userRepoKeyRing);
		updateChanged();
	}

	protected synchronized UserRepoKeyRing _setUserRepoKeyRing(final UserRepoKeyRing userRepoKeyRing) {
		final UserRepoKeyRing old = this.userRepoKeyRing;
		this.userRepoKeyRing = userRepoKeyRing;

		if (old != null)
			old.removePropertyChangeListener(userRepoKeyRingChangeListener);

		if (userRepoKeyRing != null)
			userRepoKeyRing.addPropertyChangeListener(userRepoKeyRingChangeListener);

		return old;
	}

	@Override
	public UserRepoKeyRing getUserRepoKeyRingOrCreate() {
		boolean created = false;
		UserRepoKeyRing userRepoKeyRing;
		synchronized (this) {
			if (! getUserRepoKeyPublicKeys().isEmpty())
				throw new IllegalStateException(String.format("%s has public keys! Either there is a userRepoKeyRing or there are public keys! There cannot be both!", this));

			userRepoKeyRing = getUserRepoKeyRing();
			if (userRepoKeyRing == null) {
				created = true;
				userRepoKeyRing = new UserRepoKeyRing();
				_setUserRepoKeyRing(userRepoKeyRing);
			}
		}
		if (created) {
			firePropertyChange(PropertyEnum.userRepoKeyRing, null, userRepoKeyRing);
			updateChanged();
		}
		return userRepoKeyRing;
	}

	@Override
	public UserRepoKey createUserRepoKey(final UUID serverRepositoryId) {
		assertNotNull("serverRepositoryId", serverRepositoryId);

		final PgpKey pgpKey = getPgpKeyContainingSecretKeyOrFail();

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

		final PgpKey ownPgpKey = getPgpKeyContainingSecretKeyOrFail();

		if (invitedUser.getPgpKeyIds().isEmpty())
			throw new IllegalStateException("There is no PGP key associated with the invited user!");

		if (invitedUser.getPgpKeys().isEmpty())
			throw new IllegalStateException("None of the PGP keys associated with the invited user is available in our PGP key ring!");

		final Set<PgpKey> invitedUserPgpKeys = invitedUser.getValidPgpKeys();
		if (invitedUserPgpKeys.isEmpty())
			throw new IllegalStateException("All PGP keys associated with the invited user and available in our PGP key ring are revoked or expired!");

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
			if (k != null)
				pgpKeys.add(k);
		}
		return Collections.unmodifiableSet(pgpKeys);
	}

	@Override
	public Set<PgpKey> getValidPgpKeys() {
		final Collection<? extends PgpKey> pgpKeys = getPgpKeys();
		final Date now = new Date();
		final Set<PgpKey> result = new HashSet<PgpKey>(pgpKeys.size());
		for (PgpKey pgpKey : pgpKeys) {
			if (pgpKey.isRevoked())
				continue;

			if (! pgpKey.isValid(now))
				continue;

			result.add(pgpKey);
		}
		return result;
	}

	@Override
	public PgpKey getPgpKeyContainingSecretKeyOrFail() {
		final PgpKey pgpKey = getPgpKeyContainingSecretKey();

		if (pgpKey == null)
			throw new IllegalStateException(String.format("None of the PGP keys associated with %s has a private key available!", this));

		return pgpKey;
	}

	@Override
	public PgpKey getPgpKeyContainingSecretKey() {
		final List<PgpKeyId> pgpKeyIds = getPgpKeyIds();

		if (pgpKeyIds.isEmpty())
			throw new IllegalStateException(String.format("There is no PGP key associated with %s!", this));

		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		PgpKey pgpKey = null;
		for (final PgpKeyId pgpKeyId : pgpKeyIds) {
			final PgpKey k = pgp.getPgpKey(pgpKeyId);
			if (k != null && k.isSecretKeyAvailable()) {
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
		setPropertyValue(PropertyEnum.changed, changed);
	}

	protected void updateChanged() {
		setChanged(new Date());
	}

	@Override
	public String toString() {
		return String.format("%s[%s, %s, %s, %s]", getClass().getSimpleName(), userId, firstName, lastName, emails);
	}

	@Override
	public UserImpl clone() {
		final UserImpl clone = (UserImpl) super.clone();

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
