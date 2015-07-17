package org.subshare.gui.invitation.accept.checkoutdir;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Iterator;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.invitation.accept.AcceptInvitationData;

import co.codewizards.cloudstore.core.oio.File;

public abstract class CheckOutDirectoryPane extends GridPane {
	private final AcceptInvitationData acceptInvitationData;

	@FXML
	private FileTreePane fileTreePane;

	public CheckOutDirectoryPane(final AcceptInvitationData acceptInvitationData) {
		this.acceptInvitationData = assertNotNull("acceptInvitationData", acceptInvitationData);
		loadDynamicComponentFxml(CheckOutDirectoryPane.class, this);
		fileTreePane.fileFilterProperty().set(file -> file.isDirectory());
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> onSelectedFilesChanged());
		onSelectedFilesChanged();
	}

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

	protected abstract void updateComplete();

	@Override
	public void requestFocus() {
		super.requestFocus();
		fileTreePane.requestFocus();
	}
}
