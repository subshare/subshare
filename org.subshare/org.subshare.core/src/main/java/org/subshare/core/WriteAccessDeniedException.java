package org.subshare.core;

public class WriteAccessDeniedException extends AccessDeniedException {

	private static final long serialVersionUID = 1L;

	public WriteAccessDeniedException() { }

	public WriteAccessDeniedException(final String message) {
		super(message);
	}

	public WriteAccessDeniedException(final Throwable cause) {
		super(cause);
	}

	public WriteAccessDeniedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public WriteAccessDeniedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
