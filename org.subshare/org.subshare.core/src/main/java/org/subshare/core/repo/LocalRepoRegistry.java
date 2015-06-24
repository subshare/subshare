package org.subshare.core.repo;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.bean.Bean;
import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface LocalRepoRegistry extends Bean<LocalRepoRegistry.Property> {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		localRepos,
		localRepos_localRepo
	}

	List<LocalRepo> getLocalRepos();

	LocalRepo createLocalRepo(UUID repositoryId);

	@Override
	void addPropertyChangeListener(PropertyChangeListener listener);

	@Override
	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(Property property, PropertyChangeListener listener);
}
