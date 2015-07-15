package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.core.repo.metaonly.ServerRepoFileType;

public class ServerRepoDirectoryMainTreeItem extends MainTreeItem<ServerRepoFile> {

	private boolean childrenLoaded;

	public ServerRepoDirectoryMainTreeItem(final ServerRepoFile serverRepoFile) {
		super(assertNotNull("serverRepoFile", serverRepoFile));
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

//	/**
//	 * Gets the local clear-text name (decrypted from encrypted meta-data).
//	 * @return the local clear-text name. Never <code>null</code>.
//	 */
//	public String getLocalName() {
//		return localName;
//	}
//
//	/**
//	 * Gets the random name used on the server.
//	 * @return the random name used on the server. Never <code>null</code>.
//	 */
//	public String getServerName() {
//		return serverName;
//	}

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

	private List<MainTreeItem<?>> loadChildren() {
		final List<ServerRepoFile> childServerRepoFiles = getServerRepoFile().getChildren();
		if (childServerRepoFiles == null)
			return null;

		final ArrayList<MainTreeItem<?>> result = new ArrayList<>(childServerRepoFiles.size());
		for (final ServerRepoFile childServerRepoFile : childServerRepoFiles) {
			if (childServerRepoFile.getType() == ServerRepoFileType.DIRECTORY)
				result.add(new ServerRepoDirectoryMainTreeItem(childServerRepoFile));
		}
		return result;
	}
}
