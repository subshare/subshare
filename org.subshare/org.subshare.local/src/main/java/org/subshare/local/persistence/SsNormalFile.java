package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.NormalFile;

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy = DiscriminatorStrategy.VALUE_MAP, value = "SsNormalFile")
public class SsNormalFile extends NormalFile implements SsRepoFile {

//	@Persistent(nullValue = NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn = "signatureCreated")
	private SignatureImpl signature;

	@Column(defaultValue = "-1")
	private long lengthWithPadding = -1;

	@Override
	public String getSignedDataType() {
		return SsNormalFileDto.SIGNED_DATA_TYPE;
	}

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

	/**
	 * Gets the length of the file with padding bytes.
	 * <p>
	 * On the server, this is always -1. This is only managed on the client and then transferred inside the
	 * encrypted DTO (in {@link CryptoRepoFile#getRepoFileDtoData() CryptoRepoFile.repoFileDtoData}) to the server.
	 * <p>
	 * {@link #getLength() length} does not include the padding on the client-side - on the server-side, however,
	 * {@code length} includes the padding.
	 * @return the length of the file including both payload and padding bytes. Always -1 on the server-side.
	 */
	public long getLengthWithPadding() {
		return lengthWithPadding;
	}
	public void setLengthWithPadding(long lengthWithPadding) {
		this.lengthWithPadding = lengthWithPadding;
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
		final PersistenceManager pm = assertNotNull("JDOHelper.getPersistenceManager(this)", JDOHelper.getPersistenceManager(this));
		return new CryptoRepoFileDao().persistenceManager(pm).getCryptoRepoFileOrFail(this).getCryptoRepoFileId();
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
