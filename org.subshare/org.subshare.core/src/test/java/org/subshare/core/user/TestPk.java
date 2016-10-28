package org.subshare.core.user;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

class TestPk implements UserRepoKey.PublicKeyWithSignature {
	private final Uid userRepoKeyId;
	private final UUID serverRepositoryId;

	public TestPk(Uid userRepoKeyId, UUID serverRepositoryId) {
		this.userRepoKeyId = userRepoKeyId;
		this.serverRepositoryId = serverRepositoryId;
	}

	@Override
	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	@Override
	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}

	@Override
	public AsymmetricKeyParameter getPublicKey() {
		return null;
	}

	@Override
	public Date getValidTo() {
		return null;
	}

	@Override
	public boolean isInvitation() {
		return false;
	}

	@Override
	public String getSignedDataType() {
		return null;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	@Override
	public InputStream getSignedData(int signedDataVersion) {
		return null;
	}

	@Override
	public Signature getSignature() {
		return null;
	}

	@Override
	public void setSignature(Signature signature) {
	}

	@Override
	public byte[] getSignedPublicKeyData() {
		return null;
	}

	@Override
	public byte[] getPublicKeyData() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), userRepoKeyId);
	}
}