package org.subshare.gui.pgp.assignownertrust.selectkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.pgp.assignownertrust.AssignOwnerTrustData;
import org.subshare.gui.wizard.WizardPage;

public class SelectKeyPage extends WizardPage {

	private final AssignOwnerTrustData assignOwnerTrustData;
	private SelectKeyPane selectKeyPane;

	public SelectKeyPage(final AssignOwnerTrustData assignOwnerTrustData) {
		super("Assign owner-trust to which keys?");
		this.assignOwnerTrustData = assertNotNull("assignOwnerTrustData", assignOwnerTrustData);
	}

	@Override
	protected Parent createContent() {
		return selectKeyPane = new SelectKeyPane(assignOwnerTrustData) {
			@Override
			protected void updateComplete() {
				SelectKeyPage.this.setComplete(isComplete());
			}
		};
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		if (selectKeyPane != null)
			selectKeyPane.requestFocus();
	}
}
