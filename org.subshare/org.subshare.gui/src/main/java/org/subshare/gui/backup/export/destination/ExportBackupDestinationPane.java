package org.subshare.gui.backup.export.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Iterator;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.gui.backup.export.ExportBackupData;
import org.subshare.gui.filetree.FileTreePane;

import co.codewizards.cloudstore.core.oio.File;

public abstract class ExportBackupDestinationPane extends GridPane {

	private final ExportBackupData exportBackupData;

	@FXML
	private FileTreePane fileTreePane;

	public ExportBackupDestinationPane(final ExportBackupData exportBackupData) {
		loadDynamicComponentFxml(ExportBackupDestinationPane.class, this);
		this.exportBackupData = assertNotNull("exportBackupData", exportBackupData);
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

	protected boolean isComplete() {
		return ! fileTreePane.getSelectedFiles().isEmpty();
	}

	protected void onSelectedFilesChanged() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		exportBackupData.setExportBackupDirectory(selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null);
		updateComplete();
	}

	protected abstract void updateComplete();

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
