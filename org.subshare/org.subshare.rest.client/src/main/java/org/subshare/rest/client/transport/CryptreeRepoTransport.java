package org.subshare.rest.client.transport;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.UUID;

import org.subshare.core.dto.CryptoKeyChangeSetDTO;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.CryptreeNode;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.rest.client.transport.command.GetCryptoKeyChangeSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDAO;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;

public class CryptreeRepoTransport extends AbstractRepoTransport implements ContextWithLocalRepoManager {
	private static final Logger logger = LoggerFactory.getLogger(CryptreeRepoTransport.class);
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private RestRepoTransport restRepoTransport;
	private LocalRepoManager localRepoManager;

	@Override
	public RepositoryDTO getRepositoryDTO() {
		return getRestRepoTransport().getRepositoryDTO();
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
	public ChangeSetDTO getChangeSetDTO(final boolean localSync) {
		final ChangeSetDTO changeSetDTO = getRestRepoTransport().getChangeSetDTO(localSync);
//		syncCryptoKeysFromRemoteRepo();
//		return decryptChangeSetDTO(changeSetDTO);
		// TODO implement this!
		return changeSetDTO;
	}

	private void syncCryptoKeysFromRemoteRepo() {
		final CryptoKeyChangeSetDTO cryptoKeyChangeSetDTO = getClient().execute(new GetCryptoKeyChangeSetDTO(getRepositoryId().toString()));
		// TODO implement this!
//		throw new UnsupportedOperationException("NYI");
	}

	private ChangeSetDTO decryptChangeSetDTO(final ChangeSetDTO changeSetDTO) {
		assertNotNull("changeSetDTO", changeSetDTO);

		final ChangeSetDTO decryptedChangeSetDTO = new ChangeSetDTO();
		decryptedChangeSetDTO.setRepositoryDTO(changeSetDTO.getRepositoryDTO());

		for (final ModificationDTO modificationDTO : changeSetDTO.getModificationDTOs()) {
			final ModificationDTO decryptedModificationDTO = decryptModificationDTO(modificationDTO);
			decryptedChangeSetDTO.getModificationDTOs().add(decryptedModificationDTO);
		}

		for (final RepoFileDTO repoFileDTO : changeSetDTO.getRepoFileDTOs()) {
			final RepoFileDTO decryptedRepoFileDTO = decryptRepoFileDTO(repoFileDTO);
			decryptedChangeSetDTO.getRepoFileDTOs().add(decryptedRepoFileDTO);
		}
		return decryptedChangeSetDTO;
	}

	private ModificationDTO decryptModificationDTO(final ModificationDTO modificationDTO) {
		// TODO implement this!
		throw new UnsupportedOperationException("NYI");
	}

	private RepoFileDTO decryptRepoFileDTO(final RepoFileDTO repoFileDTO) {
		final RepoFileDTO decryptedRepoFileDTO = new RepoFileDTO();
		decryptedRepoFileDTO.setId(repoFileDTO.getId());
//		decryptedRepoFileDTO.setLastModified(lastModified); // TODO!
		decryptedRepoFileDTO.setLocalRevision(repoFileDTO.getLocalRevision());
//		decryptedRepoFileDTO.setName(name); // TODO!
		decryptedRepoFileDTO.setParentId(repoFileDTO.getParentId());
//		return decryptedRepoFileDTO;
		// TODO implement this!
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void makeDirectory(final String path, final Date lastModified) {
		// TODO handle path correctly => pathPrefix on both sides possible!!!
		final LocalRepoManager localRepoManager = getLocalRepoManager();
		final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			final RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
			final RepoFile repoFile = repoFileDAO.getRepoFile(localRepoManager.getLocalRoot(), new File(localRepoManager.getLocalRoot(), path));
			assertNotNull("repoFile", repoFile);

			final CryptreeNode cryptreeNode = new CryptreeNode(getUserRepoKeyRing(), transaction, repoFile);
			final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFileOrCreate(true);

			getRestRepoTransport().makeDirectory(getServerPath(path), new Date(0));

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
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
	public RepoFileDTO getRepoFileDTO(final String path) {
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
			final RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
			final RepoFile repoFile = repoFileDAO.getRepoFile(localRepoManager.getLocalRoot(), new File(localRepoManager.getLocalRoot(), path));
			assertNotNull("repoFile", repoFile);

			final CryptreeNode cryptreeNode = new CryptreeNode(getUserRepoKeyRing(), transaction, repoFile);
			final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFileOrCreate(true);

			getRestRepoTransport().beginPutFile(getServerPath(path));

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
			final RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
			final RepoFile repoFile = repoFileDAO.getRepoFile(localRepoManager.getLocalRoot(), new File(localRepoManager.getLocalRoot(), path));
			assertNotNull("repoFile", repoFile);

			final CryptreeNode cryptreeNode = new CryptreeNode(getUserRepoKeyRing(), transaction, repoFile);
//			final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFileOrCreate(false);

			final byte[] encryptedFileData = encrypt(fileData, cryptreeNode.getDataKey());

			// TODO we *MUST* store the file chunks server-side in separate chunk-files permanently!
			// The reason is that the chunks might be bigger (and usually are!) than the unencrypted files.
			// Temporarily, we simply multiply the offset with a margin in order to have some reserve.
			getRestRepoTransport().putFileData(getServerPath(path), getServerOffset(offset), encryptedFileData);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
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
			final RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
			final RepoFile repoFile = repoFileDAO.getRepoFile(localRepoManager.getLocalRoot(), new File(localRepoManager.getLocalRoot(), path));
			assertNotNull("repoFile", repoFile);

			final CryptreeNode cryptreeNode = new CryptreeNode(getUserRepoKeyRing(), transaction, repoFile);
			final CryptoRepoFile cryptoRepoFile = cryptreeNode.getCryptoRepoFileOrCreate(true);

			// TODO need to calculate real SHA1 of encrypted data - is that feasable?! Or should we better make it optional?
			getRestRepoTransport().endPutFile(getServerPath(path), new Date(0), getServerOffset(length), "DUMMY");

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
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

	private String getServerPath(final String plainPath) {
		// TODO handle path correctly => pathPrefix on both sides possible!!! Maybe do this here?!
		assertNotNull("plainPath", plainPath);
		final StringBuilder result = new StringBuilder();
		final StringTokenizer st = new StringTokenizer(plainPath, "/", true);
		while (st.hasMoreTokens()) {
			final String token = st.nextToken();
			if ("/".equals(token))
				result.append(token);
			else
				result.append(getServerFileName(token));
		}
		return result.toString();
	}

	/**
	 * Gets the server-side name of the file named {@code plainName} locally.
	 * <p>
	 * The real (plain-text) name of the file is hashed with a repository-dependent salt. Thus the same name
	 * will have the same hash in the same repository, but a different hash in other repositories.
	 * <p>
	 * In other words: Invoking this method multiple times with the same name and the same (remote)
	 * repository is guaranteed to return the same result. Invoking it with the same name but another
	 * (remote) repository, it is guaranteed to return a different result.
	 * <p>
	 * @param plainName the local name (in plain-text) of the file. Must not be <code>null</code>.
	 * @return the server-side name of the file (a secure hash).
	 * @see #getServerPath(String)
	 */
	private String getServerFileName(final String plainName) {
		assertNotNull("plainName", plainName);
		// TODO re-implement (I'm sure I already implemented this for another project before) a CombiInputStream combining multiple streams, so that we don't need to copy things around!
		final byte[] repositoryIdBytes = new Uid(getRepositoryId().getMostSignificantBits(), getRepositoryId().getLeastSignificantBits()).toBytes();
		final byte[] plainNameBytes = plainName.getBytes(UTF8);
		final byte[] combined = new byte[repositoryIdBytes.length + plainNameBytes.length];
		System.arraycopy(repositoryIdBytes, 0, combined, 0, repositoryIdBytes.length);
		System.arraycopy(plainNameBytes, 0, combined, repositoryIdBytes.length, plainNameBytes.length);
		final String sha1 = HashUtil.sha1(combined);
		return sha1;
	}
}
