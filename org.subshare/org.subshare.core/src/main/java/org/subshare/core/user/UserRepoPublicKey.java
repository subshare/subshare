package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import co.codewizards.cloudstore.core.dto.Uid;

public class UserRepoPublicKey {

	private final UUID repositoryId;
	private final Uid userRepoKeyId;
	private final AsymmetricKeyParameter publicKey;

	public UserRepoPublicKey(final UUID repositoryId, final Uid userRepoKeyId, final AsymmetricKeyParameter publicKey) {
		this.repositoryId = assertNotNull("repositoryId", repositoryId);
		this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
		this.publicKey = assertNotNull("publicKey", publicKey);
	}

	public UUID getRepositoryId() {
		return repositoryId;
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	public AsymmetricKeyParameter getPublicKey() {
		return publicKey;
	}
}
