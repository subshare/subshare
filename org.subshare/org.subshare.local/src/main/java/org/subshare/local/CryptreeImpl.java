package org.subshare.local;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.AbstractCryptree;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.DataKey;
import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactory;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.PermissionCollisionException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.ReadUserIdentityAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoConfigPropSetDto;
import org.subshare.core.dto.CryptoKeyDeactivationDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.InvitationUserRepoKeyPublicKeyDto;
import org.subshare.core.dto.PermissionDto;
import org.subshare.core.dto.PermissionSetDto;
import org.subshare.core.dto.PermissionSetInheritanceDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.RepositoryOwnerDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.dto.SsSymlinkDto;
import org.subshare.core.dto.UserIdentityDto;
import org.subshare.core.dto.UserIdentityLinkDto;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDeletionDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyReplacementRequestDto;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyDtoWithSignatureConverter;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.dto.CollisionDtoConverter;
import org.subshare.local.dto.CryptoConfigPropSetDtoConverter;
import org.subshare.local.dto.CryptoRepoFileDtoConverter;
import org.subshare.local.dto.CurrentHistoCryptoRepoFileDtoConverter;
import org.subshare.local.dto.HistoCryptoRepoFileDtoConverter;
import org.subshare.local.dto.HistoFrameDtoConverter;
import org.subshare.local.dto.UserIdentityDtoConverter;
import org.subshare.local.dto.UserIdentityLinkDtoConverter;
import org.subshare.local.dto.UserRepoKeyPublicKeyDtoConverter;
import org.subshare.local.dto.UserRepoKeyPublicKeyReplacementRequestDeletionDtoConverter;
import org.subshare.local.dto.UserRepoKeyPublicKeyReplacementRequestDtoConverter;
import org.subshare.local.persistence.Collision;
import org.subshare.local.persistence.CollisionDao;
import org.subshare.local.persistence.CryptoConfigPropSet;
import org.subshare.local.persistence.CryptoConfigPropSetDao;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoKeyDeactivation;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.CurrentHistoCryptoRepoFile;
import org.subshare.local.persistence.CurrentHistoCryptoRepoFileDao;
import org.subshare.local.persistence.HistoCryptoRepoFile;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;
import org.subshare.local.persistence.HistoFrame;
import org.subshare.local.persistence.HistoFrameDao;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.LastCryptoKeySyncFromRemoteRepo;
import org.subshare.local.persistence.LastCryptoKeySyncFromRemoteRepoDao;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepo;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepoDao;
import org.subshare.local.persistence.LocalRepositoryType;
import org.subshare.local.persistence.Permission;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.PermissionSet;
import org.subshare.local.persistence.PermissionSetDao;
import org.subshare.local.persistence.PermissionSetInheritance;
import org.subshare.local.persistence.PermissionSetInheritanceDao;
import org.subshare.local.persistence.PlainHistoCryptoRepoFile;
import org.subshare.local.persistence.PlainHistoCryptoRepoFileDao;
import org.subshare.local.persistence.PreliminaryCollision;
import org.subshare.local.persistence.PreliminaryCollisionDao;
import org.subshare.local.persistence.PreliminaryDeletion;
import org.subshare.local.persistence.PreliminaryDeletionDao;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.RepositoryOwnerDao;
import org.subshare.local.persistence.SignableDao;
import org.subshare.local.persistence.SsDirectory;
import org.subshare.local.persistence.SsLocalRepository;
import org.subshare.local.persistence.SsNormalFile;
import org.subshare.local.persistence.SsRemoteRepository;
import org.subshare.local.persistence.SsRepoFile;
import org.subshare.local.persistence.SsSymlink;
import org.subshare.local.persistence.UserIdentity;
import org.subshare.local.persistence.UserIdentityDao;
import org.subshare.local.persistence.UserIdentityLink;
import org.subshare.local.persistence.UserIdentityLinkDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyLookupImpl;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequest;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDeletion;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDeletionDao;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.CollisionException;
import co.codewizards.cloudstore.core.util.StringUtil;
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

	private LocalRepoStorage localRepoStorage;

	private static final UUID NULL_UUID = new UUID(0, 0);

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

//		cryptreeNode.createHistoCryptoRepoFileIfNeeded();

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

//	@Override
//	public HistoCryptoRepoFileDto getCryptoRepoFileOnServerDto(String localPath) {
//		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
//		final HistoCryptoRepoFile histoCryptoRepoFile = cryptreeNode.getCryptoRepoFileOnServer();
//		if (histoCryptoRepoFile == null)
//			return null;
//
//		final HistoCryptoRepoFileDtoConverter converter = HistoCryptoRepoFileDtoConverter.create(getTransactionOrFail());
//		final HistoCryptoRepoFileDto result = converter.toCryptoRepoFileOnServerDto(histoCryptoRepoFile);
//		return result;
//	}

	@Override
	public CurrentHistoCryptoRepoFileDto createCurrentHistoCryptoRepoFileDto(String localPath, boolean withHistoCryptoRepoFileDto) {
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);

		final HistoCryptoRepoFile histoCryptoRepoFile = cryptreeNode.createHistoCryptoRepoFileIfNeeded();
//		final HistoCryptoRepoFileDtoConverter hcrfDtoConverter = HistoCryptoRepoFileDtoConverter.create(getTransactionOrFail());
//		final HistoCryptoRepoFileDto hcrfDto = hcrfDtoConverter.toHistoCryptoRepoFileDto(histoCryptoRepoFile);

		final CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = cryptreeNode.getCurrentHistoCryptoRepoFile();
		final CurrentHistoCryptoRepoFileDtoConverter chcrfDtoConverter = CurrentHistoCryptoRepoFileDtoConverter.create(getTransactionOrFail());
		final CurrentHistoCryptoRepoFileDto result = chcrfDtoConverter.toCurrentHistoCryptoRepoFileDto(currentHistoCryptoRepoFile, withHistoCryptoRepoFileDto);

		if (withHistoCryptoRepoFileDto) {
			if (! result.getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId().equals(histoCryptoRepoFile.getHistoCryptoRepoFileId()))
				throw new IllegalStateException("result.histoCryptoRepoFileDto.histoCryptoRepoFileId != histoCryptoRepoFile.histoCryptoRepoFileId");
		}
		else {
			if (! result.getHistoCryptoRepoFileId().equals(histoCryptoRepoFile.getHistoCryptoRepoFileId()))
				throw new IllegalStateException("result.histoCryptoRepoFileId != histoCryptoRepoFile.histoCryptoRepoFileId");
		}

		return result;
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoOrFail(final String localPath) {
		claimRepositoryOwnershipIfUnowned();
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);

//		cryptreeNode.createHistoCryptoRepoFileIfNeeded();

		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

	@Override
	public String getServerPath(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		return cryptoRepoFile.getServerPath();
	}

	@Override
	public String getLocalPath(final String serverPath) {
		assertNotNull(serverPath, "serverPath");
		if (StringUtil.isEmpty(serverPath))
			throw new IllegalArgumentException("serverPath is empty"); // TODO do we need to support this? Is this the root? Shouldn't this be mapped to the local root [or more precisely the local connection-point]?!

		// We don't actually need the complete serverPath as every single path-segment is a cryptoRepoFileId
		// which is globally unique. Hence we're only interested in the last path-segment.
		String cryptoRepoFileIdStr = serverPath;
		while (cryptoRepoFileIdStr.endsWith("/"))
			cryptoRepoFileIdStr = cryptoRepoFileIdStr.substring(0, cryptoRepoFileIdStr.length() - 1);

		int lastSlashIndex = cryptoRepoFileIdStr.lastIndexOf('/');
		if (lastSlashIndex >= 0)
			cryptoRepoFileIdStr = cryptoRepoFileIdStr.substring(lastSlashIndex + 1);

		final Uid cryptoRepoFileId = new Uid(cryptoRepoFileIdStr);
		final CryptoRepoFile cryptoRepoFile = getCryptreeContext().getCryptoRepoFileOrFail(cryptoRepoFileId);
		final RepoFile repoFile = cryptoRepoFile.getRepoFile();
		final String localPath = repoFile.getPath();
		return localPath;
	}

	@Override
	public DataKey getDataKeyOrFail(final String localPath) {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.getDataKeyOrFail();
	}

	@Override
	public DataKey getDataKeyOrFail(Uid cryptoKeyId) {
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(getRootCryptoRepoFileIdOrFail());
		return cryptreeNode.getDataKeyOrFail(cryptoKeyId);
	}

