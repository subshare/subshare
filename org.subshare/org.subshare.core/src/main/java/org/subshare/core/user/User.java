package org.subshare.core.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;

public class User {

	public User() { }

	private String firstName;

	private String lastName;

	private List<String> emails;

	private UserRepoKeyRing userRepoKeyRing;

	private List<Long> pgpKeyIds;

	private Collection<UserRepoKey.PublicKey> userRepoKeyPublicKeys;

	public synchronized String getFirstName() {
		return firstName;
	}

	public synchronized void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public synchronized String getLastName() {
		return lastName;
	}

	public synchronized void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public synchronized List<String> getEmails() {
		if (emails == null)
			emails = new CopyOnWriteArrayList<String>();

		return emails;
	}

	public synchronized List<Long> getPgpKeyIds() {
		if (pgpKeyIds == null)
			pgpKeyIds = new CopyOnWriteArrayList<Long>();

		return pgpKeyIds;
	}

	public synchronized UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}
	public synchronized void setUserRepoKeyRing(final UserRepoKeyRing userRepoKeyRing) {
		this.userRepoKeyRing = userRepoKeyRing;
	}
	public synchronized UserRepoKeyRing getUserRepoKeyRingOrCreate() {
		if (! getUserRepoKeyPublicKeys().isEmpty())
			throw new IllegalStateException("There are public keys! Either there is a userRepoKeyRing or there are public keys! There cannot be both!");

		if (userRepoKeyRing == null)
			userRepoKeyRing = new UserRepoKeyRing();

		return userRepoKeyRing;
	}

	public UserRepoKey createUserRepoKey(final UUID repositoryId) {
		final List<Long> pgpKeyIds = getPgpKeyIds();

		if (pgpKeyIds.isEmpty())
			throw new IllegalStateException("There is no PGP key associated with this user!");

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
			throw new IllegalStateException("None of the PGP keys associated with this user has a private key available!");

		final UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRingOrCreate();
		final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();


		final UserRepoKey userRepoKey = new UserRepoKey(userRepoKeyRing, repositoryId, keyPair, pgpKey);
		userRepoKeyRing.addUserRepoKey(userRepoKey);
		return userRepoKey;
	}

	public Collection<UserRepoKey.PublicKey> getUserRepoKeyPublicKeys() {
		if (userRepoKeyPublicKeys == null)
			userRepoKeyPublicKeys = new ArrayList<UserRepoKey.PublicKey>();

		return Collections.unmodifiableCollection(userRepoKeyPublicKeys);
	}

}
