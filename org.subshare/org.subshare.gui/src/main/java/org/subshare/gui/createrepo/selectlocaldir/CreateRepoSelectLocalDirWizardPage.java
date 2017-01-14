package org.subshare.gui.createrepo.selectlocaldir;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.createrepo.selectowner.SelectOwnerWizardPage;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class CreateRepoSelectLocalDirWizardPage extends WizardPage {

	private final CreateRepoData createRepoData;
	private CreateRepoSelectLocalDirPane createRepoSelectLocalDirPane;

	public CreateRepoSelectLocalDirWizardPage(final CreateRepoData createRepoData) {
		super("Local directory to be uploaded and shared");
		this.createRepoData = assertNotNull(createRepoData, "createRepoData");
		shownRequired.set(true);
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
		createRepoSelectLocalDirPane = new CreateRepoSelectLocalDirPane(createRepoData);
		return createRepoSelectLocalDirPane;
	}
}
