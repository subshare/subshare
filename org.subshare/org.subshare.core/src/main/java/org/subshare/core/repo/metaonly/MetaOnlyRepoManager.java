package org.subshare.core.repo.metaonly;

import java.util.List;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.sync.RepoSyncState;

import co.codewizards.cloudstore.core.oio.File;

public interface MetaOnlyRepoManager {

	List<RepoSyncState> sync();

	ServerRepoFile getRootServerRepoFile(ServerRepo serverRepo);

	File getBaseDir();

	File getLocalRoot(ServerRepo serverRepo);

}