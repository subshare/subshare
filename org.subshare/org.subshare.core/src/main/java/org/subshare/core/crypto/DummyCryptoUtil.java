package org.subshare.core.crypto;

import org.subshare.crypto.Cipher;

/**
 * @deprecated This class should be removed. It contains only temporary code.
 */
@Deprecated
public class DummyCryptoUtil {

	public static final String SYMMETRIC_ENCRYPTION_TRANSFORMATION = "Twofish/CFB/NoPadding"; // TODO This should be configurable.
	public static final String ASYMMETRIC_ENCRYPTION_TRANSFORMATION = "RSA//OAEPWITHSHA1ANDMGF1PADDING"; // TODO This should be configurable.
	/**
	 * The size of symmetric keys in bits.
	 */
	public static final int SYMMETRIC_KEY_SIZE = 256; // TODO This should be configurable!


	/**
	 * Gets an IV containing only 0 bytes.
	 * <p>
	 * We do not need an IV, if the key is used only once
	 * <p>
	 * TODO We temporarily use this for all operations - even if the key is re-used.
	 * This must not be done in a productive environment!
	 * @deprecated Get rid of this method! It MUST NOT BE USED in productive environment, because IT IS INSECURE!
	 *
	 * @param size the size in bytes as returned by {@link Cipher#getIVSize()}.
	 * @return a byte array with only zeros (0).
	 */
	@Deprecated
	public static byte[] getNullIV(final int size) {
		return new byte[size]; // java initialises this to 0.
	}

}
