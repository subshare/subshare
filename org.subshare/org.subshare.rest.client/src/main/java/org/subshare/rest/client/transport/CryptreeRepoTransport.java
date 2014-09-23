package org.subshare.rest.client.transport;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.core.crypto.CryptoConfigUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.crypto.DecrypterInputStream;
import org.subshare.core.crypto.EncrypterOutputStream;
import org.subshare.core.crypto.RandomIvFactory;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.LimitedInputStream;
import org.subshare.core.sign.SignableSigner;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.SignerOutputStream;
import org.subshare.core.sign.VerifierInputStream;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.rest.client.transport.command.SsBeginPutFile;
import org.subshare.rest.client.transport.command.SsMakeDirectory;
import org.subshare.rest.client.transport.command.EndGetCryptoChangeSetDto;
import org.subshare.rest.client.transport.command.GetCryptoChangeSetDto;
import org.subshare.rest.client.transport.command.PutCryptoChangeSetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;

public class CryptreeRepoTransport extends AbstractRepoTransport implements ContextWithLocalRepoManager {
	private static final Logger logger = LoggerFactory.getLogger(CryptreeRepoTransport.class);

	private CryptreeFactory cryptreeFactory;
	private RestRepoTransport restRepoTransport;
	private LocalRepoManager localRepoManager;

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
	public void requestRepoConnection(final byte[] publicKey) {
		getRestRepoTransport().requestRepoConnection(publicKey);
	}

