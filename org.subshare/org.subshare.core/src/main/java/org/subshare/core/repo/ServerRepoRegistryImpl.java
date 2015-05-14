package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.subshare.core.dto.ServerRepoDto;
import org.subshare.core.dto.ServerRepoRegistryDto;
import org.subshare.core.dto.jaxb.ServerRepoRegistryDtoIo;
import org.subshare.core.observable.ModificationEventType;
import org.subshare.core.observable.ObservableList;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.observable.standard.StandardPreModificationEvent;
import org.subshare.core.observable.standard.StandardPreModificationListener;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerImpl;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

public class ServerRepoRegistryImpl implements ServerRepoRegistry {

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public static final String SERVER_REPO_LIST_FILE_NAME = "serverRepoList.xml.gz";
	public static final String SERVER_REPO_LIST_LOCK = SERVER_REPO_LIST_FILE_NAME + ".lock";

	private Map<UUID, ServerRepo> repositoryId2ServerRepo;
	private final ObservableList<ServerRepo> serverRepos;
	private final PreModificationListener preModificationListener = new PreModificationListener();
	private final PostModificationListener postModificationListener = new PostModificationListener();
	private final PropertyChangeListener serverRepoPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			dirty = true;
			repositoryId2ServerRepo = null;
			final ServerRepo serverRepo = (ServerRepo) evt.getSource();
			firePropertyChange(PropertyEnum.serverRepos_serverRepo, null, serverRepo);
		}
	};
	private boolean dirty;
	private final File serverRepoListFile;

	private static final class Holder {
		public static final ServerRepoRegistryImpl instance = new ServerRepoRegistryImpl();
	}

	private class PreModificationListener implements StandardPreModificationListener {
		@Override
		public void modificationOccurring(StandardPreModificationEvent event) {
			@SuppressWarnings("unchecked")
			final Collection<ServerRepo> changeCollection = event.getChangeCollection();

			if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
				for (ServerRepo serverRepo : changeCollection)
					serverRepo.addPropertyChangeListener(serverRepoPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_REMOVE & event.getType()) != 0) {
				for (ServerRepo serverRepo : changeCollection)
					serverRepo.removePropertyChangeListener(serverRepoPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_CLEAR & event.getType()) != 0) {
				for (ServerRepo serverRepo : serverRepos) // *all* instead of (empty!) changeCollection
					serverRepo.removePropertyChangeListener(serverRepoPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_RETAIN & event.getType()) != 0) {
				for (ServerRepo serverRepo : serverRepos) {
					if (!changeCollection.contains(serverRepo)) // IMHO changeCollection is the retained collection, i.e. all elements *not* contained there are removed.
						serverRepo.removePropertyChangeListener(serverRepoPropertyChangeListener);
				}
			}
		}
	}

	private class PostModificationListener implements StandardPostModificationListener {
		@Override
		public void modificationOccurred(StandardPostModificationEvent event) {
			dirty = true;
			repositoryId2ServerRepo = null;
			firePropertyChange(PropertyEnum.serverRepos, null, getServerRepos());
		}
	};

	protected ServerRepoRegistryImpl() {
		serverRepos = ObservableList.decorate(new CopyOnWriteArrayList<ServerRepo>());
		serverRepos.getHandler().addPreModificationListener(preModificationListener);
		serverRepos.getHandler().addPostModificationListener(postModificationListener);
		serverRepoListFile = createFile(ConfigDir.getInstance().getFile(), SERVER_REPO_LIST_FILE_NAME);

		read();
		populateServerReposFromLocalRepositories();
	}

	protected synchronized Map<UUID, ServerRepo> getRepositoryId2ServerRepo() {
		if (repositoryId2ServerRepo == null) {
			final Map<UUID, ServerRepo> m = new HashMap<>();
			for (ServerRepo serverRepo : getServerRepos())
				m.put(serverRepo.getRepositoryId(), serverRepo);

			repositoryId2ServerRepo = Collections.unmodifiableMap(m);
		}
		return repositoryId2ServerRepo;
	}

	private void read() {
		final ServerRepoDtoConverter serverRepoDtoConverter = new ServerRepoDtoConverter();
		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				if (serverRepoListFile.exists()) {
					final ServerRepoRegistryDtoIo serverRepoRegistryDtoIo = new ServerRepoRegistryDtoIo();
					final ServerRepoRegistryDto serverRepoRegistryDto = serverRepoRegistryDtoIo.deserializeWithGz(serverRepoListFile);
					for (final ServerRepoDto serverRepoDto : serverRepoRegistryDto.getServerRepoDtos()) {
						final ServerRepo serverRepo = serverRepoDtoConverter.fromServerRepoDto(serverRepoDto);
						getServerRepos().add(serverRepo);
					}
				}
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
	}

	private void populateServerReposFromLocalRepositories() {
		final ServerRegistry serverRegistry = ServerRegistryImpl.getInstance();
		final Map<UUID, ServerRepo> repositoryId2ServerRepo = getRepositoryId2ServerRepo();
		final LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
		for (final UUID localRepositoryId : localRepoRegistry.getRepositoryIds()) {
			final File localRoot = localRepoRegistry.getLocalRoot(localRepositoryId);
			if (localRoot == null || !localRoot.exists())
				continue; // maybe deleted during iteration

			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
				for (final Map.Entry<UUID, URL> me : localRepoManager.getRemoteRepositoryId2RemoteRootMap().entrySet()) {
					final UUID serverRepositoryId = me.getKey();
					final URL remoteRoot = me.getValue();

					Server server = serverRegistry.getServerForRemoteRoot(remoteRoot);
					if (server == null) {
						final URL serverUrl;
						try (RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot).createRepoTransport(remoteRoot, localRepositoryId);) {
							final URL remoteRootWithoutPathPrefix = repoTransport.getRemoteRootWithoutPathPrefix();
							// remoteRootWithoutPathPrefix still contains the repository-name or repository-id as last path-segment.
							serverUrl = removeLastPathSegment(remoteRootWithoutPathPrefix);
						}

						server = new ServerImpl();
						server.setName(serverUrl.getHost());
						server.setUrl(serverUrl);
						serverRegistry.getServers().add(server);
					}

					if (repositoryId2ServerRepo.get(serverRepositoryId) == null) {
						final ServerRepo serverRepo = new ServerRepoImpl(serverRepositoryId);
						serverRepo.setName(serverRepositoryId.toString());
						serverRepo.setServerId(server.getServerId());
						getServerRepos().add(serverRepo);
					}
				}
			}
		}
	}

	private URL removeLastPathSegment(URL url) {
		assertNotNull("url", url);
		String urlStr = url.toExternalForm();
		if (urlStr.contains("?"))
			throw new IllegalArgumentException("url should not contain a query part!");

		while (urlStr.endsWith("/"))
			urlStr = urlStr.substring(0, urlStr.length() - 1);

		final int lastSlashIndex = urlStr.lastIndexOf('/');
		if (lastSlashIndex < 0)
			throw new IllegalArgumentException("No '/' found where expected!");

		urlStr = urlStr.substring(0, lastSlashIndex);

		while (urlStr.endsWith("/"))
			urlStr = urlStr.substring(0, urlStr.length() - 1);

		try {
			return new URL(urlStr);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<ServerRepo> getServerReposOfServer(final Uid serverId) {
		assertNotNull("serverId", serverId);
		final List<ServerRepo> result = new ArrayList<>();
		for (final ServerRepo serverRepo : getServerRepos()) {
			if (serverId.equals(serverRepo.getServerId()))
				result.add(serverRepo);
		}
		return result;
	}

	@Override
	public ServerRepo createServerRepo(final UUID repositoryId) {
		return new ServerRepoImpl(repositoryId);
	}

	public static ServerRepoRegistry getInstance() {
		return Holder.instance;
	}

	@Override
	public List<ServerRepo> getServerRepos() {
		return serverRepos;
	}

	private LockFile acquireLockFile() {
		final File dir = ConfigDir.getInstance().getFile();
		return LockFileFactory.getInstance().acquire(createFile(dir, SERVER_REPO_LIST_LOCK), 30000);
	}

	@Override
	public synchronized void writeIfNeeded() {
		if (dirty)
			write();
	}

	@Override
	public synchronized void write() {
		final ServerRepoRegistryDtoIo serverRepoRegistryDtoIo = new ServerRepoRegistryDtoIo();
		final ServerRepoRegistryDto serverRepoRegistryDto = createServerRepoListDto();

		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				final File newServerRepoListFile = createFile(serverRepoListFile.getParentFile(), serverRepoListFile.getName() + ".new");
				serverRepoRegistryDtoIo.serializeWithGz(serverRepoRegistryDto, newServerRepoListFile);
				serverRepoListFile.delete();
				newServerRepoListFile.renameTo(serverRepoListFile);
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
	}

	private ServerRepoRegistryDto createServerRepoListDto() {
		final ServerRepoDtoConverter converter = new ServerRepoDtoConverter();
		final ServerRepoRegistryDto result = new ServerRepoRegistryDto();
		for (ServerRepo serverRepo : serverRepos) {
			final ServerRepoDto serverRepoDto = converter.toServerRepoDto(serverRepo);
			result.getServerRepoDtos().add(serverRepoDto);
		}
		return result;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}
}
