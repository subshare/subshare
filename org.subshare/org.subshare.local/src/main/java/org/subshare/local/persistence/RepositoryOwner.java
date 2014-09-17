package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.RepositoryOwnerDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="RepositoryOwner_serverRepositoryId", members="serverRepositoryId")
@Queries({
	@Query(name="getRepositoryOwner_serverRepositoryId", value="SELECT UNIQUE WHERE this.serverRepositoryId == :serverRepositoryId")
})
public class RepositoryOwner extends Entity implements Signable, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=36)
	private String serverRepositoryId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private UserRepoKeyPublicKey userRepoKeyPublicKey;

	private long localRevision;

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//	@Persistent(nullValue=NullValue.EXCEPTION)
//	@Embedded
//	private SignatureImpl signature;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date signatureCreated;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String signingUserRepoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] signatureData;
// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

	public RepositoryOwner() { }

	@Override
	public void jdoPreStore() {
//		final PersistenceManager pm = assertNotNull("JDOHelper.getPersistenceManager(this)", JDOHelper.getPersistenceManager(this));
//		final RepositoryOwner persistentInstance = new RepositoryOwnerDao().persistenceManager(pm).getRepositoryOwner();
//		if (persistentInstance != null && ! persistentInstance.equals(this))
//			throw new IllegalStateException("Cannot persist a 2nd RepositoryOwner!");

		final Signature signature = getSignature();
		if (userRepoKeyPublicKey != null && signature != null
				&& ! signature.getSigningUserRepoKeyId().equals(userRepoKeyPublicKey.getUserRepoKeyId()))
			throw new IllegalStateException(String.format("RepositoryOwner must be self-signed! signingUserRepoKeyId != userRepoKeyPublicKey.userRepoKeyId :: %s != %s",
					signature.getSigningUserRepoKeyId(), userRepoKeyPublicKey.getUserRepoKeyId()));
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId == null ? null : UUID.fromString(serverRepositoryId);
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKey() {
		return userRepoKeyPublicKey;
	}
	public void setUserRepoKeyPublicKey(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		if (! equal(this.userRepoKeyPublicKey, userRepoKeyPublicKey))
			this.userRepoKeyPublicKey = userRepoKeyPublicKey;

		final UUID srid = userRepoKeyPublicKey == null ? null : userRepoKeyPublicKey.getServerRepositoryId();
		final String sridStr = srid == null ? null : srid.toString();

		if (! equal(this.serverRepositoryId, sridStr))
			this.serverRepositoryId = sridStr;
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

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code RepositoryOwner} must exactly match the one in {@link RepositoryOwnerDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		byte separatorIndex = 0;
		try {
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getServerRepositoryId()),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(userRepoKeyPublicKey.getUserRepoKeyId())
					);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//		@Override
//		public Signature getSignature() {
//			return signature;
//		}
//		@Override
//		public void setSignature(final Signature signature) {
//			if (!equal(this.signature, signature))
//				this.signature = SignatureImpl.copy(signature);
//		}
	@Override
	public Signature getSignature() {
		String.valueOf(signatureCreated);
		String.valueOf(signingUserRepoKeyId);
		String.valueOf(signatureData);
		return SignableEmbeddedWorkaround.getSignature(this);
	}
	@Override
	public void setSignature(final Signature signature) {
		SignableEmbeddedWorkaround.setSignature(this, signature);
	}
// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

}