	@Override
	public ChangeSetDto getChangeSetDto(final boolean localSync) {
		final ChangeSetDto changeSetDto = getRestRepoTransport().getChangeSetDto(localSync);
		syncCryptoKeysFromRemoteRepo();
		return decryptChangeSetDto(changeSetDto);
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
				final ModificationDto decryptedModificationDto = decryptModificationDto(cryptree, modificationDto);
				if (decryptedModificationDto != null) // if it's null, it could not be decrypted (missing access rights?!) and should be ignored.
					decryptedChangeSetDto.getModificationDtos().add(decryptedModificationDto);

				// TODO verify signature of modificationDTO!
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

					final RepoFileDto decryptedRepoFileDto = decryptRepoFileDto(cryptree, repoFileDto);
					// TODO we should remove the superfluous data to make sure the result looks exactly as it would do in a normal CloudStore sync!

					// if it's null, it could not be decrypted (missing access rights?!) and should be ignored.
					if (decryptedRepoFileDto == null)
						nonDecryptableRepoFileIds.add(repoFileDto.getId());
					else
						decryptedChangeSetDto.getRepoFileDtos().add(decryptedRepoFileDto);
				}
			}
			transaction.commit();
		}

		return decryptedChangeSetDto;
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
		// TODO implement this!
		throw new UnsupportedOperationException("NYI");
	}

	private RepoFileDto decryptRepoFileDto(final Cryptree cryptree, final RepoFileDto repoFileDto) {
		assertNotNull("cryptree", cryptree);
		final String name = assertNotNull("repoFileDto", repoFileDto).getName();

		final Uid cryptoRepoFileId = (repoFileDto.getParentId() == null && name.isEmpty())
				? cryptree.getRootCryptoRepoFileId()
						: new Uid(assertNotNull("repoFileDto.name", name));

		if (cryptoRepoFileId == null) // there is no root before the very first up-sync!
			return null;

		final RepoFileDto decryptedRepoFileDto;
		try {
			decryptedRepoFileDto = cryptree.getDecryptedRepoFileDtoOrFail(cryptoRepoFileId);
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
			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();
			final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(path, PermissionType.write);

			final String serverPath = cryptree.getServerPath(path);

			final SsDirectoryDto directoryDto = new SsDirectoryDto();

			final File f = createFile(serverPath);
			directoryDto.setName(f.getName());

			final File pf = f.getParentFile();
			directoryDto.setParentName(pf == null ? null : pf.getName());

			final SignableSigner signableSigner = new SignableSigner(userRepoKey);
			signableSigner.sign(directoryDto);

			getClient().execute(new SsMakeDirectory(getRepositoryId().toString(), serverPath, new Date(0), directoryDto));

			transaction.commit();
		}
	}

	protected Cryptree getCryptree(final LocalRepoTransaction transaction) {
		if (cryptreeFactory == null)
			cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();

		return cryptreeFactory.getCryptreeOrCreate(transaction, getRepositoryId(), getPathPrefix(), getUserRepoKeyRing());
	}

	protected UserRepoKeyRing getUserRepoKeyRing() {
		return assertNotNull("cryptreeRepoTransportFactory.userRepoKeyRing", getRepoTransportFactory().getUserRepoKeyRing());
	}

	@Override
	public void copy(final String fromPath, final String toPath) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void move(final String fromPath, final String toPath) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(final String path) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public RepoFileDto getRepoFileDto(final String path) {
		final RepoFileDto decryptedRepoFileDto;
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			decryptedRepoFileDto = cryptree.getDecryptedRepoFileDto(path);

			transaction.commit();
		}
		return decryptedRepoFileDto;
	}

	@Override
	public byte[] getFileData(final String path, final long offset, final int length) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final byte[] decryptedFileData;
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final KeyParameter dataKey = cryptree.getDataKeyOrFail(path);
			final String unprefixedServerPath = unprefixPath(cryptree.getServerPath(path)); // it's automatically prefixed *again*, thus we must prefix it here (if we don't want to somehow suppress the automatic prefixing, which is probably quite a lot of work).
			final int serverLength = (int) getServerOffset(length) + 65535; // TODO remove this (both multiplication + addition) as soon as we store the chunks individually on the server!
			final byte[] encryptedFileData = getRestRepoTransport().getFileData(unprefixedServerPath, getServerOffset(offset), serverLength);
			decryptedFileData = verifyAndDecrypt(encryptedFileData, dataKey, cryptree.getUserRepoKeyPublicKeyLookup());

			transaction.commit();
		}
		return decryptedFileData;
	}

	@Override
	public void beginPutFile(final String path) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();
			final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(path, PermissionType.write);

			final String serverPath = cryptree.getServerPath(path);

			final SsNormalFileDto normalFileDto = new SsNormalFileDto();

			final File f = createFile(serverPath);
			normalFileDto.setName(f.getName());

			final File pf = f.getParentFile();
			normalFileDto.setParentName(pf == null ? null : pf.getName());

			final SignableSigner signableSigner = new SignableSigner(userRepoKey);
			signableSigner.sign(normalFileDto);

			getClient().execute(new SsBeginPutFile(getRepositoryId().toString(), serverPath, normalFileDto));

			transaction.commit();
		}
	}

	@Override
	public void putFileData(final String path, final long offset, final byte[] fileData) {
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final KeyParameter dataKey = cryptree.getDataKeyOrFail(path);
			final UserRepoKey userRepoKey = cryptree.getUserRepoKeyOrFail(path, PermissionType.write);
			final byte[] encryptedFileData = encryptAndSign(fileData, dataKey, userRepoKey);
			// TODO maybe we store only one IV per file and derive the chunk's IV from this combined with the offset (and all hashed)? this could save valuable entropy and should still be secure - maybe later.

			// TODO we *MUST* store the file chunks server-side in separate chunk-files permanently!
			// The reason is that the chunks might be bigger (and usually are!) than the unencrypted files.
			// Temporarily, we simply multiply the offset with a margin in order to have some reserve.
			final String unprefixedServerPath = unprefixPath(cryptree.getServerPath(path)); // it's automatically prefixed *again*, thus we must prefix it here (if we don't want to somehow suppress the automatic prefixing, which is probably quite a lot of work).
			getRestRepoTransport().putFileData(unprefixedServerPath, getServerOffset(offset), encryptedFileData);

			transaction.commit();
		}
	}

	protected byte[] encryptAndSign(final byte[] plainText, final KeyParameter keyParameter, final UserRepoKey signingUserRepoKey) {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			try (SignerOutputStream signerOut = new SignerOutputStream(bout, signingUserRepoKey)) {
				try (
						final EncrypterOutputStream encrypterOut = new EncrypterOutputStream(signerOut,
								getSymmetricCipherTransformation(),
								keyParameter, new RandomIvFactory());
				) {
//					encrypterOut.write(1); // version
//
//					encrypterOut.write(plainText.length);
//					encrypterOut.write(plainText.length >> 8);
//					encrypterOut.write(plainText.length >> 16);
//					encrypterOut.write(plainText.length >> 24);

					IOUtil.transferStreamData(new ByteArrayInputStream(plainText), encrypterOut);
// TODO we should maybe keep the plaintext-length and then fill with a padding to hide the real length. maybe do this somewhere else though (so that we can even have additional padding-chunks, not only some padding-bytes within a chunk).
				}
			}

// TODO remove this length! we don't need it anymore once we store the chunks properly separately on the server! But we should still keep the version! So that we have the flexibility to change this again in the future.
			final byte[] raw = bout.toByteArray();
			final byte[] result = new byte[raw.length + 5];
			int idx = -1;
			result[++idx] = 1; // version
			result[++idx] = (byte) raw.length;
			result[++idx] = (byte) (raw.length >> 8);
			result[++idx] = (byte) (raw.length >> 16);
			result[++idx] = (byte) (raw.length >> 24);
			System.arraycopy(raw, 0, result, ++idx, raw.length);
			return result;
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	protected byte[] verifyAndDecrypt(final byte[] cipherText, final KeyParameter keyParameter, final UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup) {
		try {
			final ByteArrayInputStream in = new ByteArrayInputStream(cipherText);
			final int version = in.read();
			if (version != 1)
				throw new IllegalStateException("version != 1");

			final int length = in.read() + (in.read() << 8) + (in.read() << 16) + (in.read() << 24);

			try (final VerifierInputStream verifierIn = new VerifierInputStream(new LimitedInputStream(in, length, length), userRepoKeyPublicKeyLookup);) {
//				final int plainTextLength;
				final ByteArrayOutputStream bout = new ByteArrayOutputStream();
				try (final DecrypterInputStream decrypterIn = new DecrypterInputStream(verifierIn, keyParameter);) {
//					final int version = decrypterIn.read();
//					if (version != 1)
//						throw new IllegalStateException("version != 1");
//
//					plainTextLength = decrypterIn.read() + (decrypterIn.read() << 8) + (decrypterIn.read() << 16) + (decrypterIn.read() << 24);
//
//					IOUtil.transferStreamData(decrypterIn, bout, 0, plainTextLength);
//
//					// Read verifierIn completely, because verification happens only when hitting EOF.
//					final byte[] buf = new byte[4096];
//					while (verifierIn.read(buf) >= 0);
					IOUtil.transferStreamData(decrypterIn, bout);
				}

				final byte[] plainText = bout.toByteArray();
//				if (plainText.length != plainTextLength)
//					throw new IllegalStateException(String.format("plainText.length != plainTextLength :: %s != %s",
//							plainText.length, plainTextLength));

				return plainText;
			}
		} catch (final IOException x) {
			throw new RuntimeException(x.getMessage() + " cipherText.length=" + cipherText.length, x);
		}
	}

	// TODO we *MUST* store the file chunks server-side in separate chunk-files permanently!
	// The reason is that the chunks might be bigger (and usually are!) than the unencrypted files.
	// Temporarily, we simply multiply the offset with a margin in order to have some reserve.
	private static long getServerOffset(final long plainOffset) {
		// 10 % buffer should be sufficient - for now.
		return plainOffset * 110 / 100;
	}

	@Override
	public void endPutFile(final String path, final Date lastModified, final long length, final String sha1) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(transaction);
			final CryptoChangeSetDto cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoOrFail(path);
			putCryptoChangeSetDto(cryptoChangeSetDto);
			cryptree.updateLastCryptoKeySyncToRemoteRepo();

			// Calculating the SHA1 of the encrypted data is too complicated. We thus omit it (now optional in CloudStore).
			final String unprefixedServerPath = unprefixPath(cryptree.getServerPath(path)); // it's automatically prefixed *again*, thus we must prefix it here (if we don't want to somehow suppress the automatic prefixing, which is probably quite a lot of work).
			final long serverLength = getServerOffset(length) + 65535; // TODO remove this (both multiplication + addition) as soon as we store the chunks individually on the server!
			getRestRepoTransport().endPutFile(unprefixedServerPath, new Date(0), serverLength, null);

			transaction.commit();
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
			final File localRoot = LocalRepoRegistry.getInstance().getLocalRoot(getClientRepositoryIdOrFail());
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
	public final CryptreeRepoTransportFactory getRepoTransportFactory() {
		return (CryptreeRepoTransportFactory) super.getRepoTransportFactory();
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
