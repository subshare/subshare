package org.subshare.rest.client.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.net.URL;
import java.util.Date;
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
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDAO;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;

public class CryptreeRepoTransport extends AbstractRepoTransport implements ContextWithLocalRepoManager {
	private static final Logger logger = LoggerFactory.getLogger(CryptreeRepoTransport.class);

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
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public byte[] getFileData(final String path, final long offset, final int length) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void beginPutFile(final String path) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void putFileData(final String path, final long offset, final byte[] fileData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void endPutFile(final String path, final Date lastModified, final long length, final String sha1) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void endSyncFromRepository() {
		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void endSyncToRepository(final long fromLocalRevision) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
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
