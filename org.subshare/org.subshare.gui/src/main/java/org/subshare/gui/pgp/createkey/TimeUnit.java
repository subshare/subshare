package org.subshare.gui.pgp.createkey;

public enum TimeUnit {

	YEAR(365L * 24L * 3600L * 1000L + 6L * 3600L * 1000L), // 365.25 days ;-)
	MONTH(30L * 24L * 3600L * 1000L + 12L * 3600L * 1000L), // 30.5 days ;-)
	DAY(24L * 3600L * 1000L)
	;

	private final long millis;

	private TimeUnit(final long millis) {
		this.millis = millis;
	}

	public long getMillis() {
		return millis;
	}

	public long getSeconds() {
		return getMillis() / 1000L;
	}
}
