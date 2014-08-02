package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.AbstractCryptree;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepo;
import org.subshare.local.persistence.LastCryptoKeySyncToRemoteRepoDao;

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

	@Override
	public CryptoChangeSetDto createOrUpdateCryptoRepoFile(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeNode(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFileOrCreate(true);

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoOrFail(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeNode(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(cryptoRepoFile);
		return cryptoChangeSetDto;
	}

	@Override
	public String getServerPath(final String localPath) {
		final CryptreeNode cryptreeNode = getCryptreeNode(localPath);
		final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFile();
		return cryptoRepoFile.getPath();
	}

	@Override
	public KeyParameter getDataKey(final String path) {
		assertNotNull("path", path);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final RepoFile repoFile = repoFileDao.getRepoFile(localRepoManager.getLocalRoot(), new File(localRepoManager.getLocalRoot(), path));
		assertNotNull("repoFile", repoFile);

		final CryptreeNode cryptreeNode = new CryptreeNode(getUserRepoKeyOrFail(), transaction, repoFile);
		return cryptreeNode.getDataKey();
	}

	@Override
	public CryptoChangeSetDto getCryptoChangeSetDtoWithCryptoRepoFiles() {
		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();

		// First links then keys, because we query all changed *after* a certain localRevision - and not in a range.
		// Thus, we might find newer keys when querying them after the links. Since the links reference the keys
		// (collection is mapped-by) and we currently don't delete anything, this guarantees that all references can
		// be fulfilled on the remote side. Extremely unlikely, but still better ;-)
		final CryptoChangeSetDto cryptoChangeSetDto = new CryptoChangeSetDto();
		populateChangedCryptoRepoFileDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoLinkDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		return cryptoChangeSetDto;
	}

	@Override
	public void updateLastCryptoKeySyncToRemoteRepo() {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();
		lastCryptoKeySyncToRemoteRepo.setLocalRepositoryRevisionSynced(localRepository.getRevision());
	}

	@Override
	public void putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		assertNotNull("cryptoChangeSetDto", cryptoChangeSetDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();

		final Map<CryptoRepoFileDto, CryptoRepoFile> cryptoRepoFileDto2CryptoRepoFile = new HashMap<>();
		final Map<Uid, CryptoKey> cryptoKeyId2CryptoKey = new HashMap<>();

		// This order is important, because the keys must first be persisted, before links or file-meta-data can reference them.
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

		transaction.flush();
	}

//	/**
//	 * Is the code currently executed on the server?
//	 * @return <code>true</code>, if this method is invoked on the server. <code>false</code>, if this method
//	 * is invoked on the client.
//	 */
//	protected boolean isOnServer() {
//		return getUserRepoKey() == null; // We don't have user keys on the server.
//	}

	private CryptoRepoFile putCryptoRepoFileDto(final CryptoRepoFileDto cryptoRepoFileDto) {
		assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
//		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);

		final Uid cryptoRepoFileId = assertNotNull("cryptoRepoFileDto.cryptoRepoFileId", cryptoRepoFileDto.getCryptoRepoFileId());
		CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(cryptoRepoFileId);
		if (cryptoRepoFile == null)
			cryptoRepoFile = new CryptoRepoFile(cryptoRepoFileId);

		final Uid cryptoKeyId = assertNotNull("cryptoRepoFileDto.cryptoKeyId", cryptoRepoFileDto.getCryptoKeyId());
		cryptoRepoFile.setCryptoKey(cryptoKeyDao.getCryptoKey(cryptoKeyId)); // may be null, now, if it is persisted later!

		final Uid parentCryptoRepoFileId = cryptoRepoFileDto.getParentCryptoRepoFileId();
		cryptoRepoFile.setParent(parentCryptoRepoFileId == null ? null
				: cryptoRepoFileDao.getCryptoRepoFileOrFail(parentCryptoRepoFileId));

//		if (isOnServer()) {
//			final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile.getParent();
//			final RepoFile parentRepoFile = parentCryptoRepoFile == null ? null : parentCryptoRepoFile.getRepoFile();
//			if (parentCryptoRepoFile != null)
//				assertNotNull("parentCryptoRepoFile.parentRepoFile", parentRepoFile);
//
//			RepoFile repoFile = repoFileDao.getChildRepoFile(parentRepoFile, cryptoRepoFileId.toString());
//			if (repoFile == null)
//				repoFile = new
//
//			cryptoRepoFile.setRepoFile(repoFile); // TODO?!???
//		}

		if (parentCryptoRepoFileId == null) {
			// This is the root! Since the root's directory name is the empty string, it cannot be associated
			// by the AssignCryptoRepoFileRepoFileListener, which uses the RepoFile.name to associate them.
			// On the server, the RepoFile.name equals the CryptoRepoFile.cryptoRepoFileId - except for the root!
			final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
			cryptoRepoFile.setRepoFile(localRepository.getRoot());
		}

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFileDto.repoFileDtoData", cryptoRepoFileDto.getRepoFileDtoData());
		cryptoRepoFile.setRepoFileDtoData(repoFileDtoData);

		cryptoRepoFile.setLastSyncFromRepositoryId(getRemoteRepositoryId());

		return cryptoRepoFileDao.makePersistent(cryptoRepoFile);
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

		if (!cryptoKeyDto.isActive()) // it's a one-way change - we never re-activate a key.
			cryptoKey.setActive(false);

		cryptoKey.setCryptoKeyRole(cryptoKeyDto.getCryptoKeyRole());
		cryptoKey.setCryptoKeyType(cryptoKeyDto.getCryptoKeyType());

		final Uid cryptoRepoFileId = assertNotNull("cryptoKeyDto.cryptoRepoFileId", cryptoKeyDto.getCryptoRepoFileId());
		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileId);
		cryptoKey.setCryptoRepoFile(cryptoRepoFile);

		return cryptoKeyDao.makePersistent(cryptoKey);
	}

	private CryptoLink putCryptoLinkDto(final CryptoLinkDto cryptoLinkDto) {
		assertNotNull("cryptoLinkDto", cryptoLinkDto);
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final CryptoLinkDao cryptoLinkDao = transaction.getDao(CryptoLinkDao.class);
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);

		final Uid cryptoLinkId = assertNotNull("cryptoLinkDto.cryptoLinkId", cryptoLinkDto.getCryptoLinkId());
		CryptoLink cryptoLink = cryptoLinkDao.getCryptoLink(cryptoLinkId);
		if (cryptoLink == null)
			cryptoLink = new CryptoLink(cryptoLinkId);

		final Uid fromCryptoKeyId = cryptoLinkDto.getFromCryptoKeyId();
		cryptoLink.setFromCryptoKey(fromCryptoKeyId == null ? null : cryptoKeyDao.getCryptoKeyOrFail(fromCryptoKeyId));

		final Uid toCryptoKeyId = assertNotNull("cryptoLinkDto.toCryptoKeyId", cryptoLinkDto.getToCryptoKeyId());
		cryptoLink.setToCryptoKey(cryptoKeyDao.getCryptoKeyOrFail(toCryptoKeyId));

		cryptoLink.setFromUserRepoKeyId(cryptoLinkDto.getFromUserRepoKeyId());
		cryptoLink.setToCryptoKeyData(cryptoLinkDto.getToCryptoKeyData());
		cryptoLink.setToCryptoKeyPart(cryptoLinkDto.getToCryptoKeyPart());

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

	private CryptreeNode getCryptreeNode(final String localPath) {
		CryptreeNode cryptreeNode = localPath2CryptreeNode.get(localPath);
		if (cryptreeNode == null) {
			cryptreeNode = createCryptreeNode(localPath);
			localPath2CryptreeNode.put(localPath, cryptreeNode);
		}
		return cryptreeNode;
	}

	private CryptreeNode createCryptreeNode(final String localPath) {
		final RepoFile repoFile = getRepoFileOrFail(localPath);
		final CryptreeNode cryptreeNode = new CryptreeNode(getUserRepoKeyOrFail(), getTransactionOrFail(), repoFile);
		return cryptreeNode;
	}

	private RepoFile getRepoFileOrFail(final String path) {
		final LocalRepoTransaction transaction = getTransactionOrFail();
		final LocalRepoManager localRepoManager = transaction.getLocalRepoManager();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final RepoFile repoFile = repoFileDao.getRepoFile(localRepoManager.getLocalRoot(), new File(localRepoManager.getLocalRoot(), path));
		assertNotNull("repoFile", repoFile);
		return repoFile;
	}

	protected CryptoChangeSetDto getCryptoChangeSetDto(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		final LastCryptoKeySyncToRemoteRepo lastCryptoKeySyncToRemoteRepo = getLastCryptoKeySyncToRemoteRepo();

		final CryptoChangeSetDto cryptoChangeSetDto = new CryptoChangeSetDto();
		cryptoChangeSetDto.getCryptoRepoFileDtos().add(toCryptoRepoFileDto(cryptoRepoFile));
		populateChangedCryptoLinkDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);
		populateChangedCryptoKeyDtos(cryptoChangeSetDto, lastCryptoKeySyncToRemoteRepo);

		return cryptoChangeSetDto;
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

	private CryptoRepoFileDto toCryptoRepoFileDto(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		final CryptoRepoFileDto cryptoRepoFileDto = new CryptoRepoFileDto();

		cryptoRepoFileDto.setCryptoRepoFileId(cryptoRepoFile.getCryptoRepoFileId());

		final CryptoRepoFile parent = cryptoRepoFile.getParent();
		cryptoRepoFileDto.setParentCryptoRepoFileId(parent == null ? null : parent.getCryptoRepoFileId());

		final CryptoKey cryptoKey = assertNotNull("cryptoRepoFile.cryptoKey", cryptoRepoFile.getCryptoKey());
		cryptoRepoFileDto.setCryptoKeyId(cryptoKey.getCryptoKeyId());

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFile.repoFileDtoData", cryptoRepoFile.getRepoFileDtoData());
		cryptoRepoFileDto.setRepoFileDtoData(repoFileDtoData);

//		final RepoFile repoFile = cryptoRepoFile.getRepoFile();
//		if (repoFile != null)
//			cryptoRepoFileDto.setRepoFileId(repoFile.getId());

		return cryptoRepoFileDto;
	}

	private CryptoLinkDto toCryptoLinkDto(final CryptoLink cryptoLink) {
		assertNotNull("cryptoLink", cryptoLink);
		final CryptoLinkDto cryptoLinkDto = new CryptoLinkDto();
		cryptoLinkDto.setCryptoLinkId(cryptoLink.getCryptoLinkId());

		final CryptoKey fromCryptoKey = cryptoLink.getFromCryptoKey();
		cryptoLinkDto.setFromCryptoKeyId(fromCryptoKey == null ? null : fromCryptoKey.getCryptoKeyId());

		cryptoLinkDto.setFromUserRepoKeyId(cryptoLink.getFromUserRepoKeyId());
		cryptoLinkDto.setLocalRevision(cryptoLink.getLocalRevision());
		cryptoLinkDto.setToCryptoKeyData(cryptoLink.getToCryptoKeyData());
		cryptoLinkDto.setToCryptoKeyId(cryptoLink.getToCryptoKey().getCryptoKeyId());
		cryptoLinkDto.setToCryptoKeyPart(cryptoLink.getToCryptoKeyPart());

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
		return cryptoKeyDto;
	}

//	@Override
//	public String getServerPath(final String localPath) {
//		// TODO handle path correctly => pathPrefix on both sides possible!!! Maybe do this here?!
//		assertNotNull("path", localPath);
//		final StringBuilder result = new StringBuilder();
//		final StringTokenizer st = new StringTokenizer(localPath, "/", true);
//		while (st.hasMoreTokens()) {
//			final String token = st.nextToken();
//			if ("/".equals(token))
//				result.append(token);
//			else
//				result.append(getServerFileName(token));
//		}
//		return result.toString();
//	}
//
//	/**
//	 * Gets the server-side name of the file named {@code plainName} locally.
//	 * <p>
//	 * The real (plain-text) name of the file is hashed with a repository-dependent salt. Thus the same name
//	 * will have the same hash in the same repository, but a different hash in other repositories.
//	 * <p>
//	 * In other words: Invoking this method multiple times with the same name and the same (remote)
//	 * repository is guaranteed to return the same result. Invoking it with the same name but another
//	 * (remote) repository, it is guaranteed to return a different result.
//	 * <p>
//	 * @param plainName the local name (in plain-text) of the file. Must not be <code>null</code>.
//	 * @return the server-side name of the file (a secure hash).
//	 * @see #getServerPath(String)
//	 */
//	private String getServerFileName(final String plainName) {
//		assertNotNull("plainName", plainName);
//		// TODO re-implement (I'm sure I already implemented this for another project before) a CombiInputStream combining multiple streams, so that we don't need to copy things around!
//		final byte[] repositoryIdBytes = new Uid(getRemoteRepositoryId().getMostSignificantBits(), getRemoteRepositoryId().getLeastSignificantBits()).toBytes();
//		final byte[] plainNameBytes = plainName.getBytes(UTF8);
//		final byte[] combined = new byte[repositoryIdBytes.length + plainNameBytes.length];
//		System.arraycopy(repositoryIdBytes, 0, combined, 0, repositoryIdBytes.length);
//		System.arraycopy(plainNameBytes, 0, combined, repositoryIdBytes.length, plainNameBytes.length);
//		final String sha1 = HashUtil.sha1(combined);
//		return sha1;
//	}
}