//	@Override
//	public KeyParameter getHistoDataKeyOrFail(Uid histoCryptoRepoFileId) {
//		assertNotNull("histoCryptoRepoFileId", histoCryptoRepoFileId);
//
////		final LocalRepoTransaction tx = getCryptreeContext().transaction;
////		final HistoCryptoRepoFile histoCryptoRepoFile = tx.getDao(HistoCryptoRepoFileDao.class).getHistoCryptoRepoFileOrFail(histoCryptoRepoFileId);
////		final Uid cryptoRepoFileId = histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId();
////		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
//
//		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(getRootCryptoRepoFileIdOrFail());
//		return cryptreeNode.getHistoDataKeyOrFail(histoCryptoRepoFileId);
//	}

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

	private boolean resyncMode;

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoWithCryptoRepoFiles(Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced) {
		claimRepositoryOwnershipIfUnowned();
		processUserRepoKeyPublicKeyReplacementRequests();
		createMissingUserIdentities();
		final LocalRepository localRepository = getTransactionOrFail().getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();

		if (lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced != null) {
			resyncMode = lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced.longValue() != lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced();
			if (resyncMode) {
				logger.warn("getCryptoChangeSetDtoWithCryptoRepoFiles: Enabling resyncMode! lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced={} overwrites lastCryptoKeySyncToRemoteRepo.localRepositoryRevisionSynced={}",
						lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced, lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			}
			lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionSynced(lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);
		}

		lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());

		final CryptoChangeSetDto cryptoChangeSetDto = new CryptoChangeSetDto();
		populateChangedCryptoRepoFileDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		populateCryptoChangeSetDtoWithAllButCryptoRepoFiles(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		logger.debug("getCryptoChangeSetDtoWithCryptoRepoFiles(): {}", cryptoChangeSetDto);
		return cryptoChangeSetDto;
	}

	private void createMissingUserIdentities() {
		if (isOnServer())
			return;

		final UserRepoKeyPublicKeyHelper userRepoKeyPublicKeyHelper = new UserRepoKeyPublicKeyHelper(getCryptreeContext());
		userRepoKeyPublicKeyHelper.createMissingUserIdentities();
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
		assertNotNull(cryptoChangeSetDto, "cryptoChangeSetDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();

		if (cryptoChangeSetDto.getRevision() >= 0) // downward compatibility! a remote peer older than version 0.10.2 does not contain this, yet!
			setLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced(cryptoChangeSetDto.getRevision());

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

		if (isOnServer()) {
			for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFileDto2CryptoRepoFile.values()) {
				if (cryptoRepoFile.getDeleted() != null) {
					final RepoFile repoFile = cryptoRepoFile.getRepoFile();
					if (repoFile != null)
						deleteRepoFileWithAllChildrenRecursively(repoFile);
					else
						logger.warn("putCryptoChangeSetDto: repoFile == null!!! {}", cryptoRepoFile);
				}
			}
		}

		if (! isOnServer()) {
			for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFileDto2CryptoRepoFile.values()) {
				final RepoFileDto decryptedRepoFileDto;
				try {
					decryptedRepoFileDto = getDecryptedRepoFileDtoOrFail(cryptoRepoFile.getCryptoRepoFileId());
				} catch (final AccessDeniedException x) {
					continue;
				}
				cryptoRepoFile.setLocalName(decryptedRepoFileDto.getName());
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

		for (UserRepoKeyPublicKeyReplacementRequestDto requestDto : cryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDtos())
			putUserRepoKeyPublicKeyReplacementRequestDto(requestDto);

		for (UserIdentityDto userIdentityDto : cryptoChangeSetDto.getUserIdentityDtos())
			putUserIdentityDto(userIdentityDto);

		for (UserIdentityLinkDto userIdentityLinkDto : cryptoChangeSetDto.getUserIdentityLinkDtos())
			putUserIdentityLinkDto(userIdentityLinkDto);

		// putPermissionSetInheritanceDto(...) must be called *after* all PermissionSet and Permission objects are written to the DB!
		// This is because it searches for conflicts, i.e. data already signed on the server conflicting with a revoked inheritance.
		transaction.flush();
		for (final PermissionSetInheritanceDto permissionSetInheritanceDto : cryptoChangeSetDto.getPermissionSetInheritanceDtos())
			putPermissionSetInheritanceDto(permissionSetInheritanceDto);

		transaction.flush();

		final HistoCryptoRepoFileDtoConverter histoCryptoRepoFileDtoConverter = HistoCryptoRepoFileDtoConverter.create(getTransactionOrFail());

		final Map<HistoCryptoRepoFileDto, HistoCryptoRepoFile> histoCryptoRepoFileDto2HistoCryptoRepoFile =
				new HashMap<>();

		for (HistoFrameDto histoFrameDto : cryptoChangeSetDto.getHistoFrameDtos())
			putHistoFrameDto(histoFrameDto);

		putHistoCryptoRepoFileDtos(
				histoCryptoRepoFileDtoConverter,
				histoCryptoRepoFileDto2HistoCryptoRepoFile,
				cryptoChangeSetDto.getHistoCryptoRepoFileDtos()
				);

		final Set<Uid> dirtyPlainHistoCryptoRepoFileIds = new HashSet<>();
		final List<Collision> collisions = new ArrayList<>();
		for (CollisionDto collisionDto : cryptoChangeSetDto.getCollisionDtos()) {
			collisions.add(putCollisionDto(collisionDto));
			dirtyPlainHistoCryptoRepoFileIds.add(collisionDto.getHistoCryptoRepoFileId1());
			dirtyPlainHistoCryptoRepoFileIds.add(collisionDto.getHistoCryptoRepoFileId2());
		}
		dirtyPlainHistoCryptoRepoFileIds.remove(null);

		for (final HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFileDto2HistoCryptoRepoFile.values())
			dirtyPlainHistoCryptoRepoFileIds.add(histoCryptoRepoFile.getHistoCryptoRepoFileId());

		CurrentHistoCryptoRepoFileDtoConverter currentHistoCryptoRepoFileDtoConverter = CurrentHistoCryptoRepoFileDtoConverter.create(transaction);
		for (CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto : cryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos()) {
			CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = currentHistoCryptoRepoFileDtoConverter.putCurrentHistoCryptoRepoFile(currentHistoCryptoRepoFileDto);
			currentHistoCryptoRepoFile.setLastSyncFromRepositoryId(getRemoteRepositoryIdOrFail());
		}

		if (! isOnServer()) {
			final SsLocalRepository localRepository = (SsLocalRepository) transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();

			if (localRepository.getLocalRepositoryType() == LocalRepositoryType.CLIENT_META_ONLY) {
				final Map<CryptoRepoFile, RepoFileDto> cryptoRepoFile2DecryptedRepoFileDto = new HashMap<>();

				for (final HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFileDto2HistoCryptoRepoFile.values()) {
					if (histoCryptoRepoFile.getCryptoRepoFile().getDeleted() != null)
						continue;

					final RepoFileDto decryptedRepoFileDto;
					try {
						decryptedRepoFileDto = getDecryptedRepoFileOnServerDtoOrFail(histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId());
					} catch (final AccessDeniedException x) {
						continue;
					}
					cryptoRepoFile2DecryptedRepoFileDto.put(histoCryptoRepoFile.getCryptoRepoFile(), decryptedRepoFileDto);
				}
				putDecryptedRepoFiles(cryptoRepoFile2DecryptedRepoFileDto);
			}
			else {
				// history not needed in meta-only-repo
				final HistoCryptoRepoFileDao histoCryptoRepoFileDao = getTransactionOrFail().getDao(HistoCryptoRepoFileDao.class);
				for (final Uid histoCryptoRepoFileId : dirtyPlainHistoCryptoRepoFileIds) {
					final HistoCryptoRepoFile histoCryptoRepoFile = histoCryptoRepoFileDao.getHistoCryptoRepoFileOrFail(histoCryptoRepoFileId);
					final Uid cryptoRepoFileId = histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId();
					final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
					cryptreeNode.updatePlainHistoCryptoRepoFile(histoCryptoRepoFile);
				}

				// plain-collisions not needed in meta-only-repo
				for (final Collision collision : collisions) {
					final Uid cryptoRepoFileId = collision.getHistoCryptoRepoFile1().getCryptoRepoFile().getCryptoRepoFileId();
					final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
					cryptreeNode.updateCollisionPrivate(collision);
				}
			}
		}

		putCryptoConfigPropSetDtos(cryptoChangeSetDto.getCryptoConfigPropSetDtos());

		transaction.flush();

		processUserRepoKeyPublicKeyReplacementRequests();

		for (UserRepoKeyPublicKeyReplacementRequestDeletionDto requestDeletionDto : cryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDeletionDtos())
			putUserRepoKeyPublicKeyReplacementRequestDeletionDto(requestDeletionDto);

//		issue_5_cleanUpExpiredInvitationUserRepoKeys();

		if (! isOnServer())
			new UserRepoKeyPublicKeyHelper(getCryptreeContext()).updateUserRepoKeyRingFromUserIdentities();

		getCryptreeContext().getUserRegistry().writeIfNeeded();
	}

	@Override
	public ConfigPropSetDto getParentConfigPropSetDtoIfNeeded() {
		final String remotePathPrefix = getRemotePathPrefix();
		if (remotePathPrefix == null) // on server
			throw new IllegalStateException("This method should not be invoked on the server!");

		if (remotePathPrefix.isEmpty()) // on client and complete repo is checked out
			return null;

		final Uid id = getCryptreeContext().getCryptoRepoFileIdForRemotePathPrefixOrFail();
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(id);
		return cryptreeNode.getParentConfigPropSetDtoIfNeeded();
	}

//	private void issue_5_cleanUpExpiredInvitationUserRepoKeys() {
//		// This is a quick'n'dirty data-fix for the broken data of issue 5: https://github.com/subshare/subshare/issues/5
//		// There is currently no clean code to really remove expired InvitationUserRepoKeys!
//		// TODO This method is going to be removed completely in a newer version!
//		if (new Date().after(new Date(2017, 1, 1)))
//			return;
//
//		final LocalRepoTransaction transaction = getTransactionOrFail();
//		final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
//		final Date candidateInvitationThresholdDate = new Date(2016, 7, 31);
//		final Date now = new Date();
//		final long deleteReserveMillis = 7L * 24L * 3600L * 1000L;
//		final List<InvitationUserRepoKeyPublicKey> invitationKeysToDelete = new LinkedList<>();
//		for (final UserRepoKeyPublicKey userRepoKeyPublicKey : urkpkDao.getObjects()) {
//			if (! (userRepoKeyPublicKey instanceof InvitationUserRepoKeyPublicKey))
//				continue;
//
//			final InvitationUserRepoKeyPublicKey invitationUserRepoKeyPublicKey = (InvitationUserRepoKeyPublicKey) userRepoKeyPublicKey;
//			if (! invitationUserRepoKeyPublicKey.getCreated().before(candidateInvitationThresholdDate))
//				continue;
//
//			Date validTo = invitationUserRepoKeyPublicKey.getValidTo();
//			if (validTo == null)
//				validTo = new Date(invitationUserRepoKeyPublicKey.getCreated().getTime() + (31L * 24L * 3600L * 1000L));
//
//			if (now.getTime() - validTo.getTime() < deleteReserveMillis)
//				continue;
//
//			invitationKeysToDelete.add(invitationUserRepoKeyPublicKey);
//		}
//
//		for (InvitationUserRepoKeyPublicKey invitationUserRepoKeyPublicKey : invitationKeysToDelete)
//			deleteUserRepoKeyPublicKeyReplacementRequestWithOldKey(null, invitationUserRepoKeyPublicKey);
//	}

	private void putHistoCryptoRepoFileDtos(
			final HistoCryptoRepoFileDtoConverter histoCryptoRepoFileDtoConverter,
			final Map<HistoCryptoRepoFileDto, HistoCryptoRepoFile> histoCryptoRepoFileDto2HistoCryptoRepoFile,
			final List<HistoCryptoRepoFileDto> histoCryptoRepoFileDtos) {

		final Map<Uid, HistoCryptoRepoFileDto> histoCryptoRepoFileId2HistoCryptoRepoFileDto = new HashMap<>(histoCryptoRepoFileDtos.size());
		for (HistoCryptoRepoFileDto histoCryptoRepoFileDto : histoCryptoRepoFileDtos)
			histoCryptoRepoFileId2HistoCryptoRepoFileDto.put(
					histoCryptoRepoFileDto.getHistoCryptoRepoFileId(), histoCryptoRepoFileDto);

		putHistoCryptoRepoFileDtos(
				histoCryptoRepoFileDtoConverter,
				histoCryptoRepoFileDto2HistoCryptoRepoFile, histoCryptoRepoFileId2HistoCryptoRepoFileDto);
	}

	private void putHistoCryptoRepoFileDtos(
			final HistoCryptoRepoFileDtoConverter histoCryptoRepoFileDtoConverter,
			final Map<HistoCryptoRepoFileDto, HistoCryptoRepoFile> histoCryptoRepoFileDto2HistoCryptoRepoFile,
			final Map<Uid, HistoCryptoRepoFileDto> histoCryptoRepoFileId2HistoCryptoRepoFileDto) {

		for (HistoCryptoRepoFileDto histoCryptoRepoFileDto : histoCryptoRepoFileId2HistoCryptoRepoFileDto.values())
			putHistoCryptoRepoFileDto(
					histoCryptoRepoFileDtoConverter,
					histoCryptoRepoFileDto2HistoCryptoRepoFile, histoCryptoRepoFileId2HistoCryptoRepoFileDto,
					histoCryptoRepoFileDto);
	}

	private void putHistoCryptoRepoFileDto(
			final HistoCryptoRepoFileDtoConverter histoCryptoRepoFileDtoConverter,
			final Map<HistoCryptoRepoFileDto, HistoCryptoRepoFile> histoCryptoRepoFileDto2HistoCryptoRepoFile,
			final Map<Uid, HistoCryptoRepoFileDto> histoCryptoRepoFileId2HistoCryptoRepoFileDto,
			final HistoCryptoRepoFileDto histoCryptoRepoFileDto) {

		HistoCryptoRepoFile histoCryptoRepoFile = histoCryptoRepoFileDto2HistoCryptoRepoFile.get(histoCryptoRepoFileDto);
		if (histoCryptoRepoFile != null)
			return;

		final Uid previousHistoCryptoRepoFileId = histoCryptoRepoFileDto.getPreviousHistoCryptoRepoFileId();
		if (previousHistoCryptoRepoFileId != null) {
			final HistoCryptoRepoFileDto previousHistoCryptoRepoFileDto = histoCryptoRepoFileId2HistoCryptoRepoFileDto.get(previousHistoCryptoRepoFileId);
			if (previousHistoCryptoRepoFileDto != null)
				putHistoCryptoRepoFileDto(
						histoCryptoRepoFileDtoConverter,
						histoCryptoRepoFileDto2HistoCryptoRepoFile, histoCryptoRepoFileId2HistoCryptoRepoFileDto,
						previousHistoCryptoRepoFileDto);
		}

		histoCryptoRepoFile = histoCryptoRepoFileDtoConverter.putHistoCryptoRepoFile(histoCryptoRepoFileDto);
		histoCryptoRepoFile.setLastSyncFromRepositoryId(getRemoteRepositoryId());
		histoCryptoRepoFileDto2HistoCryptoRepoFile.put(histoCryptoRepoFileDto, histoCryptoRepoFile);
	}

	private void putDecryptedRepoFiles(final Map<CryptoRepoFile, RepoFileDto> cryptoRepoFile2DecryptedRepoFileDto) {
		assertNotNull(cryptoRepoFile2DecryptedRepoFileDto, "cryptoRepoFile2DecryptedRepoFileDto");
		final Map<CryptoRepoFile, RepoFileDto> cache = new HashMap<>(cryptoRepoFile2DecryptedRepoFileDto);
		for (final Map.Entry<CryptoRepoFile, RepoFileDto> me : cryptoRepoFile2DecryptedRepoFileDto.entrySet())
			putDecryptedRepoFile(cache, me.getKey(), me.getValue());
	}

	private RepoFile putDecryptedRepoFile(final Map<CryptoRepoFile, RepoFileDto> cryptoRepoFile2DecryptedRepoFileDto, final CryptoRepoFile cryptoRepoFile, final RepoFileDto decryptedRepoFileDto) {
		assertNotNull(cryptoRepoFile2DecryptedRepoFileDto, "cryptoRepoFile2DecryptedRepoFileDto");
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		assertNotNull(decryptedRepoFileDto, "decryptedRepoFileDto");

		if (cryptoRepoFile.getDeleted() != null) {
			logger.info("putDecryptedRepoFile: Skipping deleted {}", cryptoRepoFile);
			return null;
		}

		RepoFile decryptedRepoFile = cryptoRepoFile.getRepoFile();
		if (decryptedRepoFile == null) {
			final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
			RepoFile parentRepoFile = null;
			if (parentCryptoRepoFile != null) {
				parentRepoFile = parentCryptoRepoFile.getRepoFile();
				if (parentRepoFile == null) {
					RepoFileDto parentRepoFileDto = cryptoRepoFile2DecryptedRepoFileDto.get(parentCryptoRepoFile);
					if (parentRepoFileDto == null) // we should *always* be able to decrypt the parent, if we succeeded in decrypting a child.
						parentRepoFileDto = getDecryptedRepoFileDtoOrFail(parentCryptoRepoFile.getCryptoRepoFileId());

					parentRepoFile = putDecryptedRepoFile(cryptoRepoFile2DecryptedRepoFileDto, parentCryptoRepoFile, parentRepoFileDto);
					if (parentRepoFile == null) {
						logger.warn("putDecryptedRepoFile: Skipping because parentRepoFile == null! {}", cryptoRepoFile);
						return null;
					}
				}
			}

			decryptedRepoFile = putDecryptedRepoFile(cryptoRepoFile, decryptedRepoFileDto, parentRepoFile);
		}
		return decryptedRepoFile;
	}

	private RepoFile putDecryptedRepoFile(final CryptoRepoFile cryptoRepoFile, final RepoFileDto decryptedRepoFileDto, final RepoFile parentRepoFile) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		assertNotNull(decryptedRepoFileDto, "decryptedRepoFileDto");
		// parentRepoFile might be null!

		final LocalRepoTransaction transaction = getTransactionOrFail();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);

//		final String parentName = parentRepoFile == null ? null : parentRepoFile.getName();
//		if (! equal(parentName, ((SsRepoFileDto) decryptedRepoFileDto).getParentName()))
//			throw new IllegalArgumentException(String.format(
//					"parentRepoFile.name != decryptedRepoFileDto.parentName :: %s != %s",
//					parentName, ((SsRepoFileDto) decryptedRepoFileDto).getParentName()));

		RepoFile result = repoFileDao.getChildRepoFile(parentRepoFile, decryptedRepoFileDto.getName());
		if (result == null) {
			if (decryptedRepoFileDto instanceof NormalFileDto) {
				final SsNormalFileDto normalFileDto = (SsNormalFileDto) decryptedRepoFileDto;
				final SsNormalFile normalFile = createObject(SsNormalFile.class);
				result = normalFile;
				normalFile.setLength(normalFileDto.getLength());
				normalFile.setLengthWithPadding(normalFileDto.getLengthWithPadding());
				normalFile.setSha1(normalFileDto.getSha1());
			}
			else if (decryptedRepoFileDto instanceof DirectoryDto) {
//				final SsDirectoryDto directoryDto = (SsDirectoryDto) decryptedRepoFileDto;
				final SsDirectory directory = createObject(SsDirectory.class);
				result = directory;
			}
			else if (decryptedRepoFileDto instanceof SymlinkDto) {
				final SsSymlinkDto symlinkDto = (SsSymlinkDto) decryptedRepoFileDto;
				final SsSymlink symlink = createObject(SsSymlink.class);
				result = symlink;
				symlink.setTarget(symlinkDto.getTarget());
			}
			else
				throw new IllegalStateException("Unknown RepoFileDto type: " + decryptedRepoFileDto);

			result.setName(decryptedRepoFileDto.getName());
			result.setLastModified(decryptedRepoFileDto.getLastModified());
			result.setParent(parentRepoFile);
			result.setLastSyncFromRepositoryId(getServerRepositoryIdOrFail());

			((SsRepoFile) result).setSignature(((SsRepoFileDto) decryptedRepoFileDto).getSignature());
		}
		else {
			CryptoRepoFileDao crfDao = transaction.getDao(CryptoRepoFileDao.class);
			CryptoRepoFile cryptoRepoFile2 = crfDao.getCryptoRepoFile(result);
			if (cryptoRepoFile2 != null
					&& ! cryptoRepoFile.equals(cryptoRepoFile2)
					&& cryptoRepoFile2.getDeleted() != null) {
				logger.warn("putDecryptedRepoFile: RepoFile is currently associated with another CryptoRepoFile, which is already marked as deleted. Dissociating it. {}, {}, {}",
						result, cryptoRepoFile, cryptoRepoFile2);

				cryptoRepoFile2.setRepoFile(null);
				transaction.flush();
			}
		}
		cryptoRepoFile.setRepoFile(result);
		transaction.flush();
		return result;
	}

	private void processUserRepoKeyPublicKeyReplacementRequests() {
		if (isOnServer())
			return;

		if (getLocalRepoStorage().isMetaOnly())
			return;

		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserRepoKeyPublicKeyReplacementRequestDao requestDao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);

		for (final UserRepoKeyPublicKeyReplacementRequest request : requestDao.getObjects()) {
			// The requests are deleted before the tx is committed and thus they're never checked by the tx-listener. We thus
			// check them now, here.
			getCryptreeContext().signableVerifier.verify(request);
			if (!request.getOldKey().getUserRepoKeyId().equals(request.getSignature().getSigningUserRepoKeyId()))
				throw new IllegalStateException("request.oldKey.userRepoKeyId != request.signature.signingUserRepoKeyId");

			final UserRepoKey newUserRepoKey = getCryptreeContext().userRepoKeyRing.getUserRepoKey(request.getNewKey().getUserRepoKeyId());
			if (newUserRepoKey != null)
				processUserRepoKeyPublicKeyReplacementRequestAsInvitedUser(request);
			else {
				final UserRepoKey oldKeySigningUserRepoKey = getCryptreeContext().userRepoKeyRing.getUserRepoKey(
						request.getOldKey().getSignature().getSigningUserRepoKeyId());

				if (oldKeySigningUserRepoKey != null)
					processUserRepoKeyPublicKeyReplacementRequestAsInvitingUser(request);
			}
		}
		transaction.flush();
	}

	private void processUserRepoKeyPublicKeyReplacementRequestAsInvitedUser(final UserRepoKeyPublicKeyReplacementRequest request) {
		logger.info("processUserRepoKeyPublicKeyReplacementRequestAsInvitedUser: {}", request);

		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserRepoKey newUserRepoKey = getCryptreeContext().userRepoKeyRing.getUserRepoKeyOrFail(request.getNewKey().getUserRepoKeyId());
		final UserRepoKey oldUserRepoKey = getCryptreeContext().userRepoKeyRing.getUserRepoKey(request.getOldKey().getUserRepoKeyId());
		if (oldUserRepoKey == null) {
			logger.warn("processUserRepoKeyPublicKeyReplacementRequestAsInvitedUser: userRepoKeyId == {} unknown! Probably already processed?!", request.getOldKey().getUserRepoKeyId());
			return;
		}

		final CryptoLinkDao cryptoLinkDao = transaction.getDao(CryptoLinkDao.class);
		final PermissionDao permissionDao = transaction.getDao(PermissionDao.class);

		// *** read permissions = CryptoLink instances ***
		final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getCryptoLinks(request.getOldKey());
		for (final CryptoLink cryptoLink : cryptoLinks) {
			final byte[] plainToCryptoKeyData = decrypt(cryptoLink.getToCryptoKeyData(), oldUserRepoKey.getKeyPair().getPrivate());
			final byte[] newToCryptoKeyData = encrypt(plainToCryptoKeyData, newUserRepoKey.getKeyPair().getPublic());

			cryptoLink.setToCryptoKeyData(newToCryptoKeyData);
			cryptoLink.setFromUserRepoKeyPublicKey(request.getNewKey());
			cryptoLink.setLastSyncFromRepositoryId(null);
			getCryptreeContext().getSignableSigner(oldUserRepoKey).sign(cryptoLink);
		}

		// *** other permissions = Permission instances ***
		final Collection<Permission> permissions = permissionDao.getPermissions(request.getOldKey());
		for (final Permission permission : permissions) {
			permission.setUserRepoKeyPublicKey(request.getNewKey());
			getCryptreeContext().getSignableSigner(oldUserRepoKey).sign(permission);
		}

//		getCryptreeContext().userRepoKeyRing.removeUserRepoKey(oldUserRepoKey); // hmmm... this seems problematic. commented it out. it should be removed later, when the new

//		final User user = getCryptreeContext().getUserRegistry().getUserByUserRepoKeyIdOrFail(request.getOldKey().getUserRepoKeyId());
//		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
//		final List<UserRepoKey> oldUrKeys = new ArrayList<>();
//		for (final UserRepoKey pk : userRepoKeyRing.getUserRepoKeys()) {
//			if (pk.getUserRepoKeyId().equals(request.getOldKey().getUserRepoKeyId()))
//				oldUrKeys.add(pk);
//		}
//		for (UserRepoKey userRepoKey : oldUrKeys)
//			userRepoKeyRing.removeUserRepoKey(userRepoKey);
	}

	private void processUserRepoKeyPublicKeyReplacementRequestAsInvitingUser(final UserRepoKeyPublicKeyReplacementRequest request) {
		logger.info("processUserRepoKeyPublicKeyReplacementRequestAsInvitingUser: {}", request);

		final UserRepoKey oldKeySigningUserRepoKey = getCryptreeContext().userRepoKeyRing.getUserRepoKeyOrFail(
				request.getOldKey().getSignature().getSigningUserRepoKeyId());

		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserRepoKeyPublicKeyReplacementRequestDeletionDao requestDeletionDao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDeletionDao.class);
		final CryptoLinkDao cryptoLinkDao = transaction.getDao(CryptoLinkDao.class);
		final PermissionDao permissionDao = transaction.getDao(PermissionDao.class);


		// *** read permissions = CryptoLink instances ***

		// TODO what if we lost the permission to do this by now?! how do we handle this??? Simply keep things as they are
		// and somehow make the user know with a warning - we need persistent warning messages. Or do we revoke? Right now
		// there would be an exception - unhandled. This needs to be addressed!!! Later, though ;-)

		// Just in case, there are *new* CryptRepoLinks encrypted with the old invitation-user-repo-key (i.e. the read-permission
		// was granted *after* the invited user replaced them), we re-encrypt them now, too.
		// TODO we must test this! I just tested it by temporarily skipping processUserRepoKeyPublicKeyReplacementRequestAsInvitedUser(...)
		// but we need a real test scenario! => inviteUserAndSync_twoReadPermissionsOnSubdirs
		Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getCryptoLinks(request.getOldKey());
		for (final CryptoLink cryptoLink : cryptoLinks) {
			final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(
					cryptoLink.getToCryptoKey().getCryptoRepoFile().getCryptoRepoFileId());

			final PlainCryptoKey plainCryptoKey = cryptreeNode.getPlainCryptoKeyForDecrypting(cryptoLink.getToCryptoKey());
			assertNotNull(plainCryptoKey, "plainCryptoKey[cryptoKeyId=" + cryptoLink.getToCryptoKey().getCryptoKeyId() + "]");

			final byte[] newToCryptoKeyData = encrypt(plainCryptoKey.getEncodedKey(), request.getNewKey().getPublicKey().getPublicKey());

			cryptoLink.setToCryptoKeyData(newToCryptoKeyData);
			cryptoLink.setFromUserRepoKeyPublicKey(request.getNewKey());
			cryptoLink.setLastSyncFromRepositoryId(null);
			getCryptreeContext().getSignableSigner(oldKeySigningUserRepoKey).sign(cryptoLink);
		}

		// Re-sign the CryptoLinks that are currently signed with the invitation-user-repo-key.
		// We use the same key that we used to sign the old key, because this prevents an attacker from knowing that 2 of our keys belong together.
		cryptoLinks = cryptoLinkDao.getCryptoLinksSignedBy(request.getOldKey().getUserRepoKeyId());
		for (final CryptoLink cryptoLink : cryptoLinks) {
			cryptoLink.setLastSyncFromRepositoryId(null);
			getCryptreeContext().getSignableSigner(oldKeySigningUserRepoKey).sign(cryptoLink);
		}


		// *** other permissions = Permission instances ***

		// Just in case, there are new permissions for the old key, we process them first, too (just like CryptoLinks above).
		Collection<Permission> permissions = permissionDao.getPermissions(request.getOldKey());
