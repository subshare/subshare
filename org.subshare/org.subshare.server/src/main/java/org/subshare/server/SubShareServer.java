package org.subshare.server;

import org.subshare.ls.server.SsLocalServer;
import org.subshare.rest.server.SubShareRest;
import org.glassfish.jersey.server.ResourceConfig;

import co.codewizards.cloudstore.ls.server.LocalServer;
import co.codewizards.cloudstore.server.CloudStoreServer;

public class SubShareServer extends CloudStoreServer {

	public static void main(final String[] args) throws Exception {
		setCloudStoreServerClass(SubShareServer.class);
		CloudStoreServer.main(args);
	}

	public SubShareServer(final String... args) {
		super(args);
	}

	@Override
	protected LocalServer createLocalServer() {
		return new SsLocalServer();
	}

	@Override
	protected ResourceConfig createResourceConfig() {
		return new SubShareRest();
	}
}
