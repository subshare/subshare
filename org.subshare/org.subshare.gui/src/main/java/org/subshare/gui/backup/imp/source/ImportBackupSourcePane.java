package org.subshare.gui.backup.imp.source;

import static java.util.Objects.*;
import static org.subshare.gui.backup.BackupConst.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Iterator;

import org.subshare.core.file.DataFileFilter;
import org.subshare.gui.backup.imp.ImportBackupData;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.oio.File;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;

public class ImportBackupSourcePane extends WizardPageContentGridPane {

	private final ImportBackupData importBackupData;

	@FXML
	private FileTreePane fileTreePane;

	public ImportBackupSourcePane(final ImportBackupData importBackupData) {
		loadDynamicComponentFxml(ImportBackupSourcePane.class, this);
		this.importBackupData = requireNonNull(importBackupData, "importBackupData");
		fileTreePane.fileFilterProperty().set(new DataFileFilter().setAcceptContentType(BACKUP_FILE_CONTENT_TYPE_VALUE));
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

	@Override
	protected boolean isComplete() {
		return importBackupData.getImportBackupFile() != null;
	}

	protected void onSelectedFilesChanged() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		File file = selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null;

		if (file != null && ! file.isFile())
			file = null;

		importBackupData.setImportBackupFile(file);
		updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
