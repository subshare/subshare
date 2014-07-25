package org.subshare.core.dto;

public enum CryptoKeyPart {

	/**
	 * The <a href="http://en.wikipedia.org/wiki/Shared_secret">shared secret</a>. Used with
	 * {@link CryptoKeyType#symmetric} only.
	 */
	sharedSecret,

	/**
	 * The public part of a public-private-key-pair. Used with {@link CryptoKeyType#asymmetric} only.
	 */
	publicKey,

	/**
	 * The private part of a public-private-key-pair. Used with {@link CryptoKeyType#asymmetric} only.
	 */
	privateKey

}
