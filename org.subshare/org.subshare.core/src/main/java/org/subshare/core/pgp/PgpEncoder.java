package org.subshare.core.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Encoder to encrypt or sign according to the OpenPGP standard.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface PgpEncoder {

	InputStream getInputStream();
	void setInputStream(InputStream in);

	OutputStream getOutputStream();
	void setOutputStream(OutputStream out);

	/**
	 * Gets the keys of the recipients of the encrypted message.
	 * @return the keys with which the data is encrypted. Never <code>null</code>, but maybe empty.
	 */
	Set<PgpKey> getEncryptPgpKeys();

	PgpKey getSignPgpKey();
	void setSignPgpKey(PgpKey pgpKey);

	boolean isWithIntegrityCheck();
	void setWithIntegrityCheck(boolean withIntegrityCheck);

	SymmetricEncryptionAlgorithm getSymmetricEncryptionAlgorithm();
	void setSymmetricEncryptionAlgorithm(SymmetricEncryptionAlgorithm symmetricEncryptionAlgorithm);

	CompressionAlgorithm getCompressionAlgorithm();
	void setCompressionAlgorithm(CompressionAlgorithm compressionAlgorithm);

	HashAlgorithm getHashAlgorithm();
	void setHashAlgorithm(HashAlgorithm hashAlgorithm);

	/**
	 * Encode the data read from {@link #getInputStream() inputStream} and write the result to {@link #getOutputStream() outputStream}.
	 * <p>
	 * If there is at least one entry in {@link #getEncryptPgpKeys() encryptPgpKeys}, the data is encrypted.
	 * <p>
	 * If there is a {@link #getSignPgpKey() signPgpKey}, the data is signed.
	 * @throws IOException TODO
	 */
	void encode() throws IOException;
}
