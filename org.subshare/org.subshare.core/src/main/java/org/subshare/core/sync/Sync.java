package org.subshare.core.sync;

public interface Sync extends AutoCloseable {

	/**
	 * Get a human-readable name to be shown in the UI.
	 * @return a human-readable name to be shown in the UI. Never <code>null</code>.
	 */
	String getName();

	void sync();

}
