package org.subshare.gui.filetree;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * The invisible root-item of the {@link FileTreePane}'s {@link FileTreePane#getTreeTableView() treeTableView}.
 * <p>
 * This root here does <i>not</i> represent the file system's root directory, because
 * <ol>
 * <li>we might want to have 'virtual' visible roots like "Home", "Desktop", "Drives" and
 * <li>even if we displayed only the real file system without any shortcuts like "Desktop",
 * </ol>
 * we'd still have multiple roots on a crappy pseudo-OS like Windows still having these shitty drive letters.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class RootFileTreeItem extends FileTreeItem<String> {
	private static final Logger logger = LoggerFactory.getLogger(RootFileTreeItem.class);

	private final FileTreePane fileTreePane;

	public RootFileTreeItem(final FileTreePane fileTreePane) {
		super(""); // invisible ;-)
		this.fileTreePane = assertNotNull("fileTreePane", fileTreePane);
		getChildren(); // force loading of children NOW!
	}

	@Override
	protected FileTreePane getFileTreePane() {
		return fileTreePane;
	}

	@Override
	public ObservableList<TreeItem<FileTreeItem<?>>> getChildren() {
		return super.getChildren();
	}

	@Override
	protected List<FileTreeItem<?>> loadChildren() {
		final List<FileTreeItem<?>> result = new LinkedList<>();

		final File homeDir = IOUtil.getUserHome();
		result.add(new FsHomeFileTreeItem(homeDir));

		final File[] roots = listRootFiles();
		for (File root : roots) {
			if (! root.isDirectoryNoFollowSymLinks())
				logger.warn("loadChildren: root is not a directory: " + root);

			FsRootFileTreeItem treeItem = new FsRootFileTreeItem(root);
			result.add(treeItem);
		}
		return result;
	}
}