package org.subshare.core.repo;

import java.beans.PropertyChangeListener;
import java.util.UUID;

import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.Uid;

/**
 * Repository on a server.
 * <p>
 * Local repositories (on the local machine) are <b>not</b> managed by the {@link ServerRepoRegistry} and thus
 * an instance of {@code ServerRepo} never represents a local repository.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface ServerRepo {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		repositoryId,
		name,
		serverId
	}

	/**
	 * Gets the {@link RepositoryDto#getRepositoryId() repositoryId} of the server's repository.
	 * @return the repository's ID.
	 */
	UUID getRepositoryId();

	String getName();

	void setName(String name);

	/**
	 * Gets the {@link Server#getServerId() serverId} of the server on which the repository is hosted.
	 * @return the server's ID.
	 */
	Uid getServerId();

	/**
	 * Sets the {@link Server#getServerId() serverId} of the server on which the repository is hosted.
	 * @param serverId the server's ID.
	 */
	void setServerId(Uid serverId);

	ServerRepo clone();

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

}
