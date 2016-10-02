package org.subshare.test;

public enum TestUser {
	/**
	 * Dummy-user for the server.
	 */
	server(null, null),

	/**
	 * The primary user. Some code relies on this being the owner.
	 */
	marco("marco@codewizards.co", "test12345"),

	bieber("bieber@nightlabs.de", "test111"),
	daniel("daniel@nightlabs.de", "test222"),
	khaled("k.soliman@blackbytes.de", "test678"),
	xenia("xenia@subshare.net", "test888"),
	yasmin("yasmin@subshare.net", "test999")
	;

	private final String email;
	private final String pgpPrivateKeyPassword;

	private TestUser(String email, String pgpPrivateKeyPassword) {
		this.email = email;
		this.pgpPrivateKeyPassword = pgpPrivateKeyPassword;
	}

	public String getEmail() {
		return email;
	}

	public String getPgpPrivateKeyPassword() {
		return pgpPrivateKeyPassword;
	}

	public boolean isRealUser() {
		return server != this;
	}
}
