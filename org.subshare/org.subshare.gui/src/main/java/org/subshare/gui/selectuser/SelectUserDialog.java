package org.subshare.gui.selectuser;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.subshare.core.user.User;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class SelectUserDialog extends Stage {

	private final SelectUserPaneWithButtonBar selectUserPane;

	private List<User> selectedUsers;

	public SelectUserDialog(final Window owner, final List<User> users, final Collection<User> selectedUsers, final SelectionMode selectionMode, final String headerText) {
		assertNotNull("owner", owner);
		assertNotNull("users", users);
		// selectedUsers may be null

		setResizable(false);
        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        setIconified(false);

        selectUserPane = new SelectUserPaneWithButtonBar(users, selectedUsers, selectionMode, headerText) {
			@Override
			protected void okButtonClicked(ActionEvent event) {
				SelectUserDialog.this.okButtonClicked();
			}

			@Override
			protected void cancelButtonClicked(ActionEvent event) {
				SelectUserDialog.this.cancelButtonClicked();
			}
        };
        setScene(new Scene(selectUserPane));

		setOnShown(event -> {
			// First, we must make this dialog request the focus. Otherwise, the focus
			// will stay with the owner-window. IMHO very strange, wrong default behaviour...
			SelectUserDialog.this.requestFocus();

			// Now, we must make sure the correct field is focused. This causes the passphrase
			// text-field to be focused.
			selectUserPane.requestFocus();
		});

		getScene().addEventFilter(KeyEvent.ANY, event -> {
			switch (event.getCode()) {
				case ENTER:
					event.consume();

					if (event.getEventType() == KeyEvent.KEY_RELEASED)
						okButtonClicked();

					break;
				case ESCAPE:
					event.consume();

					if (event.getEventType() == KeyEvent.KEY_RELEASED)
						cancelButtonClicked();

					break;
				default:
					break;
			}
		});
	}

	protected void okButtonClicked() {
		selectedUsers = new ArrayList<>(selectUserPane.getSelectedUsers());
		close();
	}

	protected void cancelButtonClicked() {
		selectedUsers = null;
		close();
	}

	public List<User> getSelectedUsers() {
		return selectedUsers;
	}
}
