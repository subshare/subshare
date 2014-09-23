package org.subshare.core;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
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

	/**
	 * Gets a {@link CryptoChangeSetDto} with all those {@link CryptoRepoFileDto}s, {@link CryptoKeyDto}s
	 * and {@link CryptoLinkDto}s that were changed after the last invocation of
	 * {@link #updateLastCryptoKeySyncToRemoteRepo()}.
	 * <p>
	 * <b>This method is used on the server side.</b> The server sends not only the changed keys + links,
	 * but also all changed {@code CryptoRepoFile}s.
	 * @return a {@link CryptoChangeSetDto}. Never <code>null</code>.
	 */
	CryptoChangeSetDto getCryptoChangeSetDtoWithCryptoRepoFiles();

	void updateLastCryptoKeySyncToRemoteRepo();
	KeyParameter getDataKeyOrFail(String localPath);
	void putCryptoChangeSetDto(CryptoChangeSetDto cryptoChangeSetDto);

	Uid getRootCryptoRepoFileId();
	RepoFileDto getDecryptedRepoFileDtoOrFail(Uid cryptoRepoFileId) throws AccessDeniedException;
	RepoFileDto getDecryptedRepoFileDto(String localPath) throws AccessDeniedException;

	boolean isEmpty();

	UserRepoKeyPublicKeyLookup getUserRepoKeyPublicKeyLookup();

	UserRepoKey getUserRepoKey(String localPath, PermissionType permissionType);
	UserRepoKey getUserRepoKeyOrFail(String localPath, PermissionType permissionType) throws AccessDeniedException;

	void grantPermission(String localPath, PermissionType permissionType, PublicKey userRepoKeyPublicKey);
	void revokePermission(String localPath, PermissionType permissionType, Set<Uid> userRepoKeyIds);

	void grantPermission(Uid cryptoRepoFileId, PermissionType permissionType, PublicKey userRepoKeyPublicKey);
	void revokePermission(Uid cryptoRepoFileId, PermissionType permissionType, Set<Uid> userRepoKeyIds);

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
}
