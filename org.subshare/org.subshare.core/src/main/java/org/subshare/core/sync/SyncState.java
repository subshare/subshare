package org.subshare.core.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.Date;

import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.dto.Error;

public class SyncState extends co.codewizards.cloudstore.core.sync.SyncState {
	private static final long serialVersionUID = 1L;

	private final Server server;

	public SyncState(final Server server, final URL url, final Severity severity, final String message, final Error error, Date syncStarted, Date syncFinished) {
		super(url, severity, message, error, syncStarted, syncFinished);
		this.server = assertNotNull(server, "server");
	}

	public Server getServer() {
		return server;
	}

	/**
	 * Gets the URL that was used for the sync. This might have been changed already and thus does not need to match
	 * {@link Server#getUrl() Server.url}, anymore.
	 * @return the URL that was used for the sync.
	 */
	@Override
	public URL getUrl() {
		return super.getUrl();
	}
}
