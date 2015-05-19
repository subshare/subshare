package org.subshare.core.repo;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface LocalRepoRegistry {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		localRepos,
		localRepos_localRepo
	}

	List<LocalRepo> getLocalRepos();

	LocalRepo createLocalRepo(UUID repositoryId);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);
}
