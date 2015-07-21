package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.core.server.Server;
import org.subshare.gui.ls.MetaOnlyRepoManagerLs;
import org.subshare.gui.serverrepo.ServerRepoPane;

public class ServerRepoMainTreeItem extends MainTreeItem<ServerRepo> {

	private boolean childrenLoaded;

	public ServerRepoMainTreeItem(final ServerRepo serverRepo) {
		super(assertNotNull("serverRepo", serverRepo));
	}

	public Server getServer() {
		TreeItem<String> parent = getParent();
		while (parent != null) {
			if (parent instanceof ServerMainTreeItem)
				return ((ServerMainTreeItem) parent).getServer();

			parent = parent.getParent();
		}
		throw new IllegalStateException("Failed to resolve server!");
	}

	public ServerRepo getServerRepo() {
		return getValueObject();
	}

	@Override
	public ObservableList<TreeItem<String>> getChildren() {
		final ObservableList<TreeItem<String>> children = super.getChildren();
		if (! childrenLoaded) {
			childrenLoaded = true;
			final ServerRepoFile rootServerRepoFile = MetaOnlyRepoManagerLs.getMetaOnlyRepoManager().getRootServerRepoFile(getServerRepo());
			if (rootServerRepoFile == null) // not checked out, yet
				; // should we do anything?!
			else
				children.add(new ServerRepoDirectoryMainTreeItem(rootServerRepoFile));
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	protected String getValueString() {
		final ServerRepo serverRepo = getServerRepo();

		final String name = serverRepo.getName();
		return isEmpty(name) ? serverRepo.getRepositoryId().toString() : name;
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerRepoPane(getServer(), getServerRepo());
	}
}
