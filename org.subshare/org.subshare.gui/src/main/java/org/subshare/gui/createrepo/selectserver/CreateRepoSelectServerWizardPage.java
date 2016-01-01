package org.subshare.gui.createrepo.selectserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.createrepo.selectlocaldir.CreateRepoSelectLocalDirWizardPage;
import org.subshare.gui.wizard.WizardPage;

public class CreateRepoSelectServerWizardPage extends WizardPage {
	private final CreateRepoData createRepoData;
	private CreateRepoSelectServerPane createRepoSelectServerPane;

	public CreateRepoSelectServerWizardPage(final CreateRepoData createRepoData) {
		super("Server for new repository");
		this.createRepoData = assertNotNull("createRepoData", createRepoData);
	}

	@Override
	protected void init() {
		super.init();

		CreateRepoSelectLocalDirWizardPage selectLocalDirWizardPage = new CreateRepoSelectLocalDirWizardPage(createRepoData);
		setNextPage(selectLocalDirWizardPage);
	}

	@Override
	protected Parent createContent() {
		createRepoSelectServerPane = new CreateRepoSelectServerPane(createRepoData) {
			@Override
			protected void updateComplete() {
				CreateRepoSelectServerWizardPage.this.setComplete(isComplete());
			}
		};
		return createRepoSelectServerPane;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (createRepoSelectServerPane != null)
			createRepoSelectServerPane.requestFocus();
	}
}
