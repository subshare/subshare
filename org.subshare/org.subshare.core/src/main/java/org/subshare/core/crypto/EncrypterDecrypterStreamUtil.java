package org.subshare.core.crypto;

import static java.util.Objects.*;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoKeyType;

class EncrypterDecrypterStreamUtil {

	static final int MAX_UNSIGNED_2_BYTE_VALUE = 0xffff;

	static CipherParameters assertValidKey(final CipherTransformation cipherTransformation, final CipherParameters key) {
		requireNonNull(cipherTransformation, "cipherTransformation");
		requireNonNull(key, "key");
		if (key instanceof KeyParameter) {
			// symmetric encryption
			if (CryptoKeyType.symmetric != cipherTransformation.getType())
				throw new IllegalArgumentException("key is a shared secret (used for symmetric encryption), but cipherTransformation is of type: " + cipherTransformation.getType());
		}
		else if (key instanceof AsymmetricKeyParameter) {
			// asymmetric encryption
			if (CryptoKeyType.asymmetric != cipherTransformation.getType())
				throw new IllegalArgumentException("key is an asymmetric key, but cipherTransformation is of type: " + cipherTransformation.getType());
		}
		else
			throw new IllegalArgumentException("key must be an instanceo of KeyParameter or AsymmetricKeyParameter, but it is an instance of: " + key.getClass().getName());

		return key;
	}

}
