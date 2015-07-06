package org.subshare.gui.welcome.createpgpkey;

import javafx.event.ActionEvent;
import javafx.scene.Parent;

import org.subshare.gui.pgp.createkey.CreatePgpKeyPane;
import org.subshare.gui.welcome.WelcomeWizardPage;

public class CreatePgpKeyWizardPage extends WelcomeWizardPage {

	public CreatePgpKeyWizardPage() {
		super("Create identity with PGP key");
	}

	@Override
	protected Parent getContent() {
		CreatePgpKeyPane createPgpKeyPane = new CreatePgpKeyPane(getWelcomeData().getCreatePgpKeyParam()) {
			@Override
			protected void okButtonClicked(final ActionEvent event) { }

			@Override
			protected void cancelButtonClicked(final ActionEvent event) { }

			@Override
			protected void updateDisabled() {
				CreatePgpKeyWizardPage.this.completeProperty().set(isComplete());
			}
		};

		createPgpKeyPane.getChildren().removeIf(node -> "buttonBar".equals(node.getId()) );

		return createPgpKeyPane;
	}

}
