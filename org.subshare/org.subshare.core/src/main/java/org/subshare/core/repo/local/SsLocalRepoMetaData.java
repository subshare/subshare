package org.subshare.core.repo.local;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey.PublicKey;

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
	 * <b>Important:</b> In contrast to {@link #assertHasPermission(String, Uid, PermissionType, Date) assertHasPermission(...)} this
	 * method operates on the current node, only! It does not take parents / inheritance into account.
	 * <p>
	 * <b>Important:</b> If the specified user has {@link PermissionType#readUserIdentity readUserIdentity}, this
	 * {@code PermissionType} is always part of the result, no matter on which node this method is invoked! This is,
	 * because {@code readUserIdentity} is not associated with a directory - it's global! Technically, it is assigned
	 * to the root (at least right now - this might change later), but semantically, it is not associated with any.
	 *
	 * @param localPath the directory/file whose permissions to query. Must not be <code>null</code>.
	 * @param userRepoKeyId the user-key's identifier for which to determine the permissions granted. Must not be <code>null</code>.
	 * @return the {@link PermissionType}s granted. Never <code>null</code>, but maybe empty!
	 * @see #grantPermission(String, PermissionType, PublicKey)
	 * @see #revokePermission(String, PermissionType, Set)
	 * @see #assertHasPermission(String, Uid, PermissionType, Date)
	 */
	Set<PermissionType> getGrantedPermissionTypes(String localPath, Uid userRepoKeyId);

	Set<PermissionType> getEffectivePermissionTypes(String localPath, Uid userRepoKeyId);

	Set<PermissionType> getInheritedPermissionTypes(String localPath, Uid userRepoKeyId);

//	/**
//	 * Has the specified user a permission of the specified type on the specified path?
//	 * <p>
//	 * The user is represented by his repository-dependent {@code userRepoKeyId}. Inheritance, i.e. permissions granted on
//	 * a parent directory are taken into account by this method.
//	 *
//	 * @param localPath
//	 * @param userRepoKeyId
//	 * @param permissionType
//	 * @return
//	 */
//	boolean hasPermission(String localPath, Uid userRepoKeyId, PermissionType permissionType);

	/**
	 * Gets the identity of the owner's repo-key.
	 * @return the identity of the owner's repo-key. May be <code>null</code> during the initial set-up of
	 * a repository, but should never be <code>null</code> later on.
	 */
	Uid getOwnerUserRepoKeyId();
}
