package org.subshare.core.repo.metaonly;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.ServerRepoManagerImpl;
import org.subshare.core.repo.ServerRepoRegistry;
import org.subshare.core.repo.ServerRepoRegistryImpl;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;
import org.subshare.core.sync.RepoSyncState;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.UrlUtil;
import co.codewizards.cloudstore.core.util.ExceptionUtil;

public class MetaOnlyRepoManagerImpl implements MetaOnlyRepoManager {

	private static final Logger logger = LoggerFactory.getLogger(MetaOnlyRepoManagerImpl.class);
	private static final UUID NULL_UUID = new UUID(0, 0);
	private final Map<File, UUID> localRoot2LocalRepositoryId = Collections.synchronizedMap(new HashMap<File, UUID>());
	private final Map<UUID, Boolean> serverRepositoryId2triggerRedownCryptoMetaDataDone = Collections.synchronizedMap(new HashMap<UUID, Boolean>());

	private final static int REPO_FILE_DTO_DEPTH = 0;

	protected MetaOnlyRepoManagerImpl() { }

	private static final class Holder {
		public static final MetaOnlyRepoManager instance = new MetaOnlyRepoManagerImpl();
	}

	public static MetaOnlyRepoManager getInstance() {
		return Holder.instance;
	}

	@Override
	public List<RepoSyncState> sync() {
		final ServerRegistry serverRegistry = ServerRegistryImpl.getInstance();
		final ServerRepoRegistry serverRepoRegistry = ServerRepoRegistryImpl.getInstance();
		final List<ServerRepo> serverRepos = serverRepoRegistry.getServerRepos();
		final List<RepoSyncState> repoSyncStates = new ArrayList<RepoSyncState>(serverRepos.size());
		for (final ServerRepo serverRepo : serverRepos) {
			final Server server = serverRegistry.getServer(serverRepo.getServerId());
			if (server == null) {
				logger.warn("sync: serverRegistry does not know server with serverId={}!", serverRepo.getServerId());
				continue;
			}
			final File localRoot = getLocalRoot(serverRepo);
			final URL remoteRoot = getRemoteRoot(server, serverRepo);
			final Date syncStarted = now();
			try {
				sync(server, serverRepo);
				final long durationMs = System.currentTimeMillis() - syncStarted.getTime();
				repoSyncStates.add(new RepoSyncState(getLocalRepositoryId(localRoot, NULL_UUID),
						serverRepo, server, localRoot, remoteRoot,
						Severity.INFO, String.format("Sync took %s ms.", durationMs), null,
						syncStarted, now()));
			} catch (Exception x) {
				logger.warn("sync: " + x, x);
				repoSyncStates.add(new RepoSyncState(getLocalRepositoryId(localRoot, NULL_UUID),
						serverRepo, server, localRoot, remoteRoot,
						Severity.ERROR, x.getMessage(), new Error(x),
						syncStarted, now()));

				if (ExceptionUtil.getCause(x, IOException.class) == null)
					triggerRedownMetaData(server, serverRepo);
			}
		}
		return repoSyncStates;
	}

	private void triggerRedownMetaData(Server server, ServerRepo serverRepo) {
		requireNonNull(server, "server");
		requireNonNull(serverRepo, "serverRepo");
		UUID serverRepositoryId = serverRepo.getRepositoryId();
		
		Boolean done;
		synchronized (serverRepositoryId2triggerRedownCryptoMetaDataDone) {
			done = serverRepositoryId2triggerRedownCryptoMetaDataDone.get(serverRepositoryId);
			serverRepositoryId2triggerRedownCryptoMetaDataDone.put(serverRepositoryId, Boolean.TRUE);
		}
		if (Boolean.TRUE.equals(done)) {
			logger.warn("triggerRedownCryptoMetaData: serverId='{}' serverName='{}' serverRepositoryId='{}' ALREADY DONE BEFORE => SKIP!",
					server.getServerId(), server.getName(), serverRepositoryId);
			return;
		}

		logger.info("triggerRedownCryptoMetaData: serverId='{}' serverName='{}' serverRepositoryId='{}'",
				server.getServerId(), server.getName(), serverRepositoryId);

		try (final LocalRepoManager localRepoManager = createLocalRepoManager(serverRepo);) {
			final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();

			localRepoMetaData.resetLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced();

			// Do we need this, too? Seems like it's not needed -- repaired it successfully without.
//			localRepoMetaData.resetLastSyncFromRemoteRepoRemoteRepositoryRevisionSynced();
		}
	}

