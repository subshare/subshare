package org.subshare.core.locker;

import java.io.IOException;

import co.codewizards.cloudstore.core.Uid;

/**
 * A {@code LockerContent} describes a piece of unencrypted, plain data that is to be placed inside the locker (on the server).
 * <p>
 * Usually, a {@code LockerContent} is a single file in the local file system. The system then tracks when this data was modified
 * and synchronises it up to and down from the server. In this process, the data is encrypted and decrypted using the owner's
 * OpenPGP key. It is ensured that only encrypted data is sent out from the local computer so that unencrypted data never ends
 * up on the server.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface LockerContent {

	/**
	 * Gets the unique name of this locker within the scope of the owner's OpenPGP key.
	 * <p>
	 * Usually, this is the file-name (without directory) of the file to be encrypted and locked away on the server.
	 * @return the unique name of this locker within the scope of the owner's OpenPGP key. Never <code>null</code>.
	 */
	String getName();

	/**
	 * Gets the actual data to be placed in the server's locker.
	 * <p>
	 * This is plain, non-encrypted data. It is encrypted and signed by the framework (using the owner's OpenPGP key(s)),
	 * before being uploaded.
	 * @return the actual data to be placed in the server's locker. Never <code>null</code>, but maybe empty (0 length).
	 * @throws IOException if reading the local data failed.
	 */
	byte[] getLocalData() throws IOException;

	/**
	 * Gets the local version of this {@code LockerContent}'s data.
	 * <p>
	 * Whenever the result of this method changes, the data is newly uploaded. Please note that this is not directly
	 * used as server version. Since a user may have multiple active OpenPGP keys and all locker contents for all
	 * these PGP-keys are merged and synchronized together, a separate {@code Uid} is assigned for each encrypted
	 * package. This means there is one separate server-version for each OpenPGP key and each local-version.
	 * This strategy prevents an attacker from knowing which OpenPGP keys belong together (i.e. belong to the same
	 * real user). The only way to correlate them is by the upload timestamps (we might address this later).
	 *
	 * @return the local version of this {@code LockerContent}'s data. Never <code>null</code>.
	 */
	Uid getLocalVersion();

	/**
	 * Merges the data from the server into the local file.
	 * <p>
	 * <b>Important:</b> The same data might be merged multiple times (e.g. because it is synced down from multiple servers)!
	 *
	 * @param serverData the data as downloaded from the server, already decrypted (i.e. plain). Never <code>null</code>, but maybe empty (0 length).
	 * @throws IOException if reading/writing/merging data fails.
	 */
	void mergeFrom(byte[] serverData) throws IOException;
}
