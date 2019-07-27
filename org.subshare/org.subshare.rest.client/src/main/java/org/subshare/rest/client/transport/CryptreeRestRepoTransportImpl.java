package org.subshare.rest.client.transport;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.DebugUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static java.util.Objects.*;
import static org.subshare.core.crypto.CryptoConfigUtil.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.DataKey;
import org.subshare.core.FileDeletedException;
import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.crypto.DecrypterInputStream;
import org.subshare.core.crypto.EncrypterOutputStream;
import org.subshare.core.crypto.RandomIvFactory;
import org.subshare.core.dto.CreateRepositoryRequestDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.RepoFileDtoWithCurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.dto.SsRequestRepoConnectionRepositoryDto;
import org.subshare.core.dto.SsSymlinkDto;
import org.subshare.core.dto.split.CryptoChangeSetDtoSplitFileManager;
import org.subshare.core.dto.split.CryptoChangeSetDtoTooLargeException;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.repo.transport.CryptreeRestRepoTransport;
import org.subshare.core.sign.PgpSignableSigner;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.SignableSigner;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.SignerOutputStream;
import org.subshare.core.sign.VerifierInputStream;
import org.subshare.core.sign.WriteProtected;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;
import org.subshare.rest.client.transport.request.CreateRepository;
import org.subshare.rest.client.transport.request.EndGetCryptoChangeSetDto;
import org.subshare.rest.client.transport.request.GetCryptoChangeSetDto;
import org.subshare.rest.client.transport.request.GetCryptoChangeSetDtoFileData;
import org.subshare.rest.client.transport.request.GetHistoFileData;
import org.subshare.rest.client.transport.request.GetLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced;
import org.subshare.rest.client.transport.request.PutCryptoChangeSetDto;
import org.subshare.rest.client.transport.request.SsBeginPutFile;
import org.subshare.rest.client.transport.request.SsDelete;
import org.subshare.rest.client.transport.request.SsEndPutFile;
import org.subshare.rest.client.transport.request.SsMakeDirectory;
import org.subshare.rest.client.transport.request.SsMakeSymlink;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.VersionInfoDto;
import co.codewizards.cloudstore.core.dto.jaxb.ChangeSetDtoIo;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseAdapter;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseEvent;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.CollisionException;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.request.RequestRepoConnection;

public class CryptreeRestRepoTransportImpl extends AbstractRepoTransport implements CryptreeRestRepoTransport, ContextWithLocalRepoManager {
	private static final Logger logger = LoggerFactory.getLogger(CryptreeRestRepoTransportImpl.class);

	public static final String CONFIG_KEY_GET_CRYPTO_CHANGE_SET_DTO_TIMEOUT = "getCryptoChangeSetDtoTimeout";
	public static final long CONFIG_DEFAULT_VALUE_GET_CRYPTO_CHANGE_SET_DTO_TIMEOUT = 2 * 60L * 60L * 1000L; // 2 hours

	private final long cryptoChangeSetTimeout = ConfigImpl.getInstance().getPropertyAsPositiveOrZeroLong(
			CONFIG_KEY_GET_CRYPTO_CHANGE_SET_DTO_TIMEOUT, CONFIG_DEFAULT_VALUE_GET_CRYPTO_CHANGE_SET_DTO_TIMEOUT);

	public static final String DECRYPTED_CHANGE_SET_DTO_CACHE_FILE_NAME_PREFIX = "DecryptedChangeSetDto.";
	public static final String DECRYPTED_CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX = RestRepoTransport.CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX;
	public static final String TMP_FILE_NAME_SUFFIX = RestRepoTransport.TMP_FILE_NAME_SUFFIX;

	private CryptreeFactory cryptreeFactory;
	private RestRepoTransport restRepoTransport;
	private LocalRepoManager localRepoManager;
	private UserRepoKeyRing userRepoKeyRing;

	@Override
	public RepositoryDto getRepositoryDto() {
		return getRestRepoTransport().getRepositoryDto();
	}

	@Override
	public RepositoryDto getClientRepositoryDto() {
		return getRestRepoTransport().getClientRepositoryDto();
	}

	@Override
	public UUID getRepositoryId() {
		return getRestRepoTransport().getRepositoryId();
	}

	@Override
	public byte[] getPublicKey() {
		return getRestRepoTransport().getPublicKey();
	}

	@Override
	public void createRepository(final UUID serverRepositoryId, PgpKey pgpKey) {
		requireNonNull(pgpKey, "pgpKey");
		CreateRepositoryRequestDto createRepositoryRequestDto = new CreateRepositoryRequestDto();
		createRepositoryRequestDto.setServerRepositoryId(serverRepositoryId);
		new PgpSignableSigner(pgpKey).sign(createRepositoryRequestDto);
		getClient().execute(new CreateRepository(createRepositoryRequestDto));
	}

	@Override
	public void requestRepoConnection(final byte[] publicKey) {
		final UUID serverRepositoryId = getRepositoryId();
		final String repositoryName = getRestRepoTransport().getRepositoryName();
		final SsRequestRepoConnectionRepositoryDto repositoryDto = new SsRequestRepoConnectionRepositoryDto();
		repositoryDto.setRepositoryId(getClientRepositoryIdOrFail());
		repositoryDto.setPublicKey(publicKey);

		// First check the invitation keys, because if there is a new invitation, the corresponding new permanent
		// key is not yet known to the server. The invitation key is, though.
		final List<UserRepoKey> invitationUserRepoKeys = getUserRepoKeyRing().getInvitationUserRepoKeys(serverRepositoryId);

		UserRepoKey signingUserRepoKey = invitationUserRepoKeys.isEmpty() ? null : invitationUserRepoKeys.get(0);

		if (signingUserRepoKey == null) {
			final List<UserRepoKey> permanentUserRepoKeys = getUserRepoKeyRing().getPermanentUserRepoKeys(serverRepositoryId);
			if (permanentUserRepoKeys.isEmpty())
				throw new IllegalStateException("There is no UserRepoKey for serverRepositoryId=" + serverRepositoryId);

			signingUserRepoKey = permanentUserRepoKeys.get(0);
		}

		new SignableSigner(signingUserRepoKey).sign(repositoryDto);

		getClient().execute(new RequestRepoConnection(repositoryName, getPathPrefix(), repositoryDto));
	}

