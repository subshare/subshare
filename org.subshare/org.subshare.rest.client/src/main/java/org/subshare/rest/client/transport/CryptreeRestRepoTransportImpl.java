package org.subshare.rest.client.transport;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static org.subshare.core.crypto.CryptoConfigUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
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
import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.crypto.DecrypterInputStream;
import org.subshare.core.crypto.EncrypterOutputStream;
import org.subshare.core.crypto.RandomIvFactory;
import org.subshare.core.dto.CreateRepositoryRequestDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.RepoFileDtoWithCryptoRepoFileOnServerDto;
import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.dto.SsRequestRepoConnectionRepositoryDto;
import org.subshare.core.pgp.PgpKey;
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
import org.subshare.rest.client.transport.request.GetHistoFileData;
import org.subshare.rest.client.transport.request.PutCryptoChangeSetDto;
import org.subshare.rest.client.transport.request.SsBeginPutFile;
import org.subshare.rest.client.transport.request.SsDelete;
import org.subshare.rest.client.transport.request.SsEndPutFile;
import org.subshare.rest.client.transport.request.SsMakeDirectory;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.request.RequestRepoConnection;

public class CryptreeRestRepoTransportImpl extends AbstractRepoTransport implements CryptreeRestRepoTransport, ContextWithLocalRepoManager {
	private static final Logger logger = LoggerFactory.getLogger(CryptreeRestRepoTransportImpl.class);

	private CryptreeFactory cryptreeFactory;
	private RestRepoTransport restRepoTransport;
	private LocalRepoManager localRepoManager;
	private UserRepoKeyRing userRepoKeyRing;

