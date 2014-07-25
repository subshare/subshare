package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collection;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CryptoKeyChangeSetDTO;
import org.subshare.core.dto.CryptoKeyDTO;
import org.subshare.core.dto.CryptoLinkDTO;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDAO;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepoDAO;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.rest.server.service.AbstractServiceWithRepoToRepoAuth;

@Path("_CryptoKeyChangeSetDTO/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class CryptoKeyChangeSetDTOService extends AbstractServiceWithRepoToRepoAuth {

	private static final Logger logger = LoggerFactory.getLogger(CryptoKeyChangeSetDTOService.class);

	private LocalRepoTransaction transaction;

	@GET
	public CryptoKeyChangeSetDTO getCryptoKeyChangeSetDTO() {
		final CryptoKeyChangeSetDTO cryptoKeyChangeSetDTO = new CryptoKeyChangeSetDTO();
		final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			final UUID clientRepositoryId = assertNotNull("clientRepositoryId", repoTransport.getClientRepositoryId());
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			transaction = localRepoManager.beginReadTransaction();
			try {
				final RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
				final LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);

				final RemoteRepository toRemoteRepository = remoteRepositoryDAO.getRemoteRepositoryOrFail(clientRepositoryId);

				final LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepo(toRemoteRepository);
				final long localRepositoryRevisionInProgress = lastSyncToRemoteRepo == null ? -1 : lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress();
				if (localRepositoryRevisionInProgress < 0)
					throw new IllegalStateException("localRepositoryRevisionInProgress < 0 :: There is no sync in progress right now!");

				// First links then keys, because we query all changed *after* a certain localRevision - and not in a range.
				// Thus, we might find newer keys when querying them after the links. Since the links reference the keys
				// (collection is mapped-by) and we currently don't delete anything, this guarantees that all references can
				// be fulfilled on the remote side.
				populateCryptoLinkDTOs(cryptoKeyChangeSetDTO, lastSyncToRemoteRepo);
				populateCryptoKeyDTOs(cryptoKeyChangeSetDTO, lastSyncToRemoteRepo);

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			repoTransport.close();
		}
		return cryptoKeyChangeSetDTO;
	}

	private void populateCryptoLinkDTOs(final CryptoKeyChangeSetDTO cryptoKeyChangeSetDTO, final LastSyncToRemoteRepo lastSyncToRemoteRepo) {
		final CryptoLinkDAO cryptoLinkDAO = transaction.getDAO(CryptoLinkDAO.class);

		final Collection<CryptoLink> cryptoLinks = cryptoLinkDAO.getCryptoLinksChangedAfter(
				lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final CryptoLink cryptoLink : cryptoLinks)
			cryptoKeyChangeSetDTO.getCryptoLinkDTOs().add(toCryptoLinkDTO(cryptoLink));
	}

	private void populateCryptoKeyDTOs(final CryptoKeyChangeSetDTO cryptoKeyChangeSetDTO, final LastSyncToRemoteRepo lastSyncToRemoteRepo) {
		final CryptoKeyDAO cryptoKeyDAO = transaction.getDAO(CryptoKeyDAO.class);

		final Collection<CryptoKey> cryptoKeys = cryptoKeyDAO.getCryptoKeysChangedAfter(
				lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());

		for (final CryptoKey cryptoKey : cryptoKeys)
			cryptoKeyChangeSetDTO.getCryptoKeyDTOs().add(toCryptoKeyDTO(cryptoKey));
	}

	private CryptoLinkDTO toCryptoLinkDTO(final CryptoLink cryptoLink) {
		final CryptoLinkDTO cryptoLinkDTO = new CryptoLinkDTO();
		cryptoLinkDTO.setCryptoLinkId(cryptoLink.getCryptoLinkId());

		final CryptoKey fromCryptoKey = cryptoLink.getFromCryptoKey();
		cryptoLinkDTO.setFromCryptoKeyId(fromCryptoKey == null ? null : fromCryptoKey.getCryptoKeyId());

		cryptoLinkDTO.setLocalRevision(cryptoLink.getLocalRevision());
		cryptoLinkDTO.setToCryptoKeyData(cryptoLink.getToCryptoKeyData());
		cryptoLinkDTO.setToCryptoKeyId(cryptoLink.getToCryptoKey().getCryptoKeyId());
		cryptoLinkDTO.setToCryptoKeyPart(cryptoLink.getToCryptoKeyPart());

		return cryptoLinkDTO;
	}

	private CryptoKeyDTO toCryptoKeyDTO(final CryptoKey cryptoKey) {
		final CryptoKeyDTO cryptoKeyDTO = new CryptoKeyDTO();
		cryptoKeyDTO.setCryptoKeyId(cryptoKey.getCryptoKeyId());
		cryptoKeyDTO.setCryptoKeyRole(cryptoKey.getCryptoKeyRole());
		cryptoKeyDTO.setCryptoKeyType(cryptoKey.getCryptoKeyType());

		return cryptoKeyDTO;
	}
}
