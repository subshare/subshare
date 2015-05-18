package org.subshare.gui.util;

import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.Locale;
import java.util.ResourceBundle;

public final class ResourceBundleUtil {

	public static final String MESSAGES_NAME = "messages";

	private ResourceBundleUtil() { }

	public static ResourceBundle getMessages(final Class<?> clazz) {
		return getResourceBundle(clazz, MESSAGES_NAME);
	}

	public static ResourceBundle getResourceBundle(final Class<?> clazz, final String simpleName) {
		return getResourceBundle(clazz, Locale.getDefault(), simpleName);
	}

	public static ResourceBundle getResourceBundle(final Class<?> clazz, final Locale locale, final String simpleName) {
		final String pakkage = clazz.getPackage().getName();
		final String baseName = isEmpty(pakkage) ? simpleName : (pakkage + '.' + simpleName);
		final ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, clazz.getClassLoader());
		return bundle;
	}

}
