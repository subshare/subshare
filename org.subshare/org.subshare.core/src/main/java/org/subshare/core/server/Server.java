package org.subshare.core.server;

import java.beans.PropertyChangeListener;
import java.net.URL;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.Uid;

public interface Server {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		serverId,
		name,
		url
	}

	Uid getServerId();

	String getName();

	void setName(String name);

	URL getUrl();

	void setUrl(URL url);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	Server clone();

}