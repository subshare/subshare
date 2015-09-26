package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Uniques;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CryptoRepoFileOnServerDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

/**
 * Container holding the encrypted meta-data for a file (or directory) after the data was uploaded to the server.
 * <p>
 * There is a 1-1-relation to a {@link CryptoRepoFile} (via the {@link #getRepoFile() repoFile} property).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="CryptoRepoFileOnServer_cryptoRepoFile", members="cryptoRepoFile")
})
@Indices({
	@Index(name="CryptoRepoFileOnServer_localRevision", members={"localRevision"})
})
@Queries({
//	@Query(name="getCryptoRepoFileOnServer_cryptoRepoFile", value="SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(
			name="getCryptoRepoFileOnServerChangedAfter_localRevision",
			value="SELECT WHERE this.localRevision > :localRevision")
})
public class CryptoRepoFileOnServer extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoKey cryptoKey; // might be different than the one of cryptoRepoFile, when it was just changed => keep separately.

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="BLOB")
	private byte[] repoFileDtoData;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

//	// TODO 1: The direct partner-repository from which this was synced, should be a real relation to the RemoteRepository,
//	// because this is more efficient (not a String, but a long id).
//	// TODO 2: We should additionally store (and forward) the origin repositoryId (UUID/String) to use this feature during
//	// circular syncs over multiple repos - e.g. repoA ---> repoB ---> repoC ---> repoA (again) - this circle would currently
//	// cause https://github.com/cloudstore/cloudstore/issues/25 again (because issue 25 is only solved for direct partners - not indirect).
//	// TODO 3: We should switch from UUID to Uid everywhere (most importantly the repositoryId).
//	// Careful, though: Uid's String-representation is case-sensitive! Due to Windows, it must thus not be used for file names!
//	// TODO 4: It is not yet sure, how Subshare will actually detect and handle collisions. Maybe we don't need this field at all?!
//	// TODO 5: Shouldn't we better store this inside the encrypted (and signed!) repoFileDtoData?!
//	private String lastSyncFromRepositoryId;

	public CryptoRepoFileOnServer() { }

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(CryptoRepoFile cryptoRepoFile) {
		this.cryptoRepoFile = cryptoRepoFile;
	}

	/**
	 * Gets the key used to encrypt {@link #getRepoFileDtoData() repoFileDtoData} as well as the
	 * actual content of the file (if it is a normal file - no directory).
	 * @return the key used to encrypt {@link #getRepoFileDtoData() repoFileDtoData} and - if applicable -
	 * the file contents. Might be temporarily <code>null</code> during creation.
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

	/**
	 * Gets the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDto}).
	 * <p>
	 * This meta-data is encrypted on the client-side using the referenced {@link #getCryptoKey() cryptoKey}.
	 * Before encryption, it is serialised using {@link RepoFileDtoIo} and compressed by a {@link GZIPOutputStream}.
	 * @return the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDto}). Might be temporarily <code>null</code>
	 * during creation.
	 */
	public byte[] getRepoFileDtoData() {
		return repoFileDtoData;
	}
	public void setRepoFileDtoData(final byte[] repoFileDtoData) {
		if (! equal(this.repoFileDtoData, repoFileDtoData))
			this.repoFileDtoData = repoFileDtoData;
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
	public void jdoPreStore() {
	}

	@Override
	public String getSignedDataType() {
		return CryptoRepoFileOnServerDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

//	public String getLastSyncFromRepositoryId() {
//		return lastSyncFromRepositoryId;
//	}
//	public void setLastSyncFromRepositoryId(String lastSyncFromRepositoryId) {
//		this.lastSyncFromRepositoryId = lastSyncFromRepositoryId;
//	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoRepoFile} must exactly match the one in {@link CryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("cryptoRepoFile", cryptoRepoFile).getCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("cryptoKey", cryptoKey).getCryptoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getRepoFileDtoData())
					);
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
		if (!equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return assertNotNull("cryptoRepoFileId", assertNotNull("cryptoRepoFile", cryptoRepoFile).getCryptoRepoFileId());
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}

	@Override
	public String toString() {
		return String.format("%s{cryptoRepoFileId=%s}",
				super.toString(),
				cryptoRepoFile == null ? null : cryptoRepoFile.getCryptoRepoFileId());
	}
}
