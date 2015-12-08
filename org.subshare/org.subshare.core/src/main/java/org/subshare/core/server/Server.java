package org.subshare.core.server;

import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Date;

import co.codewizards.cloudstore.core.bean.CloneableBean;
import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.dto.Uid;

public interface Server extends CloneableBean<Server.Property> {

	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		serverId,
		name,
		url,
		changed
	}

	Uid getServerId();

	String getName();

	void setName(String name);

	URL getUrl();

	void setUrl(URL url);

	Date getChanged();

	void setChanged(Date changed);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	Server clone();
}