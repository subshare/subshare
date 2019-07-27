package org.subshare.core.sync;

import static java.util.Objects.*;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.oio.File;

public class RepoSyncState extends co.codewizards.cloudstore.core.repo.sync.RepoSyncState {
	private static final long serialVersionUID = 1L;

	private final Server server;
	private final ServerRepo serverRepo;
//	private final File localRoot;

	public RepoSyncState(UUID localRepositoryId, ServerRepo serverRepo, Server server, File localRoot, URL url, Severity severity, String message, Error error, Date syncStarted, Date syncFinished) {
		super(localRepositoryId, requireNonNull(serverRepo, "serverRepo").getRepositoryId(), localRoot, url, severity, message, error, syncStarted, syncFinished);
		this.serverRepo = serverRepo;
		this.server = requireNonNull(server, "server");
//		this.localRoot = requireNonNull("localRoot", localRoot);
	}

	public Server getServer() {
		return server;
	}

	public ServerRepo getServerRepo() {
		return serverRepo;
	}

//	@Override
//	public File getLocalRoot() {
//		return localRoot;
//	}
}
