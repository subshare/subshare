package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.IconSize;
import org.subshare.gui.filetree.FileIconRegistry;
import org.subshare.gui.localrepo.directory.LocalRepoDirectoryPane;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class LocalRepoDirectoryMainTreeItem extends MainTreeItem<File> {

	private boolean childrenLoaded;

	private static final FileFilter directoryOnlyFileFilter = file
			-> file.isDirectory() && !LocalRepoManager.META_DIR_NAME.equals(file.getName());

	private static final Comparator<File> fileComparator = (o1, o2) -> o1.getName().compareTo(o2.getName());

	public LocalRepoDirectoryMainTreeItem(final File file) {
		super(assertNotNull("file", file),
				new ImageView(FileIconRegistry.getInstance().getIcon(file, IconSize._16x16)));
	}

	public LocalRepo getLocalRepo() {
		final TreeItem<String> parent = getParent();
		if (parent == null)
			throw new IllegalStateException("parent == null");

		if (parent instanceof LocalRepoDirectoryMainTreeItem)
			return ((LocalRepoDirectoryMainTreeItem) parent).getLocalRepo();

		if (parent instanceof LocalRepoMainTreeItem)
			return ((LocalRepoMainTreeItem) parent).getLocalRepo();

		throw new IllegalStateException("parent is an instance of an unexpected type: " + parent.getClass().getName());
	}

	public File getFile() {
		return getValueObject();
	}

	@Override
	protected String getValueString() {
		final String fileName = getFile().getName();
		if (isEmpty(fileName)) // should never happen (who shares the root?!) but better handle anyway ;-)
			return getFile().getAbsolutePath();
		else
			return fileName;
	}

	@Override
	public ObservableList<TreeItem<String>> getChildren() {
		final ObservableList<TreeItem<String>> children = super.getChildren();
		if (! childrenLoaded) {
			childrenLoaded = true; // *must* be set before clear()/addAll(...), because of events being fired.
			final List<MainTreeItem<File>> c = loadChildren();
			if (c != null)
				children.addAll(c);
		}
		return children;
	}

	private List<MainTreeItem<File>> loadChildren() {
		final File file = getFile();
		final File[] childFiles = file.listFiles(directoryOnlyFileFilter);
		if (childFiles == null)
			return null;

		Arrays.sort(childFiles, fileComparator);
		final List<MainTreeItem<File>> result = new ArrayList<>(childFiles.length);
		for (final File childFile : childFiles)
			result.add(new LocalRepoDirectoryMainTreeItem(childFile));

		return result;
	}

	@Override
	public boolean isLeaf() { // TODO update this?! when? how?
		final File[] childFiles = getFile().listFiles(directoryOnlyFileFilter);
		return childFiles == null || childFiles.length == 0;
	}

	@Override
	protected Parent createMainDetailContent() {
		return new LocalRepoDirectoryPane(getLocalRepo(), getFile());
	}
}