//		boolean hasSeeUserIdentity = false;
		for (final Permission permission : permissions) {
			permission.setUserRepoKeyPublicKey(request.getNewKey());
			getCryptreeContext().getSignableSigner(oldKeySigningUserRepoKey).sign(permission);
//			if (PermissionType.readUserIdentity == permission.getPermissionType())
//				hasSeeUserIdentity = true;
		}

		permissions = permissionDao.getPermissionsSignedBy(request.getOldKey().getUserRepoKeyId());
		for (final Permission permission : permissions)
			getCryptreeContext().getSignableSigner(oldKeySigningUserRepoKey).sign(permission);

		// TODO extract this into a separate method (and maybe not only this!)
		final User user = getCryptreeContext().getUserRegistry().getUserByUserRepoKeyId(request.getOldKey().getUserRepoKeyId());
		if (user == null)
			logger.warn("processUserRepoKeyPublicKeyReplacementRequestAsInvitingUser: userRepoKeyId = {} unknown! Maybe already processed?!", request.getOldKey().getUserRepoKeyId());
		else {
			final UserIdentityPayloadDto userIdentityPayloadDto = getUserIdentityPayloadDtoOrFail(request.getNewKey().getUserRepoKeyId());
			UserRepoKeyPublicKeyDtoWithSignatureConverter urkpkConverter = new UserRepoKeyPublicKeyDtoWithSignatureConverter();
			UserRepoKey.PublicKeyWithSignature newPublicKey = urkpkConverter.fromUserRepoKeyPublicKeyDto(userIdentityPayloadDto.getUserRepoKeyPublicKeyDto());
			assertNotNull(newPublicKey, "newPublicKey");
			final List<UserRepoKey.PublicKeyWithSignature> oldPublicKeys = new ArrayList<>();
			for (final UserRepoKey.PublicKeyWithSignature pk : user.getUserRepoKeyPublicKeys()) {
				if (pk.getUserRepoKeyId().equals(request.getOldKey().getUserRepoKeyId()))
					oldPublicKeys.add(pk);
			}
			user.getUserRepoKeyPublicKeys().removeAll(oldPublicKeys);
			user.getUserRepoKeyPublicKeys().add(newPublicKey);
		}

//		if (hasSeeUserIdentity)
//			transferUserIdentities(request);

		// *** delete the old (invitation) key and its corresponding replacement-request ***
		UserRepoKeyPublicKeyReplacementRequestDeletion requestDeletion = new UserRepoKeyPublicKeyReplacementRequestDeletion(request);

		// We use the same key that we used to sign the old key, because this prevents an attacker from knowing that 2 of our keys belong together.
		getCryptreeContext().getSignableSigner(oldKeySigningUserRepoKey).sign(requestDeletion);

		requestDeletion = requestDeletionDao.makePersistent(requestDeletion);
		deleteUserRepoKeyPublicKeyReplacementRequestWithOldKey(request, null);
	}

	@Override
	public void removeOrphanedInvitationUserRepoKeyPublicKeys() {
		final long start = System.currentTimeMillis();
		final LocalRepoTransaction tx = getTransactionOrFail();
		final RemoteRepositoryDao rrDao = tx.getDao(RemoteRepositoryDao.class);

		final Set<UUID> serverRepositoryIds = new HashSet<>(1); // currently, we support only 1!
		for (RemoteRepository serverRepository : rrDao.getObjects())
			serverRepositoryIds.add(serverRepository.getRepositoryId());

		final UserRepoKeyPublicKeyDao urkpkDao = tx.getDao(UserRepoKeyPublicKeyDao.class);
		for (final User user : getCryptreeContext().getUserRegistry().getUsers()) {
			final List<UserRepoKey.PublicKeyWithSignature> userRepoKeyPublicKeysToBeRemoved = new ArrayList<>();
			for (final UserRepoKey.PublicKeyWithSignature pk : user.getUserRepoKeyPublicKeys()) {
				if (pk.isInvitation() && serverRepositoryIds.contains(pk.getServerRepositoryId())) {
					final UserRepoKeyPublicKey urkpk = urkpkDao.getUserRepoKeyPublicKey(pk.getUserRepoKeyId());
					if (urkpk == null)
						userRepoKeyPublicKeysToBeRemoved.add(pk);
				}
			}
			if (! userRepoKeyPublicKeysToBeRemoved.isEmpty()) {
				user.getUserRepoKeyPublicKeys().removeAll(userRepoKeyPublicKeysToBeRemoved);

				logger.warn("removeOrphanedInvitationUserRepoKeyPublicKeys: Removed {} public keys from user {}! removed={} kept={}",
						userRepoKeyPublicKeysToBeRemoved.size(), user, userRepoKeyPublicKeysToBeRemoved, user.getUserRepoKeyPublicKeys());
			}
		}
		getCryptreeContext().getUserRegistry().writeIfNeeded();
		logger.debug("removeOrphanedInvitationUserRepoKeyPublicKeys took {} ms.", System.currentTimeMillis() - start);
	}

