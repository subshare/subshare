package org.subshare.core.pgp.transport.local;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.transport.AbstractPgpTransport;

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
	public void exportPublicKeys(final Set<PgpKeyId> pgpKeyIds, final long changedAfterLocalRevision, final OutputStream out) {
		final HashSet<PgpKey> masterKeys = new HashSet<PgpKey>(pgpKeyIds.size());
		for (final PgpKeyId pgpKeyId : pgpKeyIds) {
			final PgpKey masterKey = pgp.getPgpKey(pgpKeyId);
			final long localRevision = pgp.getLocalRevision(masterKey);
			if (localRevision > changedAfterLocalRevision)
				masterKeys.add(masterKey);
		}

		pgp.exportPublicKeys(masterKeys, out);
	}

	@Override
	public void importKeys(final InputStream in) {
		pgp.importKeys(in);
	}
}
