package org.subshare.gui.filetree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.subshare.gui.IconSize;

import co.codewizards.cloudstore.core.oio.File;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FileFileTreeItem extends FileTreeItem<File> {

	public FileFileTreeItem(final File file) {
		super(assertNotNull(file, "file"));

		if (! file.isAbsolute())
			throw new IllegalArgumentException("file not absolute!");

		final Image icon = FileIconRegistry.getInstance().getIcon(file, IconSize._16x16);
		setGraphic(new ImageView(icon));
	}

	@Override
	protected void refresh() {
		super.refresh();
		refreshLastModified();
	}

	private void refreshLastModified() {
		lastModifiedProperty().set(_getLastModified());
	}

	public File getFile() {
		return getValueObject();
	}

	@Override
	public String getName() {
		return getFile().getName();
	}

	private String _getLastModified() {
		if (! getFile().exists())
			return null;

		final Date date = new Date(getFile().getLastModifiedNoFollow());
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date);
	}

	@Override
	public FileTreeItem<?> findFirst(final File file) {
		assertNotNull(file, "file");

		if (! file.isAbsolute())
			throw new IllegalArgumentException("file not absolute!");

		if (getFile().equals(file))
			return this;

		final String thisPath = getFile().getPath();
		final String otherPath = file.getPath();

		if (! otherPath.startsWith(thisPath))
			return null;

		// It is possible that our quick path-comparison leads to a false positive:
		// Imagine thisPath = "/aaa/bbb/ccc" and otherPath = "/aaa/bbb/cccD".
		// To prevent unnecessarily loading the children in these cases, we do a more thorough check now:
		if (! isParent(getFile(), file))
			return null;

		for (final TreeItem<FileTreeItem<?>> child : getChildren()) {
			final FileTreeItem<?> treeItem = child.getValue().findFirst(file);
			if (treeItem != null)
				return treeItem;
		}
		return null;
	}

	private static boolean isParent(final File parentCandidate, final File childCandidate) {
		File f = childCandidate;
		while (f != null) {
			if (parentCandidate.equals(f))
				return true;

			f = f.getParentFile();
		}
		return false;
	}

	@Override
	public List<FileTreeItem<?>> findAll(final File file) {
		assertNotNull(file, "file");

		if (! file.isAbsolute())
			throw new IllegalArgumentException("file not absolute!");

		throw new UnsupportedOperationException("NYI");
	}
}
