package org.subshare.rest.server.service;

import static java.util.Objects.*;

import java.util.UUID;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.SsRequestRepoConnectionRepositoryDto;
import org.subshare.core.sign.SignableVerifier;

import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.rest.server.service.RequestRepoConnectionService;

@Path("_requestRepoConnection/{repositoryName}")
public class SsRequestRepoConnectionService extends RequestRepoConnectionService {

	@Override
	protected void requestConnection(final RepoTransport repoTransport, final String pathPrefix, final RepositoryDto clientRepositoryDto) {
		verifyRepositoryDto(repoTransport, clientRepositoryDto);
		super.requestConnection(repoTransport, pathPrefix, clientRepositoryDto);
		acceptConnection(repoTransport, pathPrefix, clientRepositoryDto);
	}

	private void verifyRepositoryDto(final RepoTransport repoTransport, final RepositoryDto clientRepositoryDto) {
		requireNonNull(repoTransport, "repoTransport");
		requireNonNull(clientRepositoryDto, "clientRepositoryDto");
		final UUID clientRepositoryId = requireNonNull(repoTransport.getClientRepositoryId(), "repoTransport.clientRepositoryId");

		if (! clientRepositoryId.equals(clientRepositoryDto.getRepositoryId()))
			throw new IllegalArgumentException("repoTransport.clientRepositoryId != clientRepositoryDto.clientRepositoryId");

		if (! (clientRepositoryDto instanceof SsRequestRepoConnectionRepositoryDto))
			throw new WebApplicationException(Response.status(Status.FORBIDDEN).type(MediaType.TEXT_PLAIN_TYPE).entity("clientRepositoryDto is not an instance of SsRequestRepoConnectionRepositoryDto!").build());

		final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
		try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();) {

			final SsRequestRepoConnectionRepositoryDto rrcRepositoryDto = (SsRequestRepoConnectionRepositoryDto) clientRepositoryDto;

			final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
			final Cryptree cryptree = cryptreeFactory.getCryptreeOrCreate(transaction, clientRepositoryId);

			// This (the server-side) might be a virgin repository! Hence, we perform this check only, if it's not empty.
			if (! cryptree.isEmpty()) {
				final SignableVerifier signableVerifier = new SignableVerifier(cryptree.getUserRepoKeyPublicKeyLookup());
				signableVerifier.verify(rrcRepositoryDto);
			}
		}
	}

	private void acceptConnection(final RepoTransport repoTransport, final String pathPrefix, final RepositoryDto clientRepositoryDto) {
		requireNonNull(repoTransport, "repoTransport");
		requireNonNull(clientRepositoryDto, "clientRepositoryDto");
		final UUID clientRepositoryId = requireNonNull(repoTransport.getClientRepositoryId(), "clientRepositoryId");
		final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
		final byte[] remotePublicKey = clientRepositoryDto.getPublicKey();
		final String localPathPrefix = pathPrefix;
		localRepoManager.putRemoteRepository(clientRepositoryId, null, remotePublicKey, localPathPrefix); // deletes the request.
	}
}
