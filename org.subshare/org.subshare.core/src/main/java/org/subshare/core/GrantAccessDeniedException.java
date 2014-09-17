package org.subshare.core;

public class GrantAccessDeniedException extends AccessDeniedException {

	private static final long serialVersionUID = 1L;

	public GrantAccessDeniedException() { }

	public GrantAccessDeniedException(final String message) {
		super(message);
	}

	public GrantAccessDeniedException(final Throwable cause) {
		super(cause);
	}

	public GrantAccessDeniedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public GrantAccessDeniedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
