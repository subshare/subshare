package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

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
	private PublicKey publicKey;

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

	public PublicKey getPublicKey() {
		if (publicKey == null)
			publicKey = new PublicKey(
					getUserRepoKeyId(), getUserRepoKeyRing().getRepositoryId(), getKeyPair().getPublic());

		return publicKey;
	}

	public static class PublicKey {

		private final Uid userRepoKeyId;
		private final UUID repositoryId;
		private final AsymmetricKeyParameter publicKey;

		public PublicKey(final Uid userRepoKeyId, final UUID repositoryId, final AsymmetricKeyParameter publicKey) {
			this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
			this.repositoryId = assertNotNull("repositoryId", repositoryId);
			this.publicKey = assertNotNull("publicKey", publicKey);
		}

		public Uid getUserRepoKeyId() {
			return userRepoKeyId;
		}

		public UUID getRepositoryId() {
			return repositoryId;
		}

		public AsymmetricKeyParameter getPublicKey() {
			return publicKey;
		}
	}
}
