package org.subshare.core.crypto;

import java.security.SecureRandom;

public class RandomIvFactory extends AbstractIvFactory {

	private final SecureRandom secureRandom;

	public RandomIvFactory() {
		this(null);
	}

	public RandomIvFactory(final SecureRandom secureRandom) {
		this.secureRandom = secureRandom == null ? KeyFactory.secureRandom : secureRandom;
	}

	@Override
	public byte[] createIv() {
		final byte[] iv = new byte[getCipher().getIVSize()];
		secureRandom.nextBytes(iv);
		return iv;
	}

}
