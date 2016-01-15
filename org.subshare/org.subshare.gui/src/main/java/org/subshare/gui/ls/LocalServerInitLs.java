package org.subshare.gui.ls;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class LocalServerInitLs {

	private LocalServerInitLs() {
	}

	public static void initPrepare() {
		LocalServerClient.getInstance().invokeStatic("org.subshare.ls.server.cproc.LocalServerInit", "initPrepare");
	}

	public static void initFinish() {
		LocalServerClient.getInstance().invokeStatic("org.subshare.ls.server.cproc.LocalServerInit", "initFinish");
	}
}
