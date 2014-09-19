package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.local.persistence.NormalFile;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="SsNormalFile")
public class SsNormalFile extends NormalFile implements SsRepoFile {

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//		@Persistent(nullValue=NullValue.EXCEPTION)
//		@Embedded
//		private SignatureImpl signature;

//	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date signatureCreated;

//	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String signingUserRepoKeyId;

//	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] signatureData;
	// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code SsDirectory} must exactly match the one in {@code SsDirectoryDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getName()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getParent() == null ? null : getParent().getName())
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

	@Override
	public CryptoRepoFile getCryptoRepoFileControllingPermissions() {
		final PersistenceManager pm = assertNotNull("JDOHelper.getPersistenceManager(this)", JDOHelper.getPersistenceManager(this));
		return new CryptoRepoFileDao().persistenceManager(pm).getCryptoRepoFileOrFail(this);
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
