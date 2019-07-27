package org.subshare.gui.pgp.assignownertrust.selectownertrust;

import static java.util.Objects.*;

import org.subshare.gui.pgp.assignownertrust.AssignOwnerTrustData;
import org.subshare.gui.pgp.assignownertrust.selectkey.SelectKeyWizardPage;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class SelectOwnerTrustWizardPage extends WizardPage {

	private final AssignOwnerTrustData assignOwnerTrustData;

	public SelectOwnerTrustWizardPage(final AssignOwnerTrustData assignOwnerTrustData) {
		super("How much is the owner trusted?");
		this.assignOwnerTrustData = requireNonNull(assignOwnerTrustData, "assignOwnerTrustData");
	}

	@Override
	protected void init() {
		super.init();

		if (assignOwnerTrustData.getUser().getPgpKeys().size() > 1)
			setNextPage(new SelectKeyWizardPage(assignOwnerTrustData));
	}

	@Override
	protected Parent createContent() {
		return new SelectOwnerTrustPane(assignOwnerTrustData);
	}
}
