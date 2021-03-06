package org.subshare.gui.pgp.keytree;

import static java.util.Objects.*;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.pgp.PgpKeyFlag;

public class PgpKeyFlagsToUsageConverter {

	private static final Logger logger = LoggerFactory.getLogger(PgpKeyFlagsToUsageConverter.class);

	public PgpKeyFlagsToUsageConverter() {
	}

	public String toUsage(final Set<PgpKeyFlag> pgpKeyFlags) {
		requireNonNull(pgpKeyFlags, "pgpKeyFlags");

		final StringBuilder sb = new StringBuilder();
		for (final PgpKeyFlag pgpKeyFlag : pgpKeyFlags) {
			switch (pgpKeyFlag) {
				case CAN_AUTHENTICATE:
				case CAN_CERTIFY:
				case CAN_SIGN:
				case CAN_ENCRYPT_COMMS:
				case CAN_ENCRYPT_STORAGE:
					append(sb, pgpKeyFlag);
					break;
				case MAYBE_SHARED:
				case MAYBE_SPLIT:
					break; // ignore this as it's no usage and the user probably doesn't care
				default:
					logger.warn("Unknown pgpKeyFlag: " + pgpKeyFlag);
			}
		}
		return sb.toString();
	}

	private static final void append(StringBuilder sb, PgpKeyFlag pgpKeyFlag) {
		if (sb.length() > 0)
			sb.append(", ");

		sb.append(Messages.getString(String.format(PgpKeyFlagsToUsageConverter.class.getSimpleName() + ".usage[%s]", pgpKeyFlag)));
	}
}
