package org.subshare.gui.userlist;

import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;

public class UserListPane extends BorderPane {

	@FXML
	private TableView<UserListItem> tableView;

	public UserListPane() {
		loadDynamicComponentFxml(UserListPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		populateTableViewAsync();
	}

	private void populateTableViewAsync() {
		new Service<Collection<User>>() {
			@Override
			protected Task<Collection<User>> createTask() {
				return new Task<Collection<User>>() {
					@Override
					protected Collection<User> call() throws Exception {
						return UserRegistry.getInstance().getUsers();
					}

					@Override
					protected void succeeded() {
						final Collection<User> users;
						try { users = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						populateTableViewCallback(users);
					}
				};
			}
		}.start();
	}

	private void populateTableViewCallback(final Collection<User> users) {
		for (final User user : users)
			tableView.getItems().add(new UserListItem(user));

		tableView.requestLayout();
	}

}
