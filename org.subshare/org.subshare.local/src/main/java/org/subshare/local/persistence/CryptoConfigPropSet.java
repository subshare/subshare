package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Uniques;

import org.subshare.core.dto.CryptoConfigPropSetDto;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="CryptoConfigPropSet_cryptoRepoFile", members="cryptoRepoFile")
})
@Queries({
	@Query(name="getCryptoConfigPropSet_cryptoRepoFile", value="SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(
			name="getCryptoConfigPropSetsChangedAfter_localRevision_exclLastSyncFromRepositoryId",
			value="SELECT WHERE this.localRevision > :localRevision && (this.lastSyncFromRepositoryId == null || this.lastSyncFromRepositoryId != :lastSyncFromRepositoryId)") // TODO this necessary == null is IMHO a DN bug!
})
public class CryptoConfigPropSet extends Entity implements WriteProtected, AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoKey cryptoKey;

	@Column(jdbcType="BLOB")
	private byte[] configPropSetDtoData;

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn = "signatureCreated")
	private SignatureImpl signature;

	private String lastSyncFromRepositoryId;

	protected CryptoConfigPropSet() {
	}

	public CryptoConfigPropSet(CryptoRepoFile cryptoRepoFile) {
		this.cryptoRepoFile = assertNotNull("cryptoRepoFile", cryptoRepoFile);
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFile.getCryptoRepoFileId();
	}

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
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

	public UUID getLastSyncFromRepositoryId() {
		return lastSyncFromRepositoryId == null ? null : UUID.fromString(lastSyncFromRepositoryId);
	}
	public void setLastSyncFromRepositoryId(final UUID repositoryId) {
		if (! equal(this.getLastSyncFromRepositoryId(), repositoryId))
			this.lastSyncFromRepositoryId = repositoryId == null ? null : repositoryId.toString();
	}

	public byte[] getConfigPropSetDtoData() {
		return configPropSetDtoData;
	}
	public void setConfigPropSetDtoData(byte[] configPropSetDtoData) {
		if (! equal(this.configPropSetDtoData, configPropSetDtoData))
			this.configPropSetDtoData = configPropSetDtoData;
	}

	/**
	 * Gets the key used to encrypt {@link #getConfigPropSetDtoData() configPropSetDtoData}.
	 * @return the key used to encrypt {@link #getConfigPropSetDtoData() configPropSetDtoData}.
	 */
	public CryptoKey getCryptoKey() {
		return cryptoKey;
	}
	public void setCryptoKey(final CryptoKey cryptoKey) {
		if (! equal(this.cryptoKey, cryptoKey)) {
			if (cryptoKey != null) {
				final CryptoKeyRole cryptoKeyRole = assertNotNull("cryptoKey.cryptoKeyRole", cryptoKey.getCryptoKeyRole());
				if (CryptoKeyRole.dataKey != cryptoKeyRole)
					throw new IllegalArgumentException("cryptoKey.cryptoKeyRole != dataKey");
			}
			this.cryptoKey = cryptoKey;
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
	public String getSignedDataType() {
		return CryptoConfigPropSetDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoConfigPropSet} must exactly match the one in {@link CryptoConfigPropSetDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("cryptoKey", cryptoKey).getCryptoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(configPropSetDtoData)
					);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return assertNotNull("cryptoRepoFileId", this.getCryptoRepoFileId());
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}

	@Override
	public String toString() {
		return String.format("%s{cryptoRepoFileId=%s, cryptoKeyId=%s}",
				super.toString(),
				getCryptoRepoFileId(),
				cryptoKey == null ? null : cryptoKey.getCryptoKeyId());
	}
}
