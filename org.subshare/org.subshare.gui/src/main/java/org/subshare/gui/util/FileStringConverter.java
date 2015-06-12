package org.subshare.gui.util;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import javafx.util.StringConverter;
import co.codewizards.cloudstore.core.oio.File;

public class FileStringConverter extends StringConverter<File> {

	@Override
	public String toString(final File file) {
		return file == null ? null : file.toString();
	}

	@Override
	public File fromString(final String string) {
		return string == null ? null : createFile(string);
	}
}
