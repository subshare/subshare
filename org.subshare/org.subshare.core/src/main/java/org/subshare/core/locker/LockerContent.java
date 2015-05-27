package org.subshare.core.locker;

import java.io.IOException;

import co.codewizards.cloudstore.core.dto.Uid;

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

	byte[] getLocalData() throws IOException;

	Uid getLocalVersion();

	/**
	 * Merges the data from the server into the local file.
	 * @param serverData the data as downloaded from the server, already decrypted (i.e. plain). Never <code>null</code>.
	 */
	void merge(byte[] serverData) throws IOException;
}
