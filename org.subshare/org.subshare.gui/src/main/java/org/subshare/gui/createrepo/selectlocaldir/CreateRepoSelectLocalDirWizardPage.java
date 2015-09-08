package org.subshare.gui.createrepo.selectlocaldir;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.createrepo.selectowner.SelectOwnerWizardPage;
import org.subshare.gui.wizard.WizardPage;

public class CreateRepoSelectLocalDirWizardPage extends WizardPage {

	private final CreateRepoData createRepoData;
	private CreateRepoSelectLocalDirPane createRepoSelectLocalDirPane;
	private boolean shownAtLeastOnce;

	public CreateRepoSelectLocalDirWizardPage(final CreateRepoData createRepoData) {
		super("Local directory to be uploaded and shared");
		this.createRepoData = assertNotNull("createRepoData", createRepoData);
	}

	@Override
	protected void init() {
		super.init();

		final SelectOwnerWizardPage selectOwnerWizardPage = new SelectOwnerWizardPage(createRepoData);
		if (selectOwnerWizardPage.isNeeded())
			setNextPage(selectOwnerWizardPage);
	}

	@Override
	protected Parent createContent() {
		createRepoSelectLocalDirPane = new CreateRepoSelectLocalDirPane(createRepoData) {
			@Override
			protected void updateComplete() {
				CreateRepoSelectLocalDirWizardPage.this.setComplete(shownAtLeastOnce && isComplete());
			}
		};
		return createRepoSelectLocalDirPane;
	}

	@Override
	protected void onShown() {
		super.onShown();
		shownAtLeastOnce = true;
		createRepoSelectLocalDirPane.updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (createRepoSelectLocalDirPane != null)
			createRepoSelectLocalDirPane.requestFocus();
	}
}