	@Override
	public RepositoryDto getRepositoryDto() {
		return getRestRepoTransport().getRepositoryDto();
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
		assertNotNull("pgpKey", pgpKey);
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
	public ChangeSetDto getChangeSetDto(final boolean localSync) {
		final ChangeSetDto changeSetDto = getRestRepoTransport().getChangeSetDto(localSync);
		syncCryptoKeysFromRemoteRepo();
		return decryptChangeSetDto(changeSetDto);
	}

	@Override
	public void prepareForChangeSetDto(ChangeSetDto changeSetDto) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createHistoCryptoRepoFilesForDeletedCryptoRepoFiles();
			if (cryptoChangeSetDto != null) {
				putCryptoChangeSetDto(cryptoChangeSetDto);
				cryptree.updateLastCryptoKeySyncToRemoteRepo();
			}
			transaction.commit();
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
		final CryptoChangeSetDto cryptoChangeSetDto = getClient().execute(new GetCryptoChangeSetDto(getRepositoryId().toString()));
		final LocalRepoManager localRepoManager = getLocalRepoManager();

		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			cryptree.initLocalRepositoryType();
			cryptree.putCryptoChangeSetDto(cryptoChangeSetDto);
			transaction.commit();
		}
		// In case of successful commit, we notify the server in order to not receive the same changes again.
		getClient().execute(new EndGetCryptoChangeSetDto(getRepositoryId().toString()));
	}

	private ChangeSetDto decryptChangeSetDto(final ChangeSetDto changeSetDto) {
		assertNotNull("changeSetDto", changeSetDto);
		final ChangeSetDto decryptedChangeSetDto = new ChangeSetDto();

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

			final Set<Long> nonDecryptableRepoFileIds = new HashSet<Long>();

			final RepoFileDtoTreeNode tree = RepoFileDtoTreeNode.createTree(changeSetDto.getRepoFileDtos());
			if (tree != null) {
				for (final RepoFileDtoTreeNode node : tree) {
					final RepoFileDto repoFileDto = node.getRepoFileDto();

					// We silently ignore those missing signatures that are allowed to be ignored silently ;-)
					verifySignatureAndTreeStructureAndPermission(cryptree, signableVerifier, node);

					if (nonDecryptableRepoFileIds.contains(repoFileDto.getParentId())) {
						nonDecryptableRepoFileIds.add(repoFileDto.getId()); // transitive for all children and children's children
						continue;
					}

					final RepoFileDto decryptedRepoFileDto = decryptRepoFileDtoOnServer(cryptree, repoFileDto);
					// TODO we should remove the superfluous data to make sure the result looks exactly as it would do in a normal CloudStore sync!

					// if it's null, it could not be decrypted (missing access rights?!) and should be ignored.
					if (decryptedRepoFileDto == null)
						nonDecryptableRepoFileIds.add(repoFileDto.getId());
					else
						decryptedChangeSetDto.getRepoFileDtos().add(decryptedRepoFileDto);
				}
			}

			cryptree.createSyntheticDeleteModifications(decryptedChangeSetDto);

			transaction.commit();
		}

		return decryptedChangeSetDto;
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
		assertNotNull("cryptree", cryptree);
		assertNotNull("repoFileDto", repoFileDto);
		final SsDirectoryDto ssDirectoryDto = (SsDirectoryDto) repoFileDto;

		if (! repoFileDto.getName().isEmpty())
			throw new IllegalStateException(String.format("repoFileDto.name is not an empty String, but: '%s'", repoFileDto.getName()));

		if (cryptree.getRemotePathPrefix().isEmpty()) {
			if (ssDirectoryDto.getRealName() != null)
				throw new IllegalStateException(String.format("ssDirectoryDto.realName is not null, but: '%s'", ssDirectoryDto.getRealName()));
		}
		else {
			final Uid virtualRootCryptoRepoFileId = assertNotNull("cryptree.getCryptoRepoFileIdForRemotePathPrefixOrFail()", cryptree.getCryptoRepoFileIdForRemotePathPrefixOrFail());
			if (! virtualRootCryptoRepoFileId.toString().equals(ssDirectoryDto.getRealName()))
				throw new IllegalStateException(String.format("virtualRootCryptoRepoFileId != ssDirectoryDto.realName :: '%s' != '%s'",
						virtualRootCryptoRepoFileId, ssDirectoryDto.getRealName()));
		}
	}

	private void assertRepoFileParentNameMatchesParentRepoFileName(final RepoFileDtoTreeNode node) throws IllegalStateException {
		assertNotNull("node", node);
		final SsRepoFileDto ssRepoFileDto = (SsRepoFileDto) node.getRepoFileDto();
		final String childParentName = assertNotNull("ssRepoFileDto.parentName", ssRepoFileDto.getParentName());
		final RepoFileDtoTreeNode parent = assertNotNull("node.parent", node.getParent());
		final RepoFileDto parentRepoFileDto = assertNotNull("node.parent.repoFileDto", parent.getRepoFileDto());
		final SsDirectoryDto parentDirectoryDto = (SsDirectoryDto) parentRepoFileDto;
		final String parentRealName = (isVirtualRootWithDifferentRealName(parentDirectoryDto)
				? parentDirectoryDto.getRealName() : parentDirectoryDto.getName());

		if (!childParentName.equals(parentRealName))
			throw new IllegalStateException(String.format("RepoFileDtoTreeNode tree structure does not match signed structure! ssRepoFileDto.parentName != parentRealName :: '%s' != '%s'",
					childParentName, parentRealName));
	}

	private boolean isVirtualRootWithDifferentRealName(final SsDirectoryDto ssDirectoryDto) {
		assertNotNull("ssDirectoryDto", ssDirectoryDto);
		return ssDirectoryDto.getRealName() != null
				&& ssDirectoryDto.getName().isEmpty()
				&& ssDirectoryDto.getParentId() == null;
	}

	private ModificationDto decryptModificationDto(final Cryptree cryptree, final ModificationDto modificationDto) {
		assertNotNull("modificationDto", modificationDto);

		if (modificationDto instanceof SsDeleteModificationDto)
			return decryptDeleteModificationDto(cryptree, (SsDeleteModificationDto) modificationDto);

		// TODO implement this for other modifications.
		throw new UnsupportedOperationException("NYI");
	}

	private ModificationDto decryptDeleteModificationDto(final Cryptree cryptree, final SsDeleteModificationDto modificationDto) {
		assertNotNull("modificationDto", modificationDto);

		final String localPath = cryptree.getLocalPath(modificationDto.getServerPath());
		modificationDto.setPath(localPath);
		return modificationDto;
	}

	private RepoFileDto decryptRepoFileDtoOnServer(final Cryptree cryptree, final RepoFileDto repoFileDto) {
		assertNotNull("cryptree", cryptree);
		final String name = assertNotNull("repoFileDto", repoFileDto).getName();

		final Uid cryptoRepoFileId = (repoFileDto.getParentId() == null && name.isEmpty())
				? cryptree.getRootCryptoRepoFileId()
						: new Uid(assertNotNull("repoFileDto.name", name));

		if (cryptoRepoFileId == null) // there is no root before the very first up-sync!
			return null;

		final RepoFileDto decryptedRepoFileDto;
		try {
			decryptedRepoFileDto = cryptree.getDecryptedRepoFileOnServerDtoOrFail(cryptoRepoFileId);
		} catch (final AccessDeniedException x) {
			return null;
		}
		decryptedRepoFileDto.setId(repoFileDto.getId());
		decryptedRepoFileDto.setParentId(repoFileDto.getParentId());
		return decryptedRepoFileDto;
	}

	@Override
	public void makeDirectory(final String path, final Date lastModified) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			cryptree.createUnsealedHistoFrameIfNeeded();

			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

