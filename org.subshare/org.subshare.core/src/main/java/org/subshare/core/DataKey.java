package org.subshare.core;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.bouncycastle.crypto.params.KeyParameter;

import co.codewizards.cloudstore.core.dto.Uid;

public class DataKey {

	public final Uid cryptoKeyId;
	public final KeyParameter keyParameter;

	public DataKey(final Uid cryptoKeyId, final KeyParameter keyParameter) {
		this.cryptoKeyId = assertNotNull("cryptoKeyId", cryptoKeyId);
		this.keyParameter = assertNotNull("keyParameter", keyParameter);
	}
}
