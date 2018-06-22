package org.subshare.core;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.sign.WriteProtected;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public interface Cryptree {

	CryptreeFactory getCryptreeFactory();
	void setCryptreeFactory(CryptreeFactory cryptreeFactory);

	LocalRepoTransaction getTransaction();
	void setTransaction(LocalRepoTransaction transaction);

	UserRepoKeyRing getUserRepoKeyRing();
	void setUserRepoKeyRing(UserRepoKeyRing userRepoKeyRing);

	UUID getRemoteRepositoryId();
	void setRemoteRepositoryId(UUID serverRepositoryId);

	String getRemotePathPrefix();
	void setRemotePathPrefix(String remotePathPrefix);

	void initLocalRepositoryType();

	/**
	 * Creates or updates the {@code CryptoRepoFile} associated to the given path. Then returns a
	 * {@link CryptoChangeSetDto} with this one corresponding {@link CryptoRepoFileDto} and
	 * all changed {@code CryptoKey}s + {@code CryptoLink}s.
	 * <p>
	 * This method is equivalent to {@link #getCryptoChangeSetDtoOrFail(String)}, but in contrast to this
	 * method, it creates or updates the {@code CryptoRepoFile} before returning the DTO.
	 * <p>
	 * You should invoke {@link #updateLastCryptoKeySyncToRemoteRepo()} afterwards to avoid unnecessarily
	 * resending the same keys + links.
	 * <p>
	 * <b>This method is used on the client side.</b> The client uploads all changed keys + links to the server, but
	 * only individual {@code CryptoRepoFile}s.
	 * @param localPath the path. Must not be <code>null</code>.
	 * @return the {@code CryptoRepoFile} with all {@link CryptoKeyDto}s and {@link CryptoLinkDto}s changed after
	 * the last call to {@link #updateLastCryptoKeySyncToRemoteRepo()} and one single {@link CryptoRepoFileDto}
	 * identified by the given {@code path}. Never <code>null</code>.
	 */
	CryptoChangeSetDto createOrUpdateCryptoRepoFile(String localPath);

	/**
	 * Gets a {@link CryptoChangeSetDto} with the one {@link CryptoRepoFileDto} that is referenced by
	 * {@code path} and all changed {@code CryptoKey}s + {@code CryptoLink}s.
	 * <p>
	 * This method is equivalent to {@link #createOrUpdateCryptoRepoFile(String)}, but in contrast to this
	 * method, it does not modify any data in the DB - it only reads and creates the DTO.
	 * <p>
	 * You should invoke {@link #updateLastCryptoKeySyncToRemoteRepo()} afterwards to avoid unnecessarily
	 * resending the same keys + links.
	 * <p>
	 * <b>This method is used on the client side.</b> The client uploads all changed keys + links to the server, but
	 * only individual {@code CryptoRepoFile}s.
	 * @param localPath the path. Must not be <code>null</code>.
	 * @return the {@code CryptoRepoFile} with all {@link CryptoKeyDto}s and {@link CryptoLinkDto}s changed after
	 * the last call to {@link #updateLastCryptoKeySyncToRemoteRepo()} and one single {@link CryptoRepoFileDto}
	 * identified by the given {@code path}. Never <code>null</code>.
	 */
	CryptoChangeSetDto getCryptoChangeSetDtoOrFail(String localPath);

	String getServerPath(String localPath);
	String getLocalPath(String serverPath);

	void prepareGetCryptoChangeSetDtoWithCryptoRepoFiles(Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);

	/**
	 * Gets a {@link CryptoChangeSetDto} with all those {@link CryptoRepoFileDto}s, {@link CryptoKeyDto}s
	 * and {@link CryptoLinkDto}s that were changed after the last invocation of
	 * {@link #updateLastCryptoKeySyncToRemoteRepo()}.
	 * <p>
	 * <b>This method is used on the server side.</b> The server sends not only the changed keys + links,
	 * but also all changed {@code CryptoRepoFile}s.
	 * @param lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced TODO
	 * @return a {@link CryptoChangeSetDto}. Never <code>null</code>.
	 */
	CryptoChangeSetDto getCryptoChangeSetDtoWithCryptoRepoFiles(Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);

	void updateLastCryptoKeySyncToRemoteRepo();

	/**
	 * Gets the <i>current</i> data-key for the given path -- used to encrypt a data block (chunk).
	 * @param localPath the local path; must not be <code>null</code>.
	 * @return the data-key; never <code>null</code>.
	 */
	DataKey getDataKeyOrFail(String localPath);

	/**
	 * Gets the data-key identified by the given ID.
	 * @param cryptoKeyId the ID of the {@code CryptoKey} to be decrypted and returned in plain. Must not be <code>null</code>.
	 * @return the data-key; never <code>null</code>.
	 */
	DataKey getDataKeyOrFail(Uid cryptoKeyId);

	void putCryptoChangeSetDto(CryptoChangeSetDto cryptoChangeSetDto);

	Uid getRootCryptoRepoFileId();
	RepoFileDto getDecryptedRepoFileDtoOrFail(Uid cryptoRepoFileId) throws AccessDeniedException;
	RepoFileDto getDecryptedRepoFileDto(String localPath) throws AccessDeniedException;

	boolean isEmpty();

	UserRepoKeyPublicKeyLookup getUserRepoKeyPublicKeyLookup();

	UserRepoKey getUserRepoKey(String localPath, PermissionType permissionType);
	UserRepoKey getUserRepoKeyOrFail(String localPath, PermissionType permissionType) throws AccessDeniedException;

	void grantPermission(String localPath, PermissionType permissionType, UserRepoKey.PublicKey userRepoKeyPublicKey);
	void revokePermission(String localPath, PermissionType permissionType, Set<Uid> userRepoKeyIds);

	void grantPermission(Uid cryptoRepoFileId, PermissionType permissionType, UserRepoKey.PublicKey userRepoKeyPublicKey);
	void revokePermission(Uid cryptoRepoFileId, PermissionType permissionType, Set<Uid> userRepoKeyIds);

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
	 * @see #grantPermission(String, PermissionType, UserRepoKey.PublicKey)
	 * @see #revokePermission(String, PermissionType, Set)
	 * @see #assertHasPermission(String, Uid, PermissionType, Date)
	 */
	Set<PermissionType> getGrantedPermissionTypes(String localPath, Uid userRepoKeyId);

	Uid getCryptoRepoFileId(String localPath);
	Uid getCryptoRepoFileIdOrFail(String localPath);
	Uid getParentCryptoRepoFileId(Uid cryptoRepoFileId);

	void assertHasPermission(Uid cryptoRepoFileId, Uid userRepoKeyId, PermissionType permissionType, Date timestamp) throws AccessDeniedException;
	void assertHasPermission(String localPath, Uid userRepoKeyId, PermissionType permissionType, Date timestamp) throws AccessDeniedException;

	/**
	 * Gets the {@link CryptoRepoFile#getCryptoRepoFileId() cryptoRepoFileId} of the {@code CryptoRepoFile}
	 * which corresponds to the root-directory that's checked out.
	 * <p>
	 * This method can only be used, if the local repository is connected to a sub-directory of the server
	 * repository. If it is connected to the server repository's root, there is no
	 * {@link #getRemotePathPrefix() remotePathPrefix} (it is an empty string) and the ID can therefore not
	 * be read from it.
	 * <p>
	 * Additionally, this method can only be used on the client-side!
	 * @return the {@link CryptoRepoFile#getCryptoRepoFileId() cryptoRepoFileId} of the {@code CryptoRepoFile}
	 * which is the connection point of the local repository to the server's repository.
	 */
	Uid getCryptoRepoFileIdForRemotePathPrefixOrFail();
	void setPermissionsInherited(String localPath, boolean inherited);
	boolean isPermissionsInherited(String localPath);
	void setPermissionsInherited(Uid cryptoRepoFileId, boolean inherited);
	boolean isPermissionsInherited(Uid cryptoRepoFileId);

	void requestReplaceInvitationUserRepoKey(UserRepoKey invitationUserRepoKey, UserRepoKey.PublicKey publicKey);
	void registerRemotePathPrefix(String pathPrefix);

	UserIdentityPayloadDto getUserIdentityPayloadDtoOrFail(Uid userRepoKeyId)
			throws ReadUserIdentityAccessDeniedException;

	void sign(WriteProtected writeProtected) throws AccessDeniedException;
	void assertSignatureOk(WriteProtected writeProtected) throws SignatureException, AccessDeniedException;

	CurrentHistoCryptoRepoFileDto createCurrentHistoCryptoRepoFileDto(String localPath, boolean withHistoCryptoRepoFileDto);

	RepoFileDto getDecryptedRepoFileOnServerDtoOrFail(Uid cryptoRepoFileId) throws AccessDeniedException;
	RepoFileDto getDecryptedRepoFileOnServerDto(String localPath);
	Uid getOwnerUserRepoKeyId();

	LocalRepoStorage getLocalRepoStorage();

	void createUnsealedHistoFrameIfNeeded();
	void sealUnsealedHistoryFrame();
	void putHistoFrameDto(HistoFrameDto histoFrameDto);
	void preDelete(String localPath, boolean deletedByIgnoreRule);
	CryptoChangeSetDto createHistoCryptoRepoFilesForDeletedCryptoRepoFiles();
	void createSyntheticDeleteModifications(ChangeSetDto changeSetDto);
//	void createSyntheticDeleteModifications(ChangeSetDto changeSetDto, CryptoChangeSetDto cryptoChangeSetDto);
	Collection<PlainHistoCryptoRepoFileDto> getPlainHistoCryptoRepoFileDtos(PlainHistoCryptoRepoFileFilter filter);
	PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto(Uid histoCryptoRepoFileId);
	void clearCryptoRepoFileDeleted(String localPath);
	void assertIsNotDeletedDuplicateCryptoRepoFile(Uid cryptoRepoFileId);
	void putCollisionPrivateDto(CollisionPrivateDto collisionPrivateDto);
	void removeOrphanedInvitationUserRepoKeyPublicKeys();
	ConfigPropSetDto getParentConfigPropSetDtoIfNeeded();
	void updatePlainHistoCryptoRepoFiles(Set<Uid> histoCryptoRepoFileIds);

	Long getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced();
//	void setLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced(long revision);

//	void createCollisionIfNeeded(String localPath);
}
