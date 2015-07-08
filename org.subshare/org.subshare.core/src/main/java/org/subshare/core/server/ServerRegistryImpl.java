package org.subshare.core.server;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.UrlUtil.*;
import static org.subshare.core.file.FileConst.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.subshare.core.dto.DeletedUid;
import org.subshare.core.dto.ServerDto;
import org.subshare.core.dto.ServerRegistryDto;
import org.subshare.core.dto.jaxb.ServerRegistryDtoIo;
import org.subshare.core.fbor.FileBasedObjectRegistry;
import org.subshare.core.observable.ModificationEventType;
import org.subshare.core.observable.ObservableList;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.observable.standard.StandardPreModificationEvent;
import org.subshare.core.observable.standard.StandardPreModificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class ServerRegistryImpl extends FileBasedObjectRegistry implements ServerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ServerRegistryImpl.class);

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public static final String SERVER_REGISTRY_FILE_NAME = "serverRegistry" + SUBSHARE_FILE_EXTENSION;

	private static final String PAYLOAD_ENTRY_NAME = ServerRegistryDto.class.getSimpleName() + ".xml";

	private final ObservableList<Server> servers;
	private final List<DeletedUid> deletedServerIds = new CopyOnWriteArrayList<>();
	private final ThreadLocal<Boolean> suppressAddToDeletedServerIds = new ThreadLocal<Boolean>();
	private final PreModificationListener preModificationListener = new PreModificationListener();
	private final PostModificationListener postModificationListener = new PostModificationListener();
	private final PropertyChangeListener serverPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			markDirty();
			final Server server = (Server) evt.getSource();
			firePropertyChange(PropertyEnum.servers_server, null, server);
		}
	};
	private final File serverRegistryFile;
	private Uid version;

	private static final class Holder {
		public static final ServerRegistryImpl instance = new ServerRegistryImpl();
	}

	private class PreModificationListener implements StandardPreModificationListener {
		@Override
		public void modificationOccurring(StandardPreModificationEvent event) {
			@SuppressWarnings("unchecked")
			final Collection<Server> changeCollection = event.getChangeCollection();

			if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
				for (Server server : changeCollection) {
					if (getServerByServerId(server.getServerId()) != null)
						throw new UnsupportedOperationException(String.format("Cannot add the same Server (serverId=%s) twice!", server.getServerId()));

					server.addPropertyChangeListener(serverPropertyChangeListener);
					removeDeletedServerId(server.getServerId()); // support re-adding - or should we not support this?
				}
			}
			else if ((ModificationEventType.GROUP_REMOVE & event.getType()) != 0) {
				for (Server server : changeCollection) {
					server.removePropertyChangeListener(serverPropertyChangeListener);
					addToDeletedServerIds(server.getServerId());
				}
			}
			else if ((ModificationEventType.GROUP_CLEAR & event.getType()) != 0) {
				for (Server server : servers) { // *all* instead of (empty!) changeCollection
					server.removePropertyChangeListener(serverPropertyChangeListener);
					addToDeletedServerIds(server.getServerId());
				}
			}
			else if ((ModificationEventType.GROUP_RETAIN & event.getType()) != 0) {
				for (Server server : servers) {
					if (!changeCollection.contains(server)) { // IMHO changeCollection is the retained collection, i.e. all elements *not* contained there are removed.
						server.removePropertyChangeListener(serverPropertyChangeListener);
						addToDeletedServerIds(server.getServerId());
					}
				}
			}
		}
	}

	private void addToDeletedServerIds(final Uid serverId) {
		if (! Boolean.TRUE.equals(suppressAddToDeletedServerIds.get()))
			deletedServerIds.add(new DeletedUid(serverId));
	}

	private void removeDeletedServerId(final Uid serverId) {
		final List<DeletedUid> deletedUidsToRemove = new ArrayList<>();
		for (final DeletedUid deletedUid : deletedServerIds) {
			if (serverId.equals(deletedUid.getUid()))
				deletedUidsToRemove.add(deletedUid);
		}
		deletedServerIds.removeAll(deletedUidsToRemove);
	}

	private class PostModificationListener implements StandardPostModificationListener {
		@Override
		public void modificationOccurred(StandardPostModificationEvent event) {
			markDirty();
			firePropertyChange(PropertyEnum.servers, null, getServers());
		}
	};

	protected ServerRegistryImpl() {
		servers = ObservableList.decorate(new CopyOnWriteArrayList<Server>());
		servers.getHandler().addPreModificationListener(preModificationListener);
		servers.getHandler().addPostModificationListener(postModificationListener);
		serverRegistryFile = createFile(ConfigDir.getInstance().getFile(), SERVER_REGISTRY_FILE_NAME);

		read();
//		populateServersFromLocalRepositories();
	}

	@Override
	protected String getContentType() {
		return "application/vnd.subshare.server-registry";
	}

	@Override
	protected void preRead() {
		version = null;
	}

	@Override
	protected void postRead() {
		if (version == null) {
			version = new Uid();
			markDirty();
		}
	}

	@Override
	protected void readPayloadEntry(ZipInputStream zin, ZipEntry zipEntry) throws IOException {
		if (!PAYLOAD_ENTRY_NAME.equals(zipEntry.getName())) {
			logger.warn("readPayloadEntry: Ignoring unexpected zip-entry: {}", zipEntry.getName());
			return;
		}
		final ServerDtoConverter serverDtoConverter = new ServerDtoConverter();
		final ServerRegistryDtoIo serverRegistryDtoIo = new ServerRegistryDtoIo();
		final ServerRegistryDto serverRegistryDto = serverRegistryDtoIo.deserialize(zin);

		for (final ServerDto serverDto : serverRegistryDto.getServerDtos()) {
			final Server server = serverDtoConverter.fromServerDto(serverDto);
			servers.add(server);
		}

		deletedServerIds.clear();
		deletedServerIds.addAll(serverRegistryDto.getDeletedServerIds());

		version = serverRegistryDto.getVersion();
	}

	@Override
	protected File getFile() {
		return serverRegistryFile;
	}