	@Override
	public ChangeSetDto getChangeSetDto(final boolean localSync, final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {

		File decryptedChangeSetDtoCacheFile = null;
		ChangeSetDto result = null;

		try {
			decryptedChangeSetDtoCacheFile = getDecryptedChangeSetDtoCacheFile(lastSyncToRemoteRepoLocalRepositoryRevisionSynced);
			if (decryptedChangeSetDtoCacheFile.isFile() && decryptedChangeSetDtoCacheFile.length() > 0) {
				ChangeSetDtoIo changeSetDtoIo = createObject(ChangeSetDtoIo.class);
				result = changeSetDtoIo.deserializeWithGz(decryptedChangeSetDtoCacheFile);
				logger.info("getChangeSetDto: Read decrypted ChangeSetDto-cache-file: {}", decryptedChangeSetDtoCacheFile.getAbsolutePath());
				return result;
			} else {
				logger.info("getChangeSetDto: Decrypted ChangeSetDto-cache-file NOT found: {}", decryptedChangeSetDtoCacheFile.getAbsolutePath());
			}
		} catch (Exception x) {
			result = null;
			logger.error("getChangeSetDto: Reading decrypted ChangeSetDto-cache-file failed: " + x, x);
		}

		ChangeSetDto changeSetDto = getRestRepoTransport().getChangeSetDto(localSync, lastSyncToRemoteRepoLocalRepositoryRevisionSynced);

		if (logger.isInfoEnabled()) {
			logger.info("getChangeSetDto: clientRepositoryId={} serverRepositoryId={}: lastSyncToRemoteRepoLocalRepositoryRevisionSynced={} repoFileDtos.size={}",
					getClientRepositoryId(), getRepositoryId(), lastSyncToRemoteRepoLocalRepositoryRevisionSynced, changeSetDto.getRepoFileDtos().size());

			logMemoryStats(logger);

			logger.trace("getChangeSetDto: {}", changeSetDto);
		}

		syncCryptoKeysFromRemoteRepo();

		result = decryptChangeSetDto(changeSetDto);
		changeSetDto = null; // allow for gc! it was destroyed (modified) by the above method, already and should not be used anymore!

		if (decryptedChangeSetDtoCacheFile != null) {
			File tmpFile = decryptedChangeSetDtoCacheFile.getParentFile().createFile(decryptedChangeSetDtoCacheFile.getName() + TMP_FILE_NAME_SUFFIX);
			ChangeSetDtoIo changeSetDtoIo = createObject(ChangeSetDtoIo.class);
			changeSetDtoIo.serializeWithGz(result, tmpFile);
			if (! tmpFile.renameTo(decryptedChangeSetDtoCacheFile)) {
				logger.error("getChangeSetDto: Could not rename temporary file to active decrypted ChangeSetDto-cache-file: {}", decryptedChangeSetDtoCacheFile.getAbsolutePath());
			} else {
				logger.info("getChangeSetDto: Wrote decrypted ChangeSetDto-cache-file: {}", decryptedChangeSetDtoCacheFile.getAbsolutePath());
			}
		}
		return result;
	}

	@Override
	public void prepareForChangeSetDto(ChangeSetDto changeSetDto) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final CryptoChangeSetDto cryptoChangeSetDto;
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			cryptoChangeSetDto = cryptree.createHistoCryptoRepoFilesForDeletedCryptoRepoFiles();
//			if (cryptoChangeSetDto != null)
//				cryptree.createSyntheticDeleteModifications(changeSetDto, cryptoChangeSetDto);

			transaction.commit();
		}

