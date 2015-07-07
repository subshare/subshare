package org.subshare.gui.welcome.createpgpkey.validity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.wizard.WizardPage;

public class ValidityWizardPage extends WizardPage {

	private final CreatePgpKeyParam createPgpKeyParam;

	private ValidityPane validityPane;

	public ValidityWizardPage(CreatePgpKeyParam createPgpKeyParam) {
		super("OpenPGP key validity");
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);
	}

	@Override
	protected Parent createContent() {
		validityPane = new ValidityPane(createPgpKeyParam) {
			@Override
			protected void updateComplete() {
				ValidityWizardPage.this.completeProperty().set(isComplete());
			}
		};
		return validityPane;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (validityPane != null)
			validityPane.requestFocus();
	}
}
