package org.subshare.rest.client.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;

public class CryptreeRepoTransport extends AbstractRepoTransport {

	private RestRepoTransport restRepoTransport;

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
		return decryptChangeSetDTO(changeSetDTO);
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
		// TODO Auto-generated method stub
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
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void makeDirectory(final String path, final Date lastModified) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
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
		throw new UnsupportedOperationException("NYI");
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
}
