package org.subshare.core.pgp.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.net.URL;

import org.subshare.core.Severity;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.dto.Error;

public class PgpSyncState implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Server server;

	private final URL url;

	private final Severity severity;

	private final String message;

	private final Error error;

	public PgpSyncState(final Server server, final URL url, final Severity severity, final String message, final Error error) {
		this.server = assertNotNull("server", server);
		this.url = assertNotNull("url", url);
		this.severity = assertNotNull("severity", severity);
		this.message = message;
		this.error = error;
	}

	public Server getServer() {
		return server;
	}

	/**
	 * Gets the URL that was used for the sync. This might have been changed already and thus does not need to match
	 * {@link Server#getUrl() Server.url}, anymore.
	 * @return the URL that was used for the sync.
	 */
	public URL getUrl() {
		return url;
	}

	public Severity getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public Error getError() {
		return error;
	}
}
