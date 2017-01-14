package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.subshare.crypto.CipherOperationMode;

public enum CryptoKeyRole {

	clearanceKey(CryptoKeyPart.publicKey, CryptoKeyPart.privateKey),

	subdirKey(CryptoKeyPart.sharedSecret, CryptoKeyPart.publicKey, CryptoKeyPart.privateKey),

	fileKey(CryptoKeyPart.sharedSecret),

	backlinkKey(CryptoKeyPart.sharedSecret),

	dataKey(CryptoKeyPart.sharedSecret)

	;

	private final CryptoKeyPart[] cryptoKeyParts;
	private final CryptoKeyPart[][] cipherOperationModeOrdinal2CryptoKeyParts;

	private CryptoKeyRole(final CryptoKeyPart ... cryptoKeyParts) {
		assertNotNullAndNoNullElement(cryptoKeyParts, "cryptoKeyParts");
		if (cryptoKeyParts.length < 1)
			throw new IllegalArgumentException("cryptoKeyParts.length < 1");

		this.cryptoKeyParts = cryptoKeyParts;
		this.cipherOperationModeOrdinal2CryptoKeyParts = createCipherOperationModeOrdinal2CryptoKeyParts();
	}

	private CryptoKeyPart[][] createCipherOperationModeOrdinal2CryptoKeyParts() {
		final CryptoKeyPart[][] result = new CryptoKeyPart[CipherOperationMode.values().length][];
		for (final CipherOperationMode cipherOperationMode : CipherOperationMode.values()) {
			final List<CryptoKeyPart> filtered = filterCryptoKeyParts(cipherOperationMode);
			result[cipherOperationMode.ordinal()] = filtered.toArray(new CryptoKeyPart[filtered.size()]);
		}
		return result;
	}

	private List<CryptoKeyPart> filterCryptoKeyParts(final CipherOperationMode cipherOperationMode) {
		switch (cipherOperationMode) {
			case DECRYPT:
			case ENCRYPT:
				break;
			default:
				throw new IllegalStateException("Unknown cipherOperationMode: " + cipherOperationMode);
		}

		final List<CryptoKeyPart> result = new ArrayList<CryptoKeyPart>(cryptoKeyParts.length);
		for (final CryptoKeyPart cryptoKeyPart : cryptoKeyParts) {
			switch (cryptoKeyPart) {
				case privateKey:
					if (CipherOperationMode.DECRYPT == cipherOperationMode)
						result.add(cryptoKeyPart);
					break;
				case publicKey:
					if (CipherOperationMode.ENCRYPT == cipherOperationMode)
						result.add(cryptoKeyPart);
					break;
				case sharedSecret:
					result.add(cryptoKeyPart);
					break;
				default:
					throw new IllegalStateException("Unknown cryptoKeyPart: " + cryptoKeyPart);
			}
		}
		return result;
	}

	public CryptoKeyPart[] getCryptoKeyParts() {
		return cryptoKeyParts;
	}

	public CryptoKeyPart[] getCryptoKeyParts(final CipherOperationMode cipherOperationMode) {
		assertNotNull(cipherOperationMode, "cipherOperationMode");
		return cipherOperationModeOrdinal2CryptoKeyParts[cipherOperationMode.ordinal()];
	}
}
