package org.subshare.gui.ls;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class ConfigLs {

	private ConfigLs() {
	}

	/**
	 * Gets the global {@code Config} for the current user.
	 * @return the global {@code Config} for the current user. Never <code>null</code>.
	 */
	public static Config getInstance() {
		return LocalServerClient.getInstance().invokeStatic(ConfigImpl.class, "getInstance");
	}

	/**
	 * Gets the {@code Config} for the given {@code directory}.
	 * @param directory a directory inside a repository. Must not be <code>null</code>.
	 * The directory does not need to exist (it may be created later).
	 * @return the {@code Config} for the given {@code directory}. Never <code>null</code>.
	 */
	public static Config getInstanceForDirectory(final File directory) {
		return LocalServerClient.getInstance().invokeStatic(ConfigImpl.class, "getInstanceForDirectory", directory);
	}

	/**
	 * Gets the {@code Config} for the given {@code file}.
	 * @param file a file inside a repository. Must not be <code>null</code>.
	 * The file does not need to exist (it may be created later).
	 * @return the {@code Config} for the given {@code file}. Never <code>null</code>.
	 */
	public static Config getInstanceForFile(final File file) {
		return LocalServerClient.getInstance().invokeStatic(ConfigImpl.class, "getInstanceForFile", file);
	}
}
