package org.subshare.gui.pgp.createkey.userid;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.wizard.WizardPage;

public class UserIdWizardPage extends WizardPage {

	private final CreatePgpKeyParam createPgpKeyParam;

	private UserIdPane userIdPane;

	public UserIdWizardPage(final CreatePgpKeyParam createPgpKeyParam) {
		super("Identities");
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);
	}

	@Override
	protected Parent createContent() {
		userIdPane = new UserIdPane(createPgpKeyParam) {
			@Override
			protected void updateComplete() {
				UserIdWizardPage.this.setComplete(isComplete());
			}
		};
		return userIdPane;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (userIdPane != null)
			userIdPane.requestFocus();
	}
}
