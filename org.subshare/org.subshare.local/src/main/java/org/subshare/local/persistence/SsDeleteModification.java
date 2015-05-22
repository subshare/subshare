package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.DeleteModification;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="SsDeleteModification")
public class SsDeleteModification extends DeleteModification implements WriteProtected {

	@Persistent(nullValue=NullValue.EXCEPTION, defaultFetchGroup="true")
	@Column(jdbcType="CLOB")
	private String serverPath;

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Column(length = 22)
	private String cryptoRepoFileIdControllingPermissions;

	@Embedded(nullIndicatorColumn = "signatureCreated")
	private SignatureImpl signature; // may be null! actually, it's always null on the client and never null on the server.

	public String getServerPath() {
		return serverPath;
	}
	public void setServerPath(final String serverPath) {
		this.serverPath = serverPath;
	}

	@Override
	public String getSignedDataType() {
		return SsDeleteModificationDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code SsDeleteModification} must exactly match the one in {@link SsDeleteModificationDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getServerPath()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getCryptoRepoFileIdControllingPermissions())
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
		if (cryptoRepoFileIdControllingPermissions == null)
			return null;

		// parent of the deleted RepoFile!
		return new Uid(cryptoRepoFileIdControllingPermissions);
	}

	public void setCryptoRepoFileIdControllingPermissions(final Uid cryptoRepoFileIdControllingPermissions) {
		this.cryptoRepoFileIdControllingPermissions =
				cryptoRepoFileIdControllingPermissions == null ? null : cryptoRepoFileIdControllingPermissions.toString();
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
