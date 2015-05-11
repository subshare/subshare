package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.UserIdentityDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="UserIdentity_userIdentityId", members="userIdentityId")
@Index(name="UserIdentity_localRevision", members="localRevision")
@Queries({
	@Query(name="getUserIdentity_userIdentityId", value="SELECT UNIQUE WHERE this.userIdentityId == :userIdentityId"),
	@Query(name="getUserIdentitiesChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision"),
	@Query(name="getUserIdentities_ofUserRepoKeyPublicKey", value="SELECT WHERE this.ofUserRepoKeyPublicKey == :ofUserRepoKeyPublicKey")
})
public class UserIdentity extends Entity implements WriteProtectedEntity, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String userIdentityId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private UserRepoKeyPublicKey ofUserRepoKeyPublicKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] encryptedUserIdentityPayloadDtoData;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public UserIdentity() { }

	public UserIdentity(final Uid userIdentityId) {
		this.userIdentityId = userIdentityId == null ? null : userIdentityId.toString();
	}

	public Uid getUserIdentityId() {
		if (userIdentityId == null)
			userIdentityId = new Uid().toString();

		return new Uid(userIdentityId);
	}

	/**
	 * Gets the {@link UserRepoKeyPublicKey} which is described by this {@code UserIdentityLink}.
	 * <p>
	 * In other words, {@code this} is the {@code UserIdentityLink} <b>of</b> this {@link UserRepoKeyPublicKey}.
	 * @return the {@link UserRepoKeyPublicKey} which is described by this {@code UserIdentityLink}. Never <code>null</code>
	 * in persistent storage.
	 */
	public UserRepoKeyPublicKey getOfUserRepoKeyPublicKey() {
		return ofUserRepoKeyPublicKey;
	}
	public void setOfUserRepoKeyPublicKey(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		if (!equal(this.ofUserRepoKeyPublicKey, ofUserRepoKeyPublicKey))
			this.ofUserRepoKeyPublicKey = ofUserRepoKeyPublicKey;
	}

	public byte[] getEncryptedUserIdentityPayloadDtoData() {
		return encryptedUserIdentityPayloadDtoData;
	}
	public void setEncryptedUserIdentityPayloadDtoData(byte[] encryptedUserIdentityPayloadDtoData) {
		if (!equal(this.encryptedUserIdentityPayloadDtoData, encryptedUserIdentityPayloadDtoData))
			this.encryptedUserIdentityPayloadDtoData = encryptedUserIdentityPayloadDtoData;
	}

	@Override
	public String getSignedDataType() {
		return UserIdentityDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	@Override
	public InputStream getSignedData(int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getUserIdentityId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(ofUserRepoKeyPublicKey.getUserRepoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(encryptedUserIdentityPayloadDtoData)
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
	public void setSignature(Signature signature) {
		if (!equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@Override
	public void jdoPreStore() {
		getUserIdentityId();
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}

	@Override
	public void setLocalRevision(long localRevision) {
		if (! equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}

	@Override
	public CryptoRepoFile getCryptoRepoFileControllingPermissions() {
		return null; // global
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		final Signature signature = getSignature();

		// It may be signed by either the owner of this identity, i.e. ofUserRepoKeyPublicKey, or
		// by any user having 'readUserIdentity' access. Maybe we introduce a 'writeUserIdentity' later, though.
		if (signature == null || !signature.getSigningUserRepoKeyId().equals(ofUserRepoKeyPublicKey.getUserRepoKeyId()))
			return PermissionType.readUserIdentity;
		else
			return null; // no permission needed at all, if it's self-signed (everyone can and must give information about himself)
	}

}
