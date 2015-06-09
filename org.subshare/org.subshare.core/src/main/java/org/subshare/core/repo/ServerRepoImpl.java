package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.Uid;

public class ServerRepoImpl implements ServerRepo {

	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private final UUID repositoryId;
	private String name;
	private Uid serverId;
	private Uid userId;
	private Date changed = new Date();

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
		updateChanged();
	}

	@Override
	public Uid getServerId() {
		return serverId;
	}

	@Override
	public void setServerId(final Uid serverId) {
		if (this.serverId != null && ! equal(this.serverId, serverId)) // TODO do we ever need to support changing this?
			throw new IllegalStateException("Cannot modify serverId!");

		final Uid old = this.serverId;
		this.serverId = serverId;
		firePropertyChange(PropertyEnum.serverId, old, serverId);
		updateChanged();
	}

	@Override
	public Uid getUserId() {
		return userId;
	}
	@Override
	public void setUserId(Uid userId) {
		if (this.userId != null && ! equal(this.userId, userId)) // TODO do we ever need to support changing this?
			throw new IllegalStateException("Cannot modify userId!");

		final Uid old = this.userId;
		this.userId = userId;
		firePropertyChange(PropertyEnum.userId, old, userId);
		updateChanged();
	}

	@Override
	public Date getChanged() {
		return changed;
	}
	@Override
	public void setChanged(final Date changed) {
		assertNotNull("changed", changed);
		final Date old = this.changed;
		this.changed = changed;
		firePropertyChange(PropertyEnum.changed, old, changed);
	}

	protected void updateChanged() {
		setChanged(new Date());
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
