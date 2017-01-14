package org.subshare.gui.filetree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TreeItem;

public class DirectoryFileTreeItem extends FileFileTreeItem {

	private final InvalidationListener updateInvalidationListener = observable -> refresh();
	private int allChildrenCount = -1;
	private int filteredChildrenCount = -1;

	public DirectoryFileTreeItem(File file) {
		super(file);

		parentProperty().addListener((ChangeListener<TreeItem<FileTreeItem<?>>>) (observable, oldValue, newValue) -> {
			final FileTreePane oldFileTreePane = oldValue == null ? null : oldValue.getValue().getFileTreePane();
			final FileTreePane newFileTreePane = newValue == null ? null : newValue.getValue().getFileTreePane();
			if (oldFileTreePane != newFileTreePane) {
				if (oldFileTreePane != null)
					unhookUpdateInvalidationListener(oldFileTreePane);

				if (newFileTreePane != null)
					hookUpdateInvalidationListener(newFileTreePane);
			}
		});
	}

	protected void unhookUpdateInvalidationListener(FileTreePane fileTreePane) {
		fileTreePane.showHiddenFilesProperty().removeListener(updateInvalidationListener);
		fileTreePane.fileFilterProperty().removeListener(updateInvalidationListener);
	}

	protected void hookUpdateInvalidationListener(FileTreePane fileTreePane) {
		fileTreePane.showHiddenFilesProperty().addListener(updateInvalidationListener);
		fileTreePane.fileFilterProperty().addListener(updateInvalidationListener);
	}

	@Override
	protected void refresh() {
		super.refresh();
		refreshSize();
		refreshChildren();
	}

	private void refreshSize() {
		allChildrenCount = -1;
		filteredChildrenCount = -1;
		sizeProperty().set(_getSize());
	}

	private String _getSize() {
		final int allChildrenCount = getAllChildrenCount();
		final int filteredChildrenCount = getFilteredChildrenCount();

		if (filteredChildrenCount != allChildrenCount)
			return String.format("%d (%d) items", filteredChildrenCount, allChildrenCount);

		if (allChildrenCount == 1)
			return String.format("%d item", allChildrenCount);

		return String.format("%d items", allChildrenCount);
	}

	public int getAllChildrenCount() {
		if (allChildrenCount == -1) {
			final String[] allChildren = getFile().list();
			allChildrenCount = allChildren == null ? 0 : allChildren.length;
		}
		return allChildrenCount;
	}

	public int getFilteredChildrenCount() {
		if (filteredChildrenCount == -1) {
			final FileFilter combinedFileFilter = getCombinedFileFilter();
			if (combinedFileFilter == null)
				filteredChildrenCount = getAllChildrenCount();
			else {
				final File[] filteredChildren = getFile().listFiles(combinedFileFilter);
				filteredChildrenCount = filteredChildren == null ? 0 : filteredChildren.length;
			}
		}
		return filteredChildrenCount;
	}

	@Override
	public boolean isLeaf() {
		if (getParent() == null)
			return false;

		return getFilteredChildrenCount() == 0;
	}

	protected void refreshChildren() {
		if (! isChildrenLoaded())
			return; // nothing loaded => nothing to update ;-)

		final File[] children = listChildFiles();
		if (children == null || children.length == 0) {
			getChildren().clear();
			return;
		}

		// Note: The following algorithm relies on the children being alphabetically sorted!

		// TODO fix handling of sorted table! when the table is sorted, this algorithm basically removes and re-adds (nearly)
		// everything, because getChildren() returns the items in the same order as the UI shows them - not matching the order here.

		// *Temporarily*, we handle it by disabling the sorting and then re-sorting the children before our sync algo below.
		getFileTreePane().getTreeTableView().getSortOrder().clear(); // this disables sorting (in the UI, too)
		getChildren().sort((o1, o2) -> getFileName(o1).compareTo(getFileName(o2))); // this is needed, because disabling the sorting does not restore the original order.

		final ListIterator<TreeItem<FileTreeItem<?>>> treeItemIterator = getChildren().listIterator();
		FileFileTreeItem fileFileTreeItem = (FileFileTreeItem) (treeItemIterator.hasNext() ? treeItemIterator.next().getValue() : null);
		eachChild: for (final File child : children) {
			while(true) {
				if (isTreeItemAfterFile(fileFileTreeItem, child)) {
					if (fileFileTreeItem != null && treeItemIterator.hasPrevious())
						treeItemIterator.previous(); // move back, before adding

					treeItemIterator.add(createFileFileTreeItem(child));
					if (fileFileTreeItem != null) {
						// After add(...), we must invoke previous() / next() to make remove() legal! We thus navigate around a bit ;-)
						// remove() might be called (only, if fileFileTreeItem != null) afterwards and throws an exception,
						// if called after add(...) - according to javadoc :-(

						final FileFileTreeItem dbg = (FileFileTreeItem) treeItemIterator.previous();
						if (dbg.getFile() != child) // sanity check!
							throw new IllegalStateException(String.format("dbg.getFile() != child :: %s != %s", dbg.getFile(), child));

						treeItemIterator.next();
						final FileFileTreeItem dbg2 = (FileFileTreeItem) treeItemIterator.next();
						if (fileFileTreeItem != dbg2) // sanity check!
							throw new IllegalStateException(String.format("fileFileTreeItem != dbg2 :: %s != %s", fileFileTreeItem, dbg2));
					}
					continue eachChild;
				}
				else if (isTreeItemBeforeFile(fileFileTreeItem, child)) {
					if (fileFileTreeItem != null)
						treeItemIterator.remove();

					fileFileTreeItem = (FileFileTreeItem) (treeItemIterator.hasNext() ? treeItemIterator.next().getValue() : null);
				}
				else {
					final Class<? extends FileFileTreeItem> fileFileTreeItemClass = getFileFileTreeItemClass(child);
					if (!child.equals(fileFileTreeItem.getFile()) || fileFileTreeItemClass != fileFileTreeItem.getClass())
						treeItemIterator.set(createFileFileTreeItem(child));

					fileFileTreeItem = (FileFileTreeItem) (treeItemIterator.hasNext() ? treeItemIterator.next().getValue() : null);
					continue eachChild;
				}
			}
		}

		if (fileFileTreeItem != null)
			treeItemIterator.remove();

		while (treeItemIterator.hasNext()) {
			treeItemIterator.next();
			treeItemIterator.remove();
		}
	}

