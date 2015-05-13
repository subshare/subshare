package org.subshare.gui.ls;

import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class PgpPrivateKeyPassphraseManagerLs {

	private PgpPrivateKeyPassphraseManagerLs() {
	}

	public static PgpPrivateKeyPassphraseStore getPgpPrivateKeyPassphraseStore() {
		return LocalServerClient.getInstance().invokeStatic(PgpPrivateKeyPassphraseStoreImpl.class, "getInstance");
	}
}