	private UUID getLocalRepositoryId(final File localRoot, final UUID fallbackRepositoryId) {
		requireNonNull(localRoot, "localRoot");
		final UUID result = localRoot2LocalRepositoryId.get(localRoot);
		if (result == null)
			return fallbackRepositoryId;
		else
			return result;
	}

	private void sync(final Server server, final ServerRepo serverRepo) {
		requireNonNull(server, "server");
		requireNonNull(serverRepo, "serverRepo");

		final List<URL> remoteRoots = new ArrayList<URL>();
		try (final LocalRepoManager localRepoManager = createLocalRepoManager(serverRepo);) {
			connectLocalRepositoryWithServerRepositoryIfNeeded(localRepoManager, server, serverRepo);

			for (final URL url : localRepoManager.getRemoteRepositoryId2RemoteRootMap().values())
				remoteRoots.add(url);
		}

		final File localRoot = getLocalRoot(serverRepo);
		for (final URL remoteRoot : remoteRoots) {
			try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(localRoot, remoteRoot);) {
				repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			}
		}
	}

	private void connectLocalRepositoryWithServerRepositoryIfNeeded(final LocalRepoManager localRepoManager, final Server server, final ServerRepo serverRepo) {
		if (localRepoManager.getRemoteRepositoryId2RemoteRootMap().isEmpty()) {
			final URL remoteRoot = getRemoteRoot(server, serverRepo);
			ServerRepoManagerImpl.connectLocalRepositoryWithServerRepository(localRepoManager, serverRepo.getRepositoryId(), remoteRoot);

			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			localRepoMetaData.makeMetaOnly();
		}
	}

	private static URL getRemoteRoot(Server server, ServerRepo serverRepo) {
		final URL remoteRoot = UrlUtil.appendNonEncodedPath(server.getUrl(), serverRepo.getRepositoryId().toString());
		return remoteRoot;
	}

	@Override
	public File getBaseDir() {
		return createFile(ConfigDir.getInstance().getFile(), "metaOnlyRepo");
	}

	@Override
	public File getLocalRoot(final ServerRepo serverRepo) {
		requireNonNull(serverRepo, "serverRepo");
		return createFile(getBaseDir(),
				serverRepo.getServerId().toString(), serverRepo.getRepositoryId().toString());
	}

	public LocalRepoManager createLocalRepoManager(final ServerRepo serverRepo) {
		final File localRoot = getLocalRoot(serverRepo);
		if (!localRoot.isDirectory())
			localRoot.mkdirs();

		if (!localRoot.isDirectory())
			throw new IllegalStateException(new IOException("Could not create directory: " + localRoot.getAbsolutePath()));

		final LocalRepoManager localRepoManager;
		if (LocalRepoHelper.getLocalRootContainingFile(localRoot) == null)
//			localRepoManager = createLocalRepoManagerForNewRepository(serverRepo, localRoot);
			localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localRoot);
		else
			localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);

		localRoot2LocalRepositoryId.put(localRoot, localRepoManager.getRepositoryId());
		return localRepoManager;
	}

