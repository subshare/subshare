package org.subshare.gui.invitation.issue.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.issue.IssueInvitationData;

import co.codewizards.cloudstore.core.oio.File;

public abstract class IssueInvitationDestPane extends GridPane {

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

	protected boolean isComplete() {
		return issueInvitationData.getInvitationTokenDirectory() != null;
	}

	protected abstract void updateComplete();
}
