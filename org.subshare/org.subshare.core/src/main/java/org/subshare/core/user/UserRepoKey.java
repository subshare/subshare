package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.Util.*;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;

import co.codewizards.cloudstore.core.dto.Uid;

/**
 * A user's master-key for one single repository.
 * <p>
 * In order to prevent the provider from knowing who has written what and who has access to what, every
 * user might indeed have multiple such keys per repository. The provider will only know the public keys,
 * but he does not know who owns them - not even how many users there really are.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class UserRepoKey {

	private final UserRepoKeyRing userRepoKeyRing;
	private final Uid userRepoKeyId;
	private final AsymmetricCipherKeyPair keyPair;
	private UserRepoPublicKey userRepoPublicKey;

	public UserRepoKey(final UserRepoKeyRing userRepoKeyRing, final AsymmetricCipherKeyPair keyPair) {
		this.userRepoKeyRing = assertNotNull("userRepoKeyRing", userRepoKeyRing);
		this.userRepoKeyId = new Uid();
		this.keyPair = assertNotNull("keyPair", keyPair);
	}

	public UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	public AsymmetricCipherKeyPair getKeyPair() {
		return keyPair;
	}

	public UserRepoPublicKey getUserRepoPublicKey() {
		if (userRepoPublicKey == null)
			userRepoPublicKey = new UserRepoPublicKey(
					getUserRepoKeyRing().getRepositoryId(), getUserRepoKeyId(), getKeyPair().getPublic());

		return userRepoPublicKey;
	}
}
