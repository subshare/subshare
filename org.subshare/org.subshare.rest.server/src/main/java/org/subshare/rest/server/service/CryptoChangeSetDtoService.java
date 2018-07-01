package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;
import java.util.concurrent.Callable;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.CryptoChangeSetDto;

import co.codewizards.cloudstore.core.concurrent.CallableProvider;
import co.codewizards.cloudstore.core.concurrent.DeferrableExecutor;
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

	@GET
	public CryptoChangeSetDto getCryptoChangeSetDto(@QueryParam("lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced") final Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced) {
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			final String callIdentifier = CryptoChangeSetDtoService.class.getName() + ".getCryptoChangeSetDto|" + repositoryName + '|' + getAuth().getUserName() + '|' + lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<CryptoChangeSetDto>() {
						@Override
						public Callable<CryptoChangeSetDto> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							return new Callable<CryptoChangeSetDto>() {
								@Override
								public CryptoChangeSetDto call() throws Exception { // called *A*synchronously
									try {
										final CryptoChangeSetDto cryptoChangeSetDto = getCryptoChangeSetDto(rt, lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);
										return cryptoChangeSetDto;
									} finally {
										rt.close();
									}
								}
							};
						}
					});
		} finally {
			if (repoTransport[0] != null)
				repoTransport[0].close();
		}
	}

	protected CryptoChangeSetDto getCryptoChangeSetDto(final RepoTransport repoTransport, final Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced) {
		final UUID clientRepositoryId = assertNotNull(repoTransport.getClientRepositoryId(), "clientRepositoryId");
		final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
		// We *write* LastCryptoKeySyncToRemoteRepo in this *short* tx.
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction()) {
			final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
			final Cryptree cryptree = cryptreeFactory.getCryptreeOrCreate(transaction, clientRepositoryId);
			cryptree.initLocalRepositoryType();
			cryptree.prepareGetCryptoChangeSetDtoWithCryptoRepoFiles(
					lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);

			transaction.commit();
		}

		final CryptoChangeSetDto cryptoChangeSetDto;
		// We write LastCryptoKeySyncToRemoteRepo already before => *read*-only.
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction()) {
			final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
			final Cryptree cryptree = cryptreeFactory.getCryptreeOrCreate(transaction, clientRepositoryId);
			cryptoChangeSetDto = cryptree.getCryptoChangeSetDtoWithCryptoRepoFiles(
					lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);

			transaction.commit();
		}

//		final List<CryptoChangeSetDto> outCryptoChangeSetDtos = CryptoChangeSetDtoSplitter
//				.createInstance(cryptoChangeSetDto)
//				.setDestroyInput(true)
//				.split()
//				.getOutCryptoChangeSetDtos();
		// TODO check, if splitting is needed
		// TODO if yes, create files and throw exception
		// TODO provide API to retrieve files.

		return cryptoChangeSetDto;
	}

	@POST
	@Path("endGet")
	public void endGetCryptoChangeSetDto() {
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			final UUID clientRepositoryId = assertNotNull(repoTransport.getClientRepositoryId(), "clientRepositoryId");
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction()) {
				final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
				final Cryptree cryptree = cryptreeFactory.getCryptreeOrCreate(transaction, clientRepositoryId);
				cryptree.updateLastCryptoKeySyncToRemoteRepo();

				transaction.commit();
			}
		}
	}

	@PUT
	public void putCryptoChangeSetDto(final CryptoChangeSetDto cryptoChangeSetDto) {
		assertNotNull(cryptoChangeSetDto, "cryptoChangeSetDto");
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			final UUID clientRepositoryId = assertNotNull(repoTransport.getClientRepositoryId(), "clientRepositoryId");
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction()) {
				final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
				final Cryptree cryptree = cryptreeFactory.getCryptreeOrCreate(transaction, clientRepositoryId);
				cryptree.putCryptoChangeSetDto(cryptoChangeSetDto);

				transaction.commit();
			}
		}
	}
}
