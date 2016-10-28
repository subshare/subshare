package org.subshare.gui.filetree;

import co.codewizards.cloudstore.core.oio.File;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Root of the file system.
 * <p>
 * On a good OS, there's exactly one of them. On the pseudo-OS Windows, there is one per drive letter.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FsRootFileTreeItem extends DirectoryFileTreeItem {

	private static Image icon;
	static {
		final String iconUrl = FsRootFileTreeItem.class.getResource("drive-hd_16x16.png").toString();
		icon = new Image(iconUrl);
	}

	public FsRootFileTreeItem(File file) {
		super(file);
		setGraphic(new ImageView(icon));
	}

	@Override
	public String getName() {
		return getFile().getAbsolutePath();
	}
}
