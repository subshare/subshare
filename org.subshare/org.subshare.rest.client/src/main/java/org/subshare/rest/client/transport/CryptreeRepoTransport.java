package org.subshare.rest.client.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.crypto.CipherTransformation;
import org.subshare.core.crypto.DummyCryptoUtil;
import org.subshare.core.crypto.EncrypterOutputStream;
import org.subshare.core.crypto.RandomIvFactory;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.rest.client.transport.command.EndGetCryptoChangeSetDto;
import org.subshare.rest.client.transport.command.GetCryptoChangeSetDto;
import org.subshare.rest.client.transport.command.PutCryptoChangeSetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
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

	private UserRepoKey userRepoKey;
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
//		return decryptChangeSetDto(changeSetDto);
		// TODO implement this!
		return changeSetDto;
	}

	private void syncCryptoKeysFromRemoteRepo() {
		final CryptoChangeSetDto cryptoChangeSetDto = getClient().execute(new GetCryptoChangeSetDto(getRepositoryId().toString()));
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			try (final Cryptree cryptree = createCryptree(transaction);) {
				cryptree.putCryptoChangeSetDto(cryptoChangeSetDto);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		// In case of successful commit, we notify the server in order to not receive the same changes again.
		getClient().execute(new EndGetCryptoChangeSetDto(getRepositoryId().toString()));
	}

	private ChangeSetDto decryptChangeSetDto(final ChangeSetDto changeSetDto) {
		assertNotNull("changeSetDto", changeSetDto);

		final ChangeSetDto decryptedChangeSetDto = new ChangeSetDto();
		decryptedChangeSetDto.setRepositoryDto(changeSetDto.getRepositoryDto());

		for (final ModificationDto modificationDto : changeSetDto.getModificationDtos()) {
			final ModificationDto decryptedModificationDto = decryptModificationDto(modificationDto);
			decryptedChangeSetDto.getModificationDtos().add(decryptedModificationDto);
		}

		for (final RepoFileDto repoFileDto : changeSetDto.getRepoFileDtos()) {
			final RepoFileDto decryptedRepoFileDto = decryptRepoFileDto(repoFileDto);
			decryptedChangeSetDto.getRepoFileDtos().add(decryptedRepoFileDto);
		}
		return decryptedChangeSetDto;
	}

	private ModificationDto decryptModificationDto(final ModificationDto modificationDto) {
		// TODO implement this!
		throw new UnsupportedOperationException("NYI");
	}

	private RepoFileDto decryptRepoFileDto(final RepoFileDto repoFileDto) {
		final RepoFileDto decryptedRepoFileDto = new RepoFileDto();
		decryptedRepoFileDto.setId(repoFileDto.getId());
//		decryptedRepoFileDto.setLastModified(lastModified); // TODO!
		decryptedRepoFileDto.setLocalRevision(repoFileDto.getLocalRevision());
//		decryptedRepoFileDto.setName(name); // TODO!
		decryptedRepoFileDto.setParentId(repoFileDto.getParentId());
//		return decryptedRepoFileDto;
		// TODO implement this!
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void makeDirectory(final String path, final Date lastModified) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			try (final Cryptree cryptree = createCryptree(transaction);) {
				final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
				putCryptoChangeSetDto(cryptoChangeSetDto);
				cryptree.updateLastCryptoKeySyncToRemoteRepo();

				getRestRepoTransport().makeDirectory(cryptree.getServerPath(path), new Date(0));
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	protected Cryptree createCryptree(final LocalRepoTransaction transaction) {
		if (cryptreeFactory == null)
			cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();

		return cryptreeFactory.createCryptree(transaction, getRepositoryId(), getUserRepoKey());
	}

	protected UserRepoKey getUserRepoKey() {
		if (userRepoKey == null) { // we must use the same key for all operations during one sync - otherwise an attacker might find out which keys belong to the same keyring and thus same owner.
			final UserRepoKeyRing userRepoKeyRing = getUserRepoKeyRing();
			userRepoKey = userRepoKeyRing.getRandomUserRepoKeyOrFail();
		}
		return userRepoKey;
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
		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("NYI");
		return null;
	}

	@Override
	public byte[] getFileData(final String path, final long offset, final int length) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void beginPutFile(final String path) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			try (final Cryptree cryptree = createCryptree(transaction);) {
				final CryptoChangeSetDto cryptoChangeSetDto = cryptree.createOrUpdateCryptoRepoFile(path);
				putCryptoChangeSetDto(cryptoChangeSetDto);
				cryptree.updateLastCryptoKeySyncToRemoteRepo();

				getRestRepoTransport().beginPutFile(cryptree.getServerPath(path));
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void putFileData(final String path, final long offset, final byte[] fileData) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			try (final Cryptree cryptree = createCryptree(transaction);) {
				final KeyParameter dataKey = cryptree.getDataKey(path);
				final byte[] encryptedFileData = encrypt(fileData, dataKey);
				// TODO this should not only be encrypted, but also signed!
				// TODO maybe we store only one IV per file and derive the chunk's IV from this combined with the offset (and all hashed)? this could save valuable entropy and should still be secure - maybe later.

				// TODO we *MUST* store the file chunks server-side in separate chunk-files permanently!
				// The reason is that the chunks might be bigger (and usually are!) than the unencrypted files.
				// Temporarily, we simply multiply the offset with a margin in order to have some reserve.
				getRestRepoTransport().putFileData(cryptree.getServerPath(path), getServerOffset(offset), encryptedFileData);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private byte[] encrypt(final byte[] plainText, final KeyParameter keyParameter) {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			final EncrypterOutputStream encrypterOut = new EncrypterOutputStream(bout,
					CipherTransformation.fromTransformation(DummyCryptoUtil.SYMMETRIC_ENCRYPTION_TRANSFORMATION),
					keyParameter, new RandomIvFactory());
			IOUtil.transferStreamData(new ByteArrayInputStream(plainText), encrypterOut);
			return bout.toByteArray();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	// TODO we *MUST* store the file chunks server-side in separate chunk-files permanently!
	// The reason is that the chunks might be bigger (and usually are!) than the unencrypted files.
	// Temporarily, we simply multiply the offset with a margin in order to have some reserve.
	private static long getServerOffset(final long plainOffset) {
		// 20 % buffer should be sufficient - for now.
		return plainOffset * 120 / 100;
	}

	@Override
	public void endPutFile(final String path, final Date lastModified, final long length, final String sha1) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			try (final Cryptree cryptree = createCryptree(transaction);) {
				final CryptoChangeSetDto cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoOrFail(path);
				putCryptoChangeSetDto(cryptoChangeSetDto);
				cryptree.updateLastCryptoKeySyncToRemoteRepo();

				// Calculating the SHA1 of the encrypted data is too complicated. We thus omit it (now optional in CloudStore).
				getRestRepoTransport().endPutFile(cryptree.getServerPath(path), new Date(0), getServerOffset(length), null);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		assertNotNull("cryptoChangeSetDto", cryptoChangeSetDto);
		getClient().execute(new PutCryptoChangeSetDto(getRepositoryId().toString(), cryptoChangeSetDto));
	}

	@Override
	public void endSyncFromRepository() {
		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void endSyncToRepository(final long fromLocalRevision) {
		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("NYI");
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
