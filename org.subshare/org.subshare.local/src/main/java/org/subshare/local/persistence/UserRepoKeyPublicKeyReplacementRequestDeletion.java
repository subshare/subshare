package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
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
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDeletionDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
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
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
})
public class UserRepoKeyPublicKeyReplacementRequestDeletion extends Entity implements WriteProtected, AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String requestId;

//	@Persistent(nullValue=NullValue.EXCEPTION) // we cannot annotate this as mandatory, because it was added, later. Maybe we can fix this in the future.
	private String oldUserRepoKeyId;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	protected UserRepoKeyPublicKeyReplacementRequestDeletion() {
	}

	public UserRepoKeyPublicKeyReplacementRequestDeletion(final UserRepoKeyPublicKeyReplacementRequest request) {
		this(requireNonNull(request, "request").getRequestId(),
				requireNonNull(requireNonNull(request, "request").getOldKey(), "request.oldKey").getUserRepoKeyId());
	}

	public UserRepoKeyPublicKeyReplacementRequestDeletion(final Uid requestId, final Uid oldUserRepoKeyId) {
		this.requestId = requireNonNull(requestId, "requestId").toString();
		this.oldUserRepoKeyId = oldUserRepoKeyId == null ? null : oldUserRepoKeyId.toString(); // allow null because of legacy data.
	}

	public Uid getRequestId() {
		return new Uid(requireNonNull(requestId, "requestId"));
	}

	public Uid getOldUserRepoKeyId() {
		return oldUserRepoKeyId == null ? null : new Uid(oldUserRepoKeyId); // allow null because of legacy data.
	}

	@Override
	public String getSignedDataType() {
		return UserRepoKeyPublicKeyReplacementRequestDeletionDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 1;
	}

	@Override
	public InputStream getSignedData(int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			switch (signedDataVersion) {
				case 0:
					return InputStreamSource.Helper.createInputStreamSource(getRequestId()).createInputStream();

				case 1:
					return new MultiInputStream(
							InputStreamSource.Helper.createInputStreamSource(getRequestId()),

							InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
							InputStreamSource.Helper.createInputStreamSource(getOldUserRepoKeyId())
							);

				default:
					throw new IllegalStateException("signedDataVersion=" + signedDataVersion);
			}
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
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return null; // global
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.grant;
	}
}
