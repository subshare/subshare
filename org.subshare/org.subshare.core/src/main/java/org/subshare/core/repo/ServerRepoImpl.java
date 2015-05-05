package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static co.codewizards.cloudstore.core.util.Util.equal;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.Uid;

public class ServerRepoImpl implements ServerRepo {

	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private final UUID repositoryId;
	private String name;
	private Uid serverId;

	public ServerRepoImpl(final UUID repositoryId) {
		this.repositoryId = assertNotNull("repositoryId)", repositoryId);
	}

	@Override
	public UUID getRepositoryId() {
		return repositoryId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(final String name) {
		final String old = this.name;
		this.name = name;
		firePropertyChange(PropertyEnum.name, old, name);
	}

	@Override
	public Uid getServerId() {
		return serverId;
	}

	@Override
	public void setServerId(final Uid serverId) {
		if (this.serverId != null && ! equal(this.serverId, serverId)) // TODO do we ever need to support changing this?
			throw new IllegalStateException("Cannot modify serverId!");

		this.serverId = serverId;
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
	public ServerRepo clone() {
		final ServerRepoImpl clone;
		try {
			clone = (ServerRepoImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		clone.propertyChangeSupport = new PropertyChangeSupport(clone);
		return clone;
	}
}
