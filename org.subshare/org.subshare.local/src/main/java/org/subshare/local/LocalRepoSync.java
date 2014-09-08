package org.subshare.local;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class LocalRepoSync extends co.codewizards.cloudstore.local.LocalRepoSync {

	// TODO we must store in the DB whether this is a repo on the server!
	// => CryptreeImpl.isOnServer()
	// We should then skip the local sync. The server should never do a local sync.

	protected LocalRepoSync(final LocalRepoTransaction transaction) {
		super(transaction);
	}

	@Override
	public RepoFile sync(final File file, final ProgressMonitor monitor) {
		return super.sync(file, monitor);
	}

}
