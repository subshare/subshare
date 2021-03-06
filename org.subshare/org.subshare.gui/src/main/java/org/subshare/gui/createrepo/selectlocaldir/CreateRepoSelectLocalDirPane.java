package org.subshare.gui.createrepo.selectlocaldir;

import static java.util.Objects.*;
import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.ls.ServerRepoManagerLs;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.oio.File;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;

public class CreateRepoSelectLocalDirPane extends WizardPageContentGridPane {

	private final CreateRepoData createRepoData;

	@FXML
	private FileTreePane fileTreePane;

	public CreateRepoSelectLocalDirPane(final CreateRepoData createRepoData) {
		this.createRepoData = requireNonNull(createRepoData, "createRepoData");
		loadDynamicComponentFxml(CreateRepoSelectLocalDirPane.class, this);
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

	protected void onSelectedFilesChanged() {
		final ObservableSet<File> files = fileTreePane.getSelectedFiles();
		createRepoData.setLocalDirectory(files.isEmpty() ? null : files.iterator().next());
		updateComplete();
	}

	@Override
	protected boolean isComplete() {
		final File directory = createRepoData.getLocalDirectory();
		if (directory == null)
			return false;

		return ServerRepoManagerLs.getServerRepoManager().canUseLocalDirectory(directory);
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
