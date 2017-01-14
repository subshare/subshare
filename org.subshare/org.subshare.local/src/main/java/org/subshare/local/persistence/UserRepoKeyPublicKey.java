package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
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

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyImpl;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="UserRepoKeyPublicKey")
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="UserRepoKeyPublicKey_userRepoKeyId", members="userRepoKeyId")
@Index(name="UserRepoKeyPublicKey_localRevision", members="localRevision")
@Queries({
	@Query(name="getUserRepoKeyPublicKey_userRepoKeyId", value="SELECT UNIQUE WHERE this.userRepoKeyId == :userRepoKeyId"),
	@Query(name="getUserRepoKeyPublicKeysChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class UserRepoKeyPublicKey extends Entity implements AutoTrackLocalRevision {

	// TODO 2015-04-08 - I think this should be signed by whoever (another UserRepoKeyPublicKey) causes this entity to be written into the DB.
	// Or does this reveal too much data? How can we prevent this from being corrupted otherwise?

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String userRepoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=36)
	private String serverRepositoryId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="BLOB")
	private byte[] publicKeyData;

	private long localRevision;

	@NotPersistent
	private UserRepoKey.PublicKey publicKey;

	public UserRepoKeyPublicKey() { }

	public UserRepoKeyPublicKey(final Uid userRepoKeyId) {
		setUserRepoKeyId(userRepoKeyId);
	}

	public UserRepoKeyPublicKey(final UserRepoKey.PublicKey publicKey) {
		assertNotNull(publicKey, "publicKey");
		this.publicKey = publicKey;
		setUserRepoKeyId(publicKey.getUserRepoKeyId());
		setServerRepositoryId(publicKey.getServerRepositoryId());
		setPublicKeyData(CryptoRegistry.getInstance().encodePublicKey(publicKey.getPublicKey()));
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId == null ? null : new Uid(userRepoKeyId);
	}
	protected void setUserRepoKeyId(final Uid userRepoKeyId) {
		if (! equal(this.getUserRepoKeyId(), userRepoKeyId))
			this.userRepoKeyId = userRepoKeyId == null ? null : userRepoKeyId.toString();
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId == null ? null : UUID.fromString(serverRepositoryId);
	}
	public void setServerRepositoryId(final UUID serverRepositoryId) {
		if (! equal(this.getServerRepositoryId(), serverRepositoryId))
			this.serverRepositoryId = serverRepositoryId == null ? null : serverRepositoryId.toString();
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

	public int getSignedDataVersion() {
		return 0;
	}

	public InputStream getSignedData(int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getUserRepoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getPublicKeyData()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getServerRepositoryId())
			);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	public UserRepoKey.PublicKey getPublicKey() {
		if (publicKey == null)
			publicKey = createPublicKey();

		return publicKey;
	}

	protected UserRepoKey.PublicKey createPublicKey() {
		try {
			return new UserRepoKeyImpl.PublicKeyImpl(
					getUserRepoKeyId(), getServerRepositoryId(),
					CryptoRegistry.getInstance().decodePublicKey(getPublicKeyData()),
					null, false);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return String.format("%s{userRepoKeyId=%s, serverRepositoryId=%s}",
				super.toString(),
				userRepoKeyId,
				serverRepositoryId);
	}
}