//	private void transferUserIdentities(final UserRepoKeyPublicKeyReplacementRequest request) {
//		final UserIdentityLinkDao uiDao = getCryptreeContext().transaction.getDao(UserIdentityLinkDao.class);
//		final Collection<UserIdentityLink> userIdentities = uiDao.getUserIdentitiesFor(request.getOldKey());
//		for (final UserIdentityLink userIdentity : userIdentities) {
//			final UserIdentityPayloadDto dto = getUserIdentityPayloadDtoOrFail(userIdentity.getOfUserRepoKeyPublicKey().getUserRepoKeyId());
//			final UserRepoKeyPublicKeyHelper userRepoKeyPublicKeyHelper = new UserRepoKeyPublicKeyHelper(getCryptreeContext()) {
//				@Override
//				protected UserIdentityPayloadDto createUserIdentityPayloadDto(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
//					if (! ofUserRepoKeyPublicKey.equals(userIdentity.getOfUserRepoKeyPublicKey()))
//						throw new IllegalArgumentException("ofUserRepoKeyPublicKey != userIdentity.ofUserRepoKeyPublicKey");
//
//					return dto;
//				}
//			};
//			userRepoKeyPublicKeyHelper.getUserIdentityOrCreate(userIdentity.getOfUserRepoKeyPublicKey(), request.getNewKey());
//		}
//	}

	private void putUserRepoKeyPublicKeyReplacementRequestDeletionDto(final UserRepoKeyPublicKeyReplacementRequestDeletionDto requestDeletionDto) {
		assertNotNull(requestDeletionDto, "requestDeletionDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserRepoKeyPublicKeyReplacementRequestDao requestDao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);
		final UserRepoKeyPublicKeyReplacementRequestDeletionDao requestDeletionDao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDeletionDao.class);
		final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		UserRepoKeyPublicKeyReplacementRequestDeletion requestDeletion = requestDeletionDao.getUserRepoKeyPublicKeyReplacementRequestDeletion(requestDeletionDto.getRequestId());
		if (requestDeletion == null)
			requestDeletion = new UserRepoKeyPublicKeyReplacementRequestDeletion(requestDeletionDto.getRequestId(), requestDeletionDto.getOldUserRepoKeyId());

		requestDeletion.setSignature(requestDeletionDto.getSignature());
		requestDeletion = requestDeletionDao.makePersistent(requestDeletion);

		final UserRepoKeyPublicKeyReplacementRequest request = requestDao.getUserRepoKeyPublicKeyReplacementRequest(requestDeletion.getRequestId());
		InvitationUserRepoKeyPublicKey oldKey = null;
		final Uid oldUserRepoKeyId = requestDeletion.getOldUserRepoKeyId();
		if (oldUserRepoKeyId != null) {
			final UserRepoKeyPublicKey key = urkpkDao.getUserRepoKeyPublicKey(oldUserRepoKeyId);
			oldKey = (InvitationUserRepoKeyPublicKey) key;
		}

		if (request != null || oldKey != null)
			deleteUserRepoKeyPublicKeyReplacementRequestWithOldKey(request, oldKey);
	}

	private void putUserIdentityDto(UserIdentityDto userIdentityDto) {
		assertNotNull(userIdentityDto, "userIdentityDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserIdentityDao uiDao = transaction.getDao(UserIdentityDao.class);
		final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		UserIdentity userIdentity = uiDao.getUserIdentity(userIdentityDto.getUserIdentityId());
		if (userIdentity == null)
			userIdentity = new UserIdentity(userIdentityDto.getUserIdentityId());

		userIdentity.setOfUserRepoKeyPublicKey(urkpkDao.getUserRepoKeyPublicKeyOrFail(userIdentityDto.getOfUserRepoKeyId()));
		userIdentity.setEncryptedUserIdentityPayloadDtoData(userIdentityDto.getEncryptedUserIdentityPayloadDto());
		userIdentity.setSignature(userIdentityDto.getSignature());

		userIdentity = uiDao.makePersistent(userIdentity);

		deleteOtherUserIdentitiesOfSameUserRepoKeyPublicKey(userIdentity);
	}

	private void deleteOtherUserIdentitiesOfSameUserRepoKeyPublicKey(final UserIdentity userIdentity) {
		assertNotNull(userIdentity, "userIdentity");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserIdentityDao uiDao = transaction.getDao(UserIdentityDao.class);
//		final UserIdentityLinkDao uilDao = transaction.getDao(UserIdentityLinkDao.class);
		final Collection<UserIdentity> userIdentities = uiDao.getUserIdentitiesOf(userIdentity.getOfUserRepoKeyPublicKey());
		for (final UserIdentity ui : userIdentities) {
			if (! ui.equals(userIdentity))
				uiDao.deletePersistent(ui);
//				uilDao.deletePersistentAll(uilDao.getUserIdentityLinksOf(ui));

		}
	}

	private void putUserIdentityLinkDto(UserIdentityLinkDto userIdentityLinkDto) {
		assertNotNull(userIdentityLinkDto, "userIdentityLinkDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserIdentityDao uiDao = transaction.getDao(UserIdentityDao.class);
		final UserIdentityLinkDao uilDao = transaction.getDao(UserIdentityLinkDao.class);
		final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		UserIdentityLink userIdentityLink = uilDao.getUserIdentityLink(userIdentityLinkDto.getUserIdentityLinkId());
		if (userIdentityLink == null)
			userIdentityLink = new UserIdentityLink(userIdentityLinkDto.getUserIdentityLinkId());

		userIdentityLink.setUserIdentity(uiDao.getUserIdentityOrFail(userIdentityLinkDto.getUserIdentityId()));
		userIdentityLink.setForUserRepoKeyPublicKey(urkpkDao.getUserRepoKeyPublicKeyOrFail(userIdentityLinkDto.getForUserRepoKeyId()));
		userIdentityLink.setEncryptedUserIdentityKeyData(userIdentityLinkDto.getEncryptedUserIdentityKeyData());
		userIdentityLink.setSignature(userIdentityLinkDto.getSignature());

		userIdentityLink = uilDao.makePersistent(userIdentityLink);

		// TODO automatically update user in userRegistry?!
//		final UserRepoKey userRepoKey = cryptreeContext.userRepoKeyRing.getUserRepoKey(userIdentityLink.getForUserRepoKeyPublicKey().getUserRepoKeyId());
//		if (userRepoKey != null) {
//			final UserIdentityPayloadDto userIdentityPayloadDto = getUserIdentityPayloadDtoOrFail(userIdentityLink.getUserIdentity().getOfUserRepoKeyPublicKey().getUserRepoKeyId());
//			cryptreeContext.getUserRegistry().getUser
//		}
	}

	private void deleteUserRepoKeyPublicKeyReplacementRequestWithOldKey(final UserRepoKeyPublicKeyReplacementRequest request, InvitationUserRepoKeyPublicKey oldKey) {
		if (request == null && oldKey == null)
			throw new IllegalArgumentException("Both request and oldKey are null!");

		final LocalRepoTransaction transaction = getTransactionOrFail();
		transaction.flush();

		if (oldKey == null)
			oldKey = request.getOldKey();

		final UserRepoKeyPublicKeyReplacementRequestDao urkpkrrDao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);

		if (request != null)
			urkpkrrDao.deletePersistent(request);

		transaction.flush();

		final Collection<UserRepoKeyPublicKeyReplacementRequest> otherRequests = urkpkrrDao.getUserRepoKeyPublicKeyReplacementRequestsForOldKey(oldKey);
		if (otherRequests.isEmpty()) {
			final Uid oldUserRepoKeyId = oldKey.getUserRepoKeyId();

			transaction.getDao(UserRepoKeyPublicKeyDao.class).deletePersistent(oldKey);
			transaction.flush();

			final UserRepoKeyRing userRepoKeyRing = getCryptreeContext().userRepoKeyRing;
			if (userRepoKeyRing != null) // there is none on the server-side.
				userRepoKeyRing.removeUserRepoKey(oldUserRepoKeyId);
		}
		else
			logger.warn("deleteUserRepoKeyPublicKeyReplacementRequestWithOldKey: Not deleting oldKey={}, because there are other requests referencing it! {}",
					oldKey, otherRequests);
	}

	@Override
	public UserIdentityPayloadDto getUserIdentityPayloadDtoOrFail(final Uid userRepoKeyId) throws ReadUserIdentityAccessDeniedException {
		assertNotNull(userRepoKeyId, "userRepoKeyId");
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = getCryptreeContext().transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserRepoKeyPublicKey ofUserRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKey(userRepoKeyId);
		if (ofUserRepoKeyPublicKey == null)
			throw new IllegalArgumentException("There is no UserRepoKeyPublicKey with userRepoKeyId=" + userRepoKeyId);

		final UserRepoKeyPublicKeyHelper helper = new UserRepoKeyPublicKeyHelper(getCryptreeContext());
		final UserIdentityPayloadDto userIdentityPayloadDto = helper.getUserIdentityPayloadDto(ofUserRepoKeyPublicKey);
		if (userIdentityPayloadDto != null)
			return userIdentityPayloadDto;
		else
			throw new ReadUserIdentityAccessDeniedException();
	}

	@Override
	public UserRepoKey getUserRepoKey(final String localPath, final PermissionType permissionType) {
		assertNotNull(permissionType, "permissionType");
		final CryptreeNode cryptreeNode =
				localPath == null
				? getCryptreeContext().getCryptreeNodeOrCreate(getRootCryptoRepoFileIdOrFail())
				: getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.getUserRepoKey(localPath == null, permissionType);
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

	public Uid getRootCryptoRepoFileIdOrFail() {
		final Uid rootCryptoRepoFileId = getRootCryptoRepoFileId();
		return assertNotNull(rootCryptoRepoFileId, "rootCryptoRepoFileId");
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
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		final RepoFileDto repoFileDto = cryptreeNode.getRepoFileDto();
		assertNotNull(repoFileDto, "cryptreeNode.getRepoFileDto()"); // The cryptoRepoFile is present, thus this should never be null!
		return repoFileDto;
	}

	@Override
	public RepoFileDto getDecryptedRepoFileDto(final String localPath) throws AccessDeniedException {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.getRepoFileDto();
	}

	@Override
	public RepoFileDto getDecryptedRepoFileOnServerDtoOrFail(Uid cryptoRepoFileId) throws AccessDeniedException {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		final RepoFileDto repoFileDto = cryptreeNode.getRepoFileDtoOnServer();
		assertNotNull(repoFileDto, "cryptreeNode.getRepoFileDtoOnServer()"); // The cryptoRepoFile is present, thus this should never be null!
		return repoFileDto;
	}

	@Override
	public RepoFileDto getDecryptedRepoFileOnServerDto(final String localPath) {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.getRepoFileDtoOnServer();
	}

	@Override
	public void grantPermission(final String localPath, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		assertNotNull(localPath, "localPath");
		assertNotNull(permissionType, "permissionType");
		assertNotNull(userRepoKeyPublicKey, "userRepoKeyPublicKey");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		cryptreeNode.grantPermission(permissionType, userRepoKeyPublicKey);
	}

	@Override
	public void requestReplaceInvitationUserRepoKey(UserRepoKey invitationUserRepoKey, UserRepoKey.PublicKey publicKey) {
		assertNotNull(invitationUserRepoKey, "invitationUserRepoKey");
		assertNotNull(publicKey, "publicKey");

		logger.info("requestReplaceInvitationUserRepoKey: invitationUserRepoKey={}, publicKey={}", invitationUserRepoKey, publicKey);

		if (invitationUserRepoKey.getValidTo() == null)
			throw new IllegalArgumentException("invitationUserRepoKey is not a temporary UserRepoKey :: invitationUserRepoKey.getValidTo() == null");

		final LocalRepoTransaction tx = getCryptreeContext().transaction;
//		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = tx.getDao(UserRepoKeyPublicKeyDao.class);
		final UserRepoKeyPublicKeyReplacementRequestDao userRepoKeyPublicKeyReplacementRequestDao = tx.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);

//		// old key
//		final InvitationUserRepoKeyPublicKey invitationUserRepoKeyPublicKey =
//				(InvitationUserRepoKeyPublicKey) userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeyOrCreate(invitationUserRepoKey.getPublicKey());
//
//		// new key
//		final UserRepoKeyPublicKey userRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeyOrCreate(publicKey);

		final UserRepoKeyPublicKeyHelper userRepoKeyPublicKeyHelper = new UserRepoKeyPublicKeyHelper(getCryptreeContext());

		// old key
		final InvitationUserRepoKeyPublicKey invitationUserRepoKeyPublicKey =
				(InvitationUserRepoKeyPublicKey) userRepoKeyPublicKeyHelper.getUserRepoKeyPublicKeyOrCreate(invitationUserRepoKey.getPublicKey());

		// new key
		final UserRepoKeyPublicKey userRepoKeyPublicKey = userRepoKeyPublicKeyHelper.getUserRepoKeyPublicKeyOrCreate(publicKey);


		// create + sign + persist replacement request
		final UserRepoKeyPublicKeyReplacementRequest request = new UserRepoKeyPublicKeyReplacementRequest();
		request.setOldKey(invitationUserRepoKeyPublicKey);
		request.setNewKey(userRepoKeyPublicKey);
		cryptreeContext.getSignableSigner(invitationUserRepoKey).sign(request);
		userRepoKeyPublicKeyReplacementRequestDao.makePersistent(request);

		// The actual replacement is done by the new user himself, but not now, yet, as there is nothing to
		// replace at this moment. We have to first sync down from the server. Hence the replacement is done
		// by processUserRepoKeyPublicKeyReplacementRequests(), after the down-sync of all the crypto-meta-data.
	}

	@Override
	public void registerRemotePathPrefix(final String pathPrefix) { // TODO this is bad, because whenever the remote-path-prefix changes, the localRevision is updated and then the collision detection fails. Maybe move this somewhere else?
		assertNotNull(pathPrefix, "pathPrefix");
		final RemoteRepositoryDao remoteRepositoryDao = getTransactionOrFail().getDao(RemoteRepositoryDao.class);
		final RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(getServerRepositoryIdOrFail());
		final SsRemoteRepository rr = (SsRemoteRepository)remoteRepository;
		rr.setRemotePathPrefix(pathPrefix);
	}

	@Override
	public void revokePermission(final String localPath, final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		assertNotNull(localPath, "localPath");
		assertNotNull(permissionType, "permissionType");
		assertNotNull(userRepoKeyIds, "userRepoKeyIds");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		cryptreeNode.revokePermission(permissionType, userRepoKeyIds);
	}

	@Override
	public void grantPermission(final Uid cryptoRepoFileId, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		assertNotNull(permissionType, "permissionType");
		assertNotNull(userRepoKeyPublicKey, "userRepoKeyPublicKey");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		cryptreeNode.grantPermission(permissionType, userRepoKeyPublicKey);
	}

	@Override
	public void revokePermission(final Uid cryptoRepoFileId, final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		assertNotNull(permissionType, "permissionType");
		assertNotNull(userRepoKeyIds, "userRepoKeyIds");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		cryptreeNode.revokePermission(permissionType, userRepoKeyIds);
	}

	@Override
	public void setPermissionsInherited(final String localPath, final boolean inherited) {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		cryptreeNode.setPermissionsInherited(inherited);
	}

	@Override
	public boolean isPermissionsInherited(final String localPath) {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.isPermissionsInherited();
	}

	@Override
	public void setPermissionsInherited(final Uid cryptoRepoFileId, final boolean inherited) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		cryptreeNode.setPermissionsInherited(inherited);
	}

	@Override
	public boolean isPermissionsInherited(final Uid cryptoRepoFileId) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		return cryptreeNode.isPermissionsInherited();
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
		assertNotNull(cryptoRepoFileDto, "cryptoRepoFileDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final Uid cryptoRepoFileId = assertNotNull(cryptoRepoFileDto.getCryptoRepoFileId(), "cryptoRepoFileDto.cryptoRepoFileId");
		CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(cryptoRepoFileId);
		if (cryptoRepoFile == null)
			cryptoRepoFile = new CryptoRepoFile(cryptoRepoFileId);
//		else {
//			RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepository(getRemoteRepositoryId());
//			if (cryptoRepoFile.getLocalRevision() > remoteRepository.getLocalRevision()) {
//				// We must not overwrite an eventual *delete* modification. This is a collision and our policy is that
//				// collisions are silently ignored, i.e. we don't download and upload the new revision to the server, only
//				// MARKING it by a Collision object in the database. This is done, later, though.
//				logger.warn("putCryptoRepoFileDto: CryptoRepoFile {} was locally changed after last sync! Not overwriting local changes!", cryptoRepoFile.getCryptoRepoFileId());
//				return cryptoRepoFile;
//			}
//		}

		final Uid cryptoKeyId = assertNotNull(cryptoRepoFileDto.getCryptoKeyId(), "cryptoRepoFileDto.cryptoKeyId");
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

		final byte[] repoFileDtoData = assertNotNull(cryptoRepoFileDto.getRepoFileDtoData(), "cryptoRepoFileDto.repoFileDtoData");
		cryptoRepoFile.setRepoFileDtoData(repoFileDtoData);

		cryptoRepoFile.setDirectory(cryptoRepoFileDto.isDirectory());
		cryptoRepoFile.setLastSyncFromRepositoryId(getRemoteRepositoryIdOrFail());
		cryptoRepoFile.setCryptoRepoFileCreated(cryptoRepoFileDto.getCryptoRepoFileCreated());
		cryptoRepoFile.setDeleted(cryptoRepoFileDto.getDeleted());
		cryptoRepoFile.setDeletedByIgnoreRule(cryptoRepoFileDto.isDeletedByIgnoreRule());

		cryptoRepoFile.setSignature(cryptoRepoFileDto.getSignature());

		return cryptoRepoFileDao.makePersistent(cryptoRepoFile);
	}

	protected void deleteRepoFileWithAllChildrenRecursively(final RepoFile repoFile) {
		assertNotNull(repoFile, "repoFile");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		for (final RepoFile childRepoFile : repoFileDao.getChildRepoFiles(repoFile)) {
			deleteRepoFileWithAllChildrenRecursively(childRepoFile);
		}
		repoFileDao.deletePersistent(repoFile);
	}

	private UserRepoKeyPublicKey putUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto) {
		assertNotNull(userRepoKeyPublicKeyDto, "userRepoKeyPublicKeyDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		final InvitationUserRepoKeyPublicKeyDto invUserRepoKeyPublicKeyDto = (InvitationUserRepoKeyPublicKeyDto)
				(userRepoKeyPublicKeyDto instanceof InvitationUserRepoKeyPublicKeyDto ? userRepoKeyPublicKeyDto : null);

		final Uid userRepoKeyId = assertNotNull(userRepoKeyPublicKeyDto.getUserRepoKeyId(), "userRepoKeyPublicKeyDto.userRepoKeyId");
		UserRepoKeyPublicKey userRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKey(userRepoKeyId);
		if (userRepoKeyPublicKey == null)
			userRepoKeyPublicKey = invUserRepoKeyPublicKeyDto != null ? new InvitationUserRepoKeyPublicKey(userRepoKeyId) : new UserRepoKeyPublicKey(userRepoKeyId);

		userRepoKeyPublicKey.setServerRepositoryId(userRepoKeyPublicKeyDto.getRepositoryId());

		if (userRepoKeyPublicKey.getPublicKeyData() == null)
			userRepoKeyPublicKey.setPublicKeyData(userRepoKeyPublicKeyDto.getPublicKeyData());
		else if (! Arrays.equals(userRepoKeyPublicKey.getPublicKeyData(), userRepoKeyPublicKeyDto.getPublicKeyData()))
			throw new IllegalStateException("Cannot re-assign UserRepoKeyPublicKey.publicKeyData! Attack?!");


		if (invUserRepoKeyPublicKeyDto != null) {
			final InvitationUserRepoKeyPublicKey invUserRepoKeyPublicKey = (InvitationUserRepoKeyPublicKey) userRepoKeyPublicKey;
			invUserRepoKeyPublicKey.setValidTo(invUserRepoKeyPublicKeyDto.getValidTo());
			invUserRepoKeyPublicKey.setSignature(invUserRepoKeyPublicKeyDto.getSignature());
		}

		return userRepoKeyPublicKeyDao.makePersistent(userRepoKeyPublicKey);
	}

	private UserRepoKeyPublicKeyReplacementRequest putUserRepoKeyPublicKeyReplacementRequestDto(final UserRepoKeyPublicKeyReplacementRequestDto requestDto) {
		assertNotNull(requestDto, "requestDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final UserRepoKeyPublicKeyDao keyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserRepoKeyPublicKeyReplacementRequestDao requestDao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);

		UserRepoKeyPublicKeyReplacementRequest request = requestDao.getUserRepoKeyPublicKeyReplacementRequest(requestDto.getRequestId());
		if (request == null)
			request = new UserRepoKeyPublicKeyReplacementRequest(requestDto.getRequestId());

		final InvitationUserRepoKeyPublicKey oldKey = (InvitationUserRepoKeyPublicKey) keyDao.getUserRepoKeyPublicKeyOrFail(requestDto.getOldKeyId());
		request.setOldKey(oldKey);

		final UserRepoKeyPublicKey newKey = keyDao.getUserRepoKeyPublicKeyOrFail(requestDto.getNewKeyId());
		request.setNewKey(newKey);

		request.setSignature(requestDto.getSignature());

		request = requestDao.makePersistent(request);
		getCryptreeContext().signableVerifier.verify(request);
		if (!oldKey.getUserRepoKeyId().equals(request.getSignature().getSigningUserRepoKeyId()))
			throw new SignatureException("UserRepoKeyPublicKeyReplacementRequest is not signed by its oldKey!");

		return request;
	}

	private CryptoKey putCryptoKeyDto(final CryptoKeyDto cryptoKeyDto) {
		assertNotNull(cryptoKeyDto, "cryptoKeyDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final Uid cryptoKeyId = assertNotNull(cryptoKeyDto.getCryptoKeyId(), "cryptoKeyDto.cryptoKeyId");
		CryptoKey cryptoKey = cryptoKeyDao.getCryptoKey(cryptoKeyId);
//		final boolean cryptoKeyIsNew;
		if (cryptoKey == null) {
//			cryptoKeyIsNew = true;
			cryptoKey = new CryptoKey(cryptoKeyId);
		}
//		else
//			cryptoKeyIsNew = false;

//		// It is necessary to prevent a collision and ensure that the 'active' property cannot be reverted.
//		if (cryptoKeyDto.isActive() && ! cryptoKey.isActive()) {
//			logger.warn("putCryptoKeyDto: Rejecting to re-activate CryptoKey! Keeping (and re-publishing) previous state: {}", cryptoKey);
//			cryptoKey.setChanged(new Date()); // only to make it dirty => force new localRevision => force sync.
//			if (cryptoKeyIsNew)
//				throw new IllegalStateException("Cannot reject, because the CryptoKey is new! " + cryptoKey);
//
//			return cryptoKey;
//		}
//
//		cryptoKey.setActive(cryptoKeyDto.isActive());

		final CryptoKeyDeactivationDto cryptoKeyDeactivationDto = cryptoKeyDto.getCryptoKeyDeactivationDto();
		if (cryptoKeyDeactivationDto != null) {
			CryptoKeyDeactivation cryptoKeyDeactivation = cryptoKey.getCryptoKeyDeactivation();
			if (cryptoKeyDeactivation == null)
				cryptoKeyDeactivation = new CryptoKeyDeactivation();

			assertNotNull(cryptoKeyDeactivationDto.getCryptoKeyId(), "cryptoKeyDeactivationDto.cryptoKeyId");
			if (! cryptoKeyDeactivationDto.getCryptoKeyId().equals(cryptoKeyId))
				throw new IllegalStateException(String.format("cryptoKeyDeactivationDto.cryptoKeyId != cryptoKeyDto.cryptoKeyId :: %s != %s",
						cryptoKeyDeactivationDto.getCryptoKeyId(), cryptoKeyId));

			cryptoKeyDeactivation.setCryptoKey(cryptoKey);
			cryptoKeyDeactivation.setSignature(cryptoKeyDeactivationDto.getSignature());
			cryptoKey.setCryptoKeyDeactivation(cryptoKeyDeactivation);
		}

		cryptoKey.setCryptoKeyRole(cryptoKeyDto.getCryptoKeyRole());
		cryptoKey.setCryptoKeyType(cryptoKeyDto.getCryptoKeyType());

		final Uid cryptoRepoFileId = assertNotNull(cryptoKeyDto.getCryptoRepoFileId(), "cryptoKeyDto.cryptoRepoFileId");
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
		cryptoKey.setCryptoRepoFile(cryptoRepoFile);
		cryptoKey.setLastSyncFromRepositoryId(getRemoteRepositoryIdOrFail());

		assertNotNull(cryptoKeyDto.getSignature(), "cryptoKeyDto.signature");
		assertNotNull(cryptoKeyDto.getSignature().getSignatureCreated(), "cryptoKeyDto.signature.signatureCreated");
		assertNotNull(cryptoKeyDto.getSignature().getSigningUserRepoKeyId(), "cryptoKeyDto.signature.signingUserRepoKeyId");
		assertNotNull(cryptoKeyDto.getSignature().getSignatureData(), "cryptoKeyDto.signature.signatureData");

		cryptoKey.setSignature(cryptoKeyDto.getSignature());

		return cryptoKeyDao.makePersistent(cryptoKey);
	}

	private CryptoLink putCryptoLinkDto(final CryptoLinkDto cryptoLinkDto) {
		assertNotNull(cryptoLinkDto, "cryptoLinkDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoLinkDao cryptoLinkDao = transaction.getDao(CryptoLinkDao.class);
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);

		final Uid cryptoLinkId = assertNotNull(cryptoLinkDto.getCryptoLinkId(), "cryptoLinkDto.cryptoLinkId");
		CryptoLink cryptoLink = cryptoLinkDao.getCryptoLink(cryptoLinkId);
		if (cryptoLink == null)
			cryptoLink = new CryptoLink(cryptoLinkId);

		final Uid fromCryptoKeyId = cryptoLinkDto.getFromCryptoKeyId();
		cryptoLink.setFromCryptoKey(fromCryptoKeyId == null ? null : cryptoKeyDao.getCryptoKeyOrFail(fromCryptoKeyId));

		final Uid fromUserRepoKeyId = cryptoLinkDto.getFromUserRepoKeyId();
		cryptoLink.setFromUserRepoKeyPublicKey(fromUserRepoKeyId == null ? null : userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeyOrFail(fromUserRepoKeyId));

		final Uid toCryptoKeyId = assertNotNull(cryptoLinkDto.getToCryptoKeyId(), "cryptoLinkDto.toCryptoKeyId");
		final CryptoKey toCryptoKey = cryptoKeyDao.getCryptoKeyOrFail(toCryptoKeyId);
		cryptoLink.setToCryptoKey(toCryptoKey);

		cryptoLink.setToCryptoKeyData(cryptoLinkDto.getToCryptoKeyData());
		cryptoLink.setToCryptoKeyPart(cryptoLinkDto.getToCryptoKeyPart());
		cryptoLink.setSignature(cryptoLinkDto.getSignature());
		cryptoLink.setLastSyncFromRepositoryId(getRemoteRepositoryIdOrFail());

		toCryptoKey.getInCryptoLinks().add(cryptoLink);
		return cryptoLinkDao.makePersistent(cryptoLink);
	}

	private void putCryptoConfigPropSetDtos(final List<CryptoConfigPropSetDto> cryptoConfigPropSetDtos) {
		assertNotNull(cryptoConfigPropSetDtos, "cryptoConfigPropSetDtos");
		final CryptoConfigPropSetDtoConverter converter = CryptoConfigPropSetDtoConverter.create(getTransactionOrFail());
		for (CryptoConfigPropSetDto cryptoConfigPropSetDto : cryptoConfigPropSetDtos) {
			final CryptoConfigPropSet cryptoConfigPropSet = converter.putCryptoConfigPropSetDto(cryptoConfigPropSetDto);
			cryptoConfigPropSet.setLastSyncFromRepositoryId(getRemoteRepositoryIdOrFail());
		}
	}

	private void putPermissionDto(final PermissionDto permissionDto) {
		assertNotNull(permissionDto, "permissionDto");
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
		assertNotNull(permissionSetDto, "permissionSetDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final PermissionSetDao permissionSetDao = transaction.getDao(PermissionSetDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(permissionSetDto.getCryptoRepoFileId());
		PermissionSet permissionSet = permissionSetDao.getPermissionSet(cryptoRepoFile);
		if (permissionSet == null)
			permissionSet = new PermissionSet();

		permissionSet.setCryptoRepoFile(cryptoRepoFile);
		permissionSet.setSignature(permissionSetDto.getSignature());
		permissionSetDao.makePersistent(permissionSet);
	}

	private void putPermissionSetInheritanceDto(final PermissionSetInheritanceDto psInheritanceDto) {
		assertNotNull(psInheritanceDto, "psInheritanceDto");
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final PermissionSetInheritanceDao psInheritanceDao = transaction.getDao(PermissionSetInheritanceDao.class);
		final PermissionSetDao permissionSetDao = transaction.getDao(PermissionSetDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		PermissionSetInheritance psInheritance = psInheritanceDao.getPermissionSetInheritance(psInheritanceDto.getPermissionSetInheritanceId());
		if (psInheritance == null)
			psInheritance = new PermissionSetInheritance(psInheritanceDto.getPermissionSetInheritanceId());

		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(psInheritanceDto.getCryptoRepoFileId());
		final PermissionSet permissionSet = permissionSetDao.getPermissionSetOrFail(cryptoRepoFile);
		psInheritance.setPermissionSet(permissionSet);
		psInheritance.setRevoked(psInheritanceDto.getRevoked());
		psInheritance.setSignature(psInheritanceDto.getSignature());
		psInheritance.setValidFrom(psInheritanceDto.getValidFrom());

		final Date oldValidTo = psInheritance.getValidTo();
		psInheritance.setValidTo(psInheritanceDto.getValidTo());
		psInheritanceDao.makePersistent(psInheritance);

		if (psInheritance.getValidTo() != null && oldValidTo == null) {
			if (isOnServer()) {
				// We must prevent the new permissionSetInheritance from making existing data on the server illegal.
				// Hence, we roll back and throw an exception, if this happens.
				//
				// See: enactPermissionSetInheritanceRevocationIfNeededAndPossible(...)

				final Set<UserRepoKeyPublicKey> userRepoKeyPublicKeys = new HashSet<UserRepoKeyPublicKey>();
				collectUserRepoKeyPublicKeysOfThisAndParentPermissionSets(userRepoKeyPublicKeys, permissionSet);
				for (final UserRepoKeyPublicKey userRepoKeyPublicKey : userRepoKeyPublicKeys) {
					final SignableDao signableDao = getTransactionOrFail().getDao(SignableDao.class);
					if (signableDao.isEntitiesSignedByAndAfter(userRepoKeyPublicKey.getUserRepoKeyId(), psInheritance.getValidTo()))
						throw new PermissionCollisionException("There is already data written and signed after the Permission.validTo timestamp. Are clocks in-sync?");
				}

				// We are currently too restrictive and do not take into account that the inheritance chain might be interrupted
				// already in a parent. Thus these permissions above would not be affected. But since this is a corner case, anyway,
				// we don't need to be 100% exact here and better too restrictive than too lax (we must make 100% sure no illegal
				// data ends up in the database!).
			}
			else {
				// TODO we should clean up this illegal state on the client, too! It's one of the many collisions we have to cope with!
			}
		}
	}

	private void collectUserRepoKeyPublicKeysOfThisAndParentPermissionSets(final Set<UserRepoKeyPublicKey> userRepoKeyPublicKeys, final PermissionSet permissionSet) {
		assertNotNull(userRepoKeyPublicKeys, "userRepoKeyPublicKeys");
		assertNotNull(permissionSet, "permissionSet");

		for (final Permission permission : permissionSet.getPermissions())
			userRepoKeyPublicKeys.add(permission.getUserRepoKeyPublicKey());

		final PermissionSet parentPermissionSet = getParentPermissionSet(permissionSet);
		if (parentPermissionSet != null)
			collectUserRepoKeyPublicKeysOfThisAndParentPermissionSets(userRepoKeyPublicKeys, parentPermissionSet);
	}

	private PermissionSet getParentPermissionSet(final PermissionSet permissionSet) {
		final PermissionSetDao permissionSetDao = getTransactionOrFail().getDao(PermissionSetDao.class);
		CryptoRepoFile parentCryptoRepoFile = permissionSet.getCryptoRepoFile();
		while (null != (parentCryptoRepoFile = parentCryptoRepoFile.getParent())) {
			final PermissionSet parentPermissionSet = permissionSetDao.getPermissionSet(parentCryptoRepoFile);
			if (parentPermissionSet != null)
				return parentPermissionSet;
		}
		return null;
	}

	private void putRepositoryOwnerDto(final RepositoryOwnerDto repositoryOwnerDto) {
		assertNotNull(repositoryOwnerDto, "repositoryOwnerDto");
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
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");

		final LocalRepository localRepository = getTransactionOrFail().getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();
		lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());

		final CryptoChangeSetDto cryptoChangeSetDto = new CryptoChangeSetDto();
		cryptoChangeSetDto.getCryptoRepoFileDtos().add(CryptoRepoFileDtoConverter.create().toCryptoRepoFileDto(cryptoRepoFile));

//		// TODO why did I only populate this one single CryptoRepoFileDto?! Shouldn't we better always send all changed ones?!
//		// Currently trying this out... If this works fine, we should refactor!
//		populateChangedCryptoRepoFileDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		populateCryptoChangeSetDtoWithAllButCryptoRepoFiles(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		logger.debug("getCryptoChangeSetDto({}): {}", cryptoRepoFile, cryptoChangeSetDto);
		return cryptoChangeSetDto;
	}

	private void populateCryptoChangeSetDtoWithAllButCryptoRepoFiles(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		logger.info("populateCryptoChangeSetDtoWithAllButCryptoRepoFiles: localRepositoryId={} remoteRepositoryId={}, lastCryptoKeySyncToRemoteRepo.localRepositoryRevisionSynced={}, lastCryptoKeySyncToRemoteRepo.localRepositoryRevisionInProgress={}",
				getLocalRepositoryIdOrFail(),
				lastCryptoKeySyncToRemoteRepo.getRemoteRepository().getRepositoryId(),
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionInProgress());
		populateRevision(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedUserRepoKeyPublicKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoLinkDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedRepositoryOwnerDto(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedPermissionSetInheritanceDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedPermissionDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedPermissionSetDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedUserRepoKeyPublicKeyReplacementRequestDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedUserRepoKeyPublicKeyReplacementRequestDeletionDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedUserIdentityLinkDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedUserIdentityDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCurrentHistoCryptoRepoFileDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedHistoCryptoRepoFileDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedHistoFrameDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCollisionDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoConfigPropSetDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
	}

	private void populateRevision(CryptoChangeSetDto cryptoChangeSetDto, LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		cryptoChangeSetDto.setRevision(lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionInProgress());
		if (cryptoChangeSetDto.getRevision() < 0)
			throw new IllegalStateException("cryptoChangeSetDto.revision < 0");
	}

	private void populateChangedCryptoConfigPropSetDtos(CryptoChangeSetDto cryptoChangeSetDto, LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CryptoConfigPropSetDtoConverter converter = CryptoConfigPropSetDtoConverter.create(getTransactionOrFail());
		final CryptoConfigPropSetDao dao = getTransactionOrFail().getDao(CryptoConfigPropSetDao.class);

		final Collection<CryptoConfigPropSet> entities = dao.getCryptoConfigPropSetsChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				resyncMode ? NULL_UUID : getRemoteRepositoryIdOrFail());

		for (final CryptoConfigPropSet entity : entities)
			cryptoChangeSetDto.getCryptoConfigPropSetDtos().add(converter.toCryptoConfigPropSetDto(entity));
	}

	private void populateChangedCollisionDtos(CryptoChangeSetDto cryptoChangeSetDto, LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CollisionDtoConverter converter = CollisionDtoConverter.create(getTransactionOrFail());
		final CollisionDao dao = getTransactionOrFail().getDao(CollisionDao.class);

		final Collection<Collision> entities = dao.getCollisionsChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final Collision entity : entities)
			cryptoChangeSetDto.getCollisionDtos().add(converter.toCollisionDto(entity));
	}

	private void populateChangedHistoFrameDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final HistoFrameDtoConverter converter = HistoFrameDtoConverter.create(getTransactionOrFail());
		final HistoFrameDao dao = getTransactionOrFail().getDao(HistoFrameDao.class);

		final Collection<HistoFrame> entities = dao.getHistoFramesChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				resyncMode ? NULL_UUID : getRemoteRepositoryIdOrFail());

		for (final HistoFrame entity : entities)
			cryptoChangeSetDto.getHistoFrameDtos().add(converter.toHistoFrameDto(entity));
	}

	private void populateChangedHistoCryptoRepoFileDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		if (! isOnServer() && ! resyncMode)
			return; // We *up*load them exclusively individually. The CryptoChangeSet is only used for *down*load.

		final HistoCryptoRepoFileDtoConverter converter = HistoCryptoRepoFileDtoConverter.create(getTransactionOrFail());
		final HistoCryptoRepoFileDao dao = getTransactionOrFail().getDao(HistoCryptoRepoFileDao.class);

		final Collection<HistoCryptoRepoFile> entities = dao.getHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				resyncMode ? NULL_UUID : getRemoteRepositoryIdOrFail());

		for (final HistoCryptoRepoFile entity : entities)
			cryptoChangeSetDto.getHistoCryptoRepoFileDtos().add(converter.toHistoCryptoRepoFileDto(entity));
	}

	private void populateChangedCurrentHistoCryptoRepoFileDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		if (! isOnServer() && ! resyncMode)
			return; // We *up*load them exclusively individually. The CryptoChangeSet is only used for *down*load.

		final CurrentHistoCryptoRepoFileDtoConverter converter = CurrentHistoCryptoRepoFileDtoConverter.create(getTransactionOrFail());
		final CurrentHistoCryptoRepoFileDao dao = getTransactionOrFail().getDao(CurrentHistoCryptoRepoFileDao.class);

		final Collection<CurrentHistoCryptoRepoFile> entities = dao.getCurrentHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				resyncMode ? NULL_UUID : getRemoteRepositoryIdOrFail());

		for (final CurrentHistoCryptoRepoFile entity : entities)
			cryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos().add(converter.toCurrentHistoCryptoRepoFileDto(entity, false));
	}

	private void populateChangedUserRepoKeyPublicKeyDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final UserRepoKeyPublicKeyDtoConverter userRepoKeyPublicKeyDtoConverter = new UserRepoKeyPublicKeyDtoConverter();
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = getTransactionOrFail().getDao(UserRepoKeyPublicKeyDao.class);

		final Collection<UserRepoKeyPublicKey> userRepoKeyPublicKeys = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeysChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final UserRepoKeyPublicKey userRepoKeyPublicKey : userRepoKeyPublicKeys)
			cryptoChangeSetDto.getUserRepoKeyPublicKeyDtos().add(userRepoKeyPublicKeyDtoConverter.toUserRepoKeyPublicKeyDto(userRepoKeyPublicKey));
	}

	private void populateChangedCryptoRepoFileDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CryptoRepoFileDao cryptoRepoFileDao = getTransactionOrFail().getDao(CryptoRepoFileDao.class);

		final Collection<CryptoRepoFile> cryptoRepoFiles = cryptoRepoFileDao.getCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				resyncMode ? NULL_UUID : getRemoteRepositoryIdOrFail());

		final CryptoRepoFileDtoConverter cryptoRepoFileDtoConverter = CryptoRepoFileDtoConverter.create();
		for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFiles)
			cryptoChangeSetDto.getCryptoRepoFileDtos().add(cryptoRepoFileDtoConverter.toCryptoRepoFileDto(cryptoRepoFile));
	}

	private void populateChangedCryptoLinkDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CryptoLinkDao cryptoLinkDao = getTransactionOrFail().getDao(CryptoLinkDao.class);

		final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getCryptoLinksChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				resyncMode ? NULL_UUID : getRemoteRepositoryIdOrFail());

		for (final CryptoLink cryptoLink : cryptoLinks)
			cryptoChangeSetDto.getCryptoLinkDtos().add(toCryptoLinkDto(cryptoLink));
	}

	private void populateChangedCryptoKeyDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final CryptoKeyDao cryptoKeyDao = getTransactionOrFail().getDao(CryptoKeyDao.class);

		final Collection<CryptoKey> cryptoKeys = cryptoKeyDao.getCryptoKeysChangedAfterExclLastSyncFromRepositoryId(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				resyncMode ? NULL_UUID : getRemoteRepositoryIdOrFail());

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

	private void populateChangedPermissionSetInheritanceDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final PermissionSetInheritanceDao psInheritanceDao = getTransactionOrFail().getDao(PermissionSetInheritanceDao.class);

		final Collection<PermissionSetInheritance> psInheritances = psInheritanceDao.getPermissionSetInheritancesChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final PermissionSetInheritance psInheritance : psInheritances) {
			enactPermissionSetInheritanceRevocationIfNeededAndPossible(psInheritance);

			cryptoChangeSetDto.getPermissionSetInheritanceDtos().add(toPermissionSetInheritanceDto(psInheritance));
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
				logger.warn("Cannot enact revocation of Permission because of missing permission: " + x, x);
			}
		}
	}

	private void enactPermissionSetInheritanceRevocationIfNeededAndPossible(final PermissionSetInheritance psInheritance) {
		if (! isOnServer() && psInheritance.getRevoked() != null && psInheritance.getValidTo() == null) {
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

			psInheritance.setValidTo(new Date());
			try {
				sign(psInheritance);
			} catch (final GrantAccessDeniedException x) {
				psInheritance.setValidTo(null); // revert to restore the signed state.
				logger.warn("Cannot enact revocation of PermissionSetInheritance because of missing permission: " + x, x);
			}
		}
	}

	@Override
	public void sign(final WriteProtected writeProtected) throws AccessDeniedException {
		final CryptreeNode rootCryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(getRootCryptoRepoFileId());
		rootCryptreeNode.sign(writeProtected);
	}

	private void populateChangedPermissionSetDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final PermissionSetDao permissionSetDao = getTransactionOrFail().getDao(PermissionSetDao.class);

		final Collection<PermissionSet> permissionSets = permissionSetDao.getPermissionSetsChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final PermissionSet permissionSet : permissionSets)
			cryptoChangeSetDto.getPermissionSetDtos().add(toPermissionSetDto(permissionSet));
	}

	private void populateChangedUserRepoKeyPublicKeyReplacementRequestDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo)
	{
		final UserRepoKeyPublicKeyReplacementRequestDtoConverter converter = new UserRepoKeyPublicKeyReplacementRequestDtoConverter();
		final UserRepoKeyPublicKeyReplacementRequestDao dao = getTransactionOrFail().getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);

		final Collection<UserRepoKeyPublicKeyReplacementRequest> requests = dao.getUserRepoKeyPublicKeyReplacementRequestsChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final UserRepoKeyPublicKeyReplacementRequest request : requests)
			cryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDtos().add(converter.toUserRepoKeyPublicKeyReplacementRequestDto(request));
	}

	private void populateChangedUserRepoKeyPublicKeyReplacementRequestDeletionDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo)
	{
		final UserRepoKeyPublicKeyReplacementRequestDeletionDtoConverter converter = new UserRepoKeyPublicKeyReplacementRequestDeletionDtoConverter();
		final UserRepoKeyPublicKeyReplacementRequestDeletionDao dao = getTransactionOrFail().getDao(UserRepoKeyPublicKeyReplacementRequestDeletionDao.class);

		final Collection<UserRepoKeyPublicKeyReplacementRequestDeletion> requestDeletions = dao.getUserRepoKeyPublicKeyReplacementRequestDeletionsChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (UserRepoKeyPublicKeyReplacementRequestDeletion requestDeletion : requestDeletions)
			cryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDeletionDtos().add(converter.toUserRepoKeyPublicKeyReplacementRequestDeletionDto(requestDeletion));
	}

	private void populateChangedUserIdentityDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final UserIdentityDtoConverter converter = new UserIdentityDtoConverter();
		final UserIdentityDao dao = getTransactionOrFail().getDao(UserIdentityDao.class);

		final Collection<UserIdentity> userIdentities = dao.getUserIdentitiesChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (UserIdentity userIdentity : userIdentities)
			cryptoChangeSetDto.getUserIdentityDtos().add(converter.toUserIdentityDto(userIdentity));
	}

	private void populateChangedUserIdentityLinkDtos(final CryptoChangeSetDto cryptoChangeSetDto, final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo) {
		final UserIdentityLinkDtoConverter converter = new UserIdentityLinkDtoConverter();
		final UserIdentityLinkDao dao = getTransactionOrFail().getDao(UserIdentityLinkDao.class);

		final Collection<UserIdentityLink> userIdentityLinks = dao.getUserIdentityLinksChangedAfter(
				lastCryptoKeySyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (UserIdentityLink userIdentityLink : userIdentityLinks)
			cryptoChangeSetDto.getUserIdentityLinkDtos().add(converter.toUserIdentityLinkDto(userIdentityLink));
	}

	private CryptoLinkDto toCryptoLinkDto(final CryptoLink cryptoLink) {
		assertNotNull(cryptoLink, "cryptoLink");
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
		cryptoLinkDto.setSignature(assertNotNull(cryptoLink.getSignature(), "cryptoLink.signature"));
		return cryptoLinkDto;
	}

	private CryptoKeyDto toCryptoKeyDto(final CryptoKey cryptoKey) {
		assertNotNull(cryptoKey, "cryptoKey");
		final CryptoKeyDto cryptoKeyDto = new CryptoKeyDto();
		cryptoKeyDto.setCryptoKeyId(cryptoKey.getCryptoKeyId());
		cryptoKeyDto.setCryptoRepoFileId(cryptoKey.getCryptoRepoFile().getCryptoRepoFileId());

//		cryptoKeyDto.setActive(cryptoKey.isActive());
		final CryptoKeyDeactivation cryptoKeyDeactivation = cryptoKey.getCryptoKeyDeactivation();
		if (cryptoKeyDeactivation != null)
			cryptoKeyDto.setCryptoKeyDeactivationDto(toCryptoKeyDeactivationDto(cryptoKeyDeactivation));

		cryptoKeyDto.setCryptoKeyRole(assertNotNull(cryptoKey.getCryptoKeyRole(), "cryptoKey.cryptoKeyRole"));
		cryptoKeyDto.setCryptoKeyType(assertNotNull(cryptoKey.getCryptoKeyType(), "cryptoKey.cryptoKeyType"));

		final Signature signature = cryptoKey.getSignature();
		cryptoKeyDto.setSignature(assertNotNull(signature, "cryptoKey.signature"));
		assertNotNull(signature.getSignatureCreated(), "cryptoKey.signature.signatureCreated");
		assertNotNull(signature.getSigningUserRepoKeyId(), "cryptoKey.signature.signingUserRepoKeyId");
		assertNotNull(signature.getSignatureData(), "cryptoKey.signature.signatureData");

		return cryptoKeyDto;
	}

	private CryptoKeyDeactivationDto toCryptoKeyDeactivationDto(final CryptoKeyDeactivation cryptoKeyDeactivation) {
		assertNotNull(cryptoKeyDeactivation, "cryptoKeyDeactivation");
		final CryptoKeyDeactivationDto cryptoKeyDeactivationDto = new CryptoKeyDeactivationDto();
		final CryptoKey cryptoKey = assertNotNull(cryptoKeyDeactivation.getCryptoKey(), "cryptoKeyDeactivation.cryptoKey");
		cryptoKeyDeactivationDto.setCryptoKeyId(assertNotNull(cryptoKey.getCryptoKeyId(), "cryptoKeyDeactivation.cryptoKey.cryptoKeyId"));
		cryptoKeyDeactivationDto.setSignature(cryptoKeyDeactivation.getSignature());
		return cryptoKeyDeactivationDto;
	}

	private RepositoryOwnerDto toRepositoryOwnerDto(final RepositoryOwner repositoryOwner) {
		assertNotNull(repositoryOwner, "repositoryOwner");
		final RepositoryOwnerDto repositoryOwnerDto = new RepositoryOwnerDto();
		repositoryOwnerDto.setServerRepositoryId(repositoryOwner.getServerRepositoryId());
		repositoryOwnerDto.setSignature(repositoryOwner.getSignature());
		repositoryOwnerDto.setUserRepoKeyId(repositoryOwner.getUserRepoKeyPublicKey().getUserRepoKeyId());
		return repositoryOwnerDto;
	}

	private PermissionSetDto toPermissionSetDto(final PermissionSet permissionSet) {
		assertNotNull(permissionSet, "permissionSet");
		final PermissionSetDto permissionSetDto = new PermissionSetDto();
		permissionSetDto.setCryptoRepoFileId(permissionSet.getCryptoRepoFile().getCryptoRepoFileId());
		permissionSetDto.setSignature(permissionSet.getSignature());
		return permissionSetDto;
	}

	private PermissionDto toPermissionDto(final Permission permission) {
		assertNotNull(permission, "permission");
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

	private PermissionSetInheritanceDto toPermissionSetInheritanceDto(final PermissionSetInheritance psInheritance) {
		assertNotNull(psInheritance, "psInheritance");
		final PermissionSetInheritanceDto psInheritanceDto = new PermissionSetInheritanceDto();
		psInheritanceDto.setPermissionSetInheritanceId(psInheritance.getPermissionSetInheritanceId());
		psInheritanceDto.setCryptoRepoFileId(psInheritance.getPermissionSet().getCryptoRepoFile().getCryptoRepoFileId());
		psInheritanceDto.setRevoked(psInheritance.getRevoked());
		psInheritanceDto.setSignature(psInheritance.getSignature());
		psInheritanceDto.setValidFrom(psInheritance.getValidFrom());
		psInheritanceDto.setValidTo(psInheritance.getValidTo());
		return psInheritanceDto;
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
			case CLIENT_META_ONLY:
				if (isOnServer())
					throw new IllegalStateException("SsLocalRepository.localRepositoryType is already initialised to " + localRepositoryType + "! Cannot switch to SERVER!");
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
		if (isOnServer())
			return;

		final LocalRepoTransaction transaction = getTransactionOrFail();
		final RepositoryOwnerDao roDao = transaction.getDao(RepositoryOwnerDao.class);
		final UUID serverRepositoryId = getServerRepositoryIdOrFail();
		final UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRingOrFail();

		RepositoryOwner repositoryOwner = roDao.getRepositoryOwner(serverRepositoryId);
		if (repositoryOwner == null) {
			final UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
			final UserRepoKey userRepoKey = userRepoKeyRing.getPermanentUserRepoKeys(serverRepositoryId).get(0);

			UserRepoKeyPublicKey userRepoKeyPublicKey = urkpkDao.getUserRepoKeyPublicKey(userRepoKey.getUserRepoKeyId());
			if (userRepoKeyPublicKey == null)
				userRepoKeyPublicKey = urkpkDao.makePersistent(new UserRepoKeyPublicKey(userRepoKey.getPublicKey()));

			repositoryOwner = new RepositoryOwner();
			repositoryOwner.setUserRepoKeyPublicKey(userRepoKeyPublicKey);

			getCryptreeContext().getSignableSigner(userRepoKey).sign(repositoryOwner);
			repositoryOwner = roDao.makePersistent(repositoryOwner);
		}
	}

	@Override
	public void assertSignatureOk(final WriteProtected writeProtected) throws SignatureException, AccessDeniedException {
		Uid crfIdControllingPermissions = writeProtected.getCryptoRepoFileIdControllingPermissions();
		if (crfIdControllingPermissions == null)
			crfIdControllingPermissions = getTransactionOrFail().getDao(CryptoRepoFileDao.class).getRootCryptoRepoFile().getCryptoRepoFileId();

		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(crfIdControllingPermissions);
		cryptreeNode.assertSignatureOk(writeProtected);
	}

	@Override
	public Set<PermissionType> getGrantedPermissionTypes(final String localPath, final Uid userRepoKeyId) {
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		return cryptreeNode.getGrantedPermissionTypes(userRepoKeyId);
	}

	@Override
	public void assertIsNotDeletedDuplicateCryptoRepoFile(Uid cryptoRepoFileId) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		Collection<Collision> collisions = getCryptreeContext().transaction.getDao(CollisionDao.class).getCollisionsWithDuplicateCryptoRepoFileId(cryptoRepoFileId);
		if (! collisions.isEmpty())
			throw new CollisionException("CryptoRepoFile was deleted due to being a duplicate! cryptoRepoFileId = " + cryptoRepoFileId);
	}

	@Override
	public void assertHasPermission(
			final Uid cryptoRepoFileId,
			final Uid userRepoKeyId,
			final PermissionType permissionType, final Date timestamp
			) throws AccessDeniedException
	{
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		assertNotNull(userRepoKeyId, "userRepoKeyId");
		assertNotNull(permissionType, "permissionType");
		assertNotNull(timestamp, "timestamp");

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

	@Override
	public Uid getCryptoRepoFileIdForRemotePathPrefixOrFail() {
		return getCryptreeContext().getCryptoRepoFileIdForRemotePathPrefixOrFail();
	}

	@Override
	public Uid getCryptoRepoFileIdOrFail(final String localPath) {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		return cryptoRepoFile == null ? null : cryptoRepoFile.getCryptoRepoFileId();
	}

	@Override
	public Uid getCryptoRepoFileId(final String localPath) {
		assertNotNull(localPath, "localPath");
		final CryptoRepoFile cryptoRepoFile = getCryptreeContext().getCryptoRepoFile(localPath);
		return cryptoRepoFile == null ? null : cryptoRepoFile.getCryptoRepoFileId();
	}

	@Override
	public Uid getParentCryptoRepoFileId(final Uid cryptoRepoFileId) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		if (getRootCryptoRepoFileId().equals(cryptoRepoFileId))
			return null;

		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);
		final CryptoRepoFile parent = assertNotNull(cryptreeNode.getCryptoRepoFile(), "cryptreeNode.cryptoRepoFile").getParent();
		assertNotNull(parent, "cryptreeNode.cryptoRepoFile.parent");
		return parent.getCryptoRepoFileId();
	}

	@Override
	public Uid getOwnerUserRepoKeyId() {
		final RepositoryOwner repositoryOwner = getCryptreeContext().getRepositoryOwner();
		if (repositoryOwner == null)
			return null;

		return repositoryOwner.getUserRepoKeyPublicKey().getUserRepoKeyId();
	}

	@Override
	public LocalRepoStorage getLocalRepoStorage() {
		if (localRepoStorage == null) {
			final LocalRepoStorageFactoryRegistry registry = LocalRepoStorageFactoryRegistry.getInstance();
			final LocalRepoStorageFactory factory = registry.getLocalRepoStorageFactoryOrFail();
			if (getRemotePathPrefix() == null)
				return factory.getLocalRepoStorageOrCreate(getTransactionOrFail());
			else
				return factory.getLocalRepoStorageOrCreate(getTransactionOrFail(), getRemoteRepositoryIdOrFail(), getRemotePathPrefixOrFail());
		}
		return localRepoStorage;
	}

	@Override
	public CryptoChangeSetDto createHistoCryptoRepoFilesForDeletedCryptoRepoFiles() {
		convertPreliminaryDeletions();

		final LocalRepoTransaction tx = getTransactionOrFail();
		final CryptoRepoFileDao cryptoRepoFileDao = tx.getDao(CryptoRepoFileDao.class);
		final Collection<CryptoRepoFile> deletedCryptoRepoFiles = cryptoRepoFileDao.getDeletedCryptoRepoFilesWithoutCurrentHistoCryptoRepoFileAlsoDeleted();
		final List<HistoCryptoRepoFile> histoCryptoRepoFiles = new ArrayList<>();
		if (deletedCryptoRepoFiles.isEmpty())
			return null;

		createUnsealedHistoFrameIfNeeded();
		for (CryptoRepoFile deletedCryptoRepoFile : deletedCryptoRepoFiles) {
			final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(deletedCryptoRepoFile.getCryptoRepoFileId());
			histoCryptoRepoFiles.add(cryptreeNode.createHistoCryptoRepoFileIfNeeded());
		}

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDtoWithCryptoRepoFiles(null);

		final CurrentHistoCryptoRepoFileDao chcrfDao = tx.getDao(CurrentHistoCryptoRepoFileDao.class);
		final HistoCryptoRepoFileDtoConverter hcrfdConverter = HistoCryptoRepoFileDtoConverter.create(tx);
		final CurrentHistoCryptoRepoFileDtoConverter chcrfdConverter = CurrentHistoCryptoRepoFileDtoConverter.create(tx);

		// HistoCryptoRepoFiles are normally only *DOWN*-synced, but for the deleted ones, we want them to be up-synced, too.
		for (HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFiles) {
			final CryptoRepoFile cryptoRepoFile = histoCryptoRepoFile.getCryptoRepoFile();
			final CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = chcrfDao.getCurrentHistoCryptoRepoFile(cryptoRepoFile);
			cryptoChangeSetDto.getHistoCryptoRepoFileDtos().add(hcrfdConverter.toHistoCryptoRepoFileDto(histoCryptoRepoFile));
			cryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos().add(chcrfdConverter.toCurrentHistoCryptoRepoFileDto(currentHistoCryptoRepoFile, false));
		}
		return cryptoChangeSetDto;
	}

	@Override
	public void createUnsealedHistoFrameIfNeeded() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final HistoFrameDao histoFrameDao = tx.getDao(HistoFrameDao.class);
		final UUID fromRepositoryId = getLocalRepositoryIdOrFail();

		if (getRootCryptoRepoFileId() == null)
			createOrUpdateCryptoRepoFile("");

		final UserRepoKey userRepoKey = getUserRepoKeyOrFail(null, PermissionType.write);

		HistoFrame histoFrame = histoFrameDao.getUnsealedHistoFrame(fromRepositoryId);
		if (histoFrame == null) {
			histoFrame = new HistoFrame();
			histoFrame.setFromRepositoryId(fromRepositoryId);
			histoFrame.setLastSyncFromRepositoryId(null);

			getCryptreeContext().getSignableSigner(userRepoKey).sign(histoFrame);

			histoFrame = histoFrameDao.makePersistent(histoFrame);
		}
	}

	private void convertPreliminaryCollisions() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PreliminaryCollisionDao pcDao = tx.getDao(PreliminaryCollisionDao.class);
		final RepoFileDao rfDao = tx.getDao(RepoFileDao.class);
		final CryptoRepoFileDao crfDao = tx.getDao(CryptoRepoFileDao.class);

		for (PreliminaryCollision preliminaryCollision : pcDao.getObjects()) {
			File file = createFile(tx.getLocalRepoManager().getLocalRoot(), preliminaryCollision.getPath());
			RepoFile repoFile = rfDao.getRepoFile(tx.getLocalRepoManager().getLocalRoot(), file);
			CryptoRepoFile cryptoRepoFile = repoFile == null ? null : crfDao.getCryptoRepoFileOrFail(repoFile);
			if (cryptoRepoFile == null)
				cryptoRepoFile = preliminaryCollision.getCryptoRepoFile();

			if (cryptoRepoFile == null)
				throw new IllegalStateException(String.format("Could not determine CryptoRepoFile for: %s (path='%s')", preliminaryCollision, preliminaryCollision.getPath()));

			CryptoRepoFile cryptoRepoFile2 = preliminaryCollision.getCryptoRepoFile();
			if (! cryptoRepoFile.equals(cryptoRepoFile2))
				throw new IllegalStateException("preliminaryCollision.cryptoRepoFile points to different CryptoRepoFile than repoFile!");

			createCollisionIfNeeded(cryptoRepoFile, null, preliminaryCollision.getPath(), false);

			pcDao.deletePersistent(preliminaryCollision);
		}
	}

	public Collision createCollisionIfNeeded(final CryptoRepoFile cryptoRepoFile, final CryptoRepoFile duplicateCryptoRepoFile, final String localPath, boolean expectedSealedStatus) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");

		return getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFile.getCryptoRepoFileId())
				.createCollisionIfNeeded(duplicateCryptoRepoFile, localPath, expectedSealedStatus);
	}

	@Override
	public void sealUnsealedHistoryFrame() {
		convertPreliminaryCollisions();

		final LocalRepoTransaction tx = getTransactionOrFail();
		final HistoFrameDao histoFrameDao = tx.getDao(HistoFrameDao.class);
		final CryptreeNode rootCryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(getRootCryptoRepoFileId());
		final UserRepoKey userRepoKey = rootCryptreeNode.getUserRepoKey(true, PermissionType.write);
		final HistoFrame histoFrame = histoFrameDao.getUnsealedHistoFrame(getLocalRepositoryIdOrFail());
		if (histoFrame != null) {
			histoFrame.setSealed(new Date());
			histoFrame.setLastSyncFromRepositoryId(null);
			getCryptreeContext().getSignableSigner(userRepoKey).sign(histoFrame);
		}
	}

	@Override
	public void putHistoFrameDto(final HistoFrameDto histoFrameDto) {
		assertNotNull(histoFrameDto, "histoFrameDto");
		final LocalRepoTransaction tx = getTransactionOrFail();
		final HistoFrame histoFrame = HistoFrameDtoConverter.create(tx).putHistoFrameDto(histoFrameDto);
		histoFrame.setLastSyncFromRepositoryId(getRemoteRepositoryIdOrFail());
	}

	private Collision putCollisionDto(final CollisionDto collisionDto) {
		assertNotNull(collisionDto, "collisionDto");
		final LocalRepoTransaction tx = getTransactionOrFail();
		final Collision collision = CollisionDtoConverter.create(tx).putCollisionDto(collisionDto);
		DuplicateCryptoRepoFileHandler.createInstance(tx).deduplicateFromCollisionIfNeeded(collision);
		return collision;
	}

	@Override
	public void preDelete(final String localPath, final boolean deletedByIgnoreRule) {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();

		if (cryptoRepoFile != null) {
			PreliminaryDeletionDao pdDao = getTransactionOrFail().getDao(PreliminaryDeletionDao.class);
			PreliminaryDeletion preliminaryDeletion = pdDao.getPreliminaryDeletion(cryptoRepoFile);
			if (preliminaryDeletion == null) {
				preliminaryDeletion = new PreliminaryDeletion();
				preliminaryDeletion.setCryptoRepoFile(cryptoRepoFile);
				preliminaryDeletion.setDeletedByIgnoreRule(deletedByIgnoreRule);
				pdDao.makePersistent(preliminaryDeletion);
			}
		}
	}

	private void convertPreliminaryDeletions() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final PreliminaryDeletionDao pdDao = tx.getDao(PreliminaryDeletionDao.class);
		final CryptreeContext cryptreeContext = getCryptreeContext();
		for (PreliminaryDeletion preliminaryDeletion : pdDao.getObjects()) {
			final CryptoRepoFile cryptoRepoFile = preliminaryDeletion.getCryptoRepoFile();
			final CryptreeNode cryptreeNode = cryptreeContext.getCryptreeNodeOrCreate(cryptoRepoFile.getCryptoRepoFileId());
			if (cryptoRepoFile != null && cryptoRepoFile.getDeleted() == null) {
				cryptoRepoFile.setDeleted(new Date());
				cryptoRepoFile.setDeletedByIgnoreRule(preliminaryDeletion.isDeletedByIgnoreRule());
				cryptoRepoFile.setLastSyncFromRepositoryId(null);
				cryptreeNode.sign(cryptoRepoFile);
				cryptreeNode.updateCryptoConfigPropSetIfConfigFile();
			}
			pdDao.deletePersistent(preliminaryDeletion);
		}
		tx.flush();
	}

	@Override
	public void createSyntheticDeleteModifications(final ChangeSetDto changeSetDto) {
		assertNotNull(changeSetDto, "changeSetDto");
		final LocalRepoTransaction tx = getTransactionOrFail();
		final CryptoRepoFileDao cryptoRepoFileDao = tx.getDao(CryptoRepoFileDao.class);
		final Collection<CryptoRepoFile> cryptoRepoFiles = cryptoRepoFileDao.getCryptoRepoFilesWithRepoFileAndDeleted();
		for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFiles) {
			if (cryptoRepoFile.isDeletedByIgnoreRule())
				continue; // ignore

			final String path = cryptoRepoFile.getRepoFile().getPath();
			final DeleteModificationDto deleteModificationDto = new DeleteModificationDto();
			deleteModificationDto.setPath(path);
			changeSetDto.getModificationDtos().add(deleteModificationDto);
		}
	}

	@Override
	public Collection<PlainHistoCryptoRepoFileDto> getPlainHistoCryptoRepoFileDtos(PlainHistoCryptoRepoFileFilter filter) {
		assertNotNull(filter, "filter");

		if ("/".equals(filter.getLocalPath())) { // the root is normally simply "", but we are tolerant to "/".
			filter = filter.clone();
			filter.setLocalPath("");
		}

		final LocalRepoTransaction tx = getTransactionOrFail();
		final HistoCryptoRepoFileDao hcrfDao = tx.getDao(HistoCryptoRepoFileDao.class);

		final Uid histoFrameId = filter.getHistoFrameId();
		if (histoFrameId != null) {
			if (filter.getHistoCryptoRepoFileIds() != null)
				throw new UnsupportedOperationException("Filtering by both histoFrameId and histoCryptoRepoFileIds is currently not supported!");

			if (filter.getCollisionIds() != null)
				throw new UnsupportedOperationException("Filtering by both histoFrameId and collisionIds is currently not supported!");

			final HistoFrame histoFrame = tx.getDao(HistoFrameDao.class).getHistoFrameOrFail(histoFrameId);
			final Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = hcrfDao.getHistoCryptoRepoFiles(histoFrame);
			return getPlainHistoCryptoRepoFileDtos(histoCryptoRepoFiles, filter);
		}

		final Set<Uid> histoCryptoRepoFileIds = filter.getHistoCryptoRepoFileIds();
		if (histoCryptoRepoFileIds != null) {
			if (filter.getCollisionIds() != null)
				throw new UnsupportedOperationException("Filtering by both histoCryptoRepoFileIds and collisionIds is currently not supported!");

			final List<HistoCryptoRepoFile> histoCryptoRepoFiles = new ArrayList<>(histoCryptoRepoFileIds.size());
			for (final Uid histoCryptoRepoFileId : histoCryptoRepoFileIds) {
				final HistoCryptoRepoFile histoCryptoRepoFile = hcrfDao.getHistoCryptoRepoFile(histoCryptoRepoFileId);
				if (histoCryptoRepoFile != null)
					histoCryptoRepoFiles.add(histoCryptoRepoFile);
			}
			return getPlainHistoCryptoRepoFileDtos(histoCryptoRepoFiles, filter);
		}

		final Set<Uid> collisionIds = filter.getCollisionIds();
		if (collisionIds != null) {
			final Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = hcrfDao.getHistoCryptoRepoFilesByCollisions(collisionIds);
			return getPlainHistoCryptoRepoFileDtos(histoCryptoRepoFiles, filter);
		}

		throw new IllegalArgumentException("No constraints! Cannot query *all* PlainHistoCryptoRepoFiles!");
	}

	private Collection<PlainHistoCryptoRepoFileDto> getPlainHistoCryptoRepoFileDtos(final Collection<HistoCryptoRepoFile> histoCryptoRepoFiles, PlainHistoCryptoRepoFileFilter filter) {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final HistoCryptoRepoFileDao hcrfDao = tx.getDao(HistoCryptoRepoFileDao.class);
		final Map<Uid, PlainHistoCryptoRepoFileDto> cryptoRepoFileId2PlainHistoCryptoRepoFileDto = new HashMap<>();

		final CryptreeNode filterLocalPathCryptreeNode = (StringUtil.isEmpty(filter.getLocalPath())
				? null : getCryptreeContext().getCryptreeNodeOrCreate(filter.getLocalPath()));

		final CryptoRepoFile filterLocalPathCryptoRepoFile = (filterLocalPathCryptreeNode == null
				? null
				: assertNotNull(filterLocalPathCryptreeNode.getCryptoRepoFile(), "filterLocalPathCryptreeNode.cryptoRepoFile"));

		final List<PlainHistoCryptoRepoFileDto> result = new ArrayList<>(histoCryptoRepoFiles.size());
		final PlainHistoCryptoRepoFileDao plainHistoCryptoRepoFileDao = tx.getDao(PlainHistoCryptoRepoFileDao.class);
		for (final HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFiles) {
			if (filterLocalPathCryptoRepoFile != null
					&& ! isParentOrEqual(filterLocalPathCryptoRepoFile, histoCryptoRepoFile.getCryptoRepoFile()))
				continue;

			final PlainHistoCryptoRepoFile plainHistoCryptoRepoFile = plainHistoCryptoRepoFileDao.getPlainHistoCryptoRepoFileOrFail(histoCryptoRepoFile);
			final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto = plainHistoCryptoRepoFile.getPlainHistoCryptoRepoFileDto();
			if (plainHistoCryptoRepoFileDto.getAction() == null)
				UpdatePlainHistoCryptoRepoFilesMarker.getInstance(tx).getHistoCryptoRepoFileIds().add(histoCryptoRepoFile.getHistoCryptoRepoFileId());

			if (! filter.isWithFileChunkDtos())
				removeFileChunkDtos(plainHistoCryptoRepoFileDto);

			cryptoRepoFileId2PlainHistoCryptoRepoFileDto.put(plainHistoCryptoRepoFileDto.getCryptoRepoFileId(), plainHistoCryptoRepoFileDto);
			result.add(plainHistoCryptoRepoFileDto);
		}

		if (filter.isFillParents()) {
			final List<PlainHistoCryptoRepoFileDto> parentDtos = new ArrayList<>();
			for (PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto : result) {
				if (plainHistoCryptoRepoFileDto.getRepoFileDto() != null) {
					final Uid cryptoRepoFileId = plainHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto().getCryptoRepoFileId();
					final CryptoRepoFile cryptoRepoFile = getCryptreeContext().getCryptoRepoFileOrFail(cryptoRepoFileId);
					for (CryptoRepoFile parentCryptoRepoFile : cryptoRepoFile.getPathList()) {
						final Uid parentCryptoRepoFileId = parentCryptoRepoFile.getCryptoRepoFileId();
						if (cryptoRepoFileId2PlainHistoCryptoRepoFileDto.containsKey(parentCryptoRepoFileId))
							continue;

						for (final HistoCryptoRepoFile parentHistoCryptoRepoFile : hcrfDao.getHistoCryptoRepoFiles(parentCryptoRepoFile)) { // TODO sort newest first?!
							final PlainHistoCryptoRepoFile parentPlainHistoCryptoRepoFile =
									plainHistoCryptoRepoFileDao.getPlainHistoCryptoRepoFileOrFail(parentHistoCryptoRepoFile);

							final PlainHistoCryptoRepoFileDto parentPlainHistoCryptoRepoFileDto = parentPlainHistoCryptoRepoFile.getPlainHistoCryptoRepoFileDto();
							if (parentPlainHistoCryptoRepoFileDto.getRepoFileDto() != null) {
								// parents are directories => no need to invoke removeFileChunkDtos(...)
								cryptoRepoFileId2PlainHistoCryptoRepoFileDto.put(parentCryptoRepoFileId, parentPlainHistoCryptoRepoFileDto);
								parentPlainHistoCryptoRepoFileDto.setHistoCryptoRepoFileDto(null); // 1. this is an arbitrary older entry and 2. we need to indicate that it is not a real modification
								parentPlainHistoCryptoRepoFileDto.setAction(null);
								parentDtos.add(parentPlainHistoCryptoRepoFileDto);
								break;
							}
						}
					}
				}
			}
			result.addAll(parentDtos);
		}
		return result;
	}

	@Override
	public void updatePlainHistoCryptoRepoFiles(final Set<Uid> histoCryptoRepoFileIds) {
		assertNotNull(histoCryptoRepoFileIds, "histoCryptoRepoFileIds");
		final LocalRepoTransaction tx = getTransactionOrFail();
		final HistoCryptoRepoFileDao hcrfDao = tx.getDao(HistoCryptoRepoFileDao.class);
		for (final Uid histoCryptoRepoFileId : histoCryptoRepoFileIds) {
			final HistoCryptoRepoFile histoCryptoRepoFile = hcrfDao.getHistoCryptoRepoFileOrFail(histoCryptoRepoFileId);
			getCryptreeContext().getCryptreeNodeOrCreate(histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId())
			.updatePlainHistoCryptoRepoFile(histoCryptoRepoFile);
		}
	}

	private void removeFileChunkDtos(final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto) {
		RepoFileDto repoFileDto = plainHistoCryptoRepoFileDto.getRepoFileDto();
		if (repoFileDto instanceof SsNormalFileDto) {
			SsNormalFileDto normalFileDto = (SsNormalFileDto) repoFileDto;
			normalFileDto.setFileChunkDtos(null);
		}
	}

	private boolean isParentOrEqual(final CryptoRepoFile parentCandidate, final CryptoRepoFile childCandidate) {
		assertNotNull(parentCandidate, "parentCandidate");
		assertNotNull(childCandidate, "childCandidate");

		CryptoRepoFile crf = childCandidate;
		while (crf != null) {
			if (parentCandidate.equals(crf))
				return true;

			crf = crf.getParent();
		}
		return false;
	}

	@Override
	public PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto(final Uid histoCryptoRepoFileId) {
		assertNotNull(histoCryptoRepoFileId, "histoCryptoRepoFileId");
		final LocalRepoTransaction tx = getTransactionOrFail();
		final HistoCryptoRepoFileDao hcrfDao = tx.getDao(HistoCryptoRepoFileDao.class);
		final HistoCryptoRepoFile histoCryptoRepoFile = hcrfDao.getHistoCryptoRepoFileOrFail(histoCryptoRepoFileId);
		final PlainHistoCryptoRepoFileDao phcrfDao = tx.getDao(PlainHistoCryptoRepoFileDao.class);
		final PlainHistoCryptoRepoFile phcrf = phcrfDao.getPlainHistoCryptoRepoFileOrFail(histoCryptoRepoFile);
		return phcrf.getPlainHistoCryptoRepoFileDto();
	}

	@Override
	public void clearCryptoRepoFileDeleted(String localPath) {
		assertNotNull(localPath, "localPath");
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(localPath);
		cryptreeNode.clearCryptoRepoFileDeleted();
	}

	@Override
	public void putCollisionPrivateDto(final CollisionPrivateDto collisionPrivateDto) {
		assertNotNull(collisionPrivateDto, "collisionPrivateDto");
		final Uid collisionId = assertNotNull(collisionPrivateDto.getCollisionId(), "collisionPrivateDto.collisionId");

		final CollisionDao cDao = getTransactionOrFail().getDao(CollisionDao.class);
		final Collision collision = cDao.getCollisionOrFail(collisionId);

		final Uid cryptoRepoFileId = collision.getHistoCryptoRepoFile1().getCryptoRepoFile().getCryptoRepoFileId();
		final CryptreeNode cryptreeNode = getCryptreeContext().getCryptreeNodeOrCreate(cryptoRepoFileId);

		final CollisionPrivateDto oldCollisionPrivateDto = cryptreeNode.getCollisionPrivateDto(collision);
		if (! isDeeplyEqual(collisionPrivateDto, oldCollisionPrivateDto))
			cryptreeNode.putCollisionPrivateDto(collision, collisionPrivateDto);
	}

	@Override
	public Long getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced() {
		final LocalRepoTransaction tx = getTransactionOrFail();
		final RemoteRepository remoteRepository = tx.getDao(RemoteRepositoryDao.class)
				.getRemoteRepositoryOrFail(getRemoteRepositoryIdOrFail());

		final LastCryptoKeySyncFromRemoteRepo lcksfrr = tx.getDao(LastCryptoKeySyncFromRemoteRepoDao.class)
				.getLastCryptoKeySyncFromRemoteRepo(remoteRepository);
		if (lcksfrr == null)
			return null;

		final long result = lcksfrr.getRemoteRepositoryRevisionSynced();
		return result < 0 ? null : result;
	}

	protected void setLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced(long revision) {
		if (revision < 0)
			throw new IllegalArgumentException("revision < 0");

		final LocalRepoTransaction tx = getTransactionOrFail();
		final RemoteRepository remoteRepository = tx.getDao(RemoteRepositoryDao.class)
				.getRemoteRepositoryOrFail(getRemoteRepositoryIdOrFail());

		final LastCryptoKeySyncFromRemoteRepoDao lcksfrrDao = tx.getDao(LastCryptoKeySyncFromRemoteRepoDao.class);
		LastCryptoKeySyncFromRemoteRepo lcksfrr = lcksfrrDao.getLastCryptoKeySyncFromRemoteRepo(remoteRepository);
		if (lcksfrr == null) {
			lcksfrr = new LastCryptoKeySyncFromRemoteRepo();
			lcksfrr.setRemoteRepository(remoteRepository);
		}
		lcksfrr.setRemoteRepositoryRevisionSynced(revision);
		lcksfrrDao.makePersistent(lcksfrr);
	}

	private boolean isDeeplyEqual(CollisionPrivateDto dto1, CollisionPrivateDto dto2) {
		return (
				equal(dto1.getCollisionId(), dto2.getCollisionId())
				&& equal(dto1.getComment(), dto2.getComment())
				&& equal(dto1.getResolved(), dto2.getResolved())
				);
	}
}