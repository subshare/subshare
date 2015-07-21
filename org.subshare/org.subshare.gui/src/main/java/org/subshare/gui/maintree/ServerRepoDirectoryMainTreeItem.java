package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.core.repo.metaonly.ServerRepoFileType;
import org.subshare.core.server.Server;
import org.subshare.gui.serverrepo.directory.ServerRepoDirectoryPane;

public class ServerRepoDirectoryMainTreeItem extends MainTreeItem<ServerRepoFile> {

	private static final Image icon = new Image(ServerRepoDirectoryMainTreeItem.class.getResource("server-repo-directory-16x16.png").toExternalForm());

	private boolean childrenLoaded;

	public ServerRepoDirectoryMainTreeItem(final ServerRepoFile serverRepoFile) {
		super(assertNotNull("serverRepoFile", serverRepoFile));
		setGraphic(new ImageView(icon));
//		setGraphic(new ImageView(FileIconRegistry.getInstance().getIcon(FileIconRegistry.ICON_ID_DIRECTORY, IconSize._16x16)));
	}

	public Server getServer() {
		return getServerRepoFile().getServer();
	}

	@Override
	protected String getValueString() {
		final String localName = getServerRepoFile().getLocalName();
		return isEmpty(localName) ? "/" : localName;
	}

	public ServerRepoFile getServerRepoFile() {
		return getValueObject();
	}

	public ServerRepo getServerRepo() {
		final TreeItem<String> parent = getParent();
		if (parent == null)
			throw new IllegalStateException("parent == null");

		if (parent instanceof ServerRepoDirectoryMainTreeItem)
			return ((ServerRepoDirectoryMainTreeItem) parent).getServerRepo();

		if (parent instanceof ServerRepoMainTreeItem)
			return ((ServerRepoMainTreeItem) parent).getServerRepo();

		throw new IllegalStateException("parent is an instance of an unexpected type: " + parent.getClass().getName());
	}

	@Override
	public ObservableList<TreeItem<String>> getChildren() {
		final ObservableList<TreeItem<String>> children = super.getChildren();
		if (! childrenLoaded) {
			childrenLoaded = true; // *must* be set before clear()/addAll(...), because of events being fired.
			final List<MainTreeItem<?>> c = loadChildren();
			if (c != null)
				children.addAll(c);
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	private List<MainTreeItem<?>> loadChildren() {
		final List<ServerRepoFile> childServerRepoFiles = getServerRepoFile().getChildren();
		if (childServerRepoFiles == null)
			return null;

		final List<ServerRepoFile> filtered = new ArrayList<ServerRepoFile>(childServerRepoFiles.size());
		for (ServerRepoFile childServerRepoFile : childServerRepoFiles) {
			if (childServerRepoFile.getType() == ServerRepoFileType.DIRECTORY)
				filtered.add(childServerRepoFile);
		}

		Collections.sort(filtered, (o1, o2) -> o1.getLocalName().compareTo(o2.getLocalName()));

		final ArrayList<MainTreeItem<?>> result = new ArrayList<>(filtered.size());
		for (final ServerRepoFile childServerRepoFile : filtered)
				result.add(new ServerRepoDirectoryMainTreeItem(childServerRepoFile));

		return result;
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerRepoDirectoryPane(getServerRepoFile());
	}
}
