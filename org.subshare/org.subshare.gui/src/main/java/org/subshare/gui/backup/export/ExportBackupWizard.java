package org.subshare.gui.backup.export;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.backup.BackupConst.*;

import java.util.Date;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import org.subshare.gui.backup.BackupExporter;
import org.subshare.gui.backup.export.destination.ExportBackupDestinationWizardPage;
import org.subshare.gui.backup.export.first.FirstWizardPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class ExportBackupWizard extends Wizard {

	private final ExportBackupData exportBackupData = new ExportBackupData();
	private final BackupExporter backupExporter;
	private File backupFile;

	private boolean needed;

	public ExportBackupWizard() {
		super(new FirstWizardPage());
		pages.add(new ExportBackupDestinationWizardPage(exportBackupData));

		backupExporter = new BackupExporter();
		needed = backupExporter.isBackupNeeded();
	}
	@Override
	protected void finishing() {
		backupFile = createBackupFile();
		super.finishing();
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		backupExporter.exportBackup(backupFile);
	}

	@Override
	protected void preFinished() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText("The backup was successfully created!");

		final String text = String.format("File: %3$s\nDirectory: %2$s\n\nYou should not lose this file!\n\nDo not forget to properly unmount the device, if this is an external device:\nUse your operating system's \"Safely remove\" feature!",
				backupFile.getPath(), backupFile.getParentFile().getPath(), backupFile.getName());

//		alert.setContentText(text);
		// The above does not adjust the dialog size :-( Using a Text node instead works better.

		final Text contentText = new Text(text);
		final HBox contentTextContainer = new HBox();
		contentTextContainer.getChildren().add(contentText);

		GridPane.setMargin(contentText, new Insets(8));
		alert.getDialogPane().setContent(contentTextContainer);

		alert.showAndWait();
		super.preFinished();
	}

	private File createBackupFile() {
		final File directory = exportBackupData.getExportBackupDirectory();
		assertNotNull("exportBackupData.exportBackupDirectory", directory); //$NON-NLS-1$
		final String dateString = String.format("%1$tY-%1$tm-%1$te_%1$tH-%1$tM-%1$tS", new Date()); //$NON-NLS-1$ // ISO-8601 => no need for externalisation.
		final String fileName = String.format("%s%s%s", BACKUP_FILE_NAME_PREFIX, dateString, BACKUP_FILE_NAME_EXTENSION); //$NON-NLS-1$
		final File backupFile = createFile(directory, fileName);
		return backupFile;
	}

	@Override
	public String getTitle() {
		return Messages.getString("ExportBackupWizard.title"); //$NON-NLS-1$
	}

	public boolean isNeeded() {
		return needed;
	}
}
