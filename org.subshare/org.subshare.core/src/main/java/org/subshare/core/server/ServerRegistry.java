package org.subshare.core.server;

import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.List;

import co.codewizards.cloudstore.core.bean.Bean;
import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface ServerRegistry extends Bean<ServerRegistry.Property> {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		servers,
		servers_server
	}

	Server getServerForRemoteRoot(URL remoteRoot);

	List<Server> getServers();

	Server createServer();

	@Override
	void addPropertyChangeListener(PropertyChangeListener listener);

	@Override
	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	/**
	 * Write the data to file immediately, if this instance is dirty.
	 * <p>
	 * <b>Important:</b> You normally do <i>not</i> need to invoke this method, because the data is written
	 * automatically. However, this automatic writing may occur too late in rare situations (e.g. in automated tests).
	 */
	void writeIfNeeded();
}