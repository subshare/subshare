package org.subshare.gui.severity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

import org.subshare.gui.IconSize;

import co.codewizards.cloudstore.core.Severity;

public class SeverityImageRegistry {

	private final Map<Severity, Map<IconSize, Image>> severity2IconSize2Image = new HashMap<>();

	private static final class Holder {
		public static final SeverityImageRegistry instance = new SeverityImageRegistry();
	}

	private SeverityImageRegistry() {
	}

	public static SeverityImageRegistry getInstance() {
		return Holder.instance;
	}

	public synchronized Image getImage(final Severity severity, final IconSize iconSize) {
		assertNotNull("severity", severity);
		assertNotNull("iconSize", iconSize);

		Map<IconSize, Image> iconSize2Image = severity2IconSize2Image.get(severity);
		if (iconSize2Image == null) {
			iconSize2Image = new HashMap<>();
			severity2IconSize2Image.put(severity, iconSize2Image);
		}

		Image image = iconSize2Image.get(iconSize);
		if (image == null) {
			try {
				final String fileName = String.format("%s%s.png", severity.name(), iconSize.name());
				try(InputStream in = SeverityImageRegistry.class.getResourceAsStream(fileName);) {
					if (in == null)
						throw new IllegalArgumentException("There is no resource named: " + fileName);

					image = new Image(in);
					iconSize2Image.put(iconSize, image);
				}
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		return image;
	}
}