//	private void populateServersFromLocalRepositories() {
//		final LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
//		for (final UUID localRepositoryId : localRepoRegistry.getRepositoryIds()) {
//			final File localRoot = localRepoRegistry.getLocalRoot(localRepositoryId);
//			if (localRoot == null)
//				continue; // maybe deleted during iteration
//
//			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
//				for (final URL remoteRoot : localRepoManager.getRemoteRepositoryId2RemoteRootMap().values()) {
//					if (getServerForRemoteRoot(remoteRoot) != null)
//						continue; // we already have a server
//
//					final URL serverUrl;
//					try (RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot).createRepoTransport(remoteRoot, localRepositoryId);) {
//						final URL remoteRootWithoutPathPrefix = repoTransport.getRemoteRootWithoutPathPrefix();
//						// remoteRootWithoutPathPrefix still contains the repository-name or repository-id as last path-segment.
//						serverUrl = removeLastPathSegment(remoteRootWithoutPathPrefix);
//					}
//
//					final ServerImpl server = new ServerImpl();
//					server.setName(serverUrl.getHost());
//					server.setUrl(serverUrl);
//					getServers().add(server);
//				}
//			}
//		}
//	}
//
//	private URL removeLastPathSegment(URL url) {
//		assertNotNull("url", url);
//		String urlStr = url.toExternalForm();
//		if (urlStr.contains("?"))
//			throw new IllegalArgumentException("url should not contain a query part!");
//
//		while (urlStr.endsWith("/"))
//			urlStr = urlStr.substring(0, urlStr.length() - 1);
//
//		final int lastSlashIndex = urlStr.lastIndexOf('/');
//		if (lastSlashIndex < 0)
//			throw new IllegalArgumentException("No '/' found where expected!");
//
//		urlStr = urlStr.substring(0, lastSlashIndex);
//
//		while (urlStr.endsWith("/"))
//			urlStr = urlStr.substring(0, urlStr.length() - 1);
//
//		try {
//			return new URL(urlStr);
//		} catch (MalformedURLException e) {
//			throw new RuntimeException(e);
//		}
//	}

	@Override
	public Server getServerForRemoteRoot(final URL remoteRoot) {
		assertNotNull("remoteRoot", remoteRoot);

		for (Server server : servers) {
			if (server.getUrl() != null && isSubUrl(server.getUrl(), remoteRoot))
				return server;
		}
		return null;
	}

	@Override
	public Server getServer(final Uid serverId) {
		assertNotNull("serverId", serverId);

		for (Server server : servers) {
			if (serverId.equals(server.getServerId()))
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
			baseUrlStr = baseUrlStr + '/';

		String subUrlStr = canonicalizeURL(subUrl).toExternalForm();
		if (! subUrlStr.endsWith("/"))
			subUrlStr = subUrlStr + '/';

		return subUrlStr.startsWith(baseUrlStr);
	}

	public static ServerRegistry getInstance() {
		return Holder.instance;
	}

	@Override
	public List<Server> getServers() {
		return servers;
	}

	@Override
	public Server createServer() {
		return new ServerImpl();
	}

	@Override
	protected synchronized void writePayload(ZipOutputStream zout) throws IOException {
		final ServerRegistryDtoIo serverRegistryDtoIo = new ServerRegistryDtoIo();
		final ServerRegistryDto serverRegistryDto = createServerRegistryDto();

		zout.putNextEntry(new ZipEntry(PAYLOAD_ENTRY_NAME));
		serverRegistryDtoIo.serialize(serverRegistryDto, zout);
		zout.closeEntry();
	}

	private ServerRegistryDto createServerRegistryDto() {
		final ServerDtoConverter converter = new ServerDtoConverter();
		final ServerRegistryDto result = new ServerRegistryDto();
		for (Server server : servers) {
			final ServerDto serverDto = converter.toServerDto(server);
			result.getServerDtos().add(serverDto);
		}
		result.getDeletedServerIds().addAll(deletedServerIds);
		result.setVersion(version);
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

	@Override
	protected void mergeFrom(ZipInputStream zin, ZipEntry zipEntry) {
		if (PAYLOAD_ENTRY_NAME.equals(zipEntry.getName())) {
			final ServerRegistryDtoIo serverRegistryDtoIo = new ServerRegistryDtoIo();
			final ServerRegistryDto serverRegistryDto = serverRegistryDtoIo.deserialize(zin);
			mergeFrom(serverRegistryDto);
		}
	}

	protected synchronized void mergeFrom(final ServerRegistryDto serverRegistryDto) {
		assertNotNull("serverRegistryDto", serverRegistryDto);

		final Set<Uid> deletedServerIdSet = new HashSet<>(this.deletedServerIds.size());
		for (DeletedUid deletedUid : this.deletedServerIds)
			deletedServerIdSet.add(deletedUid.getUid());

		final List<ServerDto> newServerDtos = new ArrayList<>(serverRegistryDto.getServerDtos().size());
		for (final ServerDto serverDto : serverRegistryDto.getServerDtos()) {
			final Uid serverId = assertNotNull("serverDto.serverId", serverDto.getServerId());
			if (deletedServerIdSet.contains(serverId))
				continue;

			final Server server = getServerByServerId(serverId);
			if (server == null)
				newServerDtos.add(serverDto);
			else
				merge(server, serverDto);
		}

		final Set<DeletedUid> newDeletedServerIds = new HashSet<>(serverRegistryDto.getDeletedServerIds());
		newDeletedServerIds.removeAll(this.deletedServerIds);
		final Map<DeletedUid, Server> newDeletedServers = new HashMap<>(newDeletedServerIds.size());
		for (final DeletedUid deletedServerId : newDeletedServerIds) {
			final Server server = getServerByServerId(deletedServerId.getUid());
			if (server != null)
				newDeletedServers.put(deletedServerId, server);
		}

		final ServerDtoConverter serverDtoConverter = new ServerDtoConverter();
		for (final ServerDto serverDto : newServerDtos) {
			final Server server = serverDtoConverter.fromServerDto(serverDto);
			servers.add(server);
		}

		suppressAddToDeletedServerIds.set(Boolean.TRUE);
		try {
			for (final Map.Entry<DeletedUid, Server> me : newDeletedServers.entrySet()) {
				servers.remove(me.getValue());
				deletedServerIds.add(me.getKey());
			}
		} finally {
			suppressAddToDeletedServerIds.remove();
		}

		writeIfNeeded();
	}

	private void merge(final Server toServer, final ServerDto fromServerDto) {
		assertNotNull("toServer", toServer);
		assertNotNull("fromServerDto", fromServerDto);
		if (toServer.getChanged().before(fromServerDto.getChanged())) {
			toServer.setName(fromServerDto.getName());
			try {
				toServer.setUrl(fromServerDto.getUrl() == null ? null : new URL(fromServerDto.getUrl()));
			} catch (final MalformedURLException e) {
				throw new RuntimeException(e);
			}

			toServer.setChanged(fromServerDto.getChanged());
			if (!toServer.getChanged().equals(fromServerDto.getChanged())) // sanity check - to make sure listeners don't change it again
				throw new IllegalStateException("toServer.changed != fromServerDto.changed");
		}
	}

	protected Server getServerByServerId(final Uid serverId) {
		assertNotNull("serverId", serverId);
		for (final Server server : getServers()) {
			if (serverId.equals(server.getServerId()))
				return server;
		}
		return null;
	}

	@Override
	protected void markDirty() {
		super.markDirty();
		version = new Uid();
		deferredWrite();
	}

	public Uid getVersion() {
		return version;
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}
}
