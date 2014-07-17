/*
 * Cumulus4j - Securing your data in the cloud - http://cumulus4j.org
 * Copyright (C) 2011 NightLabs Consulting GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.subshare.crypto;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * <p>
 * A cipher encrypts or decrypts data.
 * </p>
 * <p>
 * This interface defines the algorithm-independent API contract to allow
 * for encrypting and decrypting data. It has been introduced in analogy
 * to {@link javax.crypto.Cipher} and with easy migration from JCE
 * to this API in mind.
 * </p>
 * <p>
 * <b>Important: <code>Cipher</code>s are not thread-safe!</b>
 * </p>
 * <p>
 * Use {@link CryptoRegistry#createCipher(String)} to obtain a <code>Cipher</code> instance.
 * </p>
 * <p>
 * This own API is used instead of the JCE, because of the following reasons:
 * </p>
 * <ul>
 *  <li>The JCE has a key length constraint (maximum 128 bit) that requires manual modifications of
 * the Java runtime environment (installing some files that are not included in the operating system's
 * package management).</li>
 * 	<li>The {@link BouncyCastleProvider} was not correctly registered in the JCE when using One-JAR to
 * package e.g. the <code>org.cumulus4j.keymanager.cli</code>. Probably because the signatures where not
 * found when looking for the MANIFEST.MF (probably the wrong MANIFEST.MF was picked by the class loader).
 * 	</li>
 * </ul>
 * <p>
 * Note: Implementors should subclass {@link AbstractCipher} instead of directly implementing this interface.
 * </p>
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public interface Cipher
{
	/**
	 * <p>
	 * Initialise the cipher.
	 * </p><p>
	 * A cipher cannot be used, before this method was called.
	 * </p><p>
	 * A cipher can be re-initialised to modify only certain parameters (and keep the others). For example to modify
	 * the <a target="_blank" href="http://en.wikipedia.org/wiki/Initialisation_vector">IV</a> while keeping the key, a cipher can
	 * be re-initialised with an IV only (i.e. <code>null</code> is passed to
	 * {@link ParametersWithIV#ParametersWithIV(CipherParameters, byte[], int, int)} instead of a {@link KeyParameter}).
	 * This is useful for performance reasons, because modifying an IV is a very fast operation while changing the key is
	 * slow (especially <a target="_blank" href="http://en.wikipedia.org/wiki/Blowfish_%28cipher%29">Blowfish</a> is known for its very
	 * slow key initialisation).
	 * </p>
	 *
	 * @param mode the operation mode; must not be <code>null</code>.
	 * @param parameters the parameters; for example an instance of {@link ParametersWithIV} with a wrapped {@link KeyParameter}
	 * to pass IV and secret key.
	 * @throws IllegalArgumentException if the given arguments are invalid - for example if the given <code>parameters</code>
	 * are not understood by the implementation (parameters not compatible with the chosen algorithm).
	 */
	void init(CipherOperationMode mode, CipherParameters parameters)
	throws IllegalArgumentException;

	/**
	 * Get the mode of this cipher. This is <code>null</code>, before
	 * {@link #init(CipherOperationMode, CipherParameters)} was called the first
	 * time.
	 * @return the mode of this cipher.
	 */
	CipherOperationMode getMode();

	/**
	 * Get the parameters of this cipher. This is <code>null</code>, before
	 * {@link #init(CipherOperationMode, CipherParameters)} was called the first
	 * time.
	 * @return the parameters of this cipher.
	 */
	CipherParameters getParameters();

	/**
	 * Get the transformation that was passed to {@link CryptoRegistry#createCipher(String)}
	 * for obtaining this <code>Cipher</code>.
	 * @return the transformation (encryption algorithm, mode and padding) of this cipher.
	 */
	String getTransformation();

	/**
	 * Reset this cipher. After resetting, the state is the same as if it was just freshly
	 * {@link #init(CipherOperationMode, CipherParameters) initialised}.
	 */
	void reset();

	/**
   * Get the input block size for this cipher (in bytes).
   * If this is a symmetric cipher, this equals {@link #getOutputBlockSize()}.
   *
   * @return the input block size for this cipher in bytes.
   */
	int getInputBlockSize();

	/**
   * Get the output block size for this cipher (in bytes).
   * If this is a symmetric cipher, this equals {@link #getInputBlockSize()}.
   *
   * @return the output block size for this cipher in bytes.
   */
	int getOutputBlockSize();

	/**
	 * Return the size of the output buffer required for an {@link #update(byte[], int, int, byte[], int) update}
	 * of an input of <code>length</code> bytes.
	 * @param length the size of the input (in bytes) that is to be passed to {@link #update(byte[], int, int, byte[], int)}.
	 * @return the required length of the output buffer in bytes.
	 */
	int getUpdateOutputSize(int length);

	/**
	 * Return the size of the output buffer required for an {@link #update(byte[], int, int, byte[], int) update} plus a
	 * {@link #doFinal(byte[], int) doFinal} with an input of <code>length</code> bytes.
	 * @param length the size of the input (in bytes) that is to be passed to {@link #update(byte[], int, int, byte[], int)}.
	 * @return the required length of the output buffer in bytes.
	 */
	int getOutputSize(int length);

	/**
	 * <p>
	 * Update this cipher with a single byte. This is synonymous to calling {@link #update(byte[], int, int, byte[], int)}
	 * with an <code>in</code> byte array of length 1 and <code>inOff = 0</code> and <code>inLen = 1</code>.
	 * </p><p>
	 * Note that data might still be unprocessed in this cipher when this method returns. That is because many ciphers work
	 * with blocks and keep a block unprocessed until it is filled up. Call {@link #doFinal(byte[], int)} after you finished
	 * updating this cipher (i.e. all input was passed completely).
	 * </p>
	 *
	 * @param in the input to be encrypted or decrypted (or a part of the input).
	 * @param out the buffer receiving the output (data is written into this byte-array). Must not be <code>null</code>.
	 * @param outOff the array-index in <code>out</code> at which to start writing. Must be &gt;=0.
	 * @return the number of bytes written into <code>out</code>.
	 * @throws DataLengthException if the buffer <code>out</code> is insufficient.
	 * @throws IllegalStateException if this cipher has not yet been {@link #init(CipherOperationMode, CipherParameters) initialised}.
	 * @throws CryptoException if there is a cryptographic error happening while processing the input. For example when
	 * decrypting a padding might be wrong or an authenticating block mode (like GCM) might recognize that the ciphertext has
	 * been manipulated/corrupted.
	 * @see #update(byte[], int, int, byte[], int)
	 * @see #doFinal(byte[], int)
	 */
	int update(byte in, byte[] out, int outOff) throws DataLengthException,
			IllegalStateException, CryptoException;

	/**
	 * <p>
	 * Update this cipher with multiple bytes.
	 * </p><p>
	 * Note that data might still be unprocessed in this cipher when this method returns. That is because many ciphers work
	 * with blocks and keep a block unprocessed until it is filled up. Call {@link #doFinal(byte[], int)} after you finished
	 * updating this cipher (i.e. all input was passed completely).
	 * </p>
	 *
	 * @param in the input to be encrypted or decrypted (or a part of the input). Must not be <code>null</code>.
	 * @param inOff the array-index in <code>in</code> at which to start reading. Must be &gt;=0.
	 * @param inLen the number of bytes that should be read from <code>in</code>.
	 * @param out the buffer receiving the output (data is written into this byte-array). Must not be <code>null</code>.
	 * @param outOff the array-index in <code>out</code> at which to start writing. Must be &gt;=0.
	 * @return the number of bytes written into <code>out</code>.
	 * @throws DataLengthException if the buffer <code>out</code> is insufficient or if <code>inOff + inLen</code> exceeds the
	 * input byte array.
	 * @throws IllegalStateException if this cipher has not yet been {@link #init(CipherOperationMode, CipherParameters) initialised}.
	 * @throws CryptoException if there is a cryptographic error happening while processing the input. For example when
	 * decrypting a padding might be wrong or an authenticating block mode (like GCM) might recognize that the ciphertext has
	 * been manipulated/corrupted.
	 * @see #update(byte, byte[], int)
	 * @see #doFinal(byte[], int)
	 */
	int update(byte[] in, int inOff, int inLen, byte[] out, int outOff)
			throws DataLengthException, IllegalStateException, CryptoException;

	/**
	 * Process the last block in the buffer. After this call, no unprocessed data is left in this
	 * cipher and it is {@link #reset()} implicitly.
	 *
	 * @param out the buffer receiving the output (data is written into this byte-array). Must not be <code>null</code>.
	 * @param outOff the array-index in <code>out</code> at which to start writing. Must be &gt;=0.
	 * @return the number of bytes written into <code>out</code>.
	 * @throws DataLengthException if the buffer <code>out</code> is insufficient or if <code>inOff + inLen</code> exceeds the
	 * input byte array.
	 * @throws IllegalStateException if this cipher has not yet been {@link #init(CipherOperationMode, CipherParameters) initialised}.
	 * @throws CryptoException if there is a cryptographic error happening while processing the input. For example when
	 * decrypting a padding might be wrong or an authenticating block mode (like GCM) might recognize that the ciphertext has
	 * been manipulated/corrupted.
	 * @see #update(byte, byte[], int)
	 * @see #update(byte[], int, int, byte[], int)
	 * @see #doFinal(byte[])
	 */
	int doFinal(byte[] out, int outOff) throws DataLengthException,
			IllegalStateException, CryptoException;

	/**
	 * Convenience method to encrypt/decrypt the complete input byte array at once. After this method was called,
	 * no unprocessed data is left in this cipher and it is {@link #reset()} implicitly.
	 *
	 * @param in the input to be encrypted or decrypted. Must not be <code>null</code>.
	 * @return the processed output.
	 * @throws IllegalStateException if the cipher isn't initialised.
	 * @throws CryptoException if padding is expected and not found or sth. else goes wrong while encrypting or decrypting.
	 */
	byte[] doFinal(byte[] in)
	throws IllegalStateException, CryptoException;

	/**
	 * Get the required size of the IV (in bytes). If a cipher supports multiple sizes, this is the optimal (most secure) IV size.
	 * If the cipher supports no IV, this is 0.
	 * @return the required size of the IV.
	 */
	int getIVSize();
}
