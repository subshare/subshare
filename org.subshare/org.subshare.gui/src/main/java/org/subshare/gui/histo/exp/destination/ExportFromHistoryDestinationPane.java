package org.subshare.gui.histo.exp.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Iterator;

import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.histo.exp.ExportFromHistoryData;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.oio.File;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;

public class ExportFromHistoryDestinationPane extends WizardPageContentGridPane {

	private final ExportFromHistoryData exportFromHistoryData;

	@FXML
	private FileTreePane fileTreePane;

	public ExportFromHistoryDestinationPane(final ExportFromHistoryData exportFromHistoryData) {
		loadDynamicComponentFxml(ExportFromHistoryDestinationPane.class, this);
		this.exportFromHistoryData = assertNotNull(exportFromHistoryData, "exportFromHistoryData");
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

	@Override
	protected boolean isComplete() {
		return exportFromHistoryData.getExportDirectory() != null;
	}

	protected void onSelectedFilesChanged() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		exportFromHistoryData.setExportDirectory(selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null);
		updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
