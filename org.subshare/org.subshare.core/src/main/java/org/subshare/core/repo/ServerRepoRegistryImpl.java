package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.core.file.FileConst.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.subshare.core.dto.DeletedUUID;
import org.subshare.core.dto.ServerRepoDto;
import org.subshare.core.dto.ServerRepoRegistryDto;
import org.subshare.core.dto.jaxb.ServerRepoRegistryDtoIo;
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

public class ServerRepoRegistryImpl extends FileBasedObjectRegistry implements ServerRepoRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ServerRepoRegistryImpl.class);

	private static final String PAYLOAD_ENTRY_NAME = ServerRepoRegistryDto.class.getSimpleName() + ".xml";

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public static final String SERVER_REPO_REGISTRY_FILE_NAME = "serverRepoRegistry" + SUBSHARE_FILE_EXTENSION;

	private Map<UUID, ServerRepo> repositoryId2ServerRepo;
	private final ObservableList<ServerRepo> serverRepos;
	private final List<DeletedUUID> deletedServerRepoIds = new CopyOnWriteArrayList<>();
	private final ThreadLocal<Boolean> suppressAddToDeletedServerRepoIds = new ThreadLocal<Boolean>();
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
	private final File serverRepoRegistryFile;
	private boolean dirty;
	private Uid version;

	private static final class Holder {
		public static final ServerRepoRegistryImpl instance = new ServerRepoRegistryImpl();
	}

	private class PreModificationListener implements StandardPreModificationListener {
		@Override
		public void modificationOccurring(StandardPreModificationEvent event) {
			@SuppressWarnings("unchecked")
			final Collection<ServerRepo> changeCollection = event.getChangeCollection();

			if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
				for (ServerRepo serverRepo : changeCollection) {
					if (getRepositoryId2ServerRepo().get(serverRepo.getRepositoryId()) != null)
						throw new UnsupportedOperationException(String.format("Cannot add the same ServerRepo (repositoryId=%s) twice!", serverRepo.getRepositoryId()));

					serverRepo.addPropertyChangeListener(serverRepoPropertyChangeListener);
					removeDeletedServerRepoId(serverRepo.getRepositoryId()); // support re-adding - or should we not support this?
				}
			}
			else if ((ModificationEventType.GROUP_REMOVE & event.getType()) != 0) {
				for (ServerRepo serverRepo : changeCollection) {
					serverRepo.removePropertyChangeListener(serverRepoPropertyChangeListener);
					addToDeletedServerRepoIds(serverRepo.getRepositoryId());
				}
			}
			else if ((ModificationEventType.GROUP_CLEAR & event.getType()) != 0) {
				for (ServerRepo serverRepo : serverRepos) { // *all* instead of (empty!) changeCollection
					serverRepo.removePropertyChangeListener(serverRepoPropertyChangeListener);
					addToDeletedServerRepoIds(serverRepo.getRepositoryId());
				}
			}
			else if ((ModificationEventType.GROUP_RETAIN & event.getType()) != 0) {
				for (ServerRepo serverRepo : serverRepos) {
					if (!changeCollection.contains(serverRepo)) { // IMHO changeCollection is the retained collection, i.e. all elements *not* contained there are removed.
						serverRepo.removePropertyChangeListener(serverRepoPropertyChangeListener);
						addToDeletedServerRepoIds(serverRepo.getRepositoryId());
					}
				}
			}
		}
	}

	private void removeDeletedServerRepoId(final UUID repositoryId) {
		final List<DeletedUUID> deletedUidsToRemove = new ArrayList<>();
		for (final DeletedUUID deletedUUID : deletedServerRepoIds) {
			if (repositoryId.equals(deletedUUID.getUuid()))
				deletedUidsToRemove.add(deletedUUID);
		}
		deletedServerRepoIds.removeAll(deletedUidsToRemove);
	}

	private void addToDeletedServerRepoIds(final UUID repositoryId) {
		if (! Boolean.TRUE.equals(suppressAddToDeletedServerRepoIds.get()))
			deletedServerRepoIds.add(new DeletedUUID(repositoryId));
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
		serverRepoRegistryFile = createFile(ConfigDir.getInstance().getFile(), SERVER_REPO_REGISTRY_FILE_NAME);

		read();
	}

	@Override
	protected String getContentType() {
		return "application/vnd.subshare.server-repo-registry";
	}

	@Override
	protected File getFile() {
		return serverRepoRegistryFile;
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

	@Override
	protected void read(InputStream in) throws IOException {
		version = null;

		super.read(in);

		if (version == null)
			version = new Uid();
	}

	@Override
	protected void readPayloadEntry(ZipInputStream zin, ZipEntry zipEntry) throws IOException {
		if (!PAYLOAD_ENTRY_NAME.equals(zipEntry.getName())) {
			logger.warn("readPayloadEntry: Ignoring unexpected zip-entry: {}", zipEntry.getName());
			return;
		}
		final ServerRepoDtoConverter serverRepoDtoConverter = new ServerRepoDtoConverter();
		final ServerRepoRegistryDtoIo serverRepoRegistryDtoIo = new ServerRepoRegistryDtoIo();
		final ServerRepoRegistryDto serverRepoRegistryDto = serverRepoRegistryDtoIo.deserialize(zin);

		for (final ServerRepoDto serverRepoDto : serverRepoRegistryDto.getServerRepoDtos()) {
			final ServerRepo serverRepo = serverRepoDtoConverter.fromServerRepoDto(serverRepoDto);
			serverRepos.add(serverRepo);
		}

		deletedServerRepoIds.clear();
		deletedServerRepoIds.addAll(serverRepoRegistryDto.getDeletedServerRepoIds());

		version = serverRepoRegistryDto.getVersion();
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

	@Override
	public synchronized void writeIfNeeded() {
		if (dirty)
			write();
	}

	@Override
	protected void writePayload(ZipOutputStream zout) throws IOException {
		final ServerRepoRegistryDtoIo serverRepoRegistryDtoIo = new ServerRepoRegistryDtoIo();
		final ServerRepoRegistryDto serverRepoRegistryDto = createServerRepoListDto();

		zout.putNextEntry(new ZipEntry(PAYLOAD_ENTRY_NAME));
		serverRepoRegistryDtoIo.serialize(serverRepoRegistryDto, zout);
		zout.closeEntry();
	}

	private ServerRepoRegistryDto createServerRepoListDto() {
		final ServerRepoDtoConverter converter = new ServerRepoDtoConverter();
		final ServerRepoRegistryDto result = new ServerRepoRegistryDto();
		for (ServerRepo serverRepo : serverRepos) {
			final ServerRepoDto serverRepoDto = converter.toServerRepoDto(serverRepo);
			result.getServerRepoDtos().add(serverRepoDto);
		}
		result.getDeletedServerRepoIds().addAll(deletedServerRepoIds);
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
		final ServerRepoRegistryDtoIo serverRepoRegistryDtoIo = new ServerRepoRegistryDtoIo();
		final ServerRepoRegistryDto serverRepoRegistryDto = serverRepoRegistryDtoIo.deserialize(zin);
		mergeFrom(serverRepoRegistryDto);
	}

	protected synchronized void mergeFrom(final ServerRepoRegistryDto serverRepoRegistryDto) {
		assertNotNull("serverRepoRegistryDto", serverRepoRegistryDto);

		final List<ServerRepoDto> newServerRepoDtos = new ArrayList<>(serverRepoRegistryDto.getServerRepoDtos().size());
		for (final ServerRepoDto serverRepoDto : serverRepoRegistryDto.getServerRepoDtos()) {
			final UUID repositoryId = assertNotNull("serverRepoDto.repositoryId", serverRepoDto.getRepositoryId());
			final ServerRepo serverRepo = getRepositoryId2ServerRepo().get(repositoryId);
			if (serverRepo == null)
				newServerRepoDtos.add(serverRepoDto);
			else
				merge(serverRepo, serverRepoDto);
		}

		final Set<DeletedUUID> newDeletedServerRepoIds = new HashSet<>(serverRepoRegistryDto.getDeletedServerRepoIds());
		newDeletedServerRepoIds.removeAll(this.deletedServerRepoIds);
		final Map<DeletedUUID, ServerRepo> newDeletedServerRepos = new HashMap<>(newDeletedServerRepoIds.size());
		for (final DeletedUUID deletedServerRepoId : newDeletedServerRepoIds) {
			final ServerRepo serverRepo = getRepositoryId2ServerRepo().get(deletedServerRepoId.getUuid());
			if (serverRepo != null)
				newDeletedServerRepos.put(deletedServerRepoId, serverRepo);
		}

		final ServerRepoDtoConverter serverRepoDtoConverter = new ServerRepoDtoConverter();
		for (final ServerRepoDto serverRepoDto : newServerRepoDtos) {
			final ServerRepo serverRepo = serverRepoDtoConverter.fromServerRepoDto(serverRepoDto);
			serverRepos.add(serverRepo);
		}

		suppressAddToDeletedServerRepoIds.set(Boolean.TRUE);
		try {
			for (final Map.Entry<DeletedUUID, ServerRepo> me : newDeletedServerRepos.entrySet()) {
				serverRepos.remove(me.getValue());
				deletedServerRepoIds.add(me.getKey());
			}
		} finally {
			suppressAddToDeletedServerRepoIds.remove();
		}

		writeIfNeeded();
	}

	private void merge(final ServerRepo toServerRepo, final ServerRepoDto fromServerRepoDto) {
		assertNotNull("toServerRepo", toServerRepo);
		assertNotNull("fromServerRepoDto", fromServerRepoDto);
		if (toServerRepo.getChanged().before(fromServerRepoDto.getChanged())) {
			toServerRepo.setName(fromServerRepoDto.getName());
			toServerRepo.setServerId(fromServerRepoDto.getServerId());
			toServerRepo.setUserId(fromServerRepoDto.getUserId());

			toServerRepo.setChanged(fromServerRepoDto.getChanged());
			if (!toServerRepo.getChanged().equals(fromServerRepoDto.getChanged())) // sanity check - to make sure listeners don't change it again
				throw new IllegalStateException("toServerRepo.changed != fromServerRepoDto.changed");
		}
	}

	public Uid getVersion() {
		return version;
	}
}
