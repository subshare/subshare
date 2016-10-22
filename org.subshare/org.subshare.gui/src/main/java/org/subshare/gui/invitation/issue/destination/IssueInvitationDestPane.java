package org.subshare.gui.invitation.issue.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.oio.File;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;

public class IssueInvitationDestPane extends WizardPageContentGridPane {

	private final IssueInvitationData issueInvitationData;

	@FXML
	private FileTreePane fileTreePane;

	public IssueInvitationDestPane(final IssueInvitationData issueInvitationData) {
		this.issueInvitationData = assertNotNull("issueInvitationData", issueInvitationData);
		loadDynamicComponentFxml(IssueInvitationDestPane.class, this);
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

	protected void onSelectedFilesChanged() {
		final ObservableSet<File> files = fileTreePane.getSelectedFiles();
		issueInvitationData.setInvitationTokenDirectory(files.isEmpty() ? null : files.iterator().next());
		updateComplete();
	}

	@Override
	protected boolean isComplete() {
		return issueInvitationData.getInvitationTokenDirectory() != null;
	}
}
