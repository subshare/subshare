package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.subshare.core.dto.CryptoKeyDeactivationDto;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
public class CryptoKeyDeactivation extends Entity implements WriteProtectedEntity {

	public CryptoKeyDeactivation() { }

	@Persistent(mappedBy="cryptoKeyDeactivation", nullValue=NullValue.EXCEPTION)
	private CryptoKey cryptoKey;

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//		@Persistent(nullValue=NullValue.EXCEPTION)
//		@Embedded
//		private SignatureImpl signature;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date signatureCreated;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String signingUserRepoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] signatureData;
// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

	public CryptoKey getCryptoKey() {
		return cryptoKey;
	}
	public void setCryptoKey(final CryptoKey cryptoKey) {
		this.cryptoKey = cryptoKey;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoKeyDeactivation} must exactly match the one in {@link CryptoKeyDeactivationDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			return InputStreamSource.Helper.createInputStreamSource(cryptoKey.getCryptoKeyId()).createInputStream();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//	@Override
//	public Signature getSignature() {
//		return signature;
//	}
//	@Override
//	public void setSignature(final Signature signature) {
//		if (!equal(this.signature, signature))
//			this.signature = SignatureImpl.copy(signature);
//	}
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
		final CryptoKey ck = assertNotNull("cryptoKey", cryptoKey);
		final CryptoKeyRole cryptoKeyRole = assertNotNull("cryptoKey.cryptoKeyRole", ck.getCryptoKeyRole());
		final CryptoRepoFile cryptoRepoFile = assertNotNull("cryptoKey.cryptoRepoFile", ck.getCryptoRepoFile());

		switch (cryptoKeyRole) {
			case backlinkKey:
			case dataKey:
				return null;
			default:
				return cryptoRepoFile;
		}
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
//		final CryptoKey ck = assertNotNull("cryptoKey", cryptoKey);
//		final CryptoKeyRole cryptoKeyRole = assertNotNull("cryptoKey.cryptoKeyRole", ck.getCryptoKeyRole());
//
//		return cryptoKeyRole == CryptoKeyRole.clearanceKey ? PermissionType.grant : PermissionType.write;
		return PermissionType.grant;
	}

}
