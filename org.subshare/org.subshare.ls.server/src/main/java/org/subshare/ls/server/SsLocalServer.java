package org.subshare.ls.server;

import org.subshare.ls.rest.server.SsLocalServerRest;
import org.glassfish.jersey.server.ResourceConfig;

import co.codewizards.cloudstore.ls.server.LocalServer;

public class SsLocalServer extends LocalServer {

	@Override
	protected ResourceConfig createResourceConfig() {
		return new SsLocalServerRest();
	}

}
