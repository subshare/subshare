package org.subshare.gui.util;

import java.net.MalformedURLException;
import java.net.URL;

import javafx.util.StringConverter;

public class UrlStringConverter extends StringConverter<URL> {

	@Override
	public String toString(final URL url) {
		return url == null ? null : url.toExternalForm();
	}

	@Override
	public URL fromString(String string) {
		if (string == null)
			return null;

		string = string.trim();

		if (string.isEmpty())
			return null;

		try {
			return new URL(string);
		} catch (MalformedURLException e) {
//			throw new IllegalArgumentException(e);
			return null;
		}
	}
}