	private static final String getFileName(TreeItem<FileTreeItem<?>> treeItem) {
		if (treeItem instanceof FileFileTreeItem)
			return ((FileFileTreeItem) treeItem).getFile().getName();

		return treeItem.toString();
	}

	private boolean isTreeItemAfterFile(FileFileTreeItem treeItem, File file) {
		assertNotNull(file, "file");

		// if we reached the end, i.e. the treeItem is null, then this is considered *after* the given file.
		if (treeItem == null)
			return true;

		return treeItem.getFile().getName().compareTo(file.getName()) > 0;
	}

	private boolean isTreeItemBeforeFile(FileFileTreeItem treeItem, File file) {
		assertNotNull(treeItem, "treeItem"); // we checked for null in isTreeItemAfterFile(...) => reject this now!
		assertNotNull(file, "file");
		return treeItem.getFile().getName().compareTo(file.getName()) < 0;
	}

	@Override
	protected List<FileTreeItem<?>> loadChildren() {
		final File[] children = listChildFiles();
		if (children == null)
			return null;

		final List<FileTreeItem<?>> result = new ArrayList<>(children.length);
		for (final File child : children)
			result.add(createFileFileTreeItem(child));

		return result;
	}

	private Class<? extends FileFileTreeItem> getFileFileTreeItemClass(File file) {
		if (file.isDirectory())
			return DirectoryFileTreeItem.class;
		else
			return NormalFileFileTreeItem.class;
	}

	private FileFileTreeItem createFileFileTreeItem(File file) {
		final Class<? extends FileFileTreeItem> clazz = getFileFileTreeItemClass(file);
		return invokeConstructor(clazz, file);
	}

	private File[] listChildFiles() {
		final FileFilter fileFilter = getCombinedFileFilter();
		final File[] children = fileFilter == null
				? getFile().listFiles(new HideCloudStoreMetaDirFileFilter())
						: getFile().listFiles(fileFilter);

		if (children != null)
			Arrays.sort(children, (o1, o2) -> o1.getName().compareTo(o2.getName()));

		return children;
	}

	private static final class AndFileFilter implements FileFilter {
		private final List<FileFilter> fileFilters;

		public AndFileFilter(List<FileFilter> fileFilters) {
			this.fileFilters = assertNotNull(fileFilters, "fileFilters");
		}

		@Override
		public boolean accept(File file) {
			for (final FileFilter fileFilter : fileFilters) {
				if (! fileFilter.accept(file))
					return false;
			}
			return true;
		}
	}

	private static final class HideHiddenFilesFileFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			return ! isHidden(file);
		}

		protected boolean isHidden(File file) {
			// TODO take DOS/Windows flags into account => extend OioFileFactory?!
			return file.getName().startsWith(".");
		}
	}

	private static final class HideCloudStoreMetaDirFileFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			return ! LocalRepoManager.META_DIR_NAME.equals(file.getName());
		}
	}

	private FileFilter getCombinedFileFilter() {
		final List<FileFilter> fileFilters = new LinkedList<>();

		fileFilters.add(new HideCloudStoreMetaDirFileFilter());

		final boolean showHiddenFiles = getFileTreePane().showHiddenFilesProperty().get();
		if (! showHiddenFiles)
			fileFilters.add(new HideHiddenFilesFileFilter());

		final FileFilter fileFilter = getFileTreePane().fileFilterProperty().get();
		if (fileFilter != null)
			fileFilters.add(fileFilter);

		if (fileFilters.isEmpty())
			return null;

		if (fileFilters.size() == 1)
			return fileFilters.get(0);

		return new AndFileFilter(fileFilters);
	}

}
