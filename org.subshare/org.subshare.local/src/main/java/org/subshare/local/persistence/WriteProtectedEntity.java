package org.subshare.local.persistence;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.sign.Signable;

public interface WriteProtectedEntity extends Signable {

	/**
	 * Gets the {@link CryptoRepoFile} to which (directly or via parent) a {@link Permission} with the indicated
	 * {@link #getPermissionTypeRequiredForWrite() PermissionType} must be granted in order to allow a 'write' access to
	 * this entity.
	 * <p>
	 * If this method returns <code>null</code>, any permission on any {@code CryptoRepoFile} is fine.
	 * @return the {@link CryptoRepoFile} controlling the permissions of this {@code WriteProtectedEntity}.
	 */
	CryptoRepoFile getCryptoRepoFileControllingPermissions();

	PermissionType getPermissionTypeRequiredForWrite();

}
