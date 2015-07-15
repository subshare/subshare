package org.subshare.core.repo.metaonly;

import java.util.List;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.sync.RepoSyncState;

public interface MetaOnlyRepoManager {

	List<RepoSyncState> sync();

	ServerRepoFile getRootServerRepoFile(ServerRepo serverRepo);

}