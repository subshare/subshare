package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Uniques;

import org.subshare.core.dto.PermissionSetDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="PermissionSet_cryptoRepoFile", members="cryptoRepoFile"),
})
@Queries({
	@Query(name="getPermissionSet_cryptoRepoFile", value="SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(name="getPermissionSetsChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class PermissionSet extends Entity implements WriteProtectedEntity, AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	private boolean permissionsInherited = true;

	private long localRevision;

	@Persistent(mappedBy="permissionSet")
	private Set<Permission> permissions;

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//	@Persistent(nullValue=NullValue.EXCEPTION)
//	@Embedded
//	private SignatureImpl signature;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date signatureCreated;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String signingUserRepoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] signatureData;
// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

	public PermissionSet() { }

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(final CryptoRepoFile cryptoRepoFile) {
		if (!equal(this.cryptoRepoFile, cryptoRepoFile))
			this.cryptoRepoFile = cryptoRepoFile;
	}

	public boolean isPermissionsInherited() {
		return permissionsInherited;
	}

	public void setPermissionsInherited(final boolean permissionsInherited) {
		if (!equal(this.permissionsInherited, permissionsInherited))
			this.permissionsInherited = permissionsInherited;
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
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code PermissionSet} must exactly match the one in {@link PermissionSetDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFile.getCryptoRepoFileId()),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(permissionsInherited)
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
		return assertNotNull("cryptoRepoFile", cryptoRepoFile);
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.grant;
	}
}
