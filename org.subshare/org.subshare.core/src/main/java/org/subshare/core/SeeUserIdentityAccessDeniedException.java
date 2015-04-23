package org.subshare.core;

import org.subshare.core.dto.PermissionType;

/**
 * Exception thrown, if {@link PermissionType#seeUserIdentity} is missing.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class SeeUserIdentityAccessDeniedException extends AccessDeniedException {

	private static final long serialVersionUID = 1L;

	public SeeUserIdentityAccessDeniedException() { }

	public SeeUserIdentityAccessDeniedException(final String message) {
		super(message);
	}

	public SeeUserIdentityAccessDeniedException(final Throwable cause) {
		super(cause);
	}

	public SeeUserIdentityAccessDeniedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public SeeUserIdentityAccessDeniedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
