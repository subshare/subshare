package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.annotations.Embedded;
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
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
public class CryptoKeyDeactivation extends Entity implements WriteProtected {

	public CryptoKeyDeactivation() { }

	@Persistent(mappedBy="cryptoKeyDeactivation", nullValue=NullValue.EXCEPTION)
	private CryptoKey cryptoKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public CryptoKey getCryptoKey() {
		return cryptoKey;
	}
	public void setCryptoKey(final CryptoKey cryptoKey) {
		if (! equal(this.cryptoKey, cryptoKey))
			this.cryptoKey = cryptoKey;
	}

	@Override
	public String getSignedDataType() {
		return CryptoKeyDeactivationDto.SIGNED_DATA_TYPE;
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

	@Override
	public Signature getSignature() {
		return signature;
	}
	@Override
	public void setSignature(final Signature signature) {
		if (! equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		final CryptoKey ck = assertNotNull("cryptoKey", cryptoKey);
		final CryptoKeyRole cryptoKeyRole = assertNotNull("cryptoKey.cryptoKeyRole", ck.getCryptoKeyRole());
		final CryptoRepoFile cryptoRepoFile = assertNotNull("cryptoKey.cryptoRepoFile", ck.getCryptoRepoFile());

		switch (cryptoKeyRole) {
			case backlinkKey:
			case dataKey:
				return null;
			default:
				return assertNotNull("cryptoRepoFile.cryptoRepoFileId", cryptoRepoFile.getCryptoRepoFileId());
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
