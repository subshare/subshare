package org.subshare.core.user;

import java.util.Date;
import java.util.UUID;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.subshare.core.sign.Signable;

import co.codewizards.cloudstore.core.Uid;

/**
 * A user's master-key for one single repository.
 * <p>
 * In order to prevent the provider from knowing who has written what and who has access to what, every
 * user might indeed have multiple such keys per repository. The provider will only know the public keys,
 * but he does not know who owns them - not even how many users there really are.
 * <p>
 * Since the {@link #getSignedPublicKeyData() signedPublicKeyData} reveals the identity of the owner,
 * the {@link PublicKey} does not contain it, but only non-signed public-key-data!
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface UserRepoKey {

	Uid getUserRepoKeyId();

	UUID getServerRepositoryId();

	AsymmetricCipherKeyPair getKeyPair();

	PublicKeyWithSignature getPublicKey();

	boolean isInvitation();

	Date getValidTo();

	byte[] getEncryptedSignedPrivateKeyData();

	byte[] getSignedPublicKeyData();

	public static interface PublicKey {

		Uid getUserRepoKeyId();

		UUID getServerRepositoryId();

		AsymmetricKeyParameter getPublicKey();

		Date getValidTo();

		boolean isInvitation();
	}

	public static interface PublicKeyWithSignature extends PublicKey, Signable {
		byte[] getSignedPublicKeyData();

		byte[] getPublicKeyData();
	}
}
