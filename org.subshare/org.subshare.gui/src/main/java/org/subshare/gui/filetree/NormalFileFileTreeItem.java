package org.subshare.gui.filetree;

import co.codewizards.cloudstore.core.oio.File;

public class NormalFileFileTreeItem extends FileFileTreeItem {

	private static final long KIB = 1024L;
	private static final long MIB = KIB * 1024;
	private static final long GIB = MIB * 1024;
	private static final long TIB = GIB * 1024;

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

		if (length >= TIB)
			return String.format("%.1f TiB", (double) length / TIB);

		if (length >= GIB)
			return String.format("%.1f GiB", (double) length / GIB);

		if (length >= MIB)
			return String.format("%.1f MiB", (double) length / MIB);

		if (length >= KIB)
			return String.format("%.1f KiB", (double) length / KIB);

		return String.format("%d B", length);
	}

	@Override
	public boolean isLeaf() {
		return true;
	}
}
