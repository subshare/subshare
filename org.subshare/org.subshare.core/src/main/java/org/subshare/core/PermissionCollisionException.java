package org.subshare.core;

public class PermissionCollisionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PermissionCollisionException() { }

	public PermissionCollisionException(final String message) {
		super(message);
	}

	public PermissionCollisionException(final Throwable cause) {
		super(cause);
	}

	public PermissionCollisionException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public PermissionCollisionException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
