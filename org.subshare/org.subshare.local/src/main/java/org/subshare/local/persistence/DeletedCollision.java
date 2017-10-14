package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;

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

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name = "DeletedCollision_collisionId", members = "collisionId")
})
@Queries({
	@Query(name = "getDeletedCollision_collisionId", value = "SELECT UNIQUE WHERE this.collisionId == :collisionId"),
	@Query(
			name="getDeletedCollisionsChangedAfter_localRevision",
			value="SELECT WHERE this.localRevision > :localRevision"),
})
public class DeletedCollision extends Entity implements WriteProtected, AutoTrackLocalRevision {

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Column(length = 22)
	private String collisionId;

	private long localRevision;

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn = "signatureCreated")
	private SignatureImpl signature;

	protected DeletedCollision() {
	}

	public DeletedCollision(final Uid collisionId) {
		this.collisionId = assertNotNull(collisionId, "collisionId").toString();
	}

	public Uid getCollisionId() {
		return new Uid(collisionId);
	}

	@Override
	public String getSignedDataType() {
		return CollisionDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code DeletedCollision} must exactly match the one in {@code DeletedCollisionDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
//			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getCollisionId())
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
		if (! equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
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
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return null;
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
