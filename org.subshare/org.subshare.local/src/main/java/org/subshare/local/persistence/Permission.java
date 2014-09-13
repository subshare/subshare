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

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

//TODO This does not work this way! We need to make this temporal and thus keep the historic data to check whether data signed
//before and synchronised after permissions were revoked were signed correctly! Otherwise we can never sync a repository
//after revocation of write permissions!

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="Permission_permissionId", members="permissionId"),
	@Unique(name="Permission_permissionSet_userRepoKeyPublicKey_permissionType", members={"permissionSet", "userRepoKeyPublicKey", "permissionType"})
})
@Indices({
	@Index(name="Permission_localRevision", members="localRevision")
})
@Queries({
	@Query(name="getPermission_permissionId", value="SELECT UNIQUE WHERE this.permissionId == :permissionId"),
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
					InputStreamSource.Helper.createInputStreamSource(permissionType.ordinal())
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
