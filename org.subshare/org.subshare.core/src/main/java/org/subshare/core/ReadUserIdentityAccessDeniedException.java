package org.subshare.core;

import org.subshare.core.dto.PermissionType;

/**
 * Exception thrown, if {@link PermissionType#readUserIdentity} is missing.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ReadUserIdentityAccessDeniedException extends AccessDeniedException {

	private static final long serialVersionUID = 1L;

	public ReadUserIdentityAccessDeniedException() { }

	public ReadUserIdentityAccessDeniedException(final String message) {
		super(message);
	}

	public ReadUserIdentityAccessDeniedException(final Throwable cause) {
		super(cause);
	}

	public ReadUserIdentityAccessDeniedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ReadUserIdentityAccessDeniedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
