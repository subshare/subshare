package org.subshare.core.repo;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.Uid;

public interface ServerRepoRegistry {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		serverRepos,
		serverRepos_serverRepo
	}

	List<ServerRepo> getServerRepos();

	List<ServerRepo> getServerReposOfServer(Uid serverId);

	ServerRepo createServerRepo(UUID repositoryId);

	void writeIfNeeded();

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

}
