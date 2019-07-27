package org.subshare.gui.createrepo.selectserver;

import static java.util.Objects.*;

import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.createrepo.selectlocaldir.CreateRepoSelectLocalDirWizardPage;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class CreateRepoSelectServerWizardPage extends WizardPage {
	private final CreateRepoData createRepoData;
	private CreateRepoSelectServerPane createRepoSelectServerPane;

	public CreateRepoSelectServerWizardPage(final CreateRepoData createRepoData) {
		super("Server for new repository");
		this.createRepoData = requireNonNull(createRepoData, "createRepoData");
	}

	@Override
	protected void init() {
		super.init();

		CreateRepoSelectLocalDirWizardPage selectLocalDirWizardPage = new CreateRepoSelectLocalDirWizardPage(createRepoData);
		setNextPage(selectLocalDirWizardPage);
	}

	@Override
	protected Parent createContent() {
		createRepoSelectServerPane = new CreateRepoSelectServerPane(createRepoData);
		return createRepoSelectServerPane;
	}
}
