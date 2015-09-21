package org.subshare.gui.pgp.assignownertrust;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashSet;
import java.util.Set;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.core.user.User;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.pgp.assignownertrust.selectownertrust.SelectOwnerTrustPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class AssignOwnerTrustWizard extends Wizard {

	private final AssignOwnerTrustData assignOwnerTrustData;
	private final Pgp pgp;

	public AssignOwnerTrustWizard(final AssignOwnerTrustData assignOwnerTrustData) {
		super(new SelectOwnerTrustPage(assignOwnerTrustData));
		this.assignOwnerTrustData = assertNotNull("assignOwnerTrustData", assignOwnerTrustData);
		final User user = assignOwnerTrustData.getUser();
		assertNotNull("assignOwnerTrustData.user", user);
		this.pgp = PgpLs.getPgpOrFail();

		final Set<PgpKey> userPgpKeys = user.getPgpKeys();
		for (final PgpKey pgpKey : assignOwnerTrustData.getPgpKeys()) {
			if (! userPgpKeys.contains(pgpKey))
				throw new IllegalArgumentException(String.format(
						"pgpKey in assignOwnerTrustData.pgpKeys does not belong to user! pgpKeyId='%s' userId='%s' userFirstName='%s' userLastName='%s'",
						pgpKey.getPgpKeyId(), user.getUserId(), user.getFirstName(), user.getLastName()));
		}
	}

	@Override
	public String getTitle() {
		return "Assign/modify owner trust";
	}

	@Override
	public void init() {
		super.init();

		determineOwnerTrust();
		determineAssignToAllPgpKeys();
	}

	private void determineOwnerTrust() {
		PgpOwnerTrust ownerTrust = assignOwnerTrustData.getOwnerTrust();
		if (ownerTrust == null) {
			for (PgpKey pgpKey : assignOwnerTrustData.getPgpKeys()) {
				final PgpOwnerTrust ot = pgp.getOwnerTrust(pgpKey);
				if (ownerTrust == null || ownerTrust.compareTo(ot) < 0)
					ownerTrust = ot;
			}

			if (ownerTrust == null) {
				for (PgpKey pgpKey : assignOwnerTrustData.getUser().getPgpKeys()) {
					final PgpOwnerTrust ot = pgp.getOwnerTrust(pgpKey);
					if (ownerTrust == null || ownerTrust.compareTo(ot) < 0)
						ownerTrust = ot;
				}
			}

			if (ownerTrust == null)
				ownerTrust = PgpOwnerTrust.UNKNOWN;

			assignOwnerTrustData.setOwnerTrust(ownerTrust);
		}
	}

	private void determineAssignToAllPgpKeys() {
		if (assignOwnerTrustData.getAssignToAllPgpKeys() == null) {
			final Set<PgpKey> userPgpKeys = assignOwnerTrustData.getUser().getPgpKeys();
			final Set<PgpKey> selectedPgpKeys = assignOwnerTrustData.getPgpKeys();
			if (selectedPgpKeys.isEmpty() || userPgpKeys.size() == 1 || userPgpKeys.equals(selectedPgpKeys))
				assignOwnerTrustData.setAssignToAllPgpKeys(true);
			else {
				final Set<PgpOwnerTrust> ownerTrustsOfSelectedKeys = new HashSet<>();
				final Set<PgpOwnerTrust> ownerTrustsOfNonSelectedKeys = new HashSet<>();
				for (final PgpKey pgpKey : userPgpKeys) {
					PgpOwnerTrust ownerTrust = pgp.getOwnerTrust(pgpKey);
					if (selectedPgpKeys.contains(pgpKey))
						ownerTrustsOfSelectedKeys.add(ownerTrust);
					else
						ownerTrustsOfNonSelectedKeys.add(ownerTrust);
				}

				if (ownerTrustsOfNonSelectedKeys.size() == 1
						&& ownerTrustsOfNonSelectedKeys.iterator().next() == PgpOwnerTrust.UNKNOWN)
					assignOwnerTrustData.setAssignToAllPgpKeys(true);
				else {
					assignOwnerTrustData.setAssignToAllPgpKeys(ownerTrustsOfNonSelectedKeys.size() == 1
							&& ownerTrustsOfSelectedKeys.containsAll(ownerTrustsOfNonSelectedKeys));
				}
			}
		}
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		final Set<PgpKey> pgpKeys;
		if (assignOwnerTrustData.getAssignToAllPgpKeys())
			pgpKeys = assignOwnerTrustData.getUser().getPgpKeys();
		else
			pgpKeys = assignOwnerTrustData.getPgpKeys();

		final PgpOwnerTrust ownerTrust = assignOwnerTrustData.getOwnerTrust();
		for (PgpKey pgpKey : pgpKeys)
			pgp.setOwnerTrust(pgpKey, ownerTrust);

		pgp.updateTrustDb();
	}
}
