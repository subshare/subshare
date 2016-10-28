package org.subshare.gui.ls;

import org.subshare.core.pgp.transport.PgpTransportFactoryRegistry;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistryImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class PgpTransportFactoryRegistryLs {

	private PgpTransportFactoryRegistryLs() {
	}

	public static PgpTransportFactoryRegistry getPgpTransportFactoryRegistry() {
		return LocalServerClient.getInstance().invokeStatic(PgpTransportFactoryRegistryImpl.class, "getInstance");
	}
}
