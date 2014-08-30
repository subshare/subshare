package org.subshare.core.pgp;

public enum PgpKeyTrustLevel {

	NOT_TRUSTED,
	NOT_TRUSTED_EXPIRED,
	REVOKED,
	TRUSTED_EXPIRED,
	TRUSTED,
	ULTIMATE;

}
