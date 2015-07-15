package org.subshare.local.persistence;

public enum LocalRepositoryType {

	UNINITIALISED,

	CLIENT,

	SERVER,

	/**
	 * A special repository similar to {@link #CLIENT}, but only having meta-data.
	 * Currently, such a repository is completely read-only, but we might make certain
	 * things (e.g. permissions) writable, later.
	 */
	CLIENT_META_ONLY

}
