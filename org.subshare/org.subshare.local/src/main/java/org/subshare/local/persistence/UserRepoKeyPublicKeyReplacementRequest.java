package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;

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
import javax.jdo.annotations.Uniques;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="UserRepoKeyPublicKeyReplacementRequest_requestId", members="requestId")
})
@Indices({
	@Index(name="UserRepoPublicKeyReplacementRequest_localRevision", members="localRevision"),
	@Index(name="UserRepoPublicKeyReplacementRequest_oldKey", members="oldKey")
})
@Queries({
	@Query(name="getUserRepoKeyPublicKeyReplacementRequest_requestId", value="SELECT UNIQUE WHERE this.requestId == :requestId"),
	@Query(name="getUserRepoKeyPublicKeyReplacementRequests_oldKey", value="SELECT WHERE this.oldKey == :oldKey"),
//	@Query(name="getUserRepoKeyPublicKeyReplacementRequests_newKey", value="SELECT WHERE this.newKey == :newKey"),
	@Query(name="getUserRepoKeyPublicKeyReplacementRequestsChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
})
public class UserRepoKeyPublicKeyReplacementRequest extends Entity implements Signable, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String requestId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private InvitationUserRepoKeyPublicKey oldKey; // NOT unique to avoid problems with sync collisions (unlikely, but possible)

	@Persistent(nullValue=NullValue.EXCEPTION)
	private UserRepoKeyPublicKey newKey; // NOT unique to avoid problems with sync collisions (unlikely, but possible)

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public UserRepoKeyPublicKeyReplacementRequest() {
	}

	public UserRepoKeyPublicKeyReplacementRequest(final Uid requestId) {
		this.requestId = requestId == null ? null : requestId.toString();
	}

	public Uid getRequestId() {
		if (requestId == null)
			requestId = new Uid().toString();

		return new Uid(requestId);
	}

	public InvitationUserRepoKeyPublicKey getOldKey() {
		return oldKey;
	}
	public void setOldKey(InvitationUserRepoKeyPublicKey oldKey) {
		if (this.oldKey != null && !this.oldKey.equals(oldKey))
			throw new IllegalArgumentException("oldKey already assigned! Cannot modify!");

		if (!equal(this.oldKey, oldKey))
			this.oldKey = oldKey;
	}

	public UserRepoKeyPublicKey getNewKey() {
		return newKey;
	}
	public void setNewKey(UserRepoKeyPublicKey newKey) {
		if (this.newKey != null && !this.newKey.equals(newKey))
			throw new IllegalArgumentException("newKey already assigned! Cannot modify!");

		if (!equal(this.newKey, newKey))
			this.newKey = newKey;
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
	public String getSignedDataType() {
		return UserRepoKeyPublicKeyReplacementRequestDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		final int result = 0;

		// This value here and the version in UserRepoKeyPublicKey must always be the same!
		if (oldKey.getSignedDataVersion() != result)
			throw new IllegalStateException("UserRepoKeyPublicKey.signedDataVersion != UserRepoKeyPublicKeyReplacementRequest.signedDataVersion");

		return result;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code UserRepoKeyPublicKeyReplacementRequest} must exactly match the one in {@link UserRepoKeyPublicKeyReplacementRequestDto}!
	 */
	@Override
	public InputStream getSignedData(int signedDataVersion) {
		assertNotNull(oldKey, "oldKey");
		assertNotNull(newKey, "newKey");

		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getRequestId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(oldKey.getSignedData(signedDataVersion)),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(newKey.getSignedData(signedDataVersion))
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
	public void jdoPreStore() {
		getRequestId();
	}

	@Override
	public String toString() {
		return super.toString()
				+ "{requestId=" + requestId
				+ ",oldKey=" + oldKey
				+ ",newKey=" + newKey
				+ '}';
	}
}
