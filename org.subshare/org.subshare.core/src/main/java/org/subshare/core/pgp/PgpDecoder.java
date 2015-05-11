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

	/**
	 * Input of a detached signature.
	 * @return input of a detached signature, or <code>null</code>.
	 */
	InputStream getSignInputStream();
	void setSignInputStream(InputStream in);

	void decode() throws SignatureException, IOException;

	/**
	 * Gets the key that was used to decrypt the data in the last {@link #decode()} invocation.
	 * <p>
	 * There might be multiple encryption keys used - this is just one of them (the first one available
	 * in our local PGP key ring).
	 * @return the key that decrypted the data in the last call of {@link #decode()}. <code>null</code>, before
	 * {@code decode()} was invoked.
	 */
	PgpKey getDecryptPgpKey();

//	/**
//	 * Gets the key that signed the data, if it was signed.
//	 * <p>
//	 * This property is <code>null</code>, before {@link #decode()} was called. It is only assigned to a non-<code>null</code>
//	 * value, if the data was signed and the signature is correct (not broken).
//	 * @return the key that signed the data or <code>null</code>.
//	 */
//	PgpKey getSignPgpKey();

	/**
	 * Gets the signature, if it was signed.
	 * <p>
	 * This property is <code>null</code>, before {@link #decode()} was called. It is only assigned to a non-<code>null</code>
	 * value, if the data was signed and the signature is correct (not broken).
	 * @return the signature or <code>null</code>.
	 */
	PgpSignature getPgpSignature();
}
