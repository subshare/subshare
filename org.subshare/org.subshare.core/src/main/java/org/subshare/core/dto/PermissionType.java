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
	 * Furthermore, granting a <i>grant</i> permission on at least one directory/file causes a global
	 * <i>readUserIdentity</i> permission to be granted, too. Though this is technically not required,
	 * it makes semantically no sense at all to manage permissions without knowing users. The UI therefore
	 * relies on being able to know and display users.
	 */
	grant,

	/**
	 * A <i>readUserIdentity</i> permission allows a user to see all other users of the current repository.
	 * <p>
	 * A <i>readUserIdentity</i> permission is global, i.e. applicable to the entire repository and not
	 * to individual directories or files. Because a {@code PermissionSet} always belongs to a file/directory,
	 * i.e. we cannot easily technically manage them globally without an (unnecessary) refactoring, we simply
	 * manage all permissions of this type in the repository's root directory.
	 * <p>
	 * TODO maybe we have to refactor this (and allow a PermissionSet without an associated CryptoRepoFile),
	 * because if I see this correctly, the user would need grant permission on the root directory to grant
	 * readUserIdentity - which is actually not necessary, if the user has grant-permission on a sub-directory
	 * and wants to grant readUserIdentity - this should work!
	 */
	readUserIdentity

}
