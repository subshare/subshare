package org.subshare.core.pgp;

import java.util.ServiceLoader;

public class PgpRegistry {

	private PgpAuthenticationCallback pgpAuthenticationCallback;
	private Pgp pgp;

	protected PgpRegistry() { }

	private static final class Holder {
		public static final PgpRegistry instance = new PgpRegistry();
	}

	public static PgpRegistry getInstance() {
		return Holder.instance;
	}

	public Pgp getPgpOrFail() {
		Pgp pgp = this.pgp;
		if (pgp == null) {
			for (final Pgp p : ServiceLoader.load(Pgp.class)) {
				if (! p.isSupported())
					continue;

				if (pgp == null || pgp.getPriority() < p.getPriority())
					pgp = p;
			}
			this.pgp = pgp;
		}
		return pgp;
	}

	public PgpAuthenticationCallback getPgpAuthenticationCallback() {
		return pgpAuthenticationCallback;
	}

	public void setPgpAuthenticationCallback(final PgpAuthenticationCallback pgpAuthenticationCallback) {
		this.pgpAuthenticationCallback = pgpAuthenticationCallback;
	}
}
