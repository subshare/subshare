package org.subshare.gui.pgp.createkey.advanced;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.wizard.WizardPage;

public class AdvancedWizardPage extends WizardPage {

	private final CreatePgpKeyParam createPgpKeyParam;

	private AdvancedPane advancedPane;

	public AdvancedWizardPage(final CreatePgpKeyParam createPgpKeyParam) {
		super("Advanced");
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);
	}

	@Override
	protected Parent createContent() {
		advancedPane = new AdvancedPane(createPgpKeyParam) {
			@Override
			protected void updateComplete() {
				AdvancedWizardPage.this.completeProperty().set(isComplete());
			}
		};
		return advancedPane;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (advancedPane != null)
			advancedPane.requestFocus();
	}
}