		if (cryptoChangeSetDto != null) {
			try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				final Cryptree cryptree = getCryptree(transaction);
				putCryptoChangeSetDto(cryptoChangeSetDto);
				cryptree.updateLastCryptoKeySyncToRemoteRepo();
				transaction.commit();
			}
		}
	}

	@Override
	protected String determinePathPrefix() {
		final String pathPrefix = super.determinePathPrefix();

		final LocalRepoManager localRepoManager = getLocalRepoManager();

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptreeFactory().getCryptreeOrCreate(transaction, getRepositoryId(), pathPrefix, getUserRepoKeyRing());
			cryptree.registerRemotePathPrefix(pathPrefix);
			transaction.commit();
		}
		return pathPrefix;
	}

	private void syncCryptoKeysFromRemoteRepo() {
		final LocalRepoManager localRepoManager = getLocalRepoManager();

		Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced = null; // naming perspective from remote side
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced =
					cryptree.getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced(); // naming perspective from this (local) side
		}

		CryptoChangeSetDtoTooLargeException cryptoChangeSetDtoTooLargeException = null;
		CryptoChangeSetDto cryptoChangeSetDto = null;
		final long beginTimestamp = System.currentTimeMillis();
		while (true) {
			try {
				cryptoChangeSetDto = getClient().execute(new GetCryptoChangeSetDto(getRepositoryId().toString(), lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced));
				break;
			} catch (final DeferredCompletionException x) {
				if (System.currentTimeMillis() > beginTimestamp + cryptoChangeSetTimeout)
					throw new TimeoutException(String.format("Could not get crypto-change-set within %s milliseconds!", cryptoChangeSetTimeout), x);

				logger.info("syncCryptoKeysFromRemoteRepo: Got DeferredCompletionException; will retry.");
			} catch (final Exception x) {
				cryptoChangeSetDtoTooLargeException = ExceptionUtil.getCause(x, CryptoChangeSetDtoTooLargeException.class);
				if (cryptoChangeSetDtoTooLargeException == null)
					throw x;

				break;
			}
		}

		if (cryptoChangeSetDto != null)
			syncCryptoKeysFromRemoteRepo_putCryptoChangeSetDto(cryptoChangeSetDto);
		else {
			if (cryptoChangeSetDtoTooLargeException == null)
				throw new IllegalStateException("cryptoChangeSetDto and cryptoChangeSetTooLargeException are both null!");

			try {
				syncMultiPartCryptoChangeSetDtosFromRemoteRepo(cryptoChangeSetDtoTooLargeException);
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}

		// In case of successful commit, we notify the server in order to not receive the same changes again.
		getClient().execute(new EndGetCryptoChangeSetDto(getRepositoryId().toString()));
		try {
			CryptoChangeSetDtoSplitFileManager.createInstance(getLocalRepoManager(), getRepositoryId()).deleteAll();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}

		if (cryptoChangeSetDtoTooLargeException != null)
			syncCryptoKeysFromRemoteRepo(); // in case of a multi-part-response, we repeat the sync immediately, because the result we just processed might be stale (old files).
	}

	protected void syncMultiPartCryptoChangeSetDtosFromRemoteRepo(CryptoChangeSetDtoTooLargeException cryptoChangeSetDtoTooLargeException) throws IOException {
		requireNonNull(cryptoChangeSetDtoTooLargeException, "cryptoChangeSetTooLargeException");
		final String exMsg = cryptoChangeSetDtoTooLargeException.getMessage();
		final int multiPartCount;
		try {
			multiPartCount = Integer.parseInt(exMsg);
		} catch (Exception x) {
			throw new IllegalArgumentException("CryptoChangeSetDtoTooLargeException.message '" + exMsg + "' could not be parsed as integer!", x);
		}

		final CryptoChangeSetDtoSplitFileManager cryptoChangeSetDtoSplitFileManager = CryptoChangeSetDtoSplitFileManager.createInstance(getLocalRepoManager(), getRepositoryId());
//		cryptoChangeSetDtoSplitFileManager.setCryptoChangeSetDtoTmpDirRandom(false);

		for (int multiPartIndex = 0; multiPartIndex < multiPartCount; ++multiPartIndex) {
			if (cryptoChangeSetDtoSplitFileManager.existsCryptoChangeSetDtoFile(multiPartIndex))
				continue;

			final byte[] fileData = getClient().execute(new GetCryptoChangeSetDtoFileData(getRepositoryId().toString(), multiPartIndex));
			cryptoChangeSetDtoSplitFileManager.writeCryptoChangeSetDtoFile(multiPartIndex, fileData);
		}

		if (cryptoChangeSetDtoSplitFileManager.getFinalFileCount() != multiPartCount)
			throw new IllegalStateException("cryptoChangeSetDtoSplitFileManager.getFinalFileCount() != multiPartCount :: " + cryptoChangeSetDtoSplitFileManager.getFinalFileCount() + " != " + multiPartCount);

		final Uid multiPartId;
		if (multiPartCount > 0) { // should *always* be > 0, but we better check ;-)
			final CryptoChangeSetDto cryptoChangeSetDto = cryptoChangeSetDtoSplitFileManager.readCryptoChangeSetDto(0);
			multiPartId = cryptoChangeSetDto.getMultiPartId();
		} else
			multiPartId = null;

		for (int multiPartIndex = 0; multiPartIndex < multiPartCount; ++multiPartIndex) {
			if (cryptoChangeSetDtoSplitFileManager.isCryptoChangeSetDtoImported(multiPartIndex))
				continue;

			final CryptoChangeSetDto cryptoChangeSetDto = cryptoChangeSetDtoSplitFileManager.readCryptoChangeSetDto(multiPartIndex);

			// The multiPartId should never be null, but it was introduced later, hence we add a null-check for backward-compatibility.
			if (multiPartId != null && ! multiPartId.equals(cryptoChangeSetDto.getMultiPartId()))
				throw new IllegalStateException("cryptoChangeSetDto.getMultiPartId() != multiPartId :: " + cryptoChangeSetDto.getMultiPartId() + " != " + multiPartId);

			if (cryptoChangeSetDto.getMultiPartCount() != multiPartCount)
				throw new IllegalStateException("cryptoChangeSetDto.getMultiPartCount() != multiPartCount :: " + cryptoChangeSetDto.getMultiPartCount() + " != " + multiPartCount);

			if (cryptoChangeSetDto.getMultiPartIndex() != multiPartIndex)
				throw new IllegalStateException("cryptoChangeSetDto.getMultiPartIndex() != multiPartIndex :: " + cryptoChangeSetDto.getMultiPartIndex() + " != " + multiPartIndex);

			syncCryptoKeysFromRemoteRepo_putCryptoChangeSetDto(cryptoChangeSetDto);

			cryptoChangeSetDtoSplitFileManager.markCryptoChangeSetDtoImported(multiPartIndex);
		}
	}

	protected void syncCryptoKeysFromRemoteRepo_putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		if (logger.isInfoEnabled()) {
			logger.info("syncCryptoKeysFromRemoteRepo_putCryptoChangeSetDto: clientRepositoryId={} serverRepositoryId={}: "
					+ "cryptoRepoFileDtos.size={}, cryptoKeyDtos.size={}, cryptoLinkDtos.size={}, "
					+ "currentHistoCryptoRepoFileDtos.size={}, histoCryptoRepoFileDtos.size={}, "
					+ "histoFrameDtos.size={}, permissionDtos.size={}, permissionSetDtos.size={}, "
					+ "permissionSetInheritanceDtos.size={}, userIdentityDtos.size={}, userIdentityLinkDtos.size={}, "
					+ "userRepoKeyPublicKeyDtos.size={}, userRepoKeyPublicKeyReplacementRequestDtos.size={}, "
					+ "userRepoKeyPublicKeyReplacementRequestDeletionDtos.size={}, collisionDtos.size={}, "
					+ "cryptoConfigPropSetDtos.size={}",
					getClientRepositoryId(), getRepositoryId(),
					cryptoChangeSetDto.getCryptoRepoFileDtos().size(),
					cryptoChangeSetDto.getCryptoKeyDtos().size(),
					cryptoChangeSetDto.getCryptoLinkDtos().size(),
					cryptoChangeSetDto.getCurrentHistoCryptoRepoFileDtos().size(),
					cryptoChangeSetDto.getHistoCryptoRepoFileDtos().size(),
					cryptoChangeSetDto.getHistoFrameDtos().size(),
					cryptoChangeSetDto.getPermissionDtos().size(),
					cryptoChangeSetDto.getPermissionSetDtos().size(),
					cryptoChangeSetDto.getPermissionSetInheritanceDtos().size(),
					cryptoChangeSetDto.getUserIdentityDtos().size(),
					cryptoChangeSetDto.getUserIdentityLinkDtos().size(),
					cryptoChangeSetDto.getUserRepoKeyPublicKeyDtos().size(),
					cryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDtos().size(),
					cryptoChangeSetDto.getUserRepoKeyPublicKeyReplacementRequestDeletionDtos().size(),
					cryptoChangeSetDto.getCollisionDtos().size(),
					cryptoChangeSetDto.getCryptoConfigPropSetDtos().size());

			logMemoryStats(logger);

			logger.trace("syncCryptoKeysFromRemoteRepo: {}", cryptoChangeSetDto);
		}

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			cryptree.initLocalRepositoryType();
			cryptree.putCryptoChangeSetDto(cryptoChangeSetDto);

			logger.info("syncCryptoKeysFromRemoteRepo: after putCryptoChangeSetDto(...)");
			logMemoryStats(logger);

			transaction.commit();
		}
	}

	/**
	 * Decrypt the given {@code changeSetDto}.
	 * <p>
	 * <b>Important:</b> This method modifies the input! The given {@code changeSetDto} is
	 * <b>destroyed</b> in order to save memory! It is not needed, anymore, as the code continues
	 * working with the result.
	 * @param changeSetDto the encrypted change-set to be decrypted. Never {@code null}.
	 * @return the decrypted change-set. Never {@code null}.
	 */
	private ChangeSetDto decryptChangeSetDto(final ChangeSetDto changeSetDto) {
		requireNonNull(changeSetDto, "changeSetDto");
		final ChangeSetDto decryptedChangeSetDto = new ChangeSetDto();

		final boolean debug = logger.isDebugEnabled();
		if (logger.isInfoEnabled()) {
			logger.info("decryptChangeSetDto: entered.");
			logMemoryStats(logger);
		}

		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) { // TODO read-transaction?!
			final Cryptree cryptree = getCryptree(transaction);

			final SignableVerifier signableVerifier = new SignableVerifier(cryptree.getUserRepoKeyPublicKeyLookup());

			decryptedChangeSetDto.setRepositoryDto(changeSetDto.getRepositoryDto());

			for (final ModificationDto modificationDto : changeSetDto.getModificationDtos()) {
				verifySignatureAndPermission(cryptree, signableVerifier, modificationDto);

				final ModificationDto decryptedModificationDto = decryptModificationDto(cryptree, modificationDto);
				if (decryptedModificationDto != null) // if it's null, it could not be decrypted (missing access rights?!) and should be ignored.
					decryptedChangeSetDto.getModificationDtos().add(decryptedModificationDto);
			}
			changeSetDto.setModificationDtos(null); // DESTROY to save memory!

			int processedTreeNodeCount = 0;
			long lastLogTimestamp = System.currentTimeMillis();

			final Set<Long> nonDecryptableRepoFileIds = new HashSet<Long>(); // non-decryptable or deleted. can deleted files really end up here? I think so, but not sure.

			final RepoFileDtoTreeNode tree = RepoFileDtoTreeNode.createTree(changeSetDto.getRepoFileDtos());
			changeSetDto.setRepoFileDtos(null); // DESTROY to save memory!
			Set<RepoFileDtoTreeNode> deletedDuplicateCryptoRepoFileNodes = new HashSet<>();
			if (tree != null) {
				for (final Iterator<RepoFileDtoTreeNode> it = tree.iterator(); it.hasNext(); ) {
					final RepoFileDtoTreeNode node = it.next();
					it.remove(); // DESTROY to save memory!
					if (deletedDuplicateCryptoRepoFileNodes.contains(node))
						continue;

					final RepoFileDto repoFileDto = node.getRepoFileDto();

					try {
						// We silently ignore those missing signatures that are allowed to be ignored silently ;-)
						verifySignatureAndTreeStructureAndPermission(cryptree, signableVerifier, node);
					} catch (CollisionException x) {
						logger.warn("decryptChangeSetDto: " + x);
						for (RepoFileDtoTreeNode nodeOrChild : node) {
							deletedDuplicateCryptoRepoFileNodes.add(nodeOrChild);
							changeSetDto.getRepoFileDtos().remove(nodeOrChild.getRepoFileDto());
						}
						continue;
					}

					if (nonDecryptableRepoFileIds.contains(repoFileDto.getParentId())) {
						nonDecryptableRepoFileIds.add(repoFileDto.getId()); // transitive for all children and children's children
						continue;
					}

					final RepoFileDto decryptedRepoFileDto = decryptRepoFileDtoOnServer(cryptree, repoFileDto);
					// We should remove the superfluous data to make sure the result looks exactly as it would do in
					// a normal CloudStore sync.
					// We're maybe not yet completely there, but very close. At least the large fileChunkDtos and the signatures
					// are removed to save memory in order to solve the OutOfMemoryErrors -- without a fundamental refactoring.
					decryptChangeSetDto_removeUnnecessaryData(decryptedRepoFileDto);

					// if it's null, it could not be decrypted (missing access rights?!) and should be ignored. or maybe it was deleted... not sure, if this happens.
					if (decryptedRepoFileDto == null)
						nonDecryptableRepoFileIds.add(repoFileDto.getId());
					else
						decryptedChangeSetDto.getRepoFileDtos().add(decryptedRepoFileDto);

					if (debug) {
						++processedTreeNodeCount;
						if (System.currentTimeMillis() - lastLogTimestamp > 10000L) {
							logger.debug("decryptChangeSetDto: processedTreeNodeCount={}", processedTreeNodeCount);
							logMemoryStats(logger); // TODO debug(..)! not info(...)!
							lastLogTimestamp = System.currentTimeMillis();
						}
					}
				}
			}

			decryptedChangeSetDto.setParentConfigPropSetDto(cryptree.getParentConfigPropSetDtoIfNeeded());

			cryptree.createSyntheticDeleteModifications(decryptedChangeSetDto);

			if (logger.isInfoEnabled()) {
				logger.info("decryptChangeSetDto: before commit.");
				logMemoryStats(logger);
			}
			transaction.commit();
		}

		if (logger.isInfoEnabled()) {
			logger.info("decryptChangeSetDto: after commit.");
			logMemoryStats(logger);
		}

		logger.trace("decryptChangeSetDto: clientRepositoryId={} serverRepositoryId={}: {}",
				getClientRepositoryId(), getRepositoryId(), decryptedChangeSetDto);

		return decryptedChangeSetDto;
	}

	/**
	 * Removes all unnecessary data from the {@code decryptedRepoFileDto}.
	 * @param decryptedRepoFileDto the {@code RepoFileDto} to be stripped of all unnecessary data. Must not be {@code null}.
	 */
	private void decryptChangeSetDto_removeUnnecessaryData(RepoFileDto decryptedRepoFileDto) {
		if (decryptedRepoFileDto instanceof SsNormalFileDto) {
			SsNormalFileDto normalFileDto = (SsNormalFileDto) decryptedRepoFileDto;
			normalFileDto.setFileChunkDtos(null);
			normalFileDto.setParentName(null);
			normalFileDto.setSignature(null);
		} else if (decryptedRepoFileDto instanceof SsDirectoryDto) {
			SsDirectoryDto directoryDto = (SsDirectoryDto) decryptedRepoFileDto;
			directoryDto.setParentName(null);
			directoryDto.setSignature(null);
		} else if (decryptedRepoFileDto instanceof SsSymlinkDto) {
			SsSymlinkDto symlinkDto = (SsSymlinkDto) decryptedRepoFileDto;
			symlinkDto.setParentName(null);
			symlinkDto.setSignature(null);
		}
	}

	private void verifySignatureAndPermission(final Cryptree cryptree, final SignableVerifier signableVerifier, final ModificationDto modificationDto) {
		if (!(modificationDto instanceof Signable))
			throw new IllegalArgumentException("modificationDto is not Signable: " + modificationDto); // We do not accept any unsigned data from the server! Server might be compromised!

		if (modificationDto instanceof WriteProtected)
			cryptree.assertSignatureOk((WriteProtected) modificationDto);
		else
			signableVerifier.verify((Signable) modificationDto);
	}

	/**
	 * Verifies (1) correct signature, (2) write permission and (3) correct tree structure at given node.
	 * <p>
	 * <u><b>(1) Correct signature</b></u>
	 * <p>
	 * This method verifies the signature of the {@code SsRepoFileDto} contained in the given {@code node},
	 * i.e. property {@code node.repoFileDto}. Every {@code SsRepoFileDto} must be signed. A missing
	 * or broken signature causes a {@link SignatureException} to be thrown.
	 * <p>
	 * <u><b>(2) Write permission</b></u>
	 * <p>
	 * This method checks, if the user having signed the {@code SsRepoFileDto} had write write permissions
	 * when the signature was made.
	 *
	 * and checks whether the parent-node (of the given node) matches
	 * the value of.
	 *
	 *
	 * @return <code>true</code>, if the signature is valid. <code>false</code>, if the signature is missing, but
	 * not required. If this state is allowed, the object will still be skipped, i.e. <i>not</i> accepted. Hence, every
	 * object must be signed in order to be processed. Non-signed objects are never processed!
	 * @throws SignatureException if the signature is broken or it is missing and required.
	 * @throws WriteAccessDeniedException if the object is signed, but the user having signed it did not have the permission to do so.
	 */
	private void verifySignatureAndTreeStructureAndPermission(final Cryptree cryptree, final SignableVerifier signableVerifier, final RepoFileDtoTreeNode node)
			throws SignatureException, WriteAccessDeniedException
	{
		final RepoFileDto repoFileDto = node.getRepoFileDto();
		final SsRepoFileDto ssRepoFileDto = (SsRepoFileDto) repoFileDto;

		// If we're connected to a sub-directory, the server sends us a modified root-DirectoryDto, because
		// the root is not supposed to have a name (name is empty, i.e. ""). The real name (on the server) is,
		// however, stored in the property SsDirectoryDto.realName in this case. This real name was the one
		// having been signed.
		boolean restoreVirtualRootName = false;
		try {
			if (ssRepoFileDto instanceof SsDirectoryDto) {
				final SsDirectoryDto ssDirectoryDto = (SsDirectoryDto) ssRepoFileDto;
				if (isVirtualRootWithDifferentRealName(ssDirectoryDto)) {
					restoreVirtualRootName = true;
					ssDirectoryDto.setName(ssDirectoryDto.getRealName());
				}
			}

			try {
				signableVerifier.verify(ssRepoFileDto);
			} catch (final SignatureException x) {
				throw new SignatureException(String.format(
						"%s repoFileDto.name='%s' repoFileDto.parentName='%s'",
						x.getMessage(), ssRepoFileDto.getName(), ssRepoFileDto.getParentName()), x);
			}

			final Signature signature = ssRepoFileDto.getSignature();
			final Uid cryptoRepoFileId = repoFileDto.getName().isEmpty() ? cryptree.getRootCryptoRepoFileId() : new Uid(repoFileDto.getName());

			cryptree.assertIsNotDeletedDuplicateCryptoRepoFile(cryptoRepoFileId);

			cryptree.assertHasPermission(
					cryptoRepoFileId, signature.getSigningUserRepoKeyId(), PermissionType.write, signature.getSignatureCreated());
			// TODO wouldn't it be better to use cryptree.assertSignatureOk(...) instead?! Need to implement WriteProtected, of course...
		} finally {
			if (restoreVirtualRootName)
				ssRepoFileDto.setName("");
		}

		if (repoFileDto.getParentId() == null)
			assertRepoFileDtoIsCorrectRoot(cryptree, repoFileDto);
		else
			assertRepoFileParentNameMatchesParentRepoFileName(node);
	}

	private void assertRepoFileDtoIsCorrectRoot(final Cryptree cryptree, final RepoFileDto repoFileDto) {
		requireNonNull(cryptree, "cryptree");
		requireNonNull(repoFileDto, "repoFileDto");
		final SsDirectoryDto ssDirectoryDto = (SsDirectoryDto) repoFileDto;

		if (! repoFileDto.getName().isEmpty())
			throw new IllegalStateException(String.format("repoFileDto.name is not an empty String, but: '%s'", repoFileDto.getName()));

		if (cryptree.getRemotePathPrefix().isEmpty()) {
			if (ssDirectoryDto.getRealName() != null)
				throw new IllegalStateException(String.format("ssDirectoryDto.realName is not null, but: '%s'", ssDirectoryDto.getRealName()));
		}
		else {
			final Uid virtualRootCryptoRepoFileId = requireNonNull(cryptree.getCryptoRepoFileIdForRemotePathPrefixOrFail(), "cryptree.getCryptoRepoFileIdForRemotePathPrefixOrFail()");
			if (! virtualRootCryptoRepoFileId.toString().equals(ssDirectoryDto.getRealName()))
				throw new IllegalStateException(String.format("virtualRootCryptoRepoFileId != ssDirectoryDto.realName :: '%s' != '%s'",
						virtualRootCryptoRepoFileId, ssDirectoryDto.getRealName()));
		}
	}

	private void assertRepoFileParentNameMatchesParentRepoFileName(final RepoFileDtoTreeNode node) throws IllegalStateException {
		requireNonNull(node, "node");
		final SsRepoFileDto ssRepoFileDto = (SsRepoFileDto) node.getRepoFileDto();
		final String childParentName = requireNonNull(ssRepoFileDto.getParentName(), "ssRepoFileDto.parentName");
		final RepoFileDtoTreeNode parent = requireNonNull(node.getParent(), "node.parent");
		final RepoFileDto parentRepoFileDto = requireNonNull(parent.getRepoFileDto(), "node.parent.repoFileDto");
		final SsDirectoryDto parentDirectoryDto = (SsDirectoryDto) parentRepoFileDto;
		final String parentRealName = (isVirtualRootWithDifferentRealName(parentDirectoryDto)
				? parentDirectoryDto.getRealName() : parentDirectoryDto.getName());

		if (!childParentName.equals(parentRealName))
			throw new IllegalStateException(String.format("RepoFileDtoTreeNode tree structure does not match signed structure! ssRepoFileDto.parentName != parentRealName :: '%s' != '%s'",
					childParentName, parentRealName));
	}

	private boolean isVirtualRootWithDifferentRealName(final SsDirectoryDto ssDirectoryDto) {
		requireNonNull(ssDirectoryDto, "ssDirectoryDto");
		return ssDirectoryDto.getRealName() != null
				&& ssDirectoryDto.getName().isEmpty()
				&& ssDirectoryDto.getParentId() == null;
	}

	private ModificationDto decryptModificationDto(final Cryptree cryptree, final ModificationDto modificationDto) {
		requireNonNull(modificationDto, "modificationDto");

		if (modificationDto instanceof SsDeleteModificationDto)
			return decryptDeleteModificationDto(cryptree, (SsDeleteModificationDto) modificationDto);

		// TODO implement this for other modifications.
		throw new UnsupportedOperationException("NYI");
	}

	private ModificationDto decryptDeleteModificationDto(final Cryptree cryptree, final SsDeleteModificationDto modificationDto) {
		requireNonNull(modificationDto, "modificationDto");

		final String localPath = cryptree.getLocalPath(modificationDto.getServerPath());
		modificationDto.setPath(localPath);
		return modificationDto;
	}

	private RepoFileDto decryptRepoFileDtoOnServer(final Cryptree cryptree, final RepoFileDto repoFileDto) {
		requireNonNull(cryptree, "cryptree");
		final String name = requireNonNull(repoFileDto, "repoFileDto").getName();

		final Uid cryptoRepoFileId = (repoFileDto.getParentId() == null && name.isEmpty())
				? cryptree.getRootCryptoRepoFileId()
						: new Uid(requireNonNull(name, "repoFileDto.name"));

		if (cryptoRepoFileId == null) // there is no root before the very first up-sync!
			return null;

		final RepoFileDto decryptedRepoFileDto;
		try {
			decryptedRepoFileDto = cryptree.getDecryptedRepoFileOnServerDtoOrFail(cryptoRepoFileId);
		} catch (final AccessDeniedException | FileDeletedException x) {
			return null;
		}
		decryptedRepoFileDto.setId(repoFileDto.getId());
		decryptedRepoFileDto.setParentId(repoFileDto.getParentId());
		return decryptedRepoFileDto;
	}

	@Override
	public void makeDirectory(final String path, final Date lastModified) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final CryptoChangeSetDto cryptoChangeSetDto;
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			cryptree.createUnsealedHistoFrameIfNeeded();

			cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
			transaction.commit();
		}

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			final CurrentHistoCryptoRepoFileDto chcrfDto = cryptree.createCurrentHistoCryptoRepoFileDto(path, true);

			final String serverPath = cryptree.getServerPath(path);
			final SsDirectoryDto directoryDto = createDirectoryDtoForMakeDirectory(cryptree, path, serverPath);

			final RepoFileDtoWithCurrentHistoCryptoRepoFileDto rfdwchcrfd = new RepoFileDtoWithCurrentHistoCryptoRepoFileDto();
			rfdwchcrfd.setRepoFileDto(directoryDto);
			rfdwchcrfd.setCurrentHistoCryptoRepoFileDto(chcrfDto);

			logger.debug("makeDirectory: clientRepositoryId={} serverRepositoryId={} path='{}' serverPath='{}'",
					getClientRepositoryId(), getRepositoryId(), path, serverPath);

			getClient().execute(new SsMakeDirectory(getRepositoryId().toString(), serverPath, rfdwchcrfd));

			transaction.commit();
		}
	}

	protected SsDirectoryDto createDirectoryDtoForMakeDirectory(final Cryptree cryptree, final String path, final String serverPath) {
		final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(path, PermissionType.write);
		final SsDirectoryDto directoryDto = new SsDirectoryDto();

		final File f = createFile(serverPath);
		directoryDto.setName(f.getName());

		final File pf = f.getParentFile();
		directoryDto.setParentName(pf == null ? null : pf.getName());

		directoryDto.setLastModified(SsDirectoryDto.DUMMY_LAST_MODIFIED);

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(directoryDto);
		return directoryDto;
	}

	@Override
	public void makeSymlink(final String path, final String target, final Date lastModified) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final CryptoChangeSetDto cryptoChangeSetDto;
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			cryptree.createUnsealedHistoFrameIfNeeded();

			cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);

			transaction.commit();
		}

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			final CurrentHistoCryptoRepoFileDto chcrfDto = cryptree.createCurrentHistoCryptoRepoFileDto(path, true);

			final String serverPath = cryptree.getServerPath(path);
			final SsSymlinkDto symlinkDto = createSymlinkDtoForMakeSymlink(cryptree, path, serverPath);

			final RepoFileDtoWithCurrentHistoCryptoRepoFileDto rfdwchcrfd = new RepoFileDtoWithCurrentHistoCryptoRepoFileDto();
			rfdwchcrfd.setRepoFileDto(symlinkDto);
			rfdwchcrfd.setCurrentHistoCryptoRepoFileDto(chcrfDto);

			logger.debug("makeSymlink: clientRepositoryId={} serverRepositoryId={} path='{}' serverPath='{}'",
					getClientRepositoryId(), getRepositoryId(), path, serverPath);

			getClient().execute(new SsMakeSymlink(getRepositoryId().toString(), serverPath, rfdwchcrfd));

			transaction.commit();
		}
	}

	private SsSymlinkDto createSymlinkDtoForMakeSymlink(Cryptree cryptree, String path, String serverPath) {
		final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(path, PermissionType.write);
		final SsSymlinkDto symlinkDto = new SsSymlinkDto();

		final File f = createFile(serverPath);
		symlinkDto.setName(f.getName());

		final File pf = f.getParentFile();
		symlinkDto.setParentName(pf == null ? null : pf.getName());

		symlinkDto.setTarget(SsSymlinkDto.DUMMY_TARGET);
		symlinkDto.setLastModified(SsSymlinkDto.DUMMY_LAST_MODIFIED);

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(symlinkDto);
		return symlinkDto;
	}

	protected CryptreeFactory getCryptreeFactory() {
		if (cryptreeFactory == null)
			cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();

		return cryptreeFactory;
	}

	protected Cryptree getCryptree(final LocalRepoTransaction transaction) {
		return getCryptreeFactory().getCryptreeOrCreate(transaction, getRepositoryId(), getPathPrefix(), getUserRepoKeyRing());
	}

	protected UserRepoKeyRing getUserRepoKeyRing() {
		if (userRepoKeyRing == null) {
			final UserRepoKeyRingLookup lookup = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup();
			final UserRepoKeyRingLookupContext context = new UserRepoKeyRingLookupContext(getClientRepositoryIdOrFail(), getRepositoryId());
			userRepoKeyRing = lookup.getUserRepoKeyRing(context);
			if (userRepoKeyRing == null)
				throw new IllegalStateException(String.format("UserRepoKeyRingLookup.getUserRepoKeyRing(context) returned null! lookup=%s context=%s", lookup, context));
		}
		return userRepoKeyRing;
	}

	@Override
	public void copy(final String fromPath, final String toPath) {
		throw new UnsupportedOperationException("not supported!");
	}

	@Override
	public void move(final String fromPath, final String toPath) {
		throw new UnsupportedOperationException("not supported!");
	}

	@Override
	public void delete(final SsDeleteModificationDto deleteModificationDto) {
		final String path = deleteModificationDto.getPath();
		try {
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
				final Cryptree cryptree = getCryptree(transaction);
				cryptree.sign(deleteModificationDto);
				deleteModificationDto.setPath(null); // path is *not* signed and *must* *not* be transferred to the server! It is secret!
				transaction.commit();
			}
			getClient().execute(new SsDelete(getRepositoryId().toString(), deleteModificationDto));
		} finally {
			deleteModificationDto.setPath(path); // restore after sending to server
		}
	}

	@Override
	public void delete(final String path) {
		// Resolving the server-path from the local path here does not work, because the CryptoRepoFile was already deleted when
		// the RepoFile was. That's why we register the server-path already at the deletion moment and store it in
		// SsDeleteModificationDto.serverPath. This is then handled by delete(SsDeleteModificationDto) above.
		throw new UnsupportedOperationException("Replaced by delete(SsDeleteModificationDto)!");
	}

	@Override
	public RepoFileDto getRepoFileDto(final String path) {
		final RepoFileDto result;
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final RepoFileDto decryptedRepoFileDto = cryptree.getDecryptedRepoFileOnServerDto(path);

			final LocalRepoStorage lrs = LocalRepoStorageFactoryRegistry.getInstance().getLocalRepoStorageFactoryOrFail().getLocalRepoStorageOrCreate(
					transaction, getRepositoryId(), getPathPrefix());

			if (decryptedRepoFileDto != null)
				result = decryptedRepoFileDto;
			else {
				// The file does not exist on the server, yet, but it was *partially* uploaded.
				// Hence, we must synthetically create a NormalFileDto and
				result = lrs.getRepoFileDto(path);
				if (result instanceof NormalFileDto) {
					final SsNormalFileDto nf = (SsNormalFileDto) result;
					nf.getFileChunkDtos().clear();
					nf.setLength(0);
					nf.setLengthWithPadding(0);
					nf.setLastModified(new Date());
					nf.setSha1(sha1(Long.toString(System.currentTimeMillis(), 36)));
				}
			}

			if (result instanceof NormalFileDto) {
				final NormalFileDto nf = (NormalFileDto) result;
				nf.getTempFileChunkDtos().clear();
				nf.getTempFileChunkDtos().addAll(lrs.getTempFileChunkDtos(path));
			}

			transaction.commit();
		}
		return result;
	}

	@Override
	public byte[] getFileData(final String path, final long offset, final int length) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final byte[] decryptedFileData;
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final String unprefixedServerPath = unprefixPath(cryptree.getServerPath(path)); // it's automatically prefixed *again*, thus we must prefix it here (if we don't want to somehow suppress the automatic prefixing, which is probably quite a lot of work).
			final byte[] encryptedFileData = getRestRepoTransport().getFileData(unprefixedServerPath, offset, length);
			final UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup = cryptree.getUserRepoKeyPublicKeyLookup();
			final Uid cryptoKeyId = readCryptoKeyId(encryptedFileData, userRepoKeyPublicKeyLookup);
			final DataKey dataKey = cryptree.getDataKeyOrFail(cryptoKeyId);
			decryptedFileData = verifyAndDecrypt(encryptedFileData, dataKey.keyParameter, userRepoKeyPublicKeyLookup);

			transaction.commit();
		}
		return decryptedFileData;
	}

	@Override
	public byte[] getHistoFileData(Uid histoCryptoRepoFileId, long offset) {
		final byte[] decryptedFileData;
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			final byte[] encryptedFileData = getClient().execute(
					new GetHistoFileData(getRepositoryId().toString(), histoCryptoRepoFileId, offset));

			final UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup = cryptree.getUserRepoKeyPublicKeyLookup();
			final Uid cryptoKeyId = readCryptoKeyId(encryptedFileData, userRepoKeyPublicKeyLookup);
			final DataKey dataKey = cryptree.getDataKeyOrFail(cryptoKeyId);
			decryptedFileData = verifyAndDecrypt(encryptedFileData, dataKey.keyParameter, userRepoKeyPublicKeyLookup);

			transaction.commit();
		}
		return decryptedFileData;
	}

	@Override
	public void beginPutFile(final String path) {
		throw new UnsupportedOperationException("Replaced by beginPutFile(String path, NormalFileDto fromNormalFileDto)!");
	}

	@Override
	public void beginPutFile(final String path, SsNormalFileDto fromNormalFileDto) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final CryptoChangeSetDto cryptoChangeSetDto;
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);

			transaction.commit();
		}

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			final String serverPath = cryptree.getServerPath(path);
			final SsNormalFileDto serverNormalFileDto = createNormalFileDtoForPutFile(
					cryptree, path, serverPath,
					assertNotNegative(fromNormalFileDto.getLengthWithPadding()) );

			logger.debug("beginPutFile: clientRepositoryId={} serverRepositoryId={} path='{}' serverPath='{}'",
					getClientRepositoryId(), getRepositoryId(), path, serverPath);

			getClient().execute(new SsBeginPutFile(getRepositoryId().toString(), serverPath, serverNormalFileDto));

			transaction.commit();
		}
	}

