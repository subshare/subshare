package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
		try (
				final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		) {
			final UUID clientRepositoryId = assertNotNull("clientRepositoryId", repoTransport.getClientRepositoryId());
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			transaction = localRepoManager.beginWriteTransaction(); // We write LastCryptoKeySyncToRemoteRepo.
			try {
				final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
				try (final Cryptree cryptree = cryptreeFactory.createCryptree(transaction, clientRepositoryId);) {
					cryptree.initLocalRepositoryType();
					cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoWithCryptoRepoFiles();
				}
				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		}
		return cryptoChangeSetDto;
	}

	@POST
	@Path("endGet")
	public void endGetCryptoChangeSetDto() {
		try (
				final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		) {
			final UUID clientRepositoryId = assertNotNull("clientRepositoryId", repoTransport.getClientRepositoryId());
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			transaction = localRepoManager.beginWriteTransaction();
			try {
				final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
				try (final Cryptree cryptree = cryptreeFactory.createCryptree(transaction, clientRepositoryId);) {
					cryptree.updateLastCryptoKeySyncToRemoteRepo();
				}
				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		}
	}

	@PUT
	public void putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		assertNotNull("cryptoChangeSetDto", cryptoChangeSetDto);
		try (
				final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		) {
			final UUID clientRepositoryId = assertNotNull("clientRepositoryId", repoTransport.getClientRepositoryId());
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			transaction = localRepoManager.beginWriteTransaction();
			try {
				final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
				try (final Cryptree cryptree = cryptreeFactory.createCryptree(transaction, clientRepositoryId);) {
					cryptree.putCryptoChangeSetDto(cryptoChangeSetDto);
				}
				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		}
	}
}
