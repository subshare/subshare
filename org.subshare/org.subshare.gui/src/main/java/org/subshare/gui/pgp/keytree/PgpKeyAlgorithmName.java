package org.subshare.gui.pgp.keytree;

import org.subshare.core.pgp.PgpKeyAlgorithm;

public final class PgpKeyAlgorithmName {

	private PgpKeyAlgorithmName() {
	}

	public static String getPgpKeyAlgorithmName(final PgpKeyAlgorithm algorithm) {
		if (algorithm == null)
			return null;

		return Messages.getString(String.format("PgpKeyAlgorithmName[%s]", algorithm.name()));
	}
}
