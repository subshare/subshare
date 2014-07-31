package org.subshare.core.crypto;

import org.subshare.crypto.Cipher;

public abstract class AbstractIvFactory implements IvFactory {

	private Cipher cipher;

	@Override
	public Cipher getCipher() {
		return cipher;
	}
	@Override
	public void setCipher(final Cipher cipher) {
		this.cipher = cipher;
	}

}
