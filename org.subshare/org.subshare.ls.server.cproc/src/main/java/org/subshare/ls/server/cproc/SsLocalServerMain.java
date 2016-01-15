package org.subshare.ls.server.cproc;

import org.subshare.ls.server.SsLocalServer;

import co.codewizards.cloudstore.ls.server.cproc.LocalServerMain;

public class SsLocalServerMain extends LocalServerMain {

	public static void main(String[] args) throws Exception {
		setLocalServerClass(SsLocalServer.class);
		LocalServerMain.main(args);
	}

}
