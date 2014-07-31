package org.subshare.core.crypto;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.subshare.core.dto.CryptoKeyType;

public enum CipherTransformation {

	AES_CBC_PKCS5PADDING(CryptoKeyType.symmetric, "AES/CBC/PKCS5Padding"),
	AES_CBC_PKCS7PADDING(CryptoKeyType.symmetric, "AES/CBC/PKCS7Padding"),
	AES_CFB_NOPADDING(CryptoKeyType.symmetric, "AES/CFB/NoPadding"),

	TWOFISH_CBC_PKCS5PADDING(CryptoKeyType.symmetric, "Twofish/CBC/PKCS5Padding"),
	TWOFISH_CBC_PKCS7PADDING(CryptoKeyType.symmetric, "Twofish/CBC/PKCS7Padding"),
	TWOFISH_CFB_NOPADDING(CryptoKeyType.symmetric, "Twofish/CFB/NoPadding"),

	RSA_OAEPWITHSHA1ANDMGF1PADDING(CryptoKeyType.asymmetric, "RSA//OAEPWITHSHA1ANDMGF1PADDING")
	;

	private static final Map<String, CipherTransformation> transformation2SymmetricCipherTransformation;
	static {
		final Map<String, CipherTransformation> m = new HashMap<String, CipherTransformation>(values().length);
		for (final CipherTransformation seTransformation : values()) {
			m.put(seTransformation.getTransformation(), seTransformation);
		}
		transformation2SymmetricCipherTransformation = Collections.unmodifiableMap(m);
	}

	private final CryptoKeyType type;
	private final String transformation;

	private CipherTransformation(final CryptoKeyType type, final String transformation) {
		this.type = assertNotNull("type", type);
		this.transformation = assertNotNull("transformation", transformation);
	}

	public String getTransformation() {
		return transformation;
	}

	public CryptoKeyType getType() {
		return type;
	}

	public static CipherTransformation fromTransformation(final String transformation) {
		assertNotNull("transformation", transformation);
		final CipherTransformation seTransformation = transformation2SymmetricCipherTransformation.get(transformation);
		if (seTransformation == null)
			throw new IllegalArgumentException("There is no CipherTransformation for this transformation: " + transformation);

		return seTransformation;
	}

}
