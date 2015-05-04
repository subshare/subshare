package org.subshare.core.server;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;
import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static co.codewizards.cloudstore.core.util.UrlUtil.canonicalizeURL;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.subshare.core.dto.ServerDto;
import org.subshare.core.dto.ServerRegistryDto;
import org.subshare.core.dto.jaxb.ServerRegistryDtoIo;
import org.subshare.core.observable.ModificationEventType;
import org.subshare.core.observable.ObservableList;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.observable.standard.StandardPreModificationEvent;
import org.subshare.core.observable.standard.StandardPreModificationListener;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

public class ServerRegistryImpl implements ServerRegistry {

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public static final String SERVER_LIST_FILE_NAME = "serverList.xml.gz";
	public static final String SERVER_LIST_LOCK = SERVER_LIST_FILE_NAME + ".lock";

	private final ObservableList<Server> servers;
	private final PreModificationListener preModificationListener = new PreModificationListener();
	private final PostModificationListener postModificationListener = new PostModificationListener();
	private final PropertyChangeListener serverPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			dirty = true;
			final Server server = (Server) evt.getSource();
			firePropertyChange(PropertyEnum.servers_server, null, server);
		}
	};
	private boolean dirty;
	private final File serverListFile;

	private static final class Holder {
		public static final ServerRegistryImpl instance = new ServerRegistryImpl();
	}

	private class PreModificationListener implements StandardPreModificationListener {
		@Override
		public void modificationOccurring(StandardPreModificationEvent event) {
			@SuppressWarnings("unchecked")
			final Collection<Server> changeCollection = event.getChangeCollection();

			if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
				for (Server server : changeCollection)
					server.addPropertyChangeListener(serverPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_REMOVE & event.getType()) != 0) {
				for (Server server : changeCollection)
					server.removePropertyChangeListener(serverPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_CLEAR & event.getType()) != 0) {
				for (Server server : servers) // *all* instead of (empty!) changeCollection
					server.removePropertyChangeListener(serverPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_RETAIN & event.getType()) != 0) {
				for (Server server : servers) {
					if (!changeCollection.contains(server)) // IMHO changeCollection is the retained collection, i.e. all elements *not* contained there are removed.
						server.removePropertyChangeListener(serverPropertyChangeListener);
				}
			}
		}
	}

	private class PostModificationListener implements StandardPostModificationListener {
		@Override
		public void modificationOccurred(StandardPostModificationEvent event) {
			dirty = true;
			firePropertyChange(PropertyEnum.servers, null, getServers());
		}
	};

	protected ServerRegistryImpl() {
		servers = ObservableList.decorate(new CopyOnWriteArrayList<Server>());
		servers.getHandler().addPreModificationListener(preModificationListener);
		servers.getHandler().addPostModificationListener(postModificationListener);
		serverListFile = createFile(ConfigDir.getInstance().getFile(), SERVER_LIST_FILE_NAME);

		read();
		populateServersFromLocalRepositories();

	}

	private void read() {
		final ServerDtoConverter serverDtoConverter = new ServerDtoConverter();
		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				if (serverListFile.exists()) {
					final ServerRegistryDtoIo serverRegistryDtoIo = new ServerRegistryDtoIo();
					final ServerRegistryDto serverRegistryDto = serverRegistryDtoIo.deserializeWithGz(serverListFile);
					for (final ServerDto serverDto : serverRegistryDto.getServerDtos()) {
						final Server server = serverDtoConverter.fromServerDto(serverDto);
						getServers().add(server);
					}
				}
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
	}

	private void populateServersFromLocalRepositories() {
		final LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
		for (final UUID localRepositoryId : localRepoRegistry.getRepositoryIds()) {
			final File localRoot = localRepoRegistry.getLocalRoot(localRepositoryId);
			if (localRoot == null)
				continue; // maybe deleted during iteration

			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
				for (final URL remoteRoot : localRepoManager.getRemoteRepositoryId2RemoteRootMap().values()) {
					if (getServerForRemoteRoot(remoteRoot) != null)
						continue; // we already have a server

					final URL serverUrl;
					try (RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot).createRepoTransport(remoteRoot, localRepositoryId);) {
						final URL remoteRootWithoutPathPrefix = repoTransport.getRemoteRootWithoutPathPrefix();
						// remoteRootWithoutPathPrefix still contains the repository-name or repository-id as last path-segment.
						serverUrl = removeLastPathSegment(remoteRootWithoutPathPrefix);
					}

					final ServerImpl server = new ServerImpl();
					server.setName(serverUrl.getHost());
					server.setUrl(serverUrl);
					getServers().add(server);
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

	/* (non-Javadoc)
	 * @see org.subshare.core.server.IServerRegistry#getServerForRemoteRoot(java.net.URL)
	 */
	@Override
	public Server getServerForRemoteRoot(final URL remoteRoot) {
		assertNotNull("remoteRoot", remoteRoot);

		for (Server server : servers) {
			if (server.getUrl() != null && isSubUrl(server.getUrl(), remoteRoot))
				return server;
		}
		return null;
	}

	private boolean isSubUrl(URL baseUrl, URL subUrl) {
		assertNotNull("baseUrl", subUrl);
		String baseUrlStr = canonicalizeURL(baseUrl).toExternalForm();

		// IMHO we should check, if the baseUrlStr ends on a '/', because http://host/aaa should maybe not be considered a super-URL of http://host/aaaxxx, while it definitely is a super-URL of http://host/aaa/xxx
		if (baseUrlStr.contains("?"))
			throw new IllegalArgumentException("baseUrl should not contain a query part!");

		if (! baseUrlStr.endsWith("/"))
			baseUrlStr = baseUrlStr + "/";

		final String subUrlStr = subUrl.toExternalForm();

		return subUrlStr.startsWith(baseUrlStr);
	}

	public static ServerRegistry getInstance() {
		return Holder.instance;
	}

	@Override
	public List<Server> getServers() {
		return servers;
	}

	private LockFile acquireLockFile() {
		final File dir = ConfigDir.getInstance().getFile();
		return LockFileFactory.getInstance().acquire(createFile(dir, SERVER_LIST_LOCK), 30000);
	}

	@Override
	public synchronized void writeIfNeeded() {
		if (dirty)
			write();
	}

	@Override
	public synchronized void write() {
		final ServerRegistryDtoIo serverRegistryDtoIo = new ServerRegistryDtoIo();
		final ServerRegistryDto serverRegistryDto = createServerListDto();

		try (LockFile lockFile = acquireLockFile();) {
			lockFile.getLock().lock();
			try {
				final File newServerListFile = createFile(serverListFile.getParentFile(), serverListFile.getName() + ".new");
				serverRegistryDtoIo.serializeWithGz(serverRegistryDto, newServerListFile);
				serverListFile.delete();
				newServerListFile.renameTo(serverListFile);
			} finally {
				lockFile.getLock().unlock();
			}
		}
		dirty = false;
	}

	private ServerRegistryDto createServerListDto() {
		final ServerDtoConverter converter = new ServerDtoConverter();
		final ServerRegistryDto result = new ServerRegistryDto();
		for (Server server : servers) {
			final ServerDto serverDto = converter.toServerDto(server);
			result.getServerDtos().add(serverDto);
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
