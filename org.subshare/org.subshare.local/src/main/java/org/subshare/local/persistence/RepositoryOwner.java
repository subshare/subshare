package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
//@Unique(name="RepositoryOwner_userRepoKeyPublicKey", members="userRepoKeyPublicKey")
public class RepositoryOwner extends Entity implements Signable, AutoTrackLocalRevision, StoreCallback {

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
		final PersistenceManager pm = assertNotNull("JDOHelper.getPersistenceManager(this)", JDOHelper.getPersistenceManager(this));
		final RepositoryOwner persistentInstance = new RepositoryOwnerDao().persistenceManager(pm).getRepositoryOwner();
		if (persistentInstance != null && ! persistentInstance.equals(this))
			throw new IllegalStateException("Cannot persist a 2nd RepositoryOwner!");

		if (userRepoKeyPublicKey != null && signingUserRepoKeyId != null
				&& ! signingUserRepoKeyId.equals(userRepoKeyPublicKey.getUserRepoKeyId()))
			throw new IllegalStateException(String.format("RepositoryOwner must be self-signed! signingUserRepoKeyId != userRepoKeyPublicKey.userRepoKeyId :: %s != %s",
					signingUserRepoKeyId, userRepoKeyPublicKey.getUserRepoKeyId()));
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
	 * <b>Important:</b> The implementation in {@code CryptoLink} must exactly match the one in {@link CryptoLinkDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(userRepoKeyPublicKey.getUserRepoKeyId())
//					localRevision
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
