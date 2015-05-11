package org.subshare.core.pgp.sync;

import java.beans.PropertyChangeListener;
import java.util.Set;

import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface PgpSyncDaemon {
	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		states,

		states_added,
		states_removed
	}

	Set<PgpSyncState> getStates();

	PgpSyncState getState(Server server);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);
}
