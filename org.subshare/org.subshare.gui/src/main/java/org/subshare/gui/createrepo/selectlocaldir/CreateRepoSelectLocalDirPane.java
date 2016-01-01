package org.subshare.gui.createrepo.selectlocaldir;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.ls.ServerRepoManagerLs;

import co.codewizards.cloudstore.core.oio.File;

public abstract class CreateRepoSelectLocalDirPane extends GridPane {

	private final CreateRepoData createRepoData;

	@FXML
	private FileTreePane fileTreePane;

	public CreateRepoSelectLocalDirPane(final CreateRepoData createRepoData) {
		this.createRepoData = assertNotNull("createRepoData", createRepoData);
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

	protected boolean isComplete() {
		final File directory = createRepoData.getLocalDirectory();
		if (directory == null)
			return false;

		return ServerRepoManagerLs.getServerRepoManager().canUseLocalDirectory(directory);
	}

	protected abstract void updateComplete();

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
