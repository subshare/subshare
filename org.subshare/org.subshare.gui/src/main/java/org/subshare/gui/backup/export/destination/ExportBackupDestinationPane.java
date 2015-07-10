package org.subshare.gui.backup.export.destination;

import static org.subshare.gui.util.FxmlUtil.*;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.gui.filetree.FileTreePane;

public abstract class ExportBackupDestinationPane extends GridPane {

	@FXML
	private FileTreePane fileTreePane;

	public ExportBackupDestinationPane() {
		loadDynamicComponentFxml(ExportBackupDestinationPane.class, this);
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> updateComplete());
		updateComplete();
	}

	protected boolean isComplete() {
		return ! fileTreePane.getSelectedFiles().isEmpty();
	}

	protected abstract void updateComplete();
}
