package org.subshare.core.pgp;

public enum PgpSignatureType {
	BINARY_DOCUMENT(-1),
    CANONICAL_TEXT_DOCUMENT(-1),
    STAND_ALONE(-1),

    DEFAULT_CERTIFICATION(10),
    NO_CERTIFICATION(20),
    CASUAL_CERTIFICATION(30),
    POSITIVE_CERTIFICATION(40),

    SUBKEY_BINDING(-1),
    PRIMARYKEY_BINDING(-1),
    DIRECT_KEY(-1),
    KEY_REVOCATION(-1),
    SUBKEY_REVOCATION(-1),
    CERTIFICATION_REVOCATION(-1),
    TIMESTAMP(-1);

	private final int trustLevel;

	private PgpSignatureType(final int trustLevel) {
		this.trustLevel = trustLevel;
	}

	public int getTrustLevel() {
		return trustLevel;
	}
}
