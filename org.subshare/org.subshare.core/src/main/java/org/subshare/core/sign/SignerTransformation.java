package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.crypto.Signer;

import co.codewizards.cloudstore.core.config.Config;

/**
 * Supported {@link Signer} transformations.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public enum SignerTransformation {
	// IMPORTANT: Do *not* modify the order of these enum values and do not insert new enum values inbetween!
	// You may add new values at the end. These enum values are encoded in signed data via their ordinal
	// (see java.lang.Enum.ordinal()). Changing the ordinal (= index) of existing values therefore causes
	// signed data to not be verifyable anymore!!!

	RSA_SHA1("RSA/SHA1"),
	RSA_SHA256("RSA/SHA256"),
	RSA_SHA512("RSA/SHA512")
	;

	private static final Map<String, SignerTransformation> transformation2SignerTransformation;
	static {
		final Map<String, SignerTransformation> m = new HashMap<String, SignerTransformation>(values().length);
		for (final SignerTransformation st : values()) {
			m.put(st.getTransformation(), st);
		}
		transformation2SignerTransformation = Collections.unmodifiableMap(m);
	}

	private final String transformation;

	private SignerTransformation(final String transformation) {
		this.transformation = assertNotNull(transformation, "transformation");
	}

	public String getTransformation() {
		return transformation;
	}

	public static SignerTransformation fromTransformation(final String transformation) {
		assertNotNull(transformation, "transformation");
		final SignerTransformation st = transformation2SignerTransformation.get(transformation);
		if (st == null)
			throw new IllegalArgumentException("There is no SignerTransformation for this transformation: " + transformation);

		return st;
	}

	/**
	 * The {@code key} used with {@link Config#getPropertyAsEnum(String, Enum)}.
	 */
	public static final String CONFIG_KEY = "signerTransformation"; //$NON-NLS-1$
	/**
	 * The {@code defaultValue} used with {@link Config#getPropertyAsEnum(String, Enum)} for asymmetric encryption.
	 */
	public static final SignerTransformation CONFIG_DEFAULT_VALUE = RSA_SHA1; // TODO should we better use SHA256 or even stronger by default?
}
