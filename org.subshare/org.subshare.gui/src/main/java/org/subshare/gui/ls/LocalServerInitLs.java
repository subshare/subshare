package org.subshare.gui.ls;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class LocalServerInitLs {

	private LocalServerInitLs() {
	}

	public static void init() {
		LocalServerClient.getInstance().invokeStatic("org.subshare.ls.server.LocalServerInit", "init");
	}
}