//	protected void createUnsealedCurrentHistoryFrameDtoIfNeeded(final Cryptree cryptree) {
//		HistoFrameDto histoFrameDto = cryptree.getUnsealedHistoFrameDto();
//		if (histoFrameDto == null) {
//			histoFrameDto = cryptree.createUnsealedHistoFrameDto();
//			getClient().execute(new PutHistoFrameDto(getRepositoryId().toString(), histoFrameDto));
//		}
//	}

//	protected void sealCurrentHistoryFrameDto(final Cryptree cryptree) {
//		HistoFrameDto histoFrameDto = cryptree.getUnsealedHistoFrameDto();
//		if (histoFrameDto != null) {
//			histoFrameDto = cryptree.sealUnsealedHistoryFrame();
//			getClient().execute(new PutHistoFrameDto(getRepositoryId().toString(), histoFrameDto));
//		}
//	}

	protected SsNormalFileDto createNormalFileDtoForPutFile(final Cryptree cryptree, final String localPath, final String serverPath, long lengthWithPadding) {
		final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(localPath, PermissionType.write);

//		length = roundLengthToChunkMaxLength(length);

		final SsNormalFileDto normalFileDto = new SsNormalFileDto();

		final File f = createFile(serverPath);
		normalFileDto.setName(f.getName());

		final File pf = f.getParentFile();
		normalFileDto.setParentName(pf == null ? null : pf.getName());

		// Calculating the SHA1 of the encrypted data is too complicated and unnecessary. We thus omit it (now optional in CloudStore).
		normalFileDto.setLength(lengthWithPadding);

		normalFileDto.setLastModified(SsRepoFileDto.DUMMY_LAST_MODIFIED);

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(normalFileDto);

		return normalFileDto;
	}

