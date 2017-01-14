package org.subshare.gui.invitation.accept.source;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.Severity;

public class CheckInvitationFileResult {

	public static enum Type {
		OK,

		SIGNING_KEY_MARGINALLY_TRUSTED,
		SIGNING_KEY_MISSING,
		SIGNING_KEY_UNKNOWN_VALIDITY,
		SIGNING_KEY_DISABLED,
		SIGNING_KEY_NOT_TRUSTED,
		SIGNING_KEY_EXPIRED,
		SIGNING_KEY_REVOKED,

		ERROR_GENERAL,
		ERROR_SIGNATURE_MISSING
	}

	private final Type type;
	private final Severity severity;
	private final String message;
	private final String longText;

	public CheckInvitationFileResult(final Type type, final Severity severity) {
		this(type, severity, null, null);
	}

	public CheckInvitationFileResult(final Type type, final Severity severity, final String message, final String longText) {
		this.type = assertNotNull(type, "type");
		this.severity = assertNotNull(severity, "severity");
		this.message = message; // may be null!
		this.longText = longText; // may be null!
	}

	public Type getType() {
		return type;
	}

	public Severity getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public String getLongText() {
		return longText;
	}
}
