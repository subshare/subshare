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

	Server getServerForRemoteRoot(URL remoteRoot);

	List<Server> getServers();

	Server createServer();

	void writeIfNeeded();

	void write();

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);
}