//	private long roundLengthToChunkMaxLength(final long length) {
//		final long multiplier = length / FileChunkDto.MAX_LENGTH;
//		final long remainder = length % FileChunkDto.MAX_LENGTH;
//		return (multiplier + (remainder == 0 ? 0 : 1)) * FileChunkDto.MAX_LENGTH;
//	}

	@Override
	public void putFileData(final String path, final long offset, final byte[] fileData) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final DataKey dataKey = cryptree.getDataKeyOrFail(path);
			final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(path, PermissionType.write);
			final byte[] encryptedFileData = encryptAndSign(fileData, dataKey, userRepoKey);
			// TODO maybe we store only one IV per file and derive the chunk's IV from this combined with the offset (and all hashed)? this could save valuable entropy and should still be secure - maybe later.

			// Note: we *MUST* store the file chunks server-side in separate chunk-files/BLOBs permanently!
			// The reason is that the chunks might be bigger (and usually are!) than the unencrypted files.
			final String serverPath = cryptree.getServerPath(path);
			final String unprefixedServerPath = unprefixPath(serverPath); // it's automatically prefixed *again*, thus we must prefix it here (if we don't want to somehow suppress the automatic prefixing, which is probably quite a lot of work).

			logger.debug("putFileData: clientRepositoryId={} serverRepositoryId={} path='{}' serverPath='{}'",
					getClientRepositoryId(), getRepositoryId(), path, serverPath);

			try {
				getRestRepoTransport().putFileData(unprefixedServerPath, offset, encryptedFileData);
			} catch (CollisionException x) {
				transaction.addPostCloseListener(new LocalRepoTransactionPostCloseAdapter() {
					@Override
					public void postRollback(LocalRepoTransactionPostCloseEvent event) {
						final LocalRepoManager localRepoManager = event.getLocalRepoManager();
						try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
							final Cryptree cryptree = getCryptree(transaction);
							cryptree.getLocalRepoStorage().clearTempFileChunkDtos(path);

							transaction.commit();
						}
						((SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData()).scheduleReupload(path);
					}
					@Override
					public void postCommit(LocalRepoTransactionPostCloseEvent event) {
						throw new IllegalStateException("Commit is not allowed, anymore!");
					}
				});
				throw x;
			}

			// Store the "tempFileChunkDto" locally here (no need to encrypt+upload).
			cryptree.getLocalRepoStorage().putTempFileChunkDto(path, offset);

			transaction.commit();
		}
	}

	@Override
	public void endPutFile(final String path, final Date lastModified, final long length, final String sha1) {
		throw new UnsupportedOperationException("Replaced by endPutFile(String path, NormalFileDto fromNormalFileDto)!");
	}

	@Override
	public void endPutFile(final String path, final NormalFileDto fromNormalFileDto) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final CryptoChangeSetDto cryptoChangeSetDto;
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			cryptree.createUnsealedHistoFrameIfNeeded();

			cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoOrFail(path);
			transaction.commit();
		}

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

