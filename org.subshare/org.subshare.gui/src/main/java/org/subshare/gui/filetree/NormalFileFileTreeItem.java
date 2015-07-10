package org.subshare.gui.filetree;

import co.codewizards.cloudstore.core.oio.File;

public class NormalFileFileTreeItem extends FileFileTreeItem {

	public NormalFileFileTreeItem(final File file) {
		super(file);
		if (file.isDirectory())
			throw new IllegalArgumentException("file is a directory!");
	}

	@Override
	protected void refresh() {
		super.refresh();
		updateSize();
	}

	private void updateSize() {
		sizeProperty().set(_getSize());
	}

	private String _getSize() {
		long length = getFile().length();

		if (length >= 1024L * 1024 * 1024 * 1024)
			return String.format("%d TiB", length / (1024L * 1204 * 1024 * 1024));

		if (length >= 1024L * 1024 * 1024)
			return String.format("%d GiB", length / (1024L * 1204 * 1024));

		if (length >= 1024L * 1024)
			return String.format("%d MiB", length / (1024L * 1204));

		if (length >= 1024L)
			return String.format("%d KiB", length / 1024L);

		return String.format("%d B", length);
	}

	@Override
	public boolean isLeaf() {
		return true;
	}
}
