package org.subshare.core.repo;

import java.beans.PropertyChangeListener;
import java.util.UUID;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.oio.File;

public interface LocalRepo {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		repositoryId,
		name,
		localRoot
	}

	/**
	 * Gets the {@link RepositoryDto#getRepositoryId() repositoryId} of the local repository (<i>not</i> the server's).
	 * @return the repository's ID.
	 */
	UUID getRepositoryId();

	String getName();

	void setName(String name);

	File getLocalRoot();

	void setLocalRoot(File localRoot);

	String getLocalPath(File file);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);
}
