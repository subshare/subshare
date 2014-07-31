package org.subshare.local;

import org.subshare.core.AbstractCryptreeFactory;
import org.subshare.core.Cryptree;

public class CryptreeFactoryImpl extends AbstractCryptreeFactory {

	@Override
	protected Cryptree _createCryptree() {
		return new CryptreeImpl();
	}

}
