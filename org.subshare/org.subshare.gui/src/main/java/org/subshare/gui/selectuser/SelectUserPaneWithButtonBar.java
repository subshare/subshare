package org.subshare.gui.selectuser;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Collection;
import java.util.List;

import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;

import org.subshare.core.user.User;

public abstract class SelectUserPaneWithButtonBar extends GridPane {

	private SelectUserPane selectUserPane;

	@FXML
	private Button okButton;

	@FXML
	private Button cancelButton;

	public SelectUserPaneWithButtonBar(final List<User> users, final Collection<User> selectedUsers, final SelectionMode selectionMode, final String headerText) {
		assertNotNull("users", users);
		// selectedUsers may be null
		assertNotNull("selectionMode", selectionMode);
		loadDynamicComponentFxml(SelectUserPaneWithButtonBar.class, this);
		selectUserPane = new SelectUserPane(users, selectedUsers, selectionMode, headerText) {
			@Override
			protected void updateComplete() {
				super.updateComplete();
				okButton.setDisable(getSelectedUsers().isEmpty());
			}
		};
		add(selectUserPane, 0, 0);
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		selectUserPane.requestFocus();
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);

	public ObservableSet<User> getSelectedUsers() {
		return selectUserPane.getSelectedUsers();
	}
}
