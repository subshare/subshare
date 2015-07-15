package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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

import org.subshare.core.observable.ModificationEventType;
import org.subshare.core.observable.ObservableList;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.observable.standard.StandardPreModificationEvent;
import org.subshare.core.observable.standard.StandardPreModificationListener;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;

public class LocalRepoRegistryImpl implements LocalRepoRegistry {
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry backendLocalRepoRegistry;

	private Map<UUID, LocalRepo> repositoryId2LocalRepo;
	private final ObservableList<LocalRepo> localRepos;
	private final PreModificationListener preModificationListener = new PreModificationListener();
	private final PostModificationListener postModificationListener = new PostModificationListener();
	private boolean backendLocalRepoRegistryChangeListenerIgnored;

	private final PropertyChangeListener localRepoPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			repositoryId2LocalRepo = null;
			final LocalRepo localRepo = (LocalRepo) evt.getSource();

			if (LocalRepo.PropertyEnum.name.name().equals(evt.getPropertyName())) {
				synchronized (LocalRepoRegistryImpl.this) {
					backendLocalRepoRegistryChangeListenerIgnored = true;
					try {
						final co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry backendLocalRepoRegistry = getBackendLocalRepoRegistry();
						final UUID repositoryId = localRepo.getRepositoryId();

						final Collection<String> repositoryAliases = backendLocalRepoRegistry.getRepositoryAliases(repositoryId.toString());
						if (repositoryAliases != null) {
							for (final String repositoryAlias : repositoryAliases)
								backendLocalRepoRegistry.removeRepositoryAlias(repositoryAlias);
						}

						final String newName = (String) evt.getNewValue();
						if (! isEmpty(newName))
							backendLocalRepoRegistry.putRepositoryAlias(newName, repositoryId);
					} finally {
						backendLocalRepoRegistryChangeListenerIgnored = false;
					}
				}
			}
			firePropertyChange(PropertyEnum.localRepos_localRepo, null, localRepo);
		}
	};

	protected LocalRepoRegistryImpl() {
		localRepos = ObservableList.decorate(new CopyOnWriteArrayList<LocalRepo>());
		localRepos.getHandler().addPreModificationListener(preModificationListener);
		localRepos.getHandler().addPostModificationListener(postModificationListener);

		read();
	}

	private void read() {
		final String configDirStr = ConfigDir.getInstance().getFile().getAbsolutePath() + java.io.File.separatorChar;
		for (final UUID repositoryId : getBackendLocalRepoRegistry().getRepositoryIds()) {
			final File localRoot = getBackendLocalRepoRegistry().getLocalRoot(repositoryId);
			if (localRoot == null)
				continue;

			// Exclude the meta-only repos, which are all inside our config-dir.
			if (localRoot.getAbsolutePath().startsWith(configDirStr))
				continue;

			final LocalRepo localRepo = createPopulatedLocalRepo(repositoryId);

			if (localRepo != null)
				localRepos.add(localRepo);
		}
	}

	private LocalRepo createPopulatedLocalRepo(final UUID repositoryId) {
		final co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry backendLocalRepoRegistry = getBackendLocalRepoRegistry();
		final LocalRepo localRepo = createLocalRepo(repositoryId);

		final File localRoot = backendLocalRepoRegistry.getLocalRoot(repositoryId);
		if (localRoot == null)
			return null;

		localRepo.setLocalRoot(localRoot);

		final Collection<String> repositoryAliases = backendLocalRepoRegistry.getRepositoryAliases(repositoryId.toString());
		if (repositoryAliases != null && !repositoryAliases.isEmpty())
			localRepo.setName(repositoryAliases.iterator().next());

		return localRepo;
	}

	@Override
	public List<LocalRepo> getLocalRepos() {
		return Collections.unmodifiableList(localRepos);
	}

	private static final class Holder {
		public static final LocalRepoRegistry instance = new LocalRepoRegistryImpl();
	}

	public static LocalRepoRegistry getInstance() {
		return Holder.instance;
	}

	private class PreModificationListener implements StandardPreModificationListener {
		@Override
		public void modificationOccurring(StandardPreModificationEvent event) {
			@SuppressWarnings("unchecked")
			final Collection<LocalRepo> changeCollection = event.getChangeCollection();

			if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
				for (LocalRepo localRepo : changeCollection)
					localRepo.addPropertyChangeListener(localRepoPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_REMOVE & event.getType()) != 0) {
				for (LocalRepo localRepo : changeCollection)
					localRepo.removePropertyChangeListener(localRepoPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_CLEAR & event.getType()) != 0) {
				for (LocalRepo localRepo : localRepos) // *all* instead of (empty!) changeCollection
					localRepo.removePropertyChangeListener(localRepoPropertyChangeListener);
			}
			else if ((ModificationEventType.GROUP_RETAIN & event.getType()) != 0) {
				for (LocalRepo localRepo : localRepos) {
					if (!changeCollection.contains(localRepo)) // IMHO changeCollection is the retained collection, i.e. all elements *not* contained there are removed.
						localRepo.removePropertyChangeListener(localRepoPropertyChangeListener);
				}
			}
		}
	}

	private class PostModificationListener implements StandardPostModificationListener {
		@Override
		public void modificationOccurred(StandardPostModificationEvent event) {
			repositoryId2LocalRepo = null;
			firePropertyChange(PropertyEnum.localRepos, null, getLocalRepos());
		}
	};

	protected synchronized co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry getBackendLocalRepoRegistry() {
		if (backendLocalRepoRegistry == null) {
			backendLocalRepoRegistry = co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl.getInstance();
			backendLocalRepoRegistry.getRepositoryIds(); // force loading *before* hooking listener.
			backendLocalRepoRegistry.addPropertyChangeListener(backendLocalRepoRegistryChangeListener);
		}
		return backendLocalRepoRegistry;
	}

	private final PropertyChangeListener backendLocalRepoRegistryChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			synchronized (LocalRepoRegistryImpl.this) {
				if (backendLocalRepoRegistryChangeListenerIgnored)
					return;

				final co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry backendLocalRepoRegistry = getBackendLocalRepoRegistry();
				final Set<UUID> repositoryIds = new HashSet<UUID>(backendLocalRepoRegistry.getRepositoryIds());

				final List<LocalRepo> newLocalRepos = new ArrayList<>();
				for (final UUID repositoryId : repositoryIds) {
					LocalRepo localRepo = getRepositoryId2LocalRepo().get(repositoryId);
					if (localRepo == null) {
						localRepo = createPopulatedLocalRepo(repositoryId);

						if (localRepo != null)
							newLocalRepos.add(localRepo);
					}
				}

				for (final LocalRepo localRepo : localRepos) {
					final UUID repositoryId = localRepo.getRepositoryId();
					final File localRoot = backendLocalRepoRegistry.getLocalRoot(repositoryId);
					if (localRoot == null)
						continue;

					localRepo.setLocalRoot(localRoot);
					final Collection<String> repositoryAliases = backendLocalRepoRegistry.getRepositoryAliases(repositoryId.toString());
					if (repositoryAliases != null) {
						if (repositoryAliases.isEmpty())
							localRepo.setName(null);
						else
							localRepo.setName(repositoryAliases.iterator().next());
					}
				}

				for (final LocalRepo localRepo : newLocalRepos)
					localRepos.add(localRepo);

				if (repositoryIds.size() < localRepos.size()) {
					final List<LocalRepo> localReposToBeRemoved = new ArrayList<>();
					for (final LocalRepo localRepo : localRepos) {
						if (!repositoryIds.contains(localRepo.getRepositoryId()))
							localReposToBeRemoved.add(localRepo);
					}
					localRepos.removeAll(localReposToBeRemoved);
				}
			}
		}
	};

	protected synchronized Map<UUID, LocalRepo> getRepositoryId2LocalRepo() {
		if (repositoryId2LocalRepo == null) {
			final Map<UUID, LocalRepo> m = new HashMap<>();
			for (LocalRepo localRepo : getLocalRepos())
				m.put(localRepo.getRepositoryId(), localRepo);

			repositoryId2LocalRepo = Collections.unmodifiableMap(m);
		}
		return repositoryId2LocalRepo;
	}

	@Override
	protected void finalize() throws Throwable {
		final co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry backendLocalRepoRegistry = this.backendLocalRepoRegistry;
		if (backendLocalRepoRegistry != null)
			backendLocalRepoRegistry.removePropertyChangeListener(backendLocalRepoRegistryChangeListener);

		super.finalize();
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
	public LocalRepo createLocalRepo(final UUID repositoryId) {
		return new LocalRepoImpl(repositoryId);
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}
}
