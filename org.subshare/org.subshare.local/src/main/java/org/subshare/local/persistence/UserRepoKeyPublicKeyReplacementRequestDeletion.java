package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static co.codewizards.cloudstore.core.util.Util.equal;

import java.io.IOException;
import java.io.InputStream;

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

import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="UserRepoKeyPublicKeyReplacementRequestDeletion_requestId", members="requestId")
})
@Queries({
	@Query(name="getUserRepoKeyPublicKeyReplacementRequestDeletion_requestId", value="SELECT UNIQUE WHERE this.requestId == :requestId"),
	@Query(name="getUserRepoKeyPublicKeyReplacementRequestDeletionsChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class UserRepoKeyPublicKeyReplacementRequestDeletion extends Entity implements WriteProtectedEntity, AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String requestId;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	protected UserRepoKeyPublicKeyReplacementRequestDeletion() {
	}

	public UserRepoKeyPublicKeyReplacementRequestDeletion(final UserRepoKeyPublicKeyReplacementRequest request) {
		this(assertNotNull("request", request).getRequestId());
	}

	public UserRepoKeyPublicKeyReplacementRequestDeletion(final Uid requestId) {
		this.requestId = assertNotNull("requestId", requestId).toString();
	}

	public Uid getRequestId() {
		return new Uid(assertNotNull("requestId", requestId));
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	@Override
	public InputStream getSignedData(int signedDataVersion) {
		try {
			return InputStreamSource.Helper.createInputStreamSource(getRequestId()).createInputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Signature getSignature() {
		return signature;
	}

	@Override
	public void setSignature(Signature signature) {
		if (!equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(long localRevision) {
		if (!equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}

	@Override
	public CryptoRepoFile getCryptoRepoFileControllingPermissions() {
		return null;
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.grant;
	}
}
