package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.SsSymlinkDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.db.IgnoreDatabaseMigraterComparison;
import co.codewizards.cloudstore.local.persistence.Symlink;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="SsSymlink")
public class SsSymlink extends Symlink implements SsRepoFile {

//	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	@Override
	public String getSignedDataType() {
		return SsSymlinkDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code SsSymlink} must exactly match the one in {@code SsSymlinkDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getName()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getParent() == null ? null : getParent().getName()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getTarget()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getLastModified())
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
		if (!equal(this.getSignature(), signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@IgnoreDatabaseMigraterComparison
	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		final PersistenceManager pm = requireNonNull(JDOHelper.getPersistenceManager(this), "JDOHelper.getPersistenceManager(this)");
		return new CryptoRepoFileDao().persistenceManager(pm).getCryptoRepoFileOrFail(this).getCryptoRepoFileId();
	}

	@IgnoreDatabaseMigraterComparison
	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
