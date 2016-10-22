package org.subshare.gui.backup.exp.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Iterator;

import org.subshare.gui.backup.exp.ExportBackupData;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.oio.File;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;

public class ExportBackupDestinationPane extends WizardPageContentGridPane {

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

	@Override
	protected boolean isComplete() {
		return exportBackupData.getExportBackupDirectory() != null;
	}

	protected void onSelectedFilesChanged() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		exportBackupData.setExportBackupDirectory(selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null);
		updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
