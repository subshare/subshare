package org.subshare.core.dto;

public enum PermissionType {

	/**
	 * A <i>read</i> permission allows a user to read a directory's or file's meta-data (name, time-stamp and more)
	 * as well as a file's actual data.
	 * <p>
	 * A <i>read</i> permission can be granted on an individual directory (applying to its children recursively until
	 * permission inheritance is explicitly interrupted) or file.
	 * <p>
	 * Revoking a <i>read</i> permission automatically causes all other permissions to be revoked, too, because
	 * it is technically required to read data when writing data.
	 */
	read,

	/**
	 * A <i>write</i> permission allows a user to write a directory's or file's meta-data (name, time-stamp and more)
	 * as well as a file's actual data.
	 * <p>
	 * A <i>write</i> permission can be granted on an individual directory (applying to its children recursively until
	 * permission inheritance is explicitly interrupted) or file.
	 * <p>
	 * Granting a <i>write</i> permission automatically causes a <i>read</i> permission to be granted, too.
	 * <p>
	 * Revoking a <i>write</i> permission automatically causes a <i>grant</i> permission to be revoked, too.
	 */
	write,

	/**
	 * A <i>grant</i> permission allows a user to grant permissions to other users.
	 * <p>
	 * A <i>grant</i> permission can be granted on an individual directory (applying to its children recursively until
	 * permission inheritance is explicitly interrupted) or file.
	 * <p>
	 * Granting a <i>grant</i> permission automatically causes a <i>write</i> permission to be granted, too.
	 * <p>
	 * Though this is technically not required, it makes semantically no sense at all to grant permissions without knowing
	 * users. Therefore, granting at least
	 */
	grant,

	/**
	 * A <i>seeUserIdentity</i> permission allows a user to see all other users of the current repository.
	 * <p>
	 * A <i>seeUserIdentity</i> permission is global, i.e. applicable to the entire repository and not
	 * to individual directories or files. Because a {@code PermissionSet} always belongs to a file/directory,
	 * i.e. we cannot easily technically manage them globally without an (unnecessary) refactoring, we simply
	 * manage all permissions of this type in the repository's root directory.
	 */
	seeUserIdentity

}
