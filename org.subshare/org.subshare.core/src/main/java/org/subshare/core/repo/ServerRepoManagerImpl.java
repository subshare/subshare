package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import javafx.util.Pair;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.repo.transport.CryptreeRestRepoTransport;
import org.subshare.core.server.Server;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistryImpl;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.UrlUtil;

public class ServerRepoManagerImpl implements ServerRepoManager {

	private static final class Holder {
		public static final ServerRepoManagerImpl instance = new ServerRepoManagerImpl();
	}

	private ServerRepoManagerImpl() {
	}

	public static ServerRepoManager getInstance() {
		return Holder.instance;
	}

	@Override
	public void createRepository(final File localDirectory, final Server server, final User owner) {
		assertNotNull("localDirectory", localDirectory);
		assertNotNull("server", server);
		assertNotNull("owner", owner);

		// We first make a check locally, before talking to the server. This does not guarantee that we can finalize the process,
		// but our risk of creating orphaned garbage on the server should be reduced to nearly 0%.
		// TODO we should maybe track this process in a file locally to make this somehow transactional (i.e. finish the process later,
		// if it fails inbetween, or roll back).
		final Pair<File, UUID> localRootAndRepositoryId = createLocalRepository(localDirectory);
		final File localRoot = localRootAndRepositoryId.getKey();
		final UUID clientRepositoryId = localRootAndRepositoryId.getValue();

		final UUID serverRepositoryId = createServerRepository(clientRepositoryId, server, owner);
		registerInServerRepoRegistry(server, serverRepositoryId, owner);

		owner.createUserRepoKey(serverRepositoryId);
		UserRegistryImpl.getInstance().writeIfNeeded(); // it's definitely needed because we just created a userRepoKey ;-)

		connectLocalRepositoryWithServerRepository(localRoot, server, serverRepositoryId);
	}

	private Pair<File, UUID> createLocalRepository(final File localDirectory) {
		assertNotNull("localDirectory", localDirectory);

		final File localRoot = LocalRepoHelper.getLocalRootContainingFile(localDirectory);
		if (localRoot == null) {
			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localDirectory);) {
				return new Pair<File, UUID>(localRepoManager.getLocalRoot(), localRepoManager.getRepositoryId());
			}
		}
		else {
			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
				final Map<UUID, URL> remoteRepositoryId2RemoteRootMap = localRepoManager.getRemoteRepositoryId2RemoteRootMap();
				if (!remoteRepositoryId2RemoteRootMap.isEmpty())
					throw new IllegalStateException(String.format("The directory '%s' is already a repository (or it is inside one) and it already is connected to the remote repository '%s'! A local repository may currently only be connected to exactly one server!",
							localDirectory, remoteRepositoryId2RemoteRootMap.values().iterator().next()));

				return new Pair<File, UUID>(localRepoManager.getLocalRoot(), localRepoManager.getRepositoryId());
			}
		}
	}

	private UUID createServerRepository(final UUID clientRepositoryId, final Server server, final User owner) {
		assertNotNull("clientRepositoryId", clientRepositoryId);
		assertNotNull("server", server);
		assertNotNull("owner", owner);

		final UUID serverRepositoryId = UUID.randomUUID(); // or should we better have the server create it? does it matter?
		final URL remoteRoot = UrlUtil.appendNonEncodedPath(server.getUrl(), serverRepositoryId.toString());
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRoot);
		try (final CryptreeRestRepoTransport repoTransport = (CryptreeRestRepoTransport) repoTransportFactory.createRepoTransport(remoteRoot, clientRepositoryId);) {
			repoTransport.createRepository(serverRepositoryId, getPgpKey(owner));
		}
		return serverRepositoryId;
	}

	private PgpKey getPgpKey(final User user) {
		// TODO we need a PgpKey selector! this method here in core should not know that PgpPrivateKeyPassphraseStore *MIGHT* be
		// used - maybe it is not!!! It's optional!
		//
		// And even if we are using it, there might be multiple active keys with each having a passphrase available. the user
		// should be able to select!

		// TODO ask the user to select one, if there are multiple?!
//		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseStoreImpl.getInstance();

		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			if (pgpKey.isPrivateKeyAvailable())
				return pgpKey;
		}
		throw new IllegalStateException("There is no PGP key with a private key available for this user: " + user);

//		final Collection<PgpKey> masterKeysWithPrivateKey = pgp.getMasterKeysWithPrivateKey();
//		if (masterKeysWithPrivateKey.isEmpty())
//			throw new IllegalStateException("There is no PGP key with a private key available!");
//
//		return masterKeysWithPrivateKey.iterator().next();
	}

	private void registerInServerRepoRegistry(final Server server, final UUID serverRepositoryId, final User owner) {
		assertNotNull("server", server);
		assertNotNull("serverRepositoryId", serverRepositoryId);
		assertNotNull("owner", owner);

		final ServerRepoRegistry serverRepoRegistry = getServerRepoRegistry();
		final ServerRepo serverRepo = serverRepoRegistry.createServerRepo(serverRepositoryId);
		serverRepo.setServerId(server.getServerId());
		serverRepo.setName(serverRepositoryId.toString());
		serverRepo.setUserId(owner.getUserId());
		serverRepoRegistry.getServerRepos().add(serverRepo);
		serverRepoRegistry.writeIfNeeded();
	}

	private ServerRepoRegistry getServerRepoRegistry() {
		return ServerRepoRegistryImpl.getInstance();
	}

	private void connectLocalRepositoryWithServerRepository(final File localRoot, final Server server, final UUID serverRepositoryId) {
		assertNotNull("localRoot", localRoot);
		assertNotNull("server", server);
		assertNotNull("serverRepositoryId", serverRepositoryId);
		final URL remoteRoot = UrlUtil.appendNonEncodedPath(server.getUrl(), serverRepositoryId.toString());

		try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
			final UUID clientRepositoryId = localRepoManager.getRepositoryId();
			final byte[] clientRepositoryPublicKey = localRepoManager.getPublicKey();

			try (final RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRoot).createRepoTransport(remoteRoot, clientRepositoryId);) {
				// *first* make the remote repository known locally.
				final byte[] remoteRepositoryPublicKey = repoTransport.getPublicKey();
				final String localPathPrefix = ""; // connected to root
				localRepoManager.putRemoteRepository(serverRepositoryId, remoteRoot, remoteRepositoryPublicKey, localPathPrefix);

				// *then* request the connection (this accesses the RemoteRepository object we persisted in the previous step).
				repoTransport.requestRepoConnection(clientRepositoryPublicKey);
			}
		}
	}
}
