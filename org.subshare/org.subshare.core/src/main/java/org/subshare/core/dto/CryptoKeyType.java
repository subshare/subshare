package org.subshare.core.dto;

public enum CryptoKeyType {

	/**
	 * The key is a {@linkplain CryptoKeyPart#sharedSecret shared secret} used in
	 * <a href="http://en.wikipedia.org/wiki/Symmetric-key_algorithm">symmetric cryptography</a>.
	 */
	symmetric,

	/**
	 * They key is actually a key-pair consisting of a {@linkplain CryptoKeyPart#publicKey public} and a
	 * {@linkplain CryptoKeyPart#privateKey private} key used in
	 * <a href="http://en.wikipedia.org/wiki/Public-key_cryptography">asymmetric cryptography</a>.
	 */
	asymmetric

}
