package org.subshare.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.AbstractCryptree;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.PermissionCollisionException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.PermissionDto;
import org.subshare.core.dto.PermissionSetDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.RepositoryOwnerDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.sign.Signature;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.SsLocalRepository;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepo;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepoDao;
import org.subshare.local.persistence.LocalRepositoryType;
import org.subshare.local.persistence.Permission;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.PermissionSet;
import org.subshare.local.persistence.PermissionSetDao;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.RepositoryOwnerDao;
import org.subshare.local.persistence.SignableDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyLookupImpl;
import org.subshare.local.persistence.WriteProtectedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class CryptreeImpl extends AbstractCryptree {

	private static final Logger logger = LoggerFactory.getLogger(CryptreeImpl.class);

	private UserRepoKeyPublicKeyLookupImpl userRepoKeyPublicKeyLookup;

	private UUID localRepositoryId;
	private UUID serverRepositoryId;

	private CryptreeContext cryptreeContext;

	private Uid rootCryptoRepoFileId;

	@Override
	public UserRepoKeyPublicKeyLookup getUserRepoKeyPublicKeyLookup() {
		if (userRepoKeyPublicKeyLookup == null)
			userRepoKeyPublicKeyLookup = new UserRepoKeyPublicKeyLookupImpl(getTransactionOrFail());

		return userRepoKeyPublicKeyLookup;
	}

	@Override
	public CryptoChangeSetDto createOrUpdateCryptoRepoFile(final String localPath) {
		claimRepositoryOwnershipIfUnowned();
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFileOrCreate(true);

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoOrFail(final String localPath) {
		claimRepositoryOwnershipIfUnowned();
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

	@Override
	public String getServerPath(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		return cryptoRepoFile.getServerPath();
	}

	@Override
	public KeyParameter getDataKeyOrFail(final String path) { // TODO shouldn't this be 'localPath'?
		assertNotNull("path", path);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final RepoFile repoFile = repoFileDao.getRepoFile(localRepoManager.getLocalRoot(), createFile(localRepoManager.getLocalRoot(), path));
		assertNotNull("repoFile", repoFile);

		final CryptreeNode cryptreeNode = new CryptreeNode(getCryptreeContext(), repoFile);
		return cryptreeNode.getDataKeyOrFail();
	}

	protected CryptreeContext getCryptreeContext() {
		if (cryptreeContext == null)
			cryptreeContext = new CryptreeContext(
					getUserRepoKeyRing(), getTransactionOrFail(),
					getLocalRepositoryIdOrFail(), getRemoteRepositoryIdOrFail(), getServerRepositoryIdOrFail(),
					getRemotePathPrefix(), isOnServer());

		return cryptreeContext;
	}

	protected UUID getLocalRepositoryIdOrFail() {
		if (localRepositoryId == null) {
			final LocalRepositoryDao localRepositoryDao = getTransactionOrFail().getDao(LocalRepositoryDao.class);
			final LocalRepository localRepository = localRepositoryDao.getLocalRepositoryOrFail();
			localRepositoryId = localRepository.getRepositoryId();
		}
		return localRepositoryId;
	}

	protected UUID getServerRepositoryIdOrFail() {
		if (serverRepositoryId == null) {
			if (isOnServer())
				serverRepositoryId = getLocalRepositoryIdOrFail();
			else
				serverRepositoryId = getRemoteRepositoryIdOrFail();
		}
		return serverRepositoryId;
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoWithCryptoRepoFiles() {
		claimRepositoryOwnershipIfUnowned();
		final LocalRepository localRepository = getTransactionOrFail().getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();
		lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());

		// First links then keys, because we query all changed *after* a certain localRevision - and not in a range.
		// Thus, we might find newer keys when querying them after the links. Since the links reference the keys
		// (collection is mapped-by) and we currently don't delete anything, this guarantees that all references can
		// be fulfilled on the remote side. Extremely unlikely, but still better ;-)
		final CryptoChangeSetDto cryptoChangeSetDto = new CryptoChangeSetDto();
		populateChangedUserRepoKeyPublicKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoRepoFileDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoLinkDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		populateChangedRepositoryOwnerDto(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedPermissionDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedPermissionSetDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		return cryptoChangeSetDto;
	}

	@Override
	public void updateLastCryptoKeySyncToRemoteRepo() {
		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();
		final long localRepositoryRevisionInProgress = lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionInProgress();
		if (localRepositoryRevisionInProgress < 0)
			throw new IllegalStateException("localRepositoryRevisionInProgress < 0 :: There is no CryptoKey-sync in progress!");

		lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionSynced(localRepositoryRevisionInProgress);
		lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionInProgress(-1);
	}

	@Override
	public void putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		assertNotNull("cryptoChangeSetDto", cryptoChangeSetDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();

		final Map<CryptoRepoFileDto, CryptoRepoFile> cryptoRepoFileDto2CryptoRepoFile = new HashMap<>();
		final Map<Uid, CryptoKey> cryptoKeyId2CryptoKey = new HashMap<>();

		// This order is important, because the keys must first be persisted, before links or file-meta-data can reference them.
		for (final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto : cryptoChangeSetDto.getUserRepoKeyPublicKeyDtos())
			putUserRepoKeyPublicKeyDto(userRepoKeyPublicKeyDto);

		for (final CryptoRepoFileDto cryptoRepoFileDto : cryptoChangeSetDto.getCryptoRepoFileDtos())
			cryptoRepoFileDto2CryptoRepoFile.put(cryptoRepoFileDto, putCryptoRepoFileDto(cryptoRepoFileDto));

		for (final CryptoKeyDto cryptoKeyDto : cryptoChangeSetDto.getCryptoKeyDtos()) {
			final CryptoKey cryptoKey = putCryptoKeyDto(cryptoKeyDto);
			cryptoKeyId2CryptoKey.put(cryptoKey.getCryptoKeyId(), cryptoKey);
		}

		for (final CryptoLinkDto cryptoLinkDto : cryptoChangeSetDto.getCryptoLinkDtos())
			putCryptoLinkDto(cryptoLinkDto);

		for (final Map.Entry<CryptoRepoFileDto, CryptoRepoFile> me : cryptoRepoFileDto2CryptoRepoFile.entrySet()) {
			final Uid cryptoKeyId = me.getKey().getCryptoKeyId();
			final CryptoRepoFile cryptoRepoFile = me.getValue();

			if (cryptoRepoFile.getCryptoKey() == null || !cryptoKeyId.equals(cryptoRepoFile.getCryptoKey().getCryptoKeyId())) {
				final CryptoKey cryptoKey = cryptoKeyId2CryptoKey.get(cryptoKeyId);
				if (cryptoKey == null)
					throw new IllegalStateException(String.format(
							"The CryptoKey with cryptoKeyId=%s was neither found in the DB nor is it contained in this CryptoChangeSetDto!",
							cryptoKeyId));

				cryptoRepoFile.setCryptoKey(cryptoKey);
			}
		}

		if (! isOnServer()) {
			for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFileDto2CryptoRepoFile.values()) {
				try {
					final RepoFileDto decryptedRepoFileDto = getDecryptedRepoFileDtoOrFail(cryptoRepoFile.getCryptoRepoFileId());
					cryptoRepoFile.setLocalName(decryptedRepoFileDto.getName());
				} catch (final AccessDeniedException x) {
					doNothing();
				}
			}
		}

		final RepositoryOwnerDto repositoryOwnerDto = cryptoChangeSetDto.getRepositoryOwnerDto();
		if (repositoryOwnerDto == null)
			claimRepositoryOwnershipIfUnowned();
		else
			putRepositoryOwnerDto(repositoryOwnerDto);

		for (final PermissionSetDto permissionSetDto : cryptoChangeSetDto.getPermissionSetDtos())
			putPermissionSetDto(permissionSetDto);

		for (final PermissionDto permissionDto : cryptoChangeSetDto.getPermissionDtos())
			putPermissionDto(permissionDto);

		transaction.flush();
	}

	@Override
	public UserRepoKey getUserRepoKey(final String localPath, final PermissionType permissionType) {
		assertNotNull("localPath", localPath);
		assertNotNull("permissionType", permissionType);
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.getUserRepoKey(false, permissionType);
	}

	@Override
	public UserRepoKey getUserRepoKeyOrFail(final String localPath, final PermissionType permissionType) throws AccessDeniedException {
		final UserRepoKey userRepoKey = getUserRepoKey(localPath, permissionType);
		if (userRepoKey == null) {
			final String message = String.format("No UserRepoKey available for '%s' at localPath='%s'!",
					permissionType, localPath);
			switch (permissionType) {
				case grant:
					throw new GrantAccessDeniedException(message);
				case read:
					throw new ReadAccessDeniedException(message);
				case write:
					throw new WriteAccessDeniedException(message);
				default:
					throw new IllegalArgumentException("Unknown PermissionType: " + permissionType);
			}
		}
		return userRepoKey;
	}

	@Override
	public Uid getRootCryptoRepoFileId() {
		if (rootCryptoRepoFileId == null) {
			final CryptoRepoFileDao cryptoRepoFileDao = getTransactionOrFail().getDao(CryptoRepoFileDao.class);
			final CryptoRepoFile rootCryptoRepoFile = cryptoRepoFileDao.getRootCryptoRepoFile();
			rootCryptoRepoFileId = rootCryptoRepoFile == null ? null : rootCryptoRepoFile.getCryptoRepoFileId();
		}
		return rootCryptoRepoFileId;
	}

	@Override
	public RepoFileDto getDecryptedRepoFileDtoOrFail(final Uid cryptoRepoFileId) throws AccessDeniedException {
		assertNotNull("cryptoRepoFileId", cryptoRepoFileId);
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		final RepoFileDto repoFileDto = cryptreeNode.getRepoFileDto();
		assertNotNull("cryptreeNode.getRepoFileDto()", repoFileDto); // The cryptoRepoFile is present, thus this should never be null!
		return repoFileDto;
	}

	@Override
	public RepoFileDto getDecryptedRepoFileDto(final String localPath) throws AccessDeniedException {
		assertNotNull("localPath", localPath);
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.getRepoFileDto();
	}

	@Override
	public void grantPermission(final String localPath, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		assertNotNull("localPath", localPath);
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		cryptreeNode.grantPermission(permissionType, userRepoKeyPublicKey);
	}

	@Override
	public void revokePermission(final String localPath, final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		assertNotNull("localPath", localPath);
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyIds", userRepoKeyIds);
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		cryptreeNode.revokePermission(permissionType, userRepoKeyIds);
	}

	@Override
	public void grantPermission(final Uid cryptoRepoFileId, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		assertNotNull("cryptoRepoFileId", cryptoRepoFileId);
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		cryptreeNode.grantPermission(permissionType, userRepoKeyPublicKey);
	}

	@Override
	public void revokePermission(final Uid cryptoRepoFileId, final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		assertNotNull("cryptoRepoFileId", cryptoRepoFileId);
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyIds", userRepoKeyIds);
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		cryptreeNode.revokePermission(permissionType, userRepoKeyIds);
	}

	/**
	 * Is the code currently executed on the server?
	 * @return <code>true</code>, if this method is invoked on the server. <code>false</code>, if this method
	 * is invoked on the client.
	 */
	protected boolean isOnServer() {
		return getUserRepoKeyRing() == null; // We don't have user keys on the server.
	}

	private CryptoRepoFile putCryptoRepoFileDto(final CryptoRepoFileDto cryptoRepoFileDto) {
		assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final Uid cryptoRepoFileId = assertNotNull("cryptoRepoFileDto.cryptoRepoFileId", cryptoRepoFileDto.getCryptoRepoFileId());
		CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(cryptoRepoFileId);
		if (cryptoRepoFile == null)
			cryptoRepoFile = new CryptoRepoFile(cryptoRepoFileId);

		final Uid cryptoKeyId = assertNotNull("cryptoRepoFileDto.cryptoKeyId", cryptoRepoFileDto.getCryptoKeyId());
		cryptoRepoFile.setCryptoKey(cryptoKeyDao.getCryptoKey(cryptoKeyId)); // may be null, now, if it is persisted later!

		final Uid parentCryptoRepoFileId = cryptoRepoFileDto.getParentCryptoRepoFileId();
		cryptoRepoFile.setParent(parentCryptoRepoFileId == null ? null
				: cryptoRepoFileDao.getCryptoRepoFileOrFail(parentCryptoRepoFileId));

		final String remotePathPrefix = getRemotePathPrefix();
		if (remotePathPrefix == null // on server
				|| remotePathPrefix.isEmpty()) { // on client and complete repo is checked out
			if (parentCryptoRepoFileId == null) {
				// This is the root! Since the root's directory name is the empty string, it cannot be associated
				// by the AssignCryptoRepoFileRepoFileListener, which uses the RepoFile.name to associate them.
				// On the server, the RepoFile.name equals the CryptoRepoFile.cryptoRepoFileId - except for the root!
				final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
				cryptoRepoFile.setRepoFile(localRepository.getRoot());
			}
		}
		else {
			final Uid id = getCryptreeContext().getCryptoRepoFileIdForRemotePathPrefixOrFail();
			if (id.equals(cryptoRepoFile.getCryptoRepoFileId())) {
				final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
				cryptoRepoFile.setRepoFile(localRepository.getRoot());
			}
		}

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFileDto.repoFileDtoData", cryptoRepoFileDto.getRepoFileDtoData());
		cryptoRepoFile.setRepoFileDtoData(repoFileDtoData);

		cryptoRepoFile.setDirectory(cryptoRepoFileDto.isDirectory());
		cryptoRepoFile.setLastSyncFromRepositoryId(getRemoteRepositoryId());

		cryptoRepoFile.setSignature(cryptoRepoFileDto.getSignature());

		return cryptoRepoFileDao.makePersistent(cryptoRepoFile);
	}

	private UserRepoKeyPublicKey putUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto) {
		assertNotNull("userRepoKeyPublicKeyDto", userRepoKeyPublicKeyDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		final Uid userRepoKeyId = assertNotNull("userRepoKeyPublicKeyDto.userRepoKeyId", userRepoKeyPublicKeyDto.getUserRepoKeyId());
		UserRepoKeyPublicKey userRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKey(userRepoKeyId);
		if (userRepoKeyPublicKey == null)
			userRepoKeyPublicKey = new UserRepoKeyPublicKey(userRepoKeyId);

		userRepoKeyPublicKey.setServerRepositoryId(userRepoKeyPublicKeyDto.getRepositoryId());

		if (userRepoKeyPublicKey.getPublicKeyData() == null)
			userRepoKeyPublicKey.setPublicKeyData(userRepoKeyPublicKeyDto.getPublicKeyData());
		else if (! Arrays.equals(userRepoKeyPublicKey.getPublicKeyData(), userRepoKeyPublicKeyDto.getPublicKeyData()))
			throw new IllegalStateException("Cannot re-assign UserRepoKeyPublicKey.publicKeyData! Attack?!");

		return userRepoKeyPublicKeyDao.makePersistent(userRepoKeyPublicKey);
	}

	private CryptoKey putCryptoKeyDto(final CryptoKeyDto cryptoKeyDto) {
		assertNotNull("cryptoKeyDto", cryptoKeyDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final Uid cryptoKeyId = assertNotNull("cryptoKeyDto.cryptoKeyId", cryptoKeyDto.getCryptoKeyId());
		CryptoKey cryptoKey = cryptoKeyDao.getCryptoKey(cryptoKeyId);
		final boolean cryptoKeyIsNew;
		if (cryptoKey == null) {
			cryptoKeyIsNew = true;
			cryptoKey = new CryptoKey(cryptoKeyId);
		}
		else
			cryptoKeyIsNew = false;

		// It is necessary to prevent a collision and ensure that the 'active' property cannot be reverted.
		if (cryptoKeyDto.isActive() && ! cryptoKey.isActive()) {
			logger.warn("putCryptoKeyDto: Rejecting to re-activate CryptoKey! Keeping (and re-publishing) previous state: {}", cryptoKey);
			cryptoKey.setChanged(new Date()); // only to make it dirty => force new localRevision => force sync.
			if (cryptoKeyIsNew)
				throw new IllegalStateException("Cannot reject, because the CryptoKey is new! " + cryptoKey);

			return cryptoKey;
		}

		cryptoKey.setActive(cryptoKeyDto.isActive());
		cryptoKey.setCryptoKeyRole(cryptoKeyDto.getCryptoKeyRole());
		cryptoKey.setCryptoKeyType(cryptoKeyDto.getCryptoKeyType());

		final Uid cryptoRepoFileId = assertNotNull("cryptoKeyDto.cryptoRepoFileId", cryptoKeyDto.getCryptoRepoFileId());
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
		cryptoKey.setCryptoRepoFile(cryptoRepoFile);

		assertNotNull("cryptoKeyDto.signature", cryptoKeyDto.getSignature());
		assertNotNull("cryptoKeyDto.signature.signatureCreated", cryptoKeyDto.getSignature().getSignatureCreated());
		assertNotNull("cryptoKeyDto.signature.signingUserRepoKeyId", cryptoKeyDto.getSignature().getSigningUserRepoKeyId());
		assertNotNull("cryptoKeyDto.signature.signatureData", cryptoKeyDto.getSignature().getSignatureData());

		cryptoKey.setSignature(cryptoKeyDto.getSignature());

		return cryptoKeyDao.makePersistent(cryptoKey);
	}

	private CryptoLink putCryptoLinkDto(final CryptoLinkDto cryptoLinkDto) {
		assertNotNull("cryptoLinkDto", cryptoLinkDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoLinkDao cryptoLinkDao = transaction.getDao(CryptoLinkDao.class);
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		final Uid cryptoLinkId = assertNotNull("cryptoLinkDto.cryptoLinkId", cryptoLinkDto.getCryptoLinkId());
		CryptoLink cryptoLink = cryptoLinkDao.getCryptoLink(cryptoLinkId);
		if (cryptoLink == null)
			cryptoLink = new CryptoLink(cryptoLinkId);

		final Uid fromCryptoKeyId = cryptoLinkDto.getFromCryptoKeyId();
		cryptoLink.setFromCryptoKey(fromCryptoKeyId == null ? null : cryptoKeyDao.getCryptoKeyOrFail(fromCryptoKeyId));

		final Uid fromUserRepoKeyId = cryptoLinkDto.getFromUserRepoKeyId();
		cryptoLink.setFromUserRepoKeyPublicKey(fromUserRepoKeyId == null ? null : userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeyOrFail(fromUserRepoKeyId));

		final Uid toCryptoKeyId = assertNotNull("cryptoLinkDto.toCryptoKeyId", cryptoLinkDto.getToCryptoKeyId());
		final CryptoKey toCryptoKey = cryptoKeyDao.getCryptoKeyOrFail(toCryptoKeyId);
		cryptoLink.setToCryptoKey(toCryptoKey);

		cryptoLink.setToCryptoKeyData(cryptoLinkDto.getToCryptoKeyData());
		cryptoLink.setToCryptoKeyPart(cryptoLinkDto.getToCryptoKeyPart());
		cryptoLink.setSignature(cryptoLinkDto.getSignature());

		toCryptoKey.getInCryptoLinks().add(cryptoLink);
		return cryptoLinkDao.makePersistent(cryptoLink);
	}

	private void putPermissionDto(final PermissionDto permissionDto) {
		assertNotNull("permissionDto", permissionDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final PermissionDao permissionDao = transaction.getDao(PermissionDao.class);
		final PermissionSetDao permissionSetDao = transaction.getDao(PermissionSetDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		Permission permission = permissionDao.getPermission(permissionDto.getPermissionId());
		if (permission == null)
			permission = new Permission(permissionDto.getPermissionId());

		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(permissionDto.getCryptoRepoFileId());
		final PermissionSet permissionSet = permissionSetDao.getPermissionSetOrFail(cryptoRepoFile);
		permission.setPermissionSet(permissionSet);
		permission.setPermissionType(permissionDto.getPermissionType());
		permission.setRevoked(permissionDto.getRevoked());
		permission.setSignature(permissionDto.getSignature());

		final UserRepoKeyPublicKey userRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeyOrFail(permissionDto.getUserRepoKeyId());
		permission.setUserRepoKeyPublicKey(userRepoKeyPublicKey);

		permission.setValidFrom(permissionDto.getValidFrom());
		final Date oldValidTo = permission.getValidTo();
		permission.setValidTo(permissionDto.getValidTo());
		permissionDao.makePersistent(permission);

		if (permission.getValidTo() != null && oldValidTo == null) {
			if (isOnServer()) {
				// We must prevent the new permission from making existing data on the server illegal.
				// Hence, we roll back and throw an exception, if this happens.
				//
				// See: enactPermissionRevocationIfNeededAndPossible(...)

				final SignableDao signableDao = getTransactionOrFail().getDao(SignableDao.class);
				if (signableDao.isEntitiesSignedByAndAfter(permission.getUserRepoKeyPublicKey().getUserRepoKeyId(), permission.getValidTo()))
					throw new PermissionCollisionException("There is already data written and signed after the Permission.validTo timestamp. Are clocks in-sync?");
			}
			else {
				// TODO we should clean up this illegal state on the client, too! It's one of the many collisions we have to cope with!
			}
		}
	}

	private void putPermissionSetDto(final PermissionSetDto permissionSetDto) {
		assertNotNull("permissionSetDto", permissionSetDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final PermissionSetDao permissionSetDao = transaction.getDao(PermissionSetDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(permissionSetDto.getCryptoRepoFileId());
		PermissionSet permissionSet = permissionSetDao.getPermissionSet(cryptoRepoFile);
		if (permissionSet == null)
			permissionSet = new PermissionSet();

		permissionSet.setCryptoRepoFile(cryptoRepoFile);
		permissionSet.setPermissionsInherited(permissionSetDto.isPermissionsInherited());
		permissionSet.setSignature(permissionSetDto.getSignature());
		permissionSetDao.makePersistent(permissionSet);
	}

	private void putRepositoryOwnerDto(final RepositoryOwnerDto repositoryOwnerDto) {
		assertNotNull("repositoryOwnerDto", repositoryOwnerDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final RepositoryOwnerDao repositoryOwnerDao = transaction.getDao(RepositoryOwnerDao.class);
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		final UserRepoKeyPublicKey userRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeyOrFail(repositoryOwnerDto.getUserRepoKeyId());
		if (! repositoryOwnerDto.getServerRepositoryId().equals(userRepoKeyPublicKey.getServerRepositoryId()))
			throw new IllegalStateException(String.format(
					"repositoryOwnerDto.serverRepositoryId != userRepoKeyPublicKey.serverRepositoryId :: %s != %s :: userRepoKeyId=%s",
					repositoryOwnerDto.getServerRepositoryId(), userRepoKeyPublicKey.getServerRepositoryId(),
					userRepoKeyPublicKey.getUserRepoKeyId()));

		RepositoryOwner repositoryOwner = repositoryOwnerDao.getRepositoryOwner(repositoryOwnerDto.getServerRepositoryId());
		if (repositoryOwner == null)
			repositoryOwner = new RepositoryOwner();

		if (repositoryOwner.getUserRepoKeyPublicKey() == null)
			repositoryOwner.setUserRepoKeyPublicKey(userRepoKeyPublicKey);
		else if (! userRepoKeyPublicKey.equals(repositoryOwner.getUserRepoKeyPublicKey()))
			throw new IllegalStateException(String.format(
					"Cannot re-assign RepositoryOwner.userRepoKeyPublicKey! Attack?! assignedUserRepoKeyId=%s newUserRepoKeyId=%s",
					repositoryOwner.getUserRepoKeyPublicKey().getUserRepoKeyId(), userRepoKeyPublicKey.getUserRepoKeyId()));

		repositoryOwner.setSignature(repositoryOwnerDto.getSignature());
		repositoryOwnerDao.makePersistent(repositoryOwner);
	}

	protected LastCryptoKeySyncToRemoteRepo getLastCryptoKeySyncToRemoteRepo() {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(getRemoteRepositoryIdOrFail());

		final LastCryptoKeySyncToRemoteRepoDao lastCryptoKeySyncToRemoteRepoDao = transaction.getDao(LastCryptoKeySyncToRemoteRepoDao.class);
		LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = lastCryptoKeySyncToRemoteRepoDao.getLastCryptoKeySyncToRemoteRepo(remoteRepository);
		if (lastCryptoKeySyncToRemoteRepo == null) {
			lastCryptoKeySyncToRemoteRepo = new LastCryptoKeySyncToRemoteRepo();
			lastCryptoKeySyncToRemoteRepo.setRemoteRepository(remoteRepository);
			lastCryptoKeySyncToRemoteRepo = lastCryptoKeySyncToRemoteRepoDao.makePersistent(lastCryptoKeySyncToRemoteRepo);
		}
		return lastCryptoKeySyncToRemoteRepo;
	}

	protected CryptoChangeSetDto getCryptoChangeSetDto(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		final LocalRepository localRepository = getTransactionOrFail().getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();
		lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());

		final CryptoChangeSetDto cryptoChangeSetDto = new CryptoChangeSetDto();
		cryptoChangeSetDto.getCryptoRepoFileDtos().add(toCryptoRepoFileDto(cryptoRepoFile));
		populateChangedUserRepoKeyPublicKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoLinkDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		populateChangedRepositoryOwnerDto(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedPermissionDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedPermissionSetDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		return cryptoChangeSetDto;
	}

	private void populateChangedUserRepoKeyPublicKeyDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = getTransactionOrFail().getDao(UserRepoKeyPublicKeyDao.class);

		final Collection<UserRepoKeyPublicKey> userRepoKeyPublicKeys = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeysChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final UserRepoKeyPublicKey userRepoKeyPublicKey : userRepoKeyPublicKeys)
			cryptoChangeSetDto.getUserRepoKeyPublicKeyDtos().add(toUserRepoKeyPublicKeyDto(userRepoKeyPublicKey));
	}

	private void populateChangedCryptoRepoFileDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CryptoRepoFileDao cryptoRepoFileDao = getTransactionOrFail().getDao(CryptoRepoFileDao.class);

		final Collection<CryptoRepoFile> cryptoRepoFiles = cryptoRepoFileDao.getCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(), getRemoteRepositoryIdOrFail());

		for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFiles)
			cryptoChangeSetDto.getCryptoRepoFileDtos().add(toCryptoRepoFileDto(cryptoRepoFile));
	}

	private void populateChangedCryptoLinkDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CryptoLinkDao cryptoLinkDao = getTransactionOrFail().getDao(CryptoLinkDao.class);

		final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getCryptoLinksChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final CryptoLink cryptoLink : cryptoLinks)
			cryptoChangeSetDto.getCryptoLinkDtos().add(toCryptoLinkDto(cryptoLink));
	}

	private void populateChangedCryptoKeyDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CryptoKeyDao cryptoKeyDao = getTransactionOrFail().getDao(CryptoKeyDao.class);

		final Collection<CryptoKey> cryptoKeys = cryptoKeyDao.getCryptoKeysChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final CryptoKey cryptoKey : cryptoKeys)
			cryptoChangeSetDto.getCryptoKeyDtos().add(toCryptoKeyDto(cryptoKey));
	}

	private void populateChangedRepositoryOwnerDto(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final RepositoryOwnerDao repositoryOwnerDao = getTransactionOrFail().getDao(RepositoryOwnerDao.class);
		final RepositoryOwner repositoryOwner = repositoryOwnerDao.getRepositoryOwner(getServerRepositoryIdOrFail());
		if (repositoryOwner != null
				&& repositoryOwner.getLocalRevision() > lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced()) {
			cryptoChangeSetDto.setRepositoryOwnerDto(toRepositoryOwnerDto(repositoryOwner));
		}
	}

	private void populateChangedPermissionDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final PermissionDao permissionDao = getTransactionOrFail().getDao(PermissionDao.class);

		final Collection<Permission> permissions = permissionDao.getPermissionsChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final Permission permission : permissions) {
			enactPermissionRevocationIfNeededAndPossible(permission);

			cryptoChangeSetDto.getPermissionDtos().add(toPermissionDto(permission));
		}
	}

	private void enactPermissionRevocationIfNeededAndPossible(final Permission permission) {
		if (! isOnServer() && permission.getRevoked() != null && permission.getValidTo() == null) {
			// We set Permission.validTo delayed (not immediately when revoking), but when synchronising
			// up to the server. This means, a revocation is not active until the next sync. It also
			// means, we can never end up with illegal data inside the server - we might only
			// end up with illegal data in any client. This can (and should) be reverted, later.
			//
			// If, due to wrong clocks (time off by a few minutes), the time here causes a collision
			// because it's earlier than data already committed into the server, the upload
			// of the crypto-change-set is rejected by the server with an exception. This causes
			// the local transaction to be rolled back (it is committed, after the crypto-change-set
			// is written to the server).
			//
			// See: putPermissionDto(...)

			permission.setValidTo(new Date());
			try {
				sign(permission);
			} catch (final GrantAccessDeniedException x) {
				permission.setValidTo(null); // revert to restore the signed state.
				logger.warn("Cannot enact revocation because of missing permission: " + x, x);
			}
		}
	}

	private void sign(final WriteProtectedEntity writeProtectedEntity) throws AccessDeniedException {
		final CryptreeNode rootCryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(getRootCryptoRepoFileId());
		rootCryptreeNode.sign(writeProtectedEntity);
	}

	private void populateChangedPermissionSetDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final PermissionSetDao permissionSetDao = getTransactionOrFail().getDao(PermissionSetDao.class);

		final Collection<PermissionSet> permissionSets = permissionSetDao.getPermissionSetsChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final PermissionSet permissionSet : permissionSets)
			cryptoChangeSetDto.getPermissionSetDtos().add(toPermissionSetDto(permissionSet));
	}

	private UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);
		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto = new UserRepoKeyPublicKeyDto();
		userRepoKeyPublicKeyDto.setLocalRevision(userRepoKeyPublicKey.getLocalRevision());
		userRepoKeyPublicKeyDto.setPublicKeyData(userRepoKeyPublicKey.getPublicKeyData());
		userRepoKeyPublicKeyDto.setRepositoryId(userRepoKeyPublicKey.getServerRepositoryId());
		userRepoKeyPublicKeyDto.setUserRepoKeyId(userRepoKeyPublicKey.getUserRepoKeyId());
		return userRepoKeyPublicKeyDto;
	}

	private CryptoRepoFileDto toCryptoRepoFileDto(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		final CryptoRepoFileDto cryptoRepoFileDto = new CryptoRepoFileDto();

		cryptoRepoFileDto.setCryptoRepoFileId(cryptoRepoFile.getCryptoRepoFileId());

		final CryptoRepoFile parent = cryptoRepoFile.getParent();
		cryptoRepoFileDto.setParentCryptoRepoFileId(parent == null ? null : parent.getCryptoRepoFileId());

		final CryptoKey cryptoKey = assertNotNull("cryptoRepoFile.cryptoKey", cryptoRepoFile.getCryptoKey());
		cryptoRepoFileDto.setCryptoKeyId(cryptoKey.getCryptoKeyId());

		cryptoRepoFileDto.setDirectory(cryptoRepoFile.isDirectory());

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFile.repoFileDtoData", cryptoRepoFile.getRepoFileDtoData());
		cryptoRepoFileDto.setRepoFileDtoData(repoFileDtoData);

		cryptoRepoFileDto.setSignature(assertNotNull("cryptoRepoFile.signature", cryptoRepoFile.getSignature()));

		return cryptoRepoFileDto;
	}

	private CryptoLinkDto toCryptoLinkDto(final CryptoLink cryptoLink) {
		assertNotNull("cryptoLink", cryptoLink);
		final CryptoLinkDto cryptoLinkDto = new CryptoLinkDto();
		cryptoLinkDto.setCryptoLinkId(cryptoLink.getCryptoLinkId());

		final CryptoKey fromCryptoKey = cryptoLink.getFromCryptoKey();
		cryptoLinkDto.setFromCryptoKeyId(fromCryptoKey == null ? null : fromCryptoKey.getCryptoKeyId());

		final UserRepoKeyPublicKey fromUserRepoKeyPublicKey = cryptoLink.getFromUserRepoKeyPublicKey();
		cryptoLinkDto.setFromUserRepoKeyId(fromUserRepoKeyPublicKey == null ? null : fromUserRepoKeyPublicKey.getUserRepoKeyId());
		cryptoLinkDto.setLocalRevision(cryptoLink.getLocalRevision());
		cryptoLinkDto.setToCryptoKeyData(cryptoLink.getToCryptoKeyData());
		cryptoLinkDto.setToCryptoKeyId(cryptoLink.getToCryptoKey().getCryptoKeyId());
		cryptoLinkDto.setToCryptoKeyPart(cryptoLink.getToCryptoKeyPart());
		cryptoLinkDto.setSignature(assertNotNull("cryptoLink.signature", cryptoLink.getSignature()));
		return cryptoLinkDto;
	}

	private CryptoKeyDto toCryptoKeyDto(final CryptoKey cryptoKey) {
		assertNotNull("cryptoKey", cryptoKey);
		final CryptoKeyDto cryptoKeyDto = new CryptoKeyDto();
		cryptoKeyDto.setCryptoKeyId(cryptoKey.getCryptoKeyId());
		cryptoKeyDto.setCryptoRepoFileId(cryptoKey.getCryptoRepoFile().getCryptoRepoFileId());
		cryptoKeyDto.setActive(cryptoKey.isActive());
		cryptoKeyDto.setCryptoKeyRole(assertNotNull("cryptoKey.cryptoKeyRole", cryptoKey.getCryptoKeyRole()));
		cryptoKeyDto.setCryptoKeyType(assertNotNull("cryptoKey.cryptoKeyType", cryptoKey.getCryptoKeyType()));

		final Signature signature = cryptoKey.getSignature();
		cryptoKeyDto.setSignature(assertNotNull("cryptoKey.signature", signature));
		assertNotNull("cryptoKey.signature.signatureCreated", signature.getSignatureCreated());
		assertNotNull("cryptoKey.signature.signingUserRepoKeyId", signature.getSigningUserRepoKeyId());
		assertNotNull("cryptoKey.signature.signatureData", signature.getSignatureData());

		return cryptoKeyDto;
	}

	private RepositoryOwnerDto toRepositoryOwnerDto(final RepositoryOwner repositoryOwner) {
		assertNotNull("repositoryOwner", repositoryOwner);
		final RepositoryOwnerDto repositoryOwnerDto = new RepositoryOwnerDto();
		repositoryOwnerDto.setServerRepositoryId(repositoryOwner.getServerRepositoryId());
		repositoryOwnerDto.setSignature(repositoryOwner.getSignature());
		repositoryOwnerDto.setUserRepoKeyId(repositoryOwner.getUserRepoKeyPublicKey().getUserRepoKeyId());
		return repositoryOwnerDto;
	}

	private PermissionSetDto toPermissionSetDto(final PermissionSet permissionSet) {
		assertNotNull("permissionSet", permissionSet);
		final PermissionSetDto permissionSetDto = new PermissionSetDto();
		permissionSetDto.setCryptoRepoFileId(permissionSet.getCryptoRepoFile().getCryptoRepoFileId());
		permissionSetDto.setPermissionsInherited(permissionSet.isPermissionsInherited());
		permissionSetDto.setSignature(permissionSet.getSignature());
		return permissionSetDto;
	}

	private PermissionDto toPermissionDto(final Permission permission) {
		assertNotNull("permission", permission);
		final PermissionDto permissionDto = new PermissionDto();
		permissionDto.setCryptoRepoFileId(permission.getPermissionSet().getCryptoRepoFile().getCryptoRepoFileId());
		permissionDto.setPermissionId(permission.getPermissionId());
		permissionDto.setPermissionType(permission.getPermissionType());
		permissionDto.setRevoked(permission.getRevoked());
		permissionDto.setSignature(permission.getSignature());
		permissionDto.setUserRepoKeyId(permission.getUserRepoKeyPublicKey().getUserRepoKeyId());
		permissionDto.setValidFrom(permission.getValidFrom());
		permissionDto.setValidTo(permission.getValidTo());
		return permissionDto;
	}

	@Override
	public boolean isEmpty() {
		final Collection<CryptoRepoFile> childCryptoRepoFiles = getTransactionOrFail().getDao(CryptoRepoFileDao.class).getChildCryptoRepoFiles(null);
		return childCryptoRepoFiles.isEmpty();
	}

	@Override
	public void initLocalRepositoryType() {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final LocalRepository lr = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		final SsLocalRepository localRepository = (SsLocalRepository) lr;

		final LocalRepositoryType localRepositoryType = localRepository.getLocalRepositoryType();
		switch (localRepositoryType) {
			case UNINITIALISED:
				localRepository.setLocalRepositoryType(isOnServer() ? LocalRepositoryType.SERVER : LocalRepositoryType.CLIENT);
				break;
			case CLIENT:
				if (isOnServer())
					throw new IllegalStateException("SsLocalRepository.localRepositoryType is already initialised to CLIENT! Cannot switch to SERVER!");
				break;
			case SERVER:
				if (! isOnServer())
					throw new IllegalStateException("SsLocalRepository.localRepositoryType is already initialised to SERVER! Cannot switch to CLIENT!");
				break;
			default:
				throw new IllegalStateException("Unknown localRepositoryType: " + localRepositoryType);
		}
	}

	private void claimRepositoryOwnershipIfUnowned() {
		if (! isOnServer()) {
			final LocalRepoTransaction transaction = getTransactionOrFail();
			final RepositoryOwnerDao roDao = transaction.getDao(RepositoryOwnerDao.class);
			final UUID serverRepositoryId = getServerRepositoryIdOrFail();
			final UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRingOrFail();

			RepositoryOwner repositoryOwner = roDao.getRepositoryOwner(serverRepositoryId);
			if (repositoryOwner == null) {
				final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
				final UserRepoKey userRepoKey = userRepoKeyRing.getUserRepoKeys(serverRepositoryId).get(0);

				UserRepoKeyPublicKey userRepoKeyPublicKey = urkpkDao.getUserRepoKeyPublicKey(userRepoKey.getUserRepoKeyId());
				if (userRepoKeyPublicKey == null)
					userRepoKeyPublicKey = urkpkDao.makePersistent(new UserRepoKeyPublicKey(userRepoKey.getPublicKey()));

				repositoryOwner = new RepositoryOwner();
				repositoryOwner.setUserRepoKeyPublicKey(userRepoKeyPublicKey);

				getCryptreeContext().getSignableSigner(userRepoKey).sign(repositoryOwner);
				repositoryOwner = roDao.makePersistent(repositoryOwner);
			}
		}
	}

	// TODO this should be exposed as public API - thus, we need to refactor the WriteProtectedEntity interface out of the persistence layer into core (similar to Signable).
	public void assertSignatureOk(final WriteProtectedEntity entity) throws SignatureException, AccessDeniedException {
		CryptoRepoFile cryptoRepoFile = entity.getCryptoRepoFileControllingPermissions();
		if (cryptoRepoFile == null)
			cryptoRepoFile = getTransactionOrFail().getDao(CryptoRepoFileDao.class).getRootCryptoRepoFile();

		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFile.getCryptoRepoFileId());
		cryptreeNode.assertSignatureOk(entity);
	}

	@Override
	public void assertHasPermission(
			final Uid cryptoRepoFileId,
			final Uid userRepoKeyId,
			final PermissionType permissionType, final Date timestamp
			) throws AccessDeniedException
	{
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		cryptreeNode.assertHasPermission(false, userRepoKeyId, permissionType, timestamp);
	}

	@Override
	public void assertHasPermission(
			final String localPath,
			final Uid userRepoKeyId,
			final PermissionType permissionType, final Date timestamp
			) throws AccessDeniedException
	{
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		cryptreeNode.assertHasPermission(false, userRepoKeyId, permissionType, timestamp);
	}
}