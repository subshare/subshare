package org.subshare.core.sign;

import org.subshare.core.dto.PermissionType;

import co.codewizards.cloudstore.core.Uid;

public interface WriteProtected extends Signable {

	/**
	 * Gets the id of the {@code CryptoRepoFile} to which (directly or via parent) a {@code Permission} with the indicated
	 * {@link #getPermissionTypeRequiredForWrite() PermissionType} must be granted in order to allow a 'write' access to
	 * this entity.
	 * <p>
	 * If this method returns <code>null</code>, any permission on any {@code CryptoRepoFile} is fine.
	 * @return the id of the {@code CryptoRepoFile} controlling the permissions of this {@code WriteProtected}
	 * or <code>null</code> for global (any existing permission is sufficient - no matter to which {@code CryptoRepoFile} it belongs)
	 */
	Uid getCryptoRepoFileIdControllingPermissions();

	PermissionType getPermissionTypeRequiredForWrite();

}
