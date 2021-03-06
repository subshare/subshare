package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
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
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.PermissionDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.db.IgnoreDatabaseMigraterComparison;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="UK_Permission_permissionId", members="permissionId")
@Indices({
	@Index(name="Permission_permissionId", members="permissionId"),
	@Index(name="Permission_permissionType", members="permissionType"),
	@Index(name="Permission_localRevision", members="localRevision"),
	@Index(name="Permission_validFrom", members="validFrom"),
	@Index(name="Permission_validTo", members="validTo")
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
			name="getNonRevokedPermissions_permissionType_userRepoKeyId",
			value="SELECT WHERE "
					+ "this.permissionType == :permissionType "
					+ "&& this.userRepoKeyPublicKey.userRepoKeyId == :userRepoKeyId "
					+ "&& this.revoked == null"
			),
	@Query(
			name="getNonRevokedPermissions_permissionType",
			value="SELECT WHERE "
					+ "this.permissionType == :permissionType "
					+ "&& this.revoked == null"
			),
	@Query(
			name="getValidPermissions_permissionSet_permissionType_userRepoKeyId_timestamp",
			value="SELECT WHERE "
					+ "this.permissionSet == :permissionSet "
					+ "&& this.permissionType == :permissionType "
					+ "&& this.userRepoKeyPublicKey.userRepoKeyId == :userRepoKeyId "
					+ "&& this.validFrom <= :timestamp "
					+ "&& ( this.validTo == null || this.validTo > :timestamp )"
			),
	@Query(
			name="getValidPermissions_permissionType_userRepoKeyId_timestamp",
			value="SELECT WHERE "
					+ "this.permissionType == :permissionType "
					+ "&& this.userRepoKeyPublicKey.userRepoKeyId == :userRepoKeyId "
					+ "&& this.validFrom <= :timestamp "
					+ "&& ( this.validTo == null || this.validTo > :timestamp )"
			),
	@Query(
			name="PermissionCountOfDirectChildCryptoRepoFiles_parentCryptoRepoFile_permissionType",
			value="SELECT count(this) WHERE "
					+ "this.permissionSet.cryptoRepoFile.parent == :parentCryptoRepoFile "
					+ "&& this.permissionType == :permissionType "
			),
	@Query(
			name="getPermissions_userRepoKeyPublicKey",
			value="SELECT WHERE this.userRepoKeyPublicKey == :userRepoKeyPublicKey "
			),
	@Query(name="getPermissions_signingUserRepoKeyId",
			value="SELECT WHERE this.signature.signingUserRepoKeyId == :signingUserRepoKeyId"
			),
	@Query(name="getPermissionsChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.PERMISSION_DTO, members = {
			@Persistent(name = "permissionSet"),
			@Persistent(name = "userRepoKeyPublicKey"),
			@Persistent(name = "signature")
	}),
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
})
public class Permission extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

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
	private Date validFrom = now();

	private Date revoked;

	private Date validTo;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

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
		if (! equal(this.userRepoKeyPublicKey, userRepoKeyPublicKey))
			this.userRepoKeyPublicKey = userRepoKeyPublicKey;
	}

	public PermissionType getPermissionType() {
		return permissionType;
	}
	public void setPermissionType(final PermissionType permissionType) {
		if (equal(this.permissionType, permissionType))
			return;

		if (permissionType != null) {
			switch (permissionType) {
				case grant:
				case write:
				case readUserIdentity:
					break;
				default:
					throw new IllegalArgumentException("PermissionType unknown or not allowed here: " + permissionType);
			}
		}
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

	/**
	 * Gets the time (including) from which on this permission is effective.
	 * @return the time (including) from which on this permission is effective. Never <code>null</code>
	 * in persistent data - and usually never <code>null</code> in transient data, too.
	 */
	public Date getValidFrom() {
		return validFrom;
	}
	public void setValidFrom(final Date validFrom) {
		if (! equal(this.validFrom, validFrom))
			this.validFrom = validFrom;
	}

	public Date getRevoked() {
		return revoked;
	}
	public void setRevoked(final Date revoked) {
		if (! equal(this.revoked, revoked))
			this.revoked = revoked;
	}

	/**
	 * Gets the time (excluding) until which this permission is effective.
	 * <p>
	 * A value of <code>null</code> means that this permission is valid forever - or at least the end
	 * timestamp is not yet known (the permission was not <i>yet</i> revoked).
	 * @return the time (excluding) until which this permission is effective. May be <code>null</code>,
	 * if there is no end.
	 */
	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(final Date validTo) {
		if (! equal(this.validTo, validTo))
			this.validTo = validTo;
	}

	@Override
	public String getSignedDataType() {
		return PermissionDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
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
					InputStreamSource.Helper.createInputStreamSource(validFrom),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(revoked),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(validTo)
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

	@IgnoreDatabaseMigraterComparison
	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		requireNonNull(permissionSet, "permissionSet");
		return requireNonNull(requireNonNull(permissionSet.getCryptoRepoFile(), "permissionSet.cryptoRepoFile").getCryptoRepoFileId(),
				"permissionSet.cryptoRepoFile.cryptoRepoFileId");
	}

	@IgnoreDatabaseMigraterComparison
	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.grant;
	}
}
