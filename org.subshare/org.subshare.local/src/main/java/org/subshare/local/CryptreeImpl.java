package org.subshare.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.AbstractCryptree;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.UserRepoKeyPublicKeyDto;
import org.subshare.core.user.UserRepoKey;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepo;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepoDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;

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
	private final Map<String, CryptreeNode> localPath2CryptreeNode = new HashMap<>();
	private final Map<Uid, CryptreeNode> cryptoRepoFileId2CryptreeNode = new HashMap<>();

	private Uid cryptoRepoFileIdForRemotePathPrefix;

	@Override
	public CryptoChangeSetDto createOrUpdateCryptoRepoFile(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeNodeOrFail(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFileOrCreate(true);

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoOrFail(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeNodeOrFail(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

	@Override
	public String getServerPath(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeNodeOrFail(localPath);
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

		final CryptreeNode cryptreeNode = new CryptreeNode(getCryptreeContextOrFail(), repoFile);
		return cryptreeNode.getDataKeyOrFail();
	}

	protected CryptreeContext getCryptreeContextOrFail() {
		return new CryptreeContext(getUserRepoKeyOrFail(), getTransactionOrFail());
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoWithCryptoRepoFiles() {
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

		transaction.flush();
	}

	@Override
	public Uid getRootCryptoRepoFileId() {
		final CryptoRepoFileDao cryptoRepoFileDao = getTransactionOrFail().getDao(CryptoRepoFileDao.class);
		final CryptoRepoFile rootCryptoRepoFile = cryptoRepoFileDao.getRootCryptoRepoFile();
		return rootCryptoRepoFile == null ? null : rootCryptoRepoFile.getCryptoRepoFileId();
	}

	@Override
	public RepoFileDto getDecryptedRepoFileDtoOrFail(final Uid cryptoRepoFileId) throws AccessDeniedException {
		assertNotNull("cryptoRepoFileId", cryptoRepoFileId);
		final CryptreeNode cryptreeNode = getCryptreeNodeOrFail(cryptoRepoFileId);
		final RepoFileDto repoFileDto = cryptreeNode.getRepoFileDto();
		assertNotNull("cryptreeNode.getRepoFileDto()", repoFileDto); // The cryptoRepoFile is present, thus this should never be null!
		return repoFileDto;
	}

	@Override
	public RepoFileDto getDecryptedRepoFileDto(final String localPath) throws AccessDeniedException {
		assertNotNull("localPath", localPath);
		final CryptreeNode cryptreeNode = getCryptreeNodeOrFail(localPath);
		return cryptreeNode.getRepoFileDto();
	}

	@Override
	public void grantReadAccess(final String localPath, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		assertNotNull("localPath", localPath);
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);
		final CryptreeNode cryptreeNode = getCryptreeNodeOrFail(localPath);
		cryptreeNode.grantReadAccess(userRepoKeyPublicKey);
	}

	@Override
	public void revokeReadAccess(final String localPath, final Set<Uid> userRepoKeyIds) {
		assertNotNull("localPath", localPath);
		assertNotNull("userRepoKeyIds", userRepoKeyIds);
		final CryptreeNode cryptreeNode = getCryptreeNodeOrFail(localPath);
		cryptreeNode.revokeReadAccess(userRepoKeyIds);
	}

	/**
	 * Is the code currently executed on the server?
	 * @return <code>true</code>, if this method is invoked on the server. <code>false</code>, if this method
	 * is invoked on the client.
	 */
	protected boolean isOnServer() {
		return getUserRepoKey() == null; // We don't have user keys on the server.
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
			final Uid id = getCryptoRepoFileIdForRemotePathPrefixOrFail();
			if (id.equals(cryptoRepoFile.getCryptoRepoFileId())) {
				final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
				cryptoRepoFile.setRepoFile(localRepository.getRoot());
			}
		}

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFileDto.repoFileDtoData", cryptoRepoFileDto.getRepoFileDtoData());
		cryptoRepoFile.setRepoFileDtoData(repoFileDtoData);

		cryptoRepoFile.setDirectory(cryptoRepoFileDto.isDirectory());
		cryptoRepoFile.setLastSyncFromRepositoryId(getRemoteRepositoryId());

		cryptoRepoFile.setSignatureData(cryptoRepoFileDto.getSignatureData());
		cryptoRepoFile.setSigningUserRepoKeyId(cryptoRepoFileDto.getSigningUserRepoKeyId());

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

		userRepoKeyPublicKey.setRepositoryId(userRepoKeyPublicKeyDto.getRepositoryId());
		userRepoKeyPublicKey.setPublicKeyData(userRepoKeyPublicKeyDto.getPublicKeyData());

		return userRepoKeyPublicKeyDao.makePersistent(userRepoKeyPublicKey);
	}

	private CryptoKey putCryptoKeyDto(final CryptoKeyDto cryptoKeyDto) {
		assertNotNull("cryptoKeyDto", cryptoKeyDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final Uid cryptoKeyId = assertNotNull("cryptoKeyDto.cryptoKeyId", cryptoKeyDto.getCryptoKeyId());
		CryptoKey cryptoKey = cryptoKeyDao.getCryptoKey(cryptoKeyId);
		if (cryptoKey == null)
			cryptoKey = new CryptoKey(cryptoKeyId);

		if (! cryptoKeyDto.isActive()) // it's a one-way change - we never re-activate a key.
			cryptoKey.setActive(false);

		cryptoKey.setCryptoKeyRole(cryptoKeyDto.getCryptoKeyRole());
		cryptoKey.setCryptoKeyType(cryptoKeyDto.getCryptoKeyType());

		final Uid cryptoRepoFileId = assertNotNull("cryptoKeyDto.cryptoRepoFileId", cryptoKeyDto.getCryptoRepoFileId());
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
		cryptoKey.setCryptoRepoFile(cryptoRepoFile);

		cryptoKey.setSignatureData(cryptoKeyDto.getSignatureData());
		cryptoKey.setSigningUserRepoKeyId(cryptoKeyDto.getSigningUserRepoKeyId());

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

		toCryptoKey.getInCryptoLinks().add(cryptoLink);

		cryptoLink.setSignatureData(cryptoLinkDto.getSignatureData());
		cryptoLink.setSigningUserRepoKeyId(cryptoLinkDto.getSigningUserRepoKeyId());

		return cryptoLinkDao.makePersistent(cryptoLink);
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

	private CryptreeNode getCryptreeNodeOrFail(final String localPath) {
		CryptreeNode cryptreeNode = localPath2CryptreeNode.get(localPath);
		if (cryptreeNode == null) {
			cryptreeNode = createCryptreeNodeOrFail(localPath);
			localPath2CryptreeNode.put(localPath, cryptreeNode);
		}
		return cryptreeNode;
	}

	private CryptreeNode getCryptreeNodeOrFail(final Uid cryptoRepoFileId) {
		CryptreeNode cryptreeNode = cryptoRepoFileId2CryptreeNode.get(cryptoRepoFileId);
		if (cryptreeNode == null) {
			cryptreeNode = createCryptreeNodeOrFail(cryptoRepoFileId);
			cryptoRepoFileId2CryptreeNode.put(cryptoRepoFileId, cryptreeNode);
		}
		return cryptreeNode;
	}

	private CryptreeNode createCryptreeNodeOrFail(final String localPath) {
		final RepoFile repoFile = getRepoFile(localPath);
		if (repoFile != null) {
			final CryptreeNode cryptreeNode = new CryptreeNode(getCryptreeContextOrFail(), repoFile);
			return cryptreeNode;
		}
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFileOrFail(localPath);
		final CryptreeNode cryptreeNode = new CryptreeNode(getCryptreeContextOrFail(), cryptoRepoFile);
		return cryptreeNode;
	}

	private CryptreeNode createCryptreeNodeOrFail(final Uid cryptoRepoFileId) {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFileOrFail(cryptoRepoFileId);
		final CryptreeNode cryptreeNode = new CryptreeNode(getCryptreeContextOrFail(), cryptoRepoFile);
		return cryptreeNode;
	}

	private CryptoRepoFile getCryptoRepoFileOrFail(final Uid cryptoRepoFileId) {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
		return cryptoRepoFile;
	}

	private CryptoRepoFile getCryptoRepoFileOrFail(final String localPath) {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(prefixLocalPath(localPath));
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		return cryptoRepoFile;
	}

	private String prefixLocalPath(final String localPath) {
		assertNotNull("localPath", localPath);

		if (getRemotePathPrefixOrFail().isEmpty())
			return localPath;

		final CryptoRepoFile prefixCryptoRepoFile = getCryptoRepoFileForRemotePathPrefixOrFail();

		if (localPath.isEmpty())
			return prefixCryptoRepoFile.getLocalPathOrFail();
		else {
			if ("/".equals(localPath))
				throw new IllegalStateException("localPath should never be '/', but instead it should be an empty String, if the real root is checked out!");

			if (!localPath.startsWith("/"))
				throw new IllegalStateException(String.format("localPath '%s' is neither empty nor does it start with '/'!", localPath));

			final String prefix = prefixCryptoRepoFile.getLocalPathOrFail();

			if (!prefix.isEmpty() && !prefix.startsWith("/"))
				throw new IllegalStateException(String.format("prefixCryptoRepoFile.localPath '%s' is neither empty nor does it start with '/'!", prefix));

			if (prefix.endsWith("/"))
				throw new IllegalStateException(String.format("prefixCryptoRepoFile.localPath '%s' ends with '/'! It should not!", prefix));

			return prefix + localPath;
		}
	}

	private CryptoRepoFile getCryptoRepoFileForRemotePathPrefixOrFail() {
		final Uid id = getCryptoRepoFileIdForRemotePathPrefixOrFail();
		final CryptoRepoFile prefixCryptoRepoFile = getCryptoRepoFileOrFail(id);
		return prefixCryptoRepoFile;
	}

	/**
	 * Gets the {@link CryptoRepoFile#getCryptoRepoFileId() cryptoRepoFileId} of the {@code CryptoRepoFile}
	 * which corresponds to the root-directory that's checked out.
	 * <p>
	 * This method can only be used, if the local repository is connected to a sub-directory of the server
	 * repository. If it is connected to the server repository's root, there is no
	 * {@link #getRemotePathPrefix() remotePathPrefix} (it is an empty string) and the ID can therefore not
	 * be read from it.
	 * @return the {@link CryptoRepoFile#getCryptoRepoFileId() cryptoRepoFileId} of the {@code CryptoRepoFile}
	 * which is the connection point of the local repository to the server's repository.
	 */
	private Uid getCryptoRepoFileIdForRemotePathPrefixOrFail() {
		if (cryptoRepoFileIdForRemotePathPrefix == null) {
			if (isOnServer())
				throw new IllegalStateException("This method cannot be used on the server!");

			final String remotePathPrefix = getRemotePathPrefixOrFail();
			if (remotePathPrefix.isEmpty())
				throw new IllegalStateException("This method cannot be used, if there is no remotePathPrefix!");

			if ("/".equals(remotePathPrefix))
				throw new IllegalStateException("The remotePathPrefix should be an empty string, if the root is checked out!");

			final int lastSlashIndex = remotePathPrefix.lastIndexOf('/');
			if (lastSlashIndex < 0)
				throw new IllegalStateException("encryptedPathPrefix is neither empty nor does it contain '/'! encryptedPathPrefix: " + remotePathPrefix);

			final String uidStr = remotePathPrefix.substring(lastSlashIndex + 1);
			cryptoRepoFileIdForRemotePathPrefix = new Uid(uidStr);
		}
		return cryptoRepoFileIdForRemotePathPrefix;
	}

	private RepoFile getRepoFile(final String localPath) {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final RepoFile repoFile = repoFileDao.getRepoFile(localRepoManager.getLocalRoot(), createFile(localRepoManager.getLocalRoot(), localPath));
		return repoFile;
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

	private UserRepoKeyPublicKeyDto toUserRepoKeyPublicKeyDto(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		assertNotNull("userRepoKeyPublicKey", userRepoKeyPublicKey);
		final UserRepoKeyPublicKeyDto userRepoKeyPublicKeyDto = new UserRepoKeyPublicKeyDto();
		userRepoKeyPublicKeyDto.setLocalRevision(userRepoKeyPublicKey.getLocalRevision());
		userRepoKeyPublicKeyDto.setPublicKeyData(userRepoKeyPublicKey.getPublicKeyData());
		userRepoKeyPublicKeyDto.setRepositoryId(userRepoKeyPublicKey.getRepositoryId());
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

		cryptoRepoFileDto.setSignatureData(cryptoRepoFile.getSignatureData());
		cryptoRepoFileDto.setSigningUserRepoKeyId(cryptoRepoFile.getSigningUserRepoKeyId());

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
		cryptoLinkDto.setSignatureData(cryptoLink.getSignatureData());
		cryptoLinkDto.setSigningUserRepoKeyId(cryptoLink.getSigningUserRepoKeyId());
		return cryptoLinkDto;
	}

	private CryptoKeyDto toCryptoKeyDto(final CryptoKey cryptoKey) {
		assertNotNull("cryptoKey", cryptoKey);
		final CryptoKeyDto cryptoKeyDto = new CryptoKeyDto();
		cryptoKeyDto.setCryptoKeyId(cryptoKey.getCryptoKeyId());
		cryptoKeyDto.setCryptoRepoFileId(cryptoKey.getCryptoRepoFile().getCryptoRepoFileId());
		cryptoKeyDto.setActive(cryptoKey.isActive());
		cryptoKeyDto.setCryptoKeyRole(cryptoKey.getCryptoKeyRole());
		cryptoKeyDto.setCryptoKeyType(cryptoKey.getCryptoKeyType());
		cryptoKeyDto.setSignatureData(cryptoKey.getSignatureData());
		cryptoKeyDto.setSigningUserRepoKeyId(cryptoKey.getSigningUserRepoKeyId());
		return cryptoKeyDto;
	}

	@Override
	public boolean isEmpty() {
		final Collection<CryptoRepoFile> childCryptoRepoFiles = getTransactionOrFail().getDao(CryptoRepoFileDao.class).getChildCryptoRepoFiles(null);
		return childCryptoRepoFiles.isEmpty();
	}
}
