package org.subshare.core;

public class ReadAccessDeniedException extends AccessDeniedException {

	private static final long serialVersionUID = 1L;

	public ReadAccessDeniedException() { }

	public ReadAccessDeniedException(final String message) {
		super(message);
	}

	public ReadAccessDeniedException(final Throwable cause) {
		super(cause);
	}

	public ReadAccessDeniedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ReadAccessDeniedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
