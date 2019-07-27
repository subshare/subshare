package org.subshare.core;

import static java.util.Objects.*;

import org.bouncycastle.crypto.params.KeyParameter;

import co.codewizards.cloudstore.core.Uid;

public class DataKey {

	public final Uid cryptoKeyId;
	public final KeyParameter keyParameter;

	public DataKey(final Uid cryptoKeyId, final KeyParameter keyParameter) {
		this.cryptoKeyId = requireNonNull(cryptoKeyId, "cryptoKeyId");
		this.keyParameter = requireNonNull(keyParameter, "keyParameter");
	}
}
