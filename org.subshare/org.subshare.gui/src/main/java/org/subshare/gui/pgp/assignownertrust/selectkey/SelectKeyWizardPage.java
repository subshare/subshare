package org.subshare.gui.pgp.assignownertrust.selectkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.pgp.assignownertrust.AssignOwnerTrustData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class SelectKeyWizardPage extends WizardPage {

	private final AssignOwnerTrustData assignOwnerTrustData;

	public SelectKeyWizardPage(final AssignOwnerTrustData assignOwnerTrustData) {
		super("Assign owner-trust to which keys?");
		this.assignOwnerTrustData = assertNotNull("assignOwnerTrustData", assignOwnerTrustData);
	}

	@Override
	protected Parent createContent() {
		return new SelectKeyPane(assignOwnerTrustData);
	}
}