//			// Calculating the SHA1 of the encrypted data is too complicated and unnecessary. We thus omit it (now optional in CloudStore).
			final String serverPath = cryptree.getServerPath(path);
			final SsNormalFileDto serverNormalFileDto = createNormalFileDtoForPutFile(
					cryptree, path, serverPath,
					assertNotNegative(((SsNormalFileDto) fromNormalFileDto).getLengthWithPadding()) );

			RepoFileDtoWithCurrentHistoCryptoRepoFileDto rfdwchcrfd = new RepoFileDtoWithCurrentHistoCryptoRepoFileDto();

			final CurrentHistoCryptoRepoFileDto chcrfDto = cryptree.createCurrentHistoCryptoRepoFileDto(path, true);
			rfdwchcrfd.setRepoFileDto(serverNormalFileDto);
			rfdwchcrfd.setCurrentHistoCryptoRepoFileDto(chcrfDto);

//			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoOrFail(path);
//			putCryptoChangeSetDto(cryptoChangeSetDto);
//			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			logger.debug("endPutFile: clientRepositoryId={} serverRepositoryId={} path='{}' serverPath='{}'",
					getClientRepositoryId(), getRepositoryId(), path, serverPath);

			getClient().execute(new SsEndPutFile(getRepositoryId().toString(), serverPath, rfdwchcrfd));

			cryptree.getLocalRepoStorage().clearTempFileChunkDtos(path);

			transaction.commit();
		}
	}

	protected static long assertNotNegative(final long value) {
		if (value < 0)
			throw new IllegalArgumentException("value < 0");

		return value;
	}

	protected byte[] encryptAndSign(final byte[] plainText, final DataKey dataKey, final UserRepoKey signingUserRepoKey) {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			try (SignerOutputStream signerOut = new SignerOutputStream(bout, signingUserRepoKey)) {
				final int version = 1;
				signerOut.write(version); // version is inside signed data - better

				signerOut.write(dataKey.cryptoKeyId.toBytes());

				try (
						final EncrypterOutputStream encrypterOut = new EncrypterOutputStream(signerOut,
								getSymmetricCipherTransformation(),
								dataKey.keyParameter, new RandomIvFactory());
				) {
					IOUtil.transferStreamData(new ByteArrayInputStream(plainText), encrypterOut);
				}
			}
			return bout.toByteArray();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	protected Uid readCryptoKeyId(final byte[] cipherText, final UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup) {
		final long startTimestamp = System.currentTimeMillis();
		try {
			final ByteArrayInputStream in = new ByteArrayInputStream(cipherText);
//			in.skip(VerifierInputStream.HEADER_LENGTH);
			try (final VerifierInputStream verifierIn = new VerifierInputStream(in, userRepoKeyPublicKeyLookup);) {
				final int version = verifierIn.read();
				if (version != 1)
					throw new IllegalStateException("version != 1");

				final byte[] bytes = new byte[Uid.LENGTH_BYTES];
				readOrFail(verifierIn, bytes, 0, bytes.length);
				final Uid cryptoKeyId = new Uid(bytes);

				// must read until the end for verification - TODO shall we instead seek directly to the right location and only read without verifying? we'll verify afterwards anyway...
				final byte[] buf = new byte[32 * 1024];
				while (verifierIn.read(buf) >= 0);

				return cryptoKeyId;
			}
		} catch (final IOException x) {
			throw new RuntimeException(x.getMessage() + " cipherText.length=" + cipherText.length, x);
		} finally {
			logger.info("readCryptoKeyId: took {} ms.", System.currentTimeMillis() - startTimestamp);
		}
	}

	protected byte[] verifyAndDecrypt(final byte[] cipherText, final KeyParameter keyParameter, final UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup) {
		try {
			final ByteArrayInputStream in = new ByteArrayInputStream(cipherText);
			try (final VerifierInputStream verifierIn = new VerifierInputStream(in, userRepoKeyPublicKeyLookup);) {
				final int version = verifierIn.read();
				if (version != 1)
					throw new IllegalStateException("version != 1");

				readOrFail(verifierIn, new byte[Uid.LENGTH_BYTES], 0, Uid.LENGTH_BYTES);

				final ByteArrayOutputStream bout = new ByteArrayOutputStream();
				try (final DecrypterInputStream decrypterIn = new DecrypterInputStream(verifierIn, keyParameter);) {
					IOUtil.transferStreamData(decrypterIn, bout);
				}

				final byte[] plainText = bout.toByteArray();
				return plainText;
			}
		} catch (final IOException x) {
			throw new RuntimeException(x.getMessage() + " cipherText.length=" + cipherText.length, x);
		}
	}

	@Override
	public void putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		requireNonNull(cryptoChangeSetDto, "cryptoChangeSetDto");
//		if (! cryptoChangeSetDto.isEmpty()) // We *always* write it, because we need to update LastCryptoKeySyncFromRemoteRepo.emoteRepositoryRevisionSynced.
		getClient().execute(new PutCryptoChangeSetDto(getRepositoryId().toString(), cryptoChangeSetDto));
	}

	@Override
	public void endSyncFromRepository() {
		for (final File file : getDecryptedChangeSetDtoCacheFiles(true)) {
			file.delete();
		}
		getRestRepoTransport().endSyncFromRepository();

		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			// We want to ensure that the root directory is readable. If it isn't this throws an AccessDeniedException.
			// Without this, we would silently sync nothing, if the root is not readable (important: this "root" might be
			// a sub-directory!).
			if (! cryptree.isEmpty())
				cryptree.getDataKeyOrFail("");

			transaction.commit();
		}
	}

	@Override
	public void endSyncToRepository(final long fromLocalRevision) {
		// In case there are no changes to actual files, but only to the crypto-meta-data,
		// there was no beginPutFile(...), makeDirectory(...) or similar. Thus, we must make
		// sure, now, that the crypto-meta-data is uploaded.
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final CryptoChangeSetDto cryptoChangeSetDto;
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			cryptree.sealUnsealedHistoryFrame();

			cryptree.prepareGetCryptoChangeSetDtoWithCryptoRepoFiles(null);
			cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoWithCryptoRepoFiles(null);

			cryptree.removeOrphanedInvitationUserRepoKeyPublicKeys(); // TODO is this location good or should we move this somewhere else?

			transaction.commit();
		}

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			transaction.commit();
		}
		getRestRepoTransport().endSyncToRepository(fromLocalRevision);
	}

	@Override
	public Long getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced() {
		return getClient().execute(new GetLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced(
				getRepositoryId().toString()));
	}

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		return getRestRepoTransport().determineRemoteRootWithoutPathPrefix();
	}

	protected CloudStoreRestClient getClient() {
		return getRestRepoTransport().getClient();
	}

	@Override
	public LocalRepoManager getLocalRepoManager() {
		if (localRepoManager == null) {
			logger.debug("getLocalRepoManager: Creating a new LocalRepoManager.");
			final File localRoot = LocalRepoRegistryImpl.getInstance().getLocalRootOrFail(getClientRepositoryIdOrFail());
			localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		}
		return localRepoManager;
	}

	protected RestRepoTransport getRestRepoTransport() {
		if (restRepoTransport == null) {
			final RestRepoTransportFactory restRepoTransportFactory = getRepoTransportFactory().restRepoTransportFactory;
			restRepoTransport = (RestRepoTransport) restRepoTransportFactory.createRepoTransport(
					requireNonNull(getRemoteRoot(), "getRemoteRoot()"),
					requireNonNull(getClientRepositoryId(), "getClientRepositoryId()"));
		}
		return restRepoTransport;
	}

	@Override
	public final CryptreeRestRepoTransportFactoryImpl getRepoTransportFactory() {
		return (CryptreeRestRepoTransportFactoryImpl) super.getRepoTransportFactory();
	}

	@Override
	public void close() {
		if (localRepoManager != null) {
			logger.debug("close: Closing localRepoManager.");
			localRepoManager.close();
		}
		else
			logger.debug("close: There is no localRepoManager.");

		if (restRepoTransport != null) {
			logger.debug("close: Closing restRepoTransport.");
			restRepoTransport.close();
		}
		else
			logger.debug("close: There is no restRepoTransport.");

		super.close();
	}

	@Override
	public VersionInfoDto getVersionInfoDto() {
		return getRestRepoTransport().getVersionInfoDto();
	}

	@Override
	public void putParentConfigPropSetDto(ConfigPropSetDto parentConfigPropSetDto) {
		throw new UnsupportedOperationException(
				"It is not supported to connect a server-repository to a client-repository's sub-directory! "
				+ "Only the other way around is supported. This method should thus never be invoked.");
	}

	protected File getLocalRepoTmpDir() {
		return getRestRepoTransport().getLocalRepoTmpDir();
	}

	protected File getDecryptedChangeSetDtoCacheFile(final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		String fileName = DECRYPTED_CHANGE_SET_DTO_CACHE_FILE_NAME_PREFIX
				+ getRepositoryId() + "."
				+ lastSyncToRemoteRepoLocalRepositoryRevisionSynced
				+ DECRYPTED_CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX;
		return getLocalRepoTmpDir().createFile(fileName);
	}

	protected List<File> getDecryptedChangeSetDtoCacheFiles(final boolean includeTmpFiles) {
		File[] fileArray = getLocalRepoTmpDir().listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (! file.getName().startsWith(DECRYPTED_CHANGE_SET_DTO_CACHE_FILE_NAME_PREFIX))
					return false;

				if (file.getName().endsWith(DECRYPTED_CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX))
					return true;

				if (includeTmpFiles && file.getName().endsWith(DECRYPTED_CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX + TMP_FILE_NAME_SUFFIX))
					return true;
				else
					return false;
			}
		});
		return fileArray == null ? Collections.<File>emptyList() : Arrays.asList(fileArray);
	}
}
