package org.subshare.gui.selectuser;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import org.subshare.core.user.User;
import org.subshare.gui.userlist.UserListItem;

public abstract class SelectUserPane extends GridPane {

	private final List<User> users;

	private final ObservableSet<User> selectedUsers;

	private final List<UserListItem> userListItems;

	private final Timer applyFilterLaterTimer = new Timer(true);
	private TimerTask applyFilterLaterTimerTask;

	@FXML
	private Text headerText;

	@FXML
	private TextField filterTextField;

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

		this.users = users;
		userListItems = new ArrayList<>(users.size());

		final Map<User, UserListItem> user2UserListItem = new IdentityHashMap<>();
		for (final User user : users) {
			final UserListItem userListItem = new UserListItem(user);

			if (user2UserListItem.put(user, userListItem) != null)
				throw new IllegalArgumentException("Duplicate user element: " + user);

			userListItems.add(userListItem);
		}
		tableView.getItems().addAll(userListItems);

		this.selectedUsers = FXCollections.observableSet(new HashSet<>(selectedUsers != null ? selectedUsers : Collections.<User>emptyList()));

		for (final User user : this.selectedUsers) {
			final UserListItem userListItem = user2UserListItem.get(user);
			if (userListItem == null)
				throw new IllegalArgumentException("selectedUsers contains user not being contained in users: " + user);

			tableView.getSelectionModel().getSelectedItems().add(userListItem);
		}

		filterTextField.textProperty().addListener((InvalidationListener) observable -> applyFilterLater());

		tableView.getSelectionModel().getSelectedItems().addListener(selectedItemsChangeListener);
		tableView.getSelectionModel().setSelectionMode(selectionMode);

		this.selectedUsers.addListener((InvalidationListener) observable -> updateDisable());
		updateDisable();
	}

	private void applyFilterLater() {
		if (applyFilterLaterTimerTask != null) {
			applyFilterLaterTimerTask.cancel();
			applyFilterLaterTimerTask = null;
		}

		applyFilterLaterTimerTask = new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						applyFilterLaterTimerTask = null;
						applyFilter();
					}
				});

			}
		};

		applyFilterLaterTimer.schedule(applyFilterLaterTimerTask, 300);
	}

	private void applyFilter() {
		final String filterText = filterTextField.getText().toLowerCase();
		final Iterator<UserListItem> sourceIterator = userListItems.iterator();
		final ListIterator<UserListItem> tableItemIterator = tableView.getItems().listIterator();

		final Set<User> includedUsers = new HashSet<>();

		while (sourceIterator.hasNext()) {
			final UserListItem sourceUserListItem = sourceIterator.next();
			final boolean matchesFilter = sourceUserListItem.matchesFilter(filterText);
			if (matchesFilter)
				includedUsers.add(sourceUserListItem.getUser());

			final UserListItem tableUserListItem = tableItemIterator.hasNext() ? tableItemIterator.next() : null;
			if (tableUserListItem == null) {
				if (matchesFilter)
					tableItemIterator.add(sourceUserListItem);
			}
			else if (tableUserListItem == sourceUserListItem) {
				if (! matchesFilter)
					tableItemIterator.remove();
			}
			else {
				if (matchesFilter) {
					tableItemIterator.previous();
					tableItemIterator.add(sourceUserListItem);
				}
				else
					tableItemIterator.previous();
			}
		}

		while (tableItemIterator.hasNext()) {
			tableItemIterator.next();
			tableItemIterator.remove();
		}

		selectedUsers.retainAll(includedUsers);
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
				List<User> users = new ArrayList<User>(c.getRemoved().size());
				for (final UserListItem userListItem : c.getRemoved())
					users.add(userListItem.getUser());

				selectedUsers.removeAll(users);

				users = new ArrayList<User>(c.getAddedSubList().size());
				for (final UserListItem userListItem : c.getAddedSubList())
					users.add(userListItem.getUser());

				selectedUsers.addAll(users);
			}
		}
	};

	protected void updateDisable() {
		okButton.setDisable(selectedUsers.isEmpty());
	}

	public ObservableSet<User> getSelectedUsers() {
		return selectedUsers;
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);
}
