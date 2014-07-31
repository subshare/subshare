package org.subshare.core.crypto;

import org.bouncycastle.crypto.params.KeyParameter;

public interface KeyParameterFactory {
	CipherTransformation getCipherTransformation();
	void setCipherTransformation(CipherTransformation cipherTransformation);

	KeyParameter createKeyParameter();
}
