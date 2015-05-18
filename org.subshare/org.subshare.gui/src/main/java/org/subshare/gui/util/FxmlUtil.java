package org.subshare.gui.util;

import static org.subshare.gui.util.ResourceBundleUtil.*;

import java.io.IOException;

import javafx.fxml.FXMLLoader;

public final class FxmlUtil {

	private FxmlUtil() { }

	private static FXMLLoader createFxmlLoader(final Class<?> componentClass) {
		final FXMLLoader fxmlLoader = new FXMLLoader(
				componentClass.getResource(componentClass.getSimpleName() + ".fxml"),
				getMessages(componentClass));
		return fxmlLoader;
	}

	/**
	 * Loads the fxml file for the given custom component.
	 * @param componentClass
	 * @param component
	 */
	public static void loadDynamicComponentFxml(final Class<?> componentClass, final Object component) {
		final FXMLLoader fxmlLoader = createFxmlLoader(componentClass);

		fxmlLoader.setRoot(component);
		fxmlLoader.setController(component);

		try {
			fxmlLoader.load();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
