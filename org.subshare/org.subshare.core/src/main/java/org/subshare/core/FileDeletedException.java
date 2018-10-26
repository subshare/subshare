package org.subshare.core;

public class FileDeletedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FileDeletedException() {
	}

	public FileDeletedException(String message) {
		super(message);
	}

	public FileDeletedException(Throwable cause) {
		super(cause);
	}

	public FileDeletedException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileDeletedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
