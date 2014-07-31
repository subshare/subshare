package org.subshare.core.crypto;

import org.bouncycastle.crypto.params.KeyParameter;

public class DefaultKeyParameterFactory extends AbstractKeyParameterFactory {

	@Override
	public KeyParameter createKeyParameter() {
		return KeyFactory.getInstance().createSymmetricKey();
	}

}
