package org.subshare.gui.pgp.assignownertrust.selectownertrust;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.pgp.assignownertrust.AssignOwnerTrustData;
import org.subshare.gui.pgp.assignownertrust.selectkey.SelectKeyWizardPage;
import org.subshare.gui.wizard.WizardPage;

public class SelectOwnerTrustWizardPage extends WizardPage {

	private final AssignOwnerTrustData assignOwnerTrustData;

	public SelectOwnerTrustWizardPage(final AssignOwnerTrustData assignOwnerTrustData) {
		super("How much is the owner trusted?");
		this.assignOwnerTrustData = assertNotNull("assignOwnerTrustData", assignOwnerTrustData);
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
