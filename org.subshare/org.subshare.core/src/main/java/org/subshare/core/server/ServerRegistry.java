package org.subshare.core.server;

public class ServerRegistry {

	private static final class Holder {
		public static final ServerRegistry instance = new ServerRegistry();
	}

	protected ServerRegistry() {
	}

	public static ServerRegistry getInstance() {
		return Holder.instance;
	}
}