//			createUnsealedCurrentHistoryFrameDtoIfNeeded(cryptree);
			final HistoCryptoRepoFileDto histoCryptoRepoFileDto = cryptree.createHistoCryptoRepoFileDto(path);

			final String serverPath = cryptree.getServerPath(path);
			SsDirectoryDto directoryDto = createDirectoryDtoForMakeDirectory(cryptree, path, serverPath);

			RepoFileDtoWithCryptoRepoFileOnServerDto rfdwcrfosd = new RepoFileDtoWithCryptoRepoFileOnServerDto();
			rfdwcrfosd.setRepoFileDto(directoryDto);
			rfdwcrfosd.setCryptoRepoFileOnServerDto(histoCryptoRepoFileDto);

//			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
//			putCryptoChangeSetDto(cryptoChangeSetDto);
//			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			getClient().execute(new SsMakeDirectory(getRepositoryId().toString(), serverPath, rfdwcrfosd));

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

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(directoryDto);
		return directoryDto;
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
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			cryptree.sign(deleteModificationDto);
			deleteModificationDto.setPath(null); // path is *not* signed and *must* *not* be transferred to the server! It is secret!
			transaction.commit();
		}
		getClient().execute(new SsDelete(getRepositoryId().toString(), deleteModificationDto));
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
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

//			createUnsealedCurrentHistoryFrameDtoIfNeeded(cryptree);

			final String serverPath = cryptree.getServerPath(path);
			final SsNormalFileDto serverNormalFileDto = createNormalFileDtoForPutFile(
					cryptree, path, serverPath,
					assertNotNegative(fromNormalFileDto.getLengthWithPadding()) );
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

	protected SsNormalFileDto createNormalFileDtoForPutFile(final Cryptree cryptree, final String localPath, final String serverPath, long length) {
		final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(localPath, PermissionType.write);

//		length = roundLengthToChunkMaxLength(length);

		final SsNormalFileDto normalFileDto = new SsNormalFileDto();

		final File f = createFile(serverPath);
		normalFileDto.setName(f.getName());

		final File pf = f.getParentFile();
		normalFileDto.setParentName(pf == null ? null : pf.getName());

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(normalFileDto);

		// Calculating the SHA1 of the encrypted data is too complicated and unnecessary. We thus omit it (now optional in CloudStore).
		normalFileDto.setLength(length);
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
			final String unprefixedServerPath = unprefixPath(cryptree.getServerPath(path)); // it's automatically prefixed *again*, thus we must prefix it here (if we don't want to somehow suppress the automatic prefixing, which is probably quite a lot of work).
			getRestRepoTransport().putFileData(unprefixedServerPath, offset, encryptedFileData);

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
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			cryptree.createUnsealedHistoFrameIfNeeded();

			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoOrFail(path);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

//			// Calculating the SHA1 of the encrypted data is too complicated and unnecessary. We thus omit it (now optional in CloudStore).
			final String serverPath = cryptree.getServerPath(path);
			final SsNormalFileDto serverNormalFileDto = createNormalFileDtoForPutFile(
					cryptree, path, serverPath,
					assertNotNegative(((SsNormalFileDto) fromNormalFileDto).getLengthWithPadding()) );

			RepoFileDtoWithCryptoRepoFileOnServerDto rfdwcrfosd = new RepoFileDtoWithCryptoRepoFileOnServerDto();

			final HistoCryptoRepoFileDto histoCryptoRepoFileDto = cryptree.createHistoCryptoRepoFileDto(path);
			rfdwcrfosd.setRepoFileDto(serverNormalFileDto);
			rfdwcrfosd.setCryptoRepoFileOnServerDto(histoCryptoRepoFileDto);

//			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoOrFail(path);
//			putCryptoChangeSetDto(cryptoChangeSetDto);
//			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			getClient().execute(new SsEndPutFile(getRepositoryId().toString(), serverPath, rfdwcrfosd));

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

	private void putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		assertNotNull("cryptoChangeSetDto", cryptoChangeSetDto);
		if (! cryptoChangeSetDto.isEmpty())
			getClient().execute(new PutCryptoChangeSetDto(getRepositoryId().toString(), cryptoChangeSetDto));
	}

	@Override
	public void endSyncFromRepository() {
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
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);

			cryptree.sealUnsealedHistoryFrame();

			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoWithCryptoRepoFiles();
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			transaction.commit();
		}
		getRestRepoTransport().endSyncToRepository(fromLocalRevision);
	}

	@Override
	public void makeSymlink(final String path, final String target, final Date lastModified) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
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
			final File localRoot = LocalRepoRegistryImpl.getInstance().getLocalRoot(getClientRepositoryIdOrFail());
			localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		}
		return localRepoManager;
	}

	protected RestRepoTransport getRestRepoTransport() {
		if (restRepoTransport == null) {
			final RestRepoTransportFactory restRepoTransportFactory = getRepoTransportFactory().restRepoTransportFactory;
			restRepoTransport = (RestRepoTransport) restRepoTransportFactory.createRepoTransport(
					assertNotNull("getRemoteRoot()", getRemoteRoot()),
					assertNotNull("getClientRepositoryId()", getClientRepositoryId()));
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

}
