package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.List;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyValidity;
import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.core.pgp.PgpSignature;

public class CertificationPgpKeyTreeItem extends PgpKeyTreeItem<PgpSignature> {

	private final PgpKeyId signaturePgpKeyId; // never null
	private final PgpKey signaturePgpKey; // may be null!

	public CertificationPgpKeyTreeItem(final Pgp pgp, final PgpSignature signature) {
		super(assertNotNull(signature, "signature"));
		signaturePgpKeyId = assertNotNull(signature.getPgpKeyId(), "signature.pgpKeyId");
		assertNotNull(pgp, "pgp");
		signaturePgpKey = pgp.getPgpKey(signaturePgpKeyId);
	}

	@Override
	public String getKeyValidity() {
		if (signaturePgpKey == null)
			return null;

		final PgpKeyValidity kv = getPgp().getKeyValidity(signaturePgpKey);
		return kv.toShortString();
	}

	@Override
	public String getOwnerTrust() {
		if (signaturePgpKey == null)
			return null;

		final PgpOwnerTrust ot = getPgp().getOwnerTrust(signaturePgpKey);
		return ot.toShortString();
	}

	@Override
	public String getName() {
		if (signaturePgpKey == null)
			return "(unknown)";

		final List<String> userIds = signaturePgpKey.getMasterKey().getUserIds();
		return userIds.isEmpty() ? getKeyId() : userIds.get(0);
	}

	@Override
	public String getKeyId() {
		if (signaturePgpKey == null)
			return signaturePgpKeyId.toHumanString();

		return signaturePgpKey.getMasterKey().getPgpKeyId().toHumanString();
	}
}
