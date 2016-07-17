package org.subshare.test;

public enum TestUser {
	bieber("test111"),
	daniel("test222"),
	khaled("test678"),
	marco("test12345")

	;

	private final String pgpPrivateKeyPassword;

	private TestUser(String pgpPrivateKeyPassword) {
		this.pgpPrivateKeyPassword = pgpPrivateKeyPassword;
	}

	public String getPgpPrivateKeyPassword() {
		return pgpPrivateKeyPassword;
	}
}
