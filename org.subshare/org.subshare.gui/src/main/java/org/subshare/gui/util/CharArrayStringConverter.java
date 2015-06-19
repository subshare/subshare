package org.subshare.gui.util;

import javafx.util.StringConverter;

public class CharArrayStringConverter extends StringConverter<char[]> {

	@Override
	public String toString(final char[] charArray) {
		return charArray == null ? null : new String(charArray);
	}

	@Override
	public char[] fromString(final String string) {
		return string == null ? null : string.toCharArray();
	}
}
