package org.subshare.core.sign;

import java.io.InputStream;

public interface Signable {

	int getSignedDataVersion();

	/**
	 * Gets a binary representation of this object's data being signed.
	 * <p>
	 * Multiple invocations of this method must return the very same bytes, if the object's data was not
	 * modified.
	 * @param signedDataVersion version of the signed data. If a new field was added later, it must not be
	 * contained in the
	 * @return a stream with the object's data to be signed. Never <code>null</code>.
	 */
	InputStream getSignedData(int signedDataVersion);

	Signature getSignature();

	void setSignature(Signature signature);
}
