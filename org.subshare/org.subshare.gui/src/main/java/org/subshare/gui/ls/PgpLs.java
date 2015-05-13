package org.subshare.gui.ls;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpRegistry;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class PgpLs {

	private PgpLs() {
	}

	public static Pgp getPgpOrFail() {
		final LocalServerClient localServerClient = LocalServerClient.getInstance();
		final Object pgpRegistry = localServerClient.invokeStatic(PgpRegistry.class, "getInstance");
		final Pgp pgp = localServerClient.invoke(pgpRegistry, "getPgpOrFail");
		return pgp;
	}
}
