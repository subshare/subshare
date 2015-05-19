package org.subshare.gui.userlist;

import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.subshare.core.user.User;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.UserRegistryLs;

public class UserListPane extends BorderPane {

	@FXML
	private Button addButton;
	@FXML
	private Button editButton;
	@FXML
	private Button deleteButton;

	@FXML
	private TableView<UserListItem> tableView;

	private final ListChangeListener<UserListItem> selectionListener = new ListChangeListener<UserListItem>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends UserListItem> c) {
			updateEnabled();
		}
	};

	public UserListPane() {
		loadDynamicComponentFxml(UserListPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tableView.getSelectionModel().getSelectedItems().addListener(selectionListener);
		populateTableViewAsync();
		updateEnabled();
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

	private void addTableItemsViewCallback(final Collection<User> users) {
		for (final User user : users)
			tableView.getItems().add(new UserListItem(user));

		tableView.requestLayout();
	}

	@FXML
	private void addButtonClicked(final ActionEvent event) {
		System.out.println("addButtonClicked: " + event);
	}

	@FXML
	private void editButtonClicked(final ActionEvent event) {
		System.out.println("editButtonClicked: " + event);
	}

	@FXML
	private void deleteButtonClicked(final ActionEvent event) {
		System.out.println("deleteButtonClicked: " + event);
	}
}
