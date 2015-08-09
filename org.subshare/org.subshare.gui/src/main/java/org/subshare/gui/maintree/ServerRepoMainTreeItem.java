package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.PlatformUtil.*;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.listener.LocalRepoCommitEventListener;
import org.subshare.core.repo.listener.LocalRepoCommitEventManager;
import org.subshare.core.repo.listener.WeakLocalRepoCommitEventListener;
import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.core.server.Server;
import org.subshare.gui.ls.LocalRepoCommitEventManagerLs;
import org.subshare.gui.ls.MetaOnlyRepoManagerLs;
import org.subshare.gui.serverrepo.ServerRepoPane;

public class ServerRepoMainTreeItem extends MainTreeItem<ServerRepo> {

	private static final Image icon = new Image(UserListMainTreeItem.class.getResource("server-repo-16x16.png").toExternalForm());
	private boolean childrenLoaded;

	private LocalRepoCommitEventListener localRepoCommitEventListener;
	private WeakLocalRepoCommitEventListener weakLocalRepoCommitEventListener;

	public ServerRepoMainTreeItem(final ServerRepo serverRepo) {
		super(assertNotNull("serverRepo", serverRepo));
		setGraphic(new ImageView(icon));
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
		assertFxApplicationThread();

		final ObservableList<TreeItem<String>> children = super.getChildren();
		if (! childrenLoaded) {
			childrenLoaded = true;
			final ServerRepoFile rootServerRepoFile = MetaOnlyRepoManagerLs.getMetaOnlyRepoManager().getRootServerRepoFile(getServerRepo());
			if (rootServerRepoFile == null) // not checked out, yet
				hookLocalRepoCommitEventListener();
			else {
				unhookLocalRepoCommitEventListenerIfNeeded();
//				children.clear(); // not needed - should always be empty.
				children.add(new ServerRepoDirectoryMainTreeItem(rootServerRepoFile));
			}
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

	private void hookLocalRepoCommitEventListener() {
		assertFxApplicationThread();

		if (weakLocalRepoCommitEventListener != null)
			throw new IllegalStateException("Already hooked!");

		// TODO we should check, if a ServerRepository was created (i.e. a connection between a local repo and a server repo established)
		// and only do this then! Or can we somehow otherwise reduce unnecessary work? And if possible only do this when a new repository was checked out?!
		localRepoCommitEventListener = event -> runLater(() -> {
			childrenLoaded = false;
			getChildren();
		});

		final LocalRepoCommitEventManager localRepoCommitEventManager = LocalRepoCommitEventManagerLs.getLocalRepoCommitEventManager();
		weakLocalRepoCommitEventListener = new WeakLocalRepoCommitEventListener(localRepoCommitEventManager, localRepoCommitEventListener);
		weakLocalRepoCommitEventListener.addLocalRepoCommitEventListener();
	}

	private void unhookLocalRepoCommitEventListenerIfNeeded() {
		assertFxApplicationThread();

		if (weakLocalRepoCommitEventListener == null)
			return;

		weakLocalRepoCommitEventListener.removeLocalRepoCommitEventListener();
		localRepoCommitEventListener = null;
		weakLocalRepoCommitEventListener = null;
	}
}
