package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import org.subshare.core.user.UserRepoKey;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="UserRepoKeyPublicKey_userRepoKeyId", members="userRepoKeyId")
@Index(name="UserRepoKeyPublicKey_localRevision", members="localRevision")
@Queries({
	@Query(name="getUserRepoKeyPublicKey_userRepoKeyId", value="SELECT UNIQUE WHERE this.userRepoKeyId == :userRepoKeyId"),
	@Query(name="getUserRepoKeyPublicKeysChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class UserRepoKeyPublicKey extends Entity implements AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String userRepoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=36)
	private String repositoryId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] publicKeyData;

	private long localRevision;

	@NotPersistent
	private UserRepoKey.PublicKey publicKey;

	public UserRepoKeyPublicKey() { }

	public UserRepoKeyPublicKey(final Uid userRepoKeyId) {
		setUserRepoKeyId(userRepoKeyId);
	}

	public UserRepoKeyPublicKey(final UserRepoKey.PublicKey publicKey) {
		assertNotNull("publicKey", publicKey);
		this.publicKey = publicKey;
		setUserRepoKeyId(publicKey.getUserRepoKeyId());
		setRepositoryId(publicKey.getRepositoryId());
		setPublicKeyData(CryptoRegistry.getInstance().encodePublicKey(publicKey.getPublicKey()));
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId == null ? null : new Uid(userRepoKeyId);
	}
	protected void setUserRepoKeyId(final Uid userRepoKeyId) {
		if (! equal(this.getUserRepoKeyId(), userRepoKeyId))
			this.userRepoKeyId = userRepoKeyId == null ? null : userRepoKeyId.toString();
	}

	public UUID getRepositoryId() {
		return repositoryId == null ? null : UUID.fromString(repositoryId);
	}
	public void setRepositoryId(final UUID repositoryId) {
		if (! equal(this.getRepositoryId(), repositoryId))
			this.repositoryId = repositoryId == null ? null : repositoryId.toString();
	}
	public byte[] getPublicKeyData() {
		return publicKeyData;
	}
	public void setPublicKeyData(final byte[] publicKeyData) {
		if (! equal(this.publicKeyData, publicKeyData))
			this.publicKeyData = publicKeyData;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (! equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}

	public UserRepoKey.PublicKey getPublicKey() {
		if (publicKey == null)
			try {
				publicKey = new UserRepoKey.PublicKey(
						getUserRepoKeyId(), getRepositoryId(),
						CryptoRegistry.getInstance().decodePublicKey(getPublicKeyData()));
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}

		return publicKey;
	}
}
