package org.subshare.core.repo;

import org.subshare.core.server.Server;
import org.subshare.core.user.User;

import co.codewizards.cloudstore.core.oio.File;

public interface ServerRepoManager {

	void createRepository(final File localDirectory, final Server server, final User owner);

}
