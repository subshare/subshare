package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.annotations.Column;
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

import org.subshare.core.dto.PermissionDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="Permission_permissionId", members="permissionId")
//	@Unique(name="Permission_permissionSet_userRepoKeyPublicKey_permissionType", members={"permissionSet", "userRepoKeyPublicKey", "permissionType"})
})
@Indices({
	@Index(name="Permission_localRevision", members="localRevision")
})
@Queries({
	@Query(name="getPermission_permissionId", value="SELECT UNIQUE WHERE this.permissionId == :permissionId"),
	@Query(
			name="getNonRevokedPermissions_permissionSet_permissionType_userRepoKeyId",
			value="SELECT WHERE "
					+ "this.permissionSet == :permissionSet "
					+ "&& this.permissionType == :permissionType "
					+ "&& this.userRepoKeyPublicKey.userRepoKeyId == :userRepoKeyId "
					+ "&& this.revoked == null"
			),
	@Query(
			name="getValidPermissions_permissionSet_permissionType_userRepoKeyId_timestamp",
			value="SELECT WHERE "
					+ "this.permissionSet == :permissionSet "
					+ "&& this.permissionType == :permissionType "
					+ "&& this.userRepoKeyPublicKey.userRepoKeyId == :userRepoKeyId "
					+ "&& this.validFrom <= :timestamp "
					+ "&& ( this.validTo == null || this.validTo >= :timestamp )"
			),
	@Query(name="getPermissionsChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class Permission extends Entity implements Signable, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String permissionId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private PermissionSet permissionSet;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private UserRepoKeyPublicKey userRepoKeyPublicKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="INTEGER")
	private PermissionType permissionType;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date validFrom = new Date();

	private Date revoked;

	private Date validTo;

	private long localRevision;

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

	public Permission() { }

	public Permission(final Uid permissionId) {
		this.permissionId = permissionId == null ? null : permissionId.toString();
	}

	public Uid getPermissionId() {
		if (permissionId == null)
			permissionId = new Uid().toString();

		return new Uid(permissionId);
	}

	public PermissionSet getPermissionSet() {
		return permissionSet;
	}
	public void setPermissionSet(final PermissionSet permissionSet) {
		if (! equal(this.permissionSet, permissionSet))
			this.permissionSet = permissionSet;
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKey() {
		return userRepoKeyPublicKey;
	}
	public void setUserRepoKeyPublicKey(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		this.userRepoKeyPublicKey = userRepoKeyPublicKey;
	}

	public PermissionType getPermissionType() {
		return permissionType;
	}
	public void setPermissionType(final PermissionType permissionType) {
		this.permissionType = permissionType;
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
		getPermissionId();
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	@Override
	public Date getCreated() {
		return super.getCreated();
	}

	public Date getRevoked() {
		return revoked;
	}
	public void setRevoked(final Date revoked) {
		this.revoked = revoked;
	}

	public Date getValidFrom() {
		return validFrom;
	}
	public void setValidFrom(final Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(final Date validTo) {
		this.validTo = validTo;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code Permission} must exactly match the one in {@link PermissionDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getPermissionId()),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(permissionSet.getCryptoRepoFile().getCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(userRepoKeyPublicKey.getUserRepoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(permissionType.ordinal()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(revoked),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(validFrom),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(validTo)
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

}
