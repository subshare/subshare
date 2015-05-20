package org.subshare.gui.selectuser;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import org.subshare.core.user.User;
import org.subshare.gui.userlist.UserListItem;

public abstract class SelectUserPane extends GridPane {

	private final ObservableList<User> users;

	private final ObservableList<User> selectedUsers;

	@FXML
	private Text headerText;

	@FXML
	private TableView<UserListItem> tableView;

	@FXML
	private Button okButton;

	@FXML
	private Button cancelButton;

	public SelectUserPane(final List<User> users, final Collection<User> selectedUsers, final SelectionMode selectionMode, final String headerText) {
		assertNotNull("users", users);
		// selectedUsers may be null
		assertNotNull("selectionMode", selectionMode);
		loadDynamicComponentFxml(SelectUserPane.class, this);

		this.headerText.setText(headerText);

		if (users instanceof ObservableList<?>)
        	this.users = (ObservableList<User>) users;
        else
        	this.users = FXCollections.observableList(users);

		final Map<User, UserListItem> user2UserListItem = new IdentityHashMap<>();
		for (final User user : users) {
			final UserListItem userListItem = new UserListItem(user);

			if (user2UserListItem.put(user, userListItem) != null)
				throw new IllegalArgumentException("Duplicate user element: " + user);

			tableView.getItems().add(userListItem);
		}

		this.selectedUsers = FXCollections.observableArrayList(selectedUsers != null ? selectedUsers : Collections.<User>emptyList());

		for (final User user : this.selectedUsers) {
			final UserListItem userListItem = user2UserListItem.get(user);
			if (userListItem == null)
				throw new IllegalArgumentException("selectedUsers contains user not being contained in users: " + user);

			tableView.getSelectionModel().getSelectedItems().add(userListItem);
		}

		tableView.getSelectionModel().getSelectedItems().addListener(selectedItemsChangeListener);
		tableView.getSelectionModel().setSelectionMode(selectionMode);
		updateDisable();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		tableView.requestFocus();
	}

	private final ListChangeListener<UserListItem> selectedItemsChangeListener = new ListChangeListener<UserListItem>() {
		@Override
		public void onChanged(ListChangeListener.Change<? extends UserListItem> c) {
			while (c.next()) {
				for (final UserListItem userListItem : c.getRemoved())
					selectedUsers.remove(userListItem.getUser());

				for (final UserListItem userListItem : c.getAddedSubList())
					selectedUsers.add(userListItem.getUser());
			}
			updateDisable();
		}
	};

	protected void updateDisable() {
		okButton.setDisable(selectedUsers.isEmpty());
	}

	public ObservableList<User> getUsers() {
		return users;
	}

	public ObservableList<User> getSelectedUsers() {
		return selectedUsers;
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);
}
