package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
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
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="UserIdentityLink_userIdentityLinkId", members="userIdentityLinkId")
@Index(name="UserIdentityLink_localRevision", members="localRevision")
@Queries({
	@Query(name="getUserIdentityLink_userIdentityLinkId", value="SELECT UNIQUE WHERE this.userIdentityLinkId == :userIdentityLinkId"),
	@Query(name="getUserIdentityLinksChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision"),
	@Query(name="getUserIdentityLinks_userIdentity", value="SELECT WHERE this.userIdentity == :userIdentity"),
	@Query(name="getUserIdentityLinks_ofUserRepoKeyPublicKey", value="SELECT WHERE this.userIdentity.ofUserRepoKeyPublicKey == :ofUserRepoKeyPublicKey"),
	@Query(name="getUserIdentityLinks_forUserRepoKeyPublicKey", value="SELECT WHERE this.forUserRepoKeyPublicKey == :forUserRepoKeyPublicKey"),
	@Query(name="getUserIdentityLinks_ofUserRepoKeyPublicKey_forUserRepoKeyPublicKey", value="SELECT WHERE this.userIdentity.ofUserRepoKeyPublicKey == :ofUserRepoKeyPublicKey && this.forUserRepoKeyPublicKey == :forUserRepoKeyPublicKey")
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
})
public class UserIdentityLink extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {
	public static final String SIGNED_DATA_TYPE = "UserIdentityLink";

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String userIdentityLinkId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private UserIdentity userIdentity;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private UserRepoKeyPublicKey forUserRepoKeyPublicKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="BLOB")
	private byte[] encryptedUserIdentityKeyData;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public UserIdentityLink() { }

	public UserIdentityLink(final Uid userIdentityLinkId) {
		this.userIdentityLinkId = userIdentityLinkId == null ? null : userIdentityLinkId.toString();
	}

	public Uid getUserIdentityLinkId() {
		if (userIdentityLinkId == null)
			userIdentityLinkId = new Uid().toString();

		return new Uid(userIdentityLinkId);
	}

	public UserIdentity getUserIdentity() {
		return userIdentity;
	}
	public void setUserIdentity(UserIdentity userIdentity) {
		if (!equal(this.userIdentity, userIdentity))
			this.userIdentity = userIdentity;
	}

	/**
	 * Gets the {@link UserRepoKeyPublicKey} <b>for</b> whom this {@code UserIdentityLink} is encrypted.
	 * <p>
	 * The user having the corresponding private key is able to decrypt the {@linkplain #getEncryptedUserIdentityPayloadDtoData() payload}.
	 * @return the {@link UserRepoKeyPublicKey} <b>for</b> whom this {@code UserIdentityLink} is encrypted.
	 */
	public UserRepoKeyPublicKey getForUserRepoKeyPublicKey() {
		return forUserRepoKeyPublicKey;
	}
	public void setForUserRepoKeyPublicKey(UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		if (!equal(this.forUserRepoKeyPublicKey, forUserRepoKeyPublicKey))
			this.forUserRepoKeyPublicKey = forUserRepoKeyPublicKey;
	}

	/**
	 * Gets the encrypted symmetric key needed to en-/decrypt the payload of {@link #getUserIdentity() userIdentity}.
	 * This symmetric key was encrypted with {@link #getForUserRepoKeyPublicKey() forUserRepoKeyPublicKey} and can thus be
	 * decrypted using the private key corresponding to {@code forUserRepoKeyPublicKey}.
	 * @return the encrypted symmetric key needed to en-/decrypt the payload of {@link #getUserIdentity() userIdentity}.
	 * Never <code>null</code> in persistence.
	 */
	public byte[] getEncryptedUserIdentityKeyData() {
		return encryptedUserIdentityKeyData;
	}
	public void setEncryptedUserIdentityKeyData(final byte[] encryptedUserIdentityKeyData) {
		if (!equal(this.encryptedUserIdentityKeyData, encryptedUserIdentityKeyData))
			this.encryptedUserIdentityKeyData = encryptedUserIdentityKeyData;
	}

	@Override
	public String getSignedDataType() {
		return UserIdentityLink.SIGNED_DATA_TYPE;
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
					InputStreamSource.Helper.createInputStreamSource(getUserIdentityLinkId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(userIdentity.getUserIdentityId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(forUserRepoKeyPublicKey.getUserRepoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(encryptedUserIdentityKeyData)
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
		getUserIdentityLinkId();
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
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return null; // global
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		final Signature signature = getSignature();

		// It may be signed by either the owner of this identity, i.e. ofUserRepoKeyPublicKey, or
		// by any user having 'readUserIdentity' access. Maybe we introduce a 'writeUserIdentity' later, though.
		if (signature == null || !signature.getSigningUserRepoKeyId().equals(requireNonNull(requireNonNull(userIdentity, "userIdentity").getOfUserRepoKeyPublicKey(), "userIdentity.ofUserRepoKeyPublicKey").getUserRepoKeyId()))
			return PermissionType.readUserIdentity;
		else
			return null; // no permission needed at all, if it's self-signed (everyone can and must give information about himself)
	}

}
