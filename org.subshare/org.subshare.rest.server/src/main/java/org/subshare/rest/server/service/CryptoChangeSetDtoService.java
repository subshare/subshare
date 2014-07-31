package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.rest.server.service.AbstractServiceWithRepoToRepoAuth;

@Path("_CryptoChangeSetDto/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class CryptoChangeSetDtoService extends AbstractServiceWithRepoToRepoAuth {

	private static final Logger logger = LoggerFactory.getLogger(CryptoChangeSetDtoService.class);

	private LocalRepoTransaction transaction;

	@GET
	public CryptoChangeSetDto getCryptoChangeSetDto() {
		CryptoChangeSetDto cryptoChangeSetDto;
		final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			final UUID clientRepositoryId = assertNotNull("clientRepositoryId", repoTransport.getClientRepositoryId());
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			transaction = localRepoManager.beginReadTransaction();
			try {
				final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
				try (
						Cryptree cryptree = cryptreeFactory.createCryptree(transaction, clientRepositoryId);
				) {
					cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoWithCryptoRepoFiles();
					cryptree.updateLastCryptoKeySyncToRemoteRepo();
				}
				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			repoTransport.close();
		}
		return cryptoChangeSetDto;
	}

}
