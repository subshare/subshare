package org.subshare.core.server;

import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.List;

import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface ServerRegistry {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		servers,
		servers_server
	}

	public abstract Server getServerForRemoteRoot(URL remoteRoot);

	public abstract List<Server> getServers();

	public abstract void writeIfNeeded();

	public abstract void write();

	public abstract void addPropertyChangeListener(
			PropertyChangeListener listener);

	public abstract void addPropertyChangeListener(Property property,
			PropertyChangeListener listener);

	public abstract void removePropertyChangeListener(
			PropertyChangeListener listener);

	public abstract void removePropertyChangeListener(Property property,
			PropertyChangeListener listener);

}