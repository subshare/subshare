package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static java.util.Objects.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.observable.ModificationEventType;
import org.subshare.core.observable.ObservableList;
import org.subshare.core.observable.ObservableSet;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.observable.standard.StandardPreModificationEvent;
import org.subshare.core.observable.standard.StandardPreModificationListener;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.sign.SignableSigner;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.bean.AbstractBean;

public class UserImpl extends AbstractBean<User.Property> implements User {

	private static final Logger logger = LoggerFactory.getLogger(UserImpl.class);

	public UserImpl() { }

	private Uid userId;

	private String firstName;

	private String lastName;

	private ObservableList<String> emails;

	private UserRepoKeyRing userRepoKeyRing;

	private ObservableList<PgpKeyId> pgpKeyIds;

	private ObservableSet<UserRepoKey.PublicKeyWithSignature> userRepoKeyPublicKeys;

	private final UserRepoKeyRingChangeListener userRepoKeyRingChangeListener = new UserRepoKeyRingChangeListener();

	private Date changed = now();

	private class PostModificationListener implements StandardPostModificationListener {
		private final Property property;

		public PostModificationListener(Property property) {
			this.property = requireNonNull(property, "property");
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

	private class PreventUserRepoKeyRingAndPublicKeysCollisionListener implements StandardPreModificationListener {
		@Override
		public void modificationOccurring(StandardPreModificationEvent event) {
			if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
				if (! event.getChangeCollection().isEmpty() && getUserRepoKeyRing() != null)
					throw new IllegalStateException(String.format(
							"%s already has a userRepoKeyRing! Cannot add public keys! Either there is a userRepoKeyRing or there are public keys! There cannot be both! userRepoKeyRing=%s, userRepoKeyPublicKeys=%s, event.changeCollection=%s",
							UserImpl.this, getUserRepoKeyRing(), getUserRepoKeyPublicKeys(), event.getChangeCollection()));
			}
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
				throw new IllegalStateException(String.format(
						"%s already has public keys! Cannot create a userRepoKeyRing! Either there is a userRepoKeyRing or there are public keys! There cannot be both! userRepoKeyPublicKeys=%s, userRepoKeyRing=%s",
						this, getUserRepoKeyPublicKeys(), getUserRepoKeyRing()));

			userRepoKeyRing = getUserRepoKeyRing();
			if (userRepoKeyRing == null) {
				created = true;
				userRepoKeyRing = new UserRepoKeyRingImpl();
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
		requireNonNull(serverRepositoryId, "serverRepositoryId");

		final PgpKey pgpKey = getPgpKeyContainingSecretKeyOrFail();

		final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
		final UserRepoKey userRepoKey = new UserRepoKeyImpl(serverRepositoryId, keyPair, Collections.singleton(pgpKey), pgpKey, null);
		logger.debug("createUserRepoKey: pgpKey={}, userRepoKey={}", pgpKey, userRepoKey);

		final UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRingOrCreate();
		userRepoKeyRing.addUserRepoKey(userRepoKey);
		return userRepoKey;
	}

	@Override
	public UserRepoKey createInvitationUserRepoKey(final User invitedUser, final UUID serverRepositoryId, final long validityDurationMillis) {
		requireNonNull(invitedUser, "invitedUser");
		requireNonNull(serverRepositoryId, "serverRepositoryId");

		final PgpKey ownPgpKey = getPgpKeyContainingSecretKeyOrFail();

		if (invitedUser.getPgpKeyIds().isEmpty())
			throw new IllegalStateException("There is no PGP key associated with the invited user!");

		if (invitedUser.getPgpKeys().isEmpty())
			throw new IllegalStateException("None of the PGP keys associated with the invited user is available in our PGP key ring!");

		final Set<PgpKey> invitedUserPgpKeys = invitedUser.getValidPgpKeys();
		if (invitedUserPgpKeys.isEmpty())
			throw new IllegalStateException("All PGP keys associated with the invited user and available in our PGP key ring are revoked or expired!");

		final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();
		final UserRepoKey userRepoKey = new UserRepoKeyImpl(serverRepositoryId, keyPair, invitedUserPgpKeys, ownPgpKey, new Date(System.currentTimeMillis() + validityDurationMillis));
		logger.debug("createInvitationUserRepoKey: ownPgpKey={}, userRepoKey={}", ownPgpKey, userRepoKey);
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
		final Date now = now();
		final Set<PgpKey> result = new HashSet<PgpKey>(pgpKeys.size());
		for (PgpKey pgpKey : pgpKeys) {
			if (pgpKey.isValid(now))
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
	public Set<UserRepoKey.PublicKeyWithSignature> getUserRepoKeyPublicKeys() {
		if (userRepoKeyPublicKeys == null) {
			userRepoKeyPublicKeys = ObservableSet.decorate(new CopyOnWriteArraySet<UserRepoKey.PublicKeyWithSignature>());
			userRepoKeyPublicKeys.getHandler().addPreModificationListener(new PreventUserRepoKeyRingAndPublicKeysCollisionListener());
			userRepoKeyPublicKeys.getHandler().addPostModificationListener(new PostModificationListener(PropertyEnum.userRepoKeyPublicKeys));
		}
		return userRepoKeyPublicKeys;
	}

	@Override
	public List<UserRepoKey.PublicKeyWithSignature> getUserRepoKeyPublicKeys(final UUID serverRepositoryId) {
		List<UserRepoKey.PublicKeyWithSignature> result = new ArrayList<>();
		for (final UserRepoKey.PublicKeyWithSignature publicKey : getUserRepoKeyPublicKeys()) {
			if (serverRepositoryId == null || serverRepositoryId.equals(publicKey.getServerRepositoryId()))
				result.add(publicKey);
		}
		result = Collections.unmodifiableList(result);
		return result;
	}

	@Override
	public Date getChanged() {
		return changed;
	}
	@Override
	public void setChanged(final Date changed) {
		requireNonNull(changed, "changed");
		setPropertyValue(PropertyEnum.changed, changed);
	}

	protected void updateChanged() {
		setChanged(now());
	}

	@Override
	public String toString() {
		return String.format("%s[%s, %s, %s, %s, %s]", getClass().getSimpleName(), userId, firstName, lastName, emails, pgpKeyIds);
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
