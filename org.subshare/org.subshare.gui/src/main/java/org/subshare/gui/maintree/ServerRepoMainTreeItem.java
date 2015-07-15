package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.gui.ls.MetaOnlyRepoManagerLs;

public class ServerRepoMainTreeItem extends MainTreeItem<ServerRepo> {

	public ServerRepoMainTreeItem(final ServerRepo serverRepo) {
		super(assertNotNull("serverRepo", serverRepo));

		final ServerRepoFile rootServerRepoFile = MetaOnlyRepoManagerLs.getMetaOnlyRepoManager().getRootServerRepoFile(serverRepo);
		getChildren().add(new ServerRepoDirectoryMainTreeItem(rootServerRepoFile));
	}

	public ServerRepo getServerRepo() {
		return getValueObject();
	}

	@Override
	protected String getValueString() {
		final ServerRepo serverRepo = getServerRepo();

		final String name = serverRepo.getName();
		return isEmpty(name) ? serverRepo.getRepositoryId().toString() : name;
	}

//	@Override
//	protected Parent createMainDetailContent() { // TODO implement!
//		return new ServerRepoPane(getServerRepo());
//	}
}
