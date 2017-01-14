package org.subshare.gui.invitation.accept.checkoutdir;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Iterator;

import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.oio.File;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;

public class CheckOutDirectoryPane extends WizardPageContentGridPane {
	private final AcceptInvitationData acceptInvitationData;

	@FXML
	private FileTreePane fileTreePane;

	public CheckOutDirectoryPane(final AcceptInvitationData acceptInvitationData) {
		this.acceptInvitationData = assertNotNull(acceptInvitationData, "acceptInvitationData");
		loadDynamicComponentFxml(CheckOutDirectoryPane.class, this);
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

	@Override
	protected boolean isComplete() {
		return acceptInvitationData.getCheckOutDirectory() != null;
	}

	protected void onSelectedFilesChanged() {
		final Iterator<File> selectedFilesIterator = fileTreePane.getSelectedFiles().iterator();
		File file = selectedFilesIterator.hasNext() ? selectedFilesIterator.next() : null;

		if (file != null && ! file.isDirectory())
			file = null;

		acceptInvitationData.setCheckOutDirectory(file);
		updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
