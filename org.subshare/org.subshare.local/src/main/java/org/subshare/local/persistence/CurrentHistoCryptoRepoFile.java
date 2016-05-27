package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="UK_CurrentHistoCryptoRepoFile_cryptoRepoFile", members="cryptoRepoFile")
})
@Indices({
	@Index(name="CurrentHistoCryptoRepoFile_localRevision", members="localRevision"),
	@Index(name="CurrentHistoCryptoRepoFile_cryptoRepoFile", members="cryptoRepoFile")
})
@Queries({
	@Query(name="getCurrentHistoCryptoRepoFile_cryptoRepoFile", value="SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(
			name="getCurrentHistoCryptoRepoFilesChangedAfter_localRevision",
			value="SELECT WHERE this.localRevision > :localRevision")
})
public class CurrentHistoCryptoRepoFile extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

	private static final Logger logger = LoggerFactory.getLogger(CurrentHistoCryptoRepoFile.class);

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public CurrentHistoCryptoRepoFile() {
	}

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(CryptoRepoFile cryptoRepoFile) {
		if (equal(this.cryptoRepoFile, cryptoRepoFile))
			return;

		if (this.cryptoRepoFile != null)
			throw new IllegalStateException("this.cryptoRepoFile already assigned! Cannot re-assign!");

		this.cryptoRepoFile = cryptoRepoFile;
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile() {
		return histoCryptoRepoFile;
	}
	public void setHistoCryptoRepoFile(HistoCryptoRepoFile histoCryptoRepoFile) {
		this.histoCryptoRepoFile = histoCryptoRepoFile;
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
		logger.debug("jdoPreStore: {} {}",
				cryptoRepoFile, histoCryptoRepoFile);

		final Uid cryptoRepoFileId = assertNotNull("cryptoRepoFile", cryptoRepoFile).getCryptoRepoFileId();
		final CryptoRepoFile crf = assertNotNull("histoCryptoRepoFile", histoCryptoRepoFile).getCryptoRepoFile();
		assertNotNull("histoCryptoRepoFile.cryptoRepoFile", crf);

		if (! cryptoRepoFileId.equals(crf.getCryptoRepoFileId()))
			throw new IllegalStateException(String.format("cryptoRepoFile.cryptoRepoFileId != histoCryptoRepoFile.cryptoRepoFile.cryptoRepoFileId :: %s != %s",
					cryptoRepoFileId, crf.getCryptoRepoFileId()));
	}

	@Override
	public String getSignedDataType() {
		return CurrentHistoCryptoRepoFileDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CurrentHistoCryptoRepoFile} must exactly match the one in {@code CurrentHistoCryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			final Uid cryptoRepoFileId = assertNotNull("cryptoRepoFile", cryptoRepoFile).getCryptoRepoFileId();
			final Uid histoCryptoRepoFileId = assertNotNull("histoCryptoRepoFile", histoCryptoRepoFile).getHistoCryptoRepoFileId();

			assertNotNull("cryptoRepoFileId", cryptoRepoFileId);
			assertNotNull("histoCryptoRepoFileId", histoCryptoRepoFileId);

			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFileId)
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
}
