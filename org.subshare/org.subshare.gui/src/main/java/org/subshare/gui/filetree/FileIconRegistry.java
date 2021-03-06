package org.subshare.gui.filetree;

import static java.util.Objects.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.subshare.gui.IconSize;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;
import javafx.scene.image.Image;

public class FileIconRegistry {

	public static final String ICON_ID_DIRECTORY = "directory";
	public static final String ICON_ID_HOME = "home";
	public static final String ICON_ID_FILE = "file-empty";

	public static final String ICON_ID_SYMLINK_TO_FILE = "symlink"; // TODO change image!
	public static final String ICON_ID_SYMLINK_TO_DIRECTORY = "symlink"; // TODO change image!
	public static final String ICON_ID_SYMLINK_BROKEN = "symlink"; // TODO change image!
	public static final String ICON_ID_SYMLINK = "symlink"; // TODO remove this?

	private static final FileIconRegistry instance = new FileIconRegistry();
	private final Map<String, Map<IconSize, Image>> iconId2IconSize2Image = new HashMap<>();

	private FileIconRegistry() {
	}

	public static FileIconRegistry getInstance() {
		return instance;
	}

	/**
	 * Gets an icon representing the given {@code file}, having the size specified by {@code iconSize}.
	 *
	 * @param file the file for which to return an appropriate icon. Must not be <code>null</code>.
	 * @param iconSize the size of the desired icon. Must not be <code>null</code>.
	 * @return the icon. Never <code>null</code>.
	 */
	public Image getIcon(final File file, final IconSize iconSize) {
		requireNonNull(file, "file");
		requireNonNull(iconSize, "iconSize");

		final String iconId = getIconId(file);
		return getIcon(iconId, iconSize);
	}

	/**
	 * Gets an icon identified by the given {@code iconId}, having the size specified by {@code iconSize}.
	 *
	 * @param iconId the identifier of the icon.
	 * @param iconSize the size of the desired icon. Must not be <code>null</code>.
	 * @return the icon. Never <code>null</code>.
	 * @throws IllegalArgumentException if there is no icon for the given
	 */
	public Image getIcon(final String iconId, final IconSize iconSize) {
		requireNonNull(iconId, "iconId");
		requireNonNull(iconSize, "iconSize");

		Map<IconSize, Image> iconSize2Image;
		synchronized (this) {
			iconSize2Image = iconId2IconSize2Image.get(iconId);
			if (iconSize2Image == null) {
				iconSize2Image = new HashMap<>();
				iconId2IconSize2Image.put(iconId, iconSize2Image);
			}

			Image image = iconSize2Image.get(iconSize);
			if (image == null) {
				final String fileName = getFileName(iconId, iconSize);
				final URL url = FileIconRegistry.class.getResource(fileName);
				if (url == null)
					throw new IllegalArgumentException(String.format(
							"Unknown iconId '%s' (resource '%s' not found)!", iconId, fileName));

				image = new Image(url.toExternalForm());
				iconSize2Image.put(iconSize, image);
			}
			return image;
		}
	}

	private String getFileName(final String iconId, final IconSize iconSize) {
		requireNonNull(iconId, "iconId");
		requireNonNull(iconSize, "iconSize");
		return iconId + iconSize.name() + ".png";
	}

	private String getIconId(final File file) {
		requireNonNull(file, "file");
		if (file.isDirectory()) {
			if (IOUtil.getUserHome().equals(file))
				return ICON_ID_HOME;
			else {
				if (file.isSymbolicLink())
					return ICON_ID_SYMLINK_TO_DIRECTORY;
				else
					return ICON_ID_DIRECTORY;
			}
		}

		if (file.isFile() && file.isSymbolicLink())
			return ICON_ID_SYMLINK_TO_FILE;

		if (file.isSymbolicLink()) // neither isDirectory() nor isFile() was true => broken
			return ICON_ID_SYMLINK_BROKEN;

		return ICON_ID_FILE;
	}
}
