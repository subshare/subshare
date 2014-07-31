package org.subshare.core.crypto;

import org.subshare.crypto.Cipher;

public interface IvFactory {
	Cipher getCipher();
	void setCipher(Cipher cipher);

	byte[] createIv();
}
