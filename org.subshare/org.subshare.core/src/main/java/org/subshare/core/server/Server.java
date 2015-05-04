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

	public abstract Uid getServerId();

	public abstract String getName();

	public abstract void setName(String name);

	public abstract URL getUrl();

	public abstract void setUrl(URL url);

	public abstract void addPropertyChangeListener(
			PropertyChangeListener listener);

	public abstract void addPropertyChangeListener(Property property,
			PropertyChangeListener listener);

	public abstract void removePropertyChangeListener(
			PropertyChangeListener listener);

	public abstract void removePropertyChangeListener(Property property,
			PropertyChangeListener listener);

	public abstract Server clone();

}