package org.subshare.core.pgp.transport.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.transport.AbstractPgpTransport;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;

public class LocalPgpTransport extends AbstractPgpTransport {

	private final Pgp pgp;

	public LocalPgpTransport() {
		pgp = PgpRegistry.getInstance().getPgpOrFail();
	}

	@Override
	public long getLocalRevision() {
		return pgp.getLocalRevision();
	}

	@Override
	public Set<PgpKeyId> getMasterKeyIds() {
		final Collection<PgpKey> masterKeys = pgp.getMasterKeys();
		final Set<PgpKeyId> pgpKeyIds = new HashSet<PgpKeyId>(masterKeys.size());
		for (final PgpKey pgpKey : masterKeys)
			pgpKeyIds.add(pgpKey.getPgpKeyId());

		return pgpKeyIds;
	}

	@Override
	public void exportPublicKeys(final Set<PgpKeyId> pgpKeyIds, final long changedAfterLocalRevision, final IOutputStream out) {
		assertNotNull("pgpKeyIds", pgpKeyIds);
		assertNotNull("out", out);
		final HashSet<PgpKey> masterKeys = new HashSet<PgpKey>(pgpKeyIds.size());
		for (final PgpKeyId pgpKeyId : pgpKeyIds) {
			final PgpKey masterKey = pgp.getPgpKey(pgpKeyId);
			if (masterKey != null) {
				final long localRevision = pgp.getLocalRevision(masterKey);
				if (localRevision > changedAfterLocalRevision)
					masterKeys.add(masterKey);
			}
		}

		pgp.exportPublicKeys(masterKeys, out);
	}

	@Override
	public void exportPublicKeysMatchingQuery(final String queryString, final IOutputStream out) {
		assertNotNull("queryString", queryString);
		assertNotNull("out", out);
		final HashSet<PgpKey> masterKeys = new HashSet<PgpKey>();
		final String userId = queryString.trim().toLowerCase();
		final String emailWithSeparators = appendEmailSeparators(userId.toLowerCase());

		for (PgpKey masterKey : pgp.getMasterKeys()) {
			if (matches(masterKey, userId, emailWithSeparators))
				masterKeys.add(masterKey);
		}
		pgp.exportPublicKeys(masterKeys, out);
	}

	private String appendEmailSeparators(String email) {
		email = assertNotNull("email", email).trim();

		final StringBuilder sb = new StringBuilder(email.length() + 2);

		if (! email.startsWith("<"))
			sb.append('<');

		sb.append(email);

		if (! email.endsWith(">"))
			sb.append('>');

		return sb.toString();
	}

	private boolean matches(final PgpKey pgpKey, final String searchedUserId, String searchedEmailWithSeparators) {
		assertNotNull("pgpKey", pgpKey);
		assertNotNull("searchedUserId", searchedUserId);
		assertNotNull("searchedEmailWithSeparators", searchedEmailWithSeparators);
		for (final String userId : pgpKey.getUserIds()) {
			String userIdLowerCase = userId.toLowerCase();
			if (userIdLowerCase.equals(searchedUserId)
					|| userIdLowerCase.contains(searchedEmailWithSeparators))
				return true;
		}
		return false;
	}

	@Override
	public void importKeys(final IInputStream in) {
		pgp.importKeys(in);
	}
}
