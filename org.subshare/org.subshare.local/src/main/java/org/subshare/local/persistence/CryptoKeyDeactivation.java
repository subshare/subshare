package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
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
import co.codewizards.cloudstore.local.db.IgnoreDatabaseMigraterComparison;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.CRYPTO_KEY_DTO, members = {
//			@Persistent(name = "cryptoKey"), // is passed through stack during DTO-creation -- no need to include here!
			@Persistent(name = "signature")
	})
})
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

	@IgnoreDatabaseMigraterComparison
	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		final CryptoKey ck = requireNonNull(cryptoKey, "cryptoKey");
		final CryptoKeyRole cryptoKeyRole = requireNonNull(ck.getCryptoKeyRole(), "cryptoKey.cryptoKeyRole");
		final CryptoRepoFile cryptoRepoFile = requireNonNull(ck.getCryptoRepoFile(), "cryptoKey.cryptoRepoFile");

		switch (cryptoKeyRole) {
			case backlinkKey:
			case dataKey:
				return null;
			default:
				return requireNonNull(cryptoRepoFile.getCryptoRepoFileId(), "cryptoRepoFile.cryptoRepoFileId");
		}
	}

	@IgnoreDatabaseMigraterComparison
	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
//		final CryptoKey ck = requireNonNull("cryptoKey", cryptoKey);
//		final CryptoKeyRole cryptoKeyRole = requireNonNull("cryptoKey.cryptoKeyRole", ck.getCryptoKeyRole());
//
//		return cryptoKeyRole == CryptoKeyRole.clearanceKey ? PermissionType.grant : PermissionType.write;
		return PermissionType.grant;
	}

}
