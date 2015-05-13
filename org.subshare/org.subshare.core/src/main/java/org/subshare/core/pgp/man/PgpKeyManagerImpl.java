package org.subshare.core.pgp.man;

public class PgpKeyManagerImpl implements PgpKeyManager {

	private static final class Holder {
		public static final PgpKeyManagerImpl instance = new PgpKeyManagerImpl();
	}

	private PgpKeyManagerImpl() {
	}

	public static PgpKeyManager getInstance() {
		return Holder.instance;
	}


}
