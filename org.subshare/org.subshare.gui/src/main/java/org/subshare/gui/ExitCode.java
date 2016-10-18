package org.subshare.gui;

public enum ExitCode {

	SUCCESS(0),

	WELCOME_WIZARD_ABORTED(1),

	INTRO_WIZARD_ABORTED(2),

	EXCEPTION_CAUGHT(666)
	;

	private final int numericCode;

	private ExitCode(final int numeric) {
		this.numericCode = numeric;
	}

	/**
	 * Gets the actual code returned to the operating system (passed to {@link System#exit(int)}).
	 * @return the actual code returned to the operating system.
	 */
	public int getNumericCode() {
		return numericCode;
	}
}