//	private LocalRepoManager createLocalRepoManagerForNewRepository(final ServerRepo serverRepo, final File localRoot) {
//		boolean successful = false;
//		final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localRoot);
//		try {
//			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
//			localRepoMetaData.makeMetaOnly();
//			successful = true;
//		} finally {
//			if (! successful)
//				localRepoManager.close();
//		}
//		return localRepoManager;
//	}

	public ServerRepoFile getServerRepoFile(final ServerRepo serverRepo, long repoFileId) {
		requireNonNull(serverRepo, "serverRepo");
		final Server server = ServerRegistryImpl.getInstance().getServer(serverRepo.getServerId());
		requireNonNull(server, "serverRegistry.getServer(" + serverRepo.getServerId() + ")"); // or should we better return null?!

		try (final LocalRepoManager localRepoManager = createLocalRepoManager(serverRepo);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			final RepoFileDto repoFileDto = localRepoMetaData.getRepoFileDto(repoFileId, REPO_FILE_DTO_DEPTH);
			if (repoFileDto == null)
				return null;

			final CryptoRepoFileDto cryptoRepoFileDto = localRepoMetaData.getCryptoRepoFileDto(repoFileDto.getId());
			if (cryptoRepoFileDto == null)
				return null;

			final UUID localRepositoryId = localRepoManager.getRepositoryId();

			return new ServerRepoFileImpl(server, serverRepo, localRepositoryId, cryptoRepoFileDto, repoFileDto);
		}
	}

	@Override
	public ServerRepoFile getRootServerRepoFile(final ServerRepo serverRepo) {
		requireNonNull(serverRepo, "serverRepo");
		final Server server = ServerRegistryImpl.getInstance().getServer(serverRepo.getServerId());
		requireNonNull(server, "serverRegistry.getServer(" + serverRepo.getServerId() + ")"); // or should we better return null?!

		try (final LocalRepoManager localRepoManager = createLocalRepoManager(serverRepo);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			final RepoFileDto repoFileDto = localRepoMetaData.getRepoFileDto("", REPO_FILE_DTO_DEPTH);
			if (repoFileDto == null)
				return null;

			final CryptoRepoFileDto cryptoRepoFileDto = localRepoMetaData.getCryptoRepoFileDto(repoFileDto.getId());
			if (cryptoRepoFileDto == null)
				return null;

			final UUID localRepositoryId = localRepoManager.getRepositoryId();

			return new ServerRepoFileImpl(server, serverRepo, localRepositoryId, cryptoRepoFileDto, repoFileDto);
		}
	}

	protected List<ServerRepoFile> getChildServerRepoFiles(final ServerRepoFileImpl serverRepoFile) {
		requireNonNull(serverRepoFile, "serverRepoFile");

		final ServerRepo serverRepo = serverRepoFile.getServerRepo();
		final long repoFileId = serverRepoFile.getRepoFileId();

		try (final LocalRepoManager localRepoManager = createLocalRepoManager(serverRepo);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();

			final List<RepoFileDto> childRepoFileDtos = localRepoMetaData.getChildRepoFileDtos(repoFileId, REPO_FILE_DTO_DEPTH);
			if (childRepoFileDtos == null)
				return null; // parent doesn't exist (anymore)

			final List<Long> repoFileIds = new ArrayList<>(childRepoFileDtos.size());
			for (final RepoFileDto repoFileDto : childRepoFileDtos)
				repoFileIds.add(repoFileDto.getId());

			final Map<Long, CryptoRepoFileDto> repoFileId2CryptoRepoFileDto = localRepoMetaData.getCryptoRepoFileDtos(repoFileIds);

			final List<ServerRepoFile> result = new ArrayList<>(repoFileId2CryptoRepoFileDto.size());
			for (final RepoFileDto repoFileDto : childRepoFileDtos) {
				final CryptoRepoFileDto cryptoRepoFileDto = repoFileId2CryptoRepoFileDto.get(repoFileDto.getId());
				if (cryptoRepoFileDto != null)
					result.add(new ServerRepoFileImpl(serverRepoFile, cryptoRepoFileDto, repoFileDto));
			}
			return result;
		}
	}
}
