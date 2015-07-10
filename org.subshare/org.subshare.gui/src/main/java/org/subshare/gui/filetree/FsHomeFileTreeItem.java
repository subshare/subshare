package org.subshare.gui.filetree;

import co.codewizards.cloudstore.core.oio.File;

/**
 * Virtual (top-most visible) root representing the current user's home directory.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FsHomeFileTreeItem extends DirectoryFileTreeItem {

	public FsHomeFileTreeItem(File file) {
		super(file);
	}

	@Override
	public String getName() {
		return String.format("Home (%s)", getFile().getAbsolutePath());
	}
}
