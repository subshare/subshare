package org.subshare.core.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import co.codewizards.cloudstore.core.auth.SignatureException;

public interface PgpDecoder {

	InputStream getInputStream();
	void setInputStream(InputStream in);

	OutputStream getOutputStream();
	void setOutputStream(OutputStream out);

	void decode() throws SignatureException, IOException;

	/**
	 * Get the key that signed the data, if it was signed.
	 * <p>
	 * This property is <code>null</code>, before {@link #decode()} was called. It is only assigned to a non-<code>null</code>
	 * value, if the data was signed and the signature is correct (not broken).
	 * @return the key that signed the data or <code>null</code>.
	 */
	PgpKey getSignPgpKey();

}
