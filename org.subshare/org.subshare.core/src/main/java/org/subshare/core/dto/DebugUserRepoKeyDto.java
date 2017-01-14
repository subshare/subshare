package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.subshare.core.Cryptree;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.Uid;

@SuppressWarnings("serial")
@XmlRootElement
public class DebugUserRepoKeyDto implements Serializable {

	private Uid userRepoKeyId;
	private UUID serverRepositoryId;
	private boolean owner;
	private boolean inDatabase;
	private KeyRingType keyRingType = KeyRingType.NONE;
	private int userIdentityCount;
	private boolean invitation;
	private UserIdentityPayloadDto userIdentityPayloadDto;

	public static enum KeyRingType {
		/**
		 * Not located in any key-ring (likely existing in the database, only).
		 */
		NONE,

		/**
		 * Located in the {@link UserRepoKeyRing} that is currently used by the {@link Cryptree}.
		 * <p>
		 * This means, we have (and use) both the private and the public key, i.e. the complete key-pair.
		 */
		PAIR_CURRENT,

		/**
		 * Located in a {@link UserRepoKeyRing} that is not currently used by the {@link Cryptree}.
		 * <p>
		 * This means, we have (but do not use) both the private and the public key, i.e. the complete key-pair (but use only the public key, if needed).
		 */
		PAIR_OTHER,

		/**
		 * Located in {@link User#getUserRepoKeyPublicKeys()} -- not in the key-ring.
		 * <p>
		 * This means, we have the public key, only -- not the corresponding private key.
		 */
		PUBLIC
	}

	public DebugUserRepoKeyDto() {
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}
	public void setUserRepoKeyId(Uid userRepoKeyId) {
		this.userRepoKeyId = userRepoKeyId;
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
	public void setServerRepositoryId(UUID serverRepositoryId) {
		this.serverRepositoryId = serverRepositoryId;
	}

	public boolean isOwner() {
		return owner;
	}
	public void setOwner(boolean owner) {
		this.owner = owner;
	}

	public boolean isInDatabase() {
		return inDatabase;
	}
	public void setInDatabase(boolean inDatabase) {
		this.inDatabase = inDatabase;
	}

	public KeyRingType getKeyRingType() {
		return keyRingType;
	}
	public void setKeyRingType(KeyRingType keyRingType) {
		this.keyRingType = assertNotNull(keyRingType, "keyRingType");
	}

	public int getUserIdentityCount() {
		return userIdentityCount;
	}
	public void setUserIdentityCount(int userIdentityCount) {
		this.userIdentityCount = userIdentityCount;
	}

	public boolean isInvitation() {
		return invitation;
	}
	public void setInvitation(boolean invitation) {
		this.invitation = invitation;
	}

	public UserIdentityPayloadDto getUserIdentityPayloadDto() {
		return userIdentityPayloadDto;
	}
	public void setUserIdentityPayloadDto(UserIdentityPayloadDto userIdentityPayloadDto) {
		this.userIdentityPayloadDto = userIdentityPayloadDto;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[userRepoKeyId=" + userRepoKeyId
				+ ", serverRepositoryId=" + serverRepositoryId
				+ ", owner=" + owner
				+ ", inDatabase=" + inDatabase
				+ ", keyRingType=" + keyRingType
				+ ", userIdentityCount=" + userIdentityCount
				+ ", invitation=" + invitation
				+ ", userIdentityPayloadDto=" + userIdentityPayloadDto
				+ ']';
	}
}
