package org.subshare.core.repo.sync;

import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static java.util.Objects.*;

public class PaddingUtil {

	private PaddingUtil() {
	}

	private static final int chunkPayloadLengthBytesLength = 4;

	public static byte[] addPadding(final byte[] fileData, final int paddingLength) {
		requireNonNull(fileData, "fileData");

		if (paddingLength < 0)
			throw new IllegalArgumentException("paddingLength < 0");

		int index = -1;
		final byte[] result = new byte[1 + chunkPayloadLengthBytesLength + fileData.length + paddingLength];
		result[++index] = 1; // version

		final byte[] lengthBytes = intToBytes(fileData.length);
		if (lengthBytes.length != chunkPayloadLengthBytesLength)
			throw new IllegalStateException("lengthBytes.length != chunkPayloadLengthBytesLength");

		for (int i = 0; i < lengthBytes.length; ++i)
			result[++index] = lengthBytes[i];

		System.arraycopy(fileData, 0, result, ++index, fileData.length); // 0-padding
		return result;
	}

	public static byte[] removePadding(final byte[] fileData) {
		requireNonNull(fileData, "fileData");
		// We do *not* pass the paddingLength as parameter but instead encode it (or more precisely the payload-length)
		// into the fileData to ensure we *never* encounter an inconsistency between data and meta-data. Such
		// inconsistencies may happen, if a file is modified during transport and in our current solution, this
		// situation is cleanly detected and causes a warning + abortion of the transfer (causing the transfer
		// to be re-tried later).

		int index = -1;
		final byte version = fileData[++index];
		if (version != 1)
			throw new IllegalArgumentException(String.format("version == %d != 1", version));

		final byte[] lengthBytes = new byte[chunkPayloadLengthBytesLength];
		for (int i = 0; i < lengthBytes.length; ++i)
			lengthBytes[i] = fileData[++index];

		final int length = bytesToInt(lengthBytes);

		final byte[] result = new byte[length];
		System.arraycopy(fileData, ++index, result, 0, result.length);
		return result;
	}
}
