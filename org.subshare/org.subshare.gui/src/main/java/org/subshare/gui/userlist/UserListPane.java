package org.subshare.gui.userlist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.user.EditUserManager;

public class UserListPane extends BorderPane {

	private final EditUserManager editUserManager;

	@FXML
	private Button addButton;
	@FXML
	private Button editButton;
	@FXML
	private Button deleteButton;

	@FXML
	private TextField filterTextField;

	@FXML
	private TableView<UserListItem> tableView;

	private final List<UserListItem> userListItems = new ArrayList<>();

	private final Timer applyFilterLaterTimer = new Timer(true);
	private TimerTask applyFilterLaterTimerTask;

	private final ListChangeListener<UserListItem> selectionListener = new ListChangeListener<UserListItem>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends UserListItem> c) {
			updateEnabled();
		}
	};

	private final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			if (event.getButton().equals(MouseButton.PRIMARY)){
	            if (event.getClickCount() >= 2)
	                editSelectedUsers();
	        }
		}
	};

	protected UserListPane() {
		this(new EditUserManager());
	}

	public UserListPane(final EditUserManager editUserManager) {
		this.editUserManager = assertNotNull("", editUserManager);
		loadDynamicComponentFxml(UserListPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tableView.getSelectionModel().getSelectedItems().addListener(selectionListener);
		tableView.setOnMouseClicked(mouseEventHandler);
		populateTableViewAsync();
		updateEnabled();
		filterTextField.textProperty().addListener((InvalidationListener) observable -> applyFilterLater());
	}

	private void updateEnabled() {
		final boolean selectionEmpty = tableView.getSelectionModel().getSelectedItems().isEmpty();
		editButton.disableProperty().set(selectionEmpty);
		deleteButton.disableProperty().set(selectionEmpty);
	}

	private void populateTableViewAsync() {
		new Service<Collection<User>>() {
			@Override
			protected Task<Collection<User>> createTask() {
				return new SsTask<Collection<User>>() {
					@Override
					protected Collection<User> call() throws Exception {
						return UserRegistryLs.getUserRegistry().getUsers();
					}

					@Override
					protected void succeeded() {
						final Collection<User> users;
						try { users = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addTableItemsViewCallback(users);
					}
				};
			}
		}.start();
	}

	private List<UserListItem> addTableItemsViewCallback(final Collection<User> users) {
		final List<UserListItem> result = new ArrayList<>(users.size());
		for (final User user : users) {
			final UserListItem userListItem = new UserListItem(user);
			result.add(userListItem);
			userListItems.add(userListItem);
			tableView.getItems().add(userListItem);
		}

		tableView.requestLayout();
		return result;
	}

	@FXML
	private void addButtonClicked(final ActionEvent event) {
		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();
		final User user = userRegistry.createUser();
		userRegistry.addUser(user);
		final List<UserListItem> userListItems = addTableItemsViewCallback(Collections.singleton(user));
		tableView.getSelectionModel().clearSelection();
		tableView.getSelectionModel().select(userListItems.get(0));
		editSelectedUsers();
	}

	@FXML
	private void editButtonClicked(final ActionEvent event) {
		editSelectedUsers();
	}

	private void editSelectedUsers() {
		List<User> users = new ArrayList<>(tableView.getSelectionModel().getSelectedItems().size());
		for (final UserListItem userListItem : tableView.getSelectionModel().getSelectedItems())
			users.add(userListItem.getUser());

		editUserManager.edit(users);
	}

	@FXML
	private void deleteButtonClicked(final ActionEvent event) {
		System.out.println("deleteButtonClicked: " + event);
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
	}
}
