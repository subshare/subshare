package org.subshare.gui.pgp.assignownertrust.selectownertrust;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.pgp.assignownertrust.AssignOwnerTrustData;
import org.subshare.gui.wizard.WizardPage;

public class SelectOwnerTrustPage extends WizardPage {

	private final AssignOwnerTrustData assignOwnerTrustData;

	public SelectOwnerTrustPage(final AssignOwnerTrustData assignOwnerTrustData) {
		super("How much is the owner trusted?");
		this.assignOwnerTrustData = assertNotNull("assignOwnerTrustData", assignOwnerTrustData);
	}

	@Override
	protected Parent createContent() {
		return new SelectOwnerTrustPane(assignOwnerTrustData);
	}

}
