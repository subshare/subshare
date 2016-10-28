package org.subshare.core.repo;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.bean.Bean;
import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface ServerRepoRegistry extends Bean<ServerRepoRegistry.Property> {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		serverRepos,
		serverRepos_serverRepo
	}

	List<ServerRepo> getServerRepos();

	List<ServerRepo> getServerReposOfServer(Uid serverId);

	ServerRepo createServerRepo(UUID repositoryId);

	ServerRepo getServerRepo(UUID repositoryId);

	@Override
	void addPropertyChangeListener(PropertyChangeListener listener);

	@Override
	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	/**
	 * Write the data to file immediately, if this instance is dirty.
	 * <p>
	 * <b>Important:</b> You normally do <i>not</i> need to invoke this method, because the data is written
	 * automatically. However, this automatic writing may occur too late in rare situations (e.g. in automated tests).
	 */
	void writeIfNeeded();
}
