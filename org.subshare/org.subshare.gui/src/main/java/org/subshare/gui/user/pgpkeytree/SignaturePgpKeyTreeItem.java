package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.List;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpSignature;

public class SignaturePgpKeyTreeItem extends PgpKeyTreeItem<PgpSignature> {

	private final PgpKeyId signaturePgpKeyId; // never null
	private final PgpKey signaturePgpKey; // may be null!

	public SignaturePgpKeyTreeItem(final Pgp pgp, final PgpSignature signature) {
		super(assertNotNull("signature", signature));
		signaturePgpKeyId = assertNotNull("signature.pgpKeyId", signature.getPgpKeyId());
		assertNotNull("pgp", pgp);
		signaturePgpKey = pgp.getPgpKey(signaturePgpKeyId);
	}

	@Override
	public String getName() {
		if (signaturePgpKey == null)
			return signaturePgpKeyId.toHumanString();

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
