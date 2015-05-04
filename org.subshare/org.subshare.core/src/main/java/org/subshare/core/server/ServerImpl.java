package org.subshare.core.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;

import co.codewizards.cloudstore.core.dto.Uid;

public class ServerImpl implements Cloneable, Server {

	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public ServerImpl() {
		this(null);
	}

	public ServerImpl(final Uid serverId) {
		this.serverId = serverId == null ? new Uid() : serverId;
	}

	private Uid serverId;

	private String name;

	private URL url;

	@Override
	public Uid getServerId() {
		return serverId;
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
	public URL getUrl() {
		return url;
	}
	@Override
	public void setUrl(final URL url) {
		final URL old = this.url;
		this.url = url;
		firePropertyChange(PropertyEnum.url, old, url);
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
	public Server clone() {
		final ServerImpl clone;
		try {
			clone = (ServerImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		clone.propertyChangeSupport = new PropertyChangeSupport(clone);
		return clone;
	}
}
