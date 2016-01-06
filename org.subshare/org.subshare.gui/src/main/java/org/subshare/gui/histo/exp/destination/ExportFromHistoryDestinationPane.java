package org.subshare.gui.histo.exp.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Iterator;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.histo.exp.ExportFromHistoryData;

import co.codewizards.cloudstore.core.oio.File;

public abstract class ExportFromHistoryDestinationPane extends GridPane {

	private final ExportFromHistoryData exportFromHistoryData;

	@FXML
	private FileTreePane fileTreePane;

	public ExportFromHistoryDestinationPane(final ExportFromHistoryData exportFromHistoryData) {
		loadDynamicComponentFxml(ExportFromHistoryDestinationPane.class, this);
		this.exportFromHistoryData = assertNotNull("exportFromHistoryData", exportFromHistoryData);
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

	protected boolean isComplete() {
		return exportFromHistoryData.getExportDirectory() != null;
	}

	protected void onSelectedFilesChanged() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		exportFromHistoryData.setExportDirectory(selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null);
		updateComplete();
	}

	protected abstract void updateComplete();

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
