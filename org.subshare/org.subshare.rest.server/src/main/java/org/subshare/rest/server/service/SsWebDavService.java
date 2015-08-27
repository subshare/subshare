package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.sign.VerifierInputStream;
import org.subshare.core.user.UserRepoKey;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.rest.server.service.WebDavService;

@Path("{repositoryName:[^_/][^/]*}")
public class SsWebDavService extends WebDavService {

	@Override
	public void putFileData(final String path, final long offset, final byte[] fileData) {
		assertNotNull("path", path);
		assertNotNull("fileData", fileData);

		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			final UUID clientRepositoryId = assertNotNull("clientRepositoryId", repoTransport.getClientRepositoryId());
			final LocalRepoManager localRepoManager = ((ContextWithLocalRepoManager) repoTransport).getLocalRepoManager();
			final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();
			try {
				final CryptreeFactory cryptreeFactory = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail();
				final Cryptree cryptree = cryptreeFactory.getCryptreeOrCreate(transaction, clientRepositoryId);

				final Date signatureCreated;
				final UserRepoKey.PublicKey signingUserRepoKeyPublicKey;

				try {
					final byte[] buf = new byte[64 * 1024];

					final ByteArrayInputStream in = new ByteArrayInputStream(fileData);
					try (final VerifierInputStream verifierIn = new VerifierInputStream(in, cryptree.getUserRepoKeyPublicKeyLookup());) {
						signatureCreated = verifierIn.getSignatureCreated();
						signingUserRepoKeyPublicKey = verifierIn.getSigningUserRepoKeyPublicKey();

						while (verifierIn.read(buf) >= 0);
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}

				Uid cryptoRepoFileId;
				if (path.isEmpty() || "/".equals(path))
					cryptoRepoFileId = cryptree.getRootCryptoRepoFileId();
				else {
					final int lastSlashIndex = path.lastIndexOf('/');
					final String uidStr = lastSlashIndex < 0 ? path : path.substring(lastSlashIndex + 1);
					cryptoRepoFileId = new Uid(uidStr);
				}

				cryptree.assertHasPermission(
						cryptoRepoFileId, signingUserRepoKeyPublicKey.getUserRepoKeyId(),
						PermissionType.write, signatureCreated);

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		}

		super.putFileData(path, offset, fileData);
	}

	// Replaced by DeleteService, because we need a PUT with a *signed* DTO!
	@DELETE
	@Path("{path:.*}")
	@Override
	public void delete(String path) {
		throw new WebApplicationException(Response.status(Status.NOT_FOUND).build());
	}
}
