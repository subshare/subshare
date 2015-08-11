package org.subshare.core.repo.local;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoMetaData;

public interface SsLocalRepoMetaData extends LocalRepoMetaData {

	CryptoRepoFileDto getCryptoRepoFileDto(long repoFileId);

	Map<Long, CryptoRepoFileDto> getCryptoRepoFileDtos(Collection<Long> repoFileIds);

	boolean isPermissionsInherited(String localPath);

	void setPermissionsInherited(String localPath, boolean inherited);

	/**
	 * Gets the {@link PermissionType}s granted to the specified user on the level of the specified directory/file.
	 * <p>
	 * <b>Important:</b> In contrast to {@link #getEffectivePermissionTypes(String, Uid) getEffectivePermissionTypes(...)} this
	 * method operates on the specified directory/file, only! It does not take parents (inheritance) into account.
	 * <p>
	 * <b>Important:</b> If the specified user has {@link PermissionType#readUserIdentity readUserIdentity}, this
	 * {@code PermissionType} is always part of the result, no matter on which node this method is invoked! This is,
	 * because {@code readUserIdentity} is not associated with a directory - it's global! Technically, it is assigned
	 * to the root (at least right now - this might change later), but semantically, it is not associated with any.
	 *
	 * @param localPath the directory/file whose permissions to query. Must not be <code>null</code>.
	 * @param userRepoKeyId the user-key's identifier for which to determine the permissions granted. Must not be <code>null</code>.
	 * @return the {@link PermissionType}s granted. Never <code>null</code>, but maybe empty!
	 * @see #grantPermission(String, PermissionType, UserRepoKey.PublicKey)
	 * @see #revokePermission(String, PermissionType, Set)
	 * @see #assertHasPermission(String, Uid, PermissionType, Date)
	 */
	Set<PermissionType> getGrantedPermissionTypes(String localPath, Uid userRepoKeyId);

	Set<PermissionType> getEffectivePermissionTypes(String localPath, Uid userRepoKeyId);

	Set<PermissionType> getInheritedPermissionTypes(String localPath, Uid userRepoKeyId);

	/**
	 * Gets the identity of the owner's repo-key.
	 * @return the identity of the owner's repo-key. May be <code>null</code> during the initial set-up of
	 * a repository, but should never be <code>null</code> later on.
	 */
	Uid getOwnerUserRepoKeyId();

	void grantPermission(String localPath, PermissionType permissionType, UserRepoKey.PublicKey publicKey);

	void revokePermission(String localPath, PermissionType permissionType, Set<Uid> userRepoKeyIds);

	boolean isMetaOnly();

	void makeMetaOnly();

}
