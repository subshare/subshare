package org.subshare.core.crypto;


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
}
