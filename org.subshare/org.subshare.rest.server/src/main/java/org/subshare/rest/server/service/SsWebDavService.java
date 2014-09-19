package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.Path;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactory;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.LimitedInputStream;
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
					final int version = in.read();
					if (version != 1)
						throw new IllegalStateException("version != 1");

					final int length = in.read() + (in.read() << 8) + (in.read() << 16) + (in.read() << 24);

					try (final VerifierInputStream verifierIn = new VerifierInputStream(new LimitedInputStream(in, length, length), cryptree.getUserRepoKeyPublicKeyLookup());) {
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
					if (lastSlashIndex < 0)
						throw new IllegalStateException("path is neither empty nor does it contain '/'! path: " + path);

					final String uidStr = path.substring(lastSlashIndex + 1);
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
}
