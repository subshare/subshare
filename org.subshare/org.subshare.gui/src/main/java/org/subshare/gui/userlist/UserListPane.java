package org.subshare.gui.userlist;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.pgp.ImportKeysResult.ImportedMasterKey;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.user.ImportUsersFromPgpKeysResult;
import org.subshare.core.user.ImportUsersFromPgpKeysResult.ImportedUser;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.pgp.imp.fromserver.ImportPgpKeyFromServerWizard;
import org.subshare.gui.user.EditUserManager;
import org.subshare.gui.wizard.WizardDialog;
import org.subshare.gui.wizard.WizardState;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.util.FileLs;
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
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

public class UserListPane extends GridPane {

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

	private UserRegistry userRegistry;

	private final ListChangeListener<UserListItem> selectionListener = new ListChangeListener<UserListItem>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends UserListItem> c) {
			updateDisable();
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

	private final PropertyChangeListener usersPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final Set<User> users = new LinkedHashSet<User>((List<User>) evt.getNewValue());
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					addOrRemoveItemTablesViewCallback(users);
				}
			});
		}
	};

	/**
	 * @deprecated This constructor is solely to be used by the UI designer! It must not be invoked programmatically!
	 */
	@Deprecated
	public UserListPane() {
		this.editUserManager = null;
		loadDynamicComponentFxml(UserListPane.class, this);
	}

	public UserListPane(final EditUserManager editUserManager) {
		this.editUserManager = assertNotNull("", editUserManager);
		loadDynamicComponentFxml(UserListPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tableView.getSelectionModel().getSelectedItems().addListener(selectionListener);
		tableView.setOnMouseClicked(mouseEventHandler);
		populateTableViewAsync();
		updateDisable();
		filterTextField.textProperty().addListener((InvalidationListener) observable -> applyFilterLater());
	}

	private void updateDisable() {
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
						return getUserRegistry().getUsers();
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

	private void addOrRemoveItemTablesViewCallback(final Set<User> users) {
		assertNotNull("users", users);
		final Map<User, UserListItem> viewUser2UserListItem = new HashMap<>();
		for (final UserListItem uli : tableView.getItems())
			viewUser2UserListItem.put(uli.getUser(), uli);

		for (final User user : users) {
			if (! viewUser2UserListItem.containsKey(user)) {
				final UserListItem uli = new UserListItem(user);
				viewUser2UserListItem.put(user, uli);
				tableView.getItems().add(uli);
			}
		}

		if (users.size() < viewUser2UserListItem.size()) {
			for (final User user : users)
				viewUser2UserListItem.remove(user);

			for (final UserListItem uli : viewUser2UserListItem.values())
				tableView.getItems().remove(uli);
		}

//		tableView.requestLayout();
	}

	@FXML
	private void addButtonClicked(final ActionEvent event) {
		final UserRegistry userRegistry = getUserRegistry();
		final User user = userRegistry.createUser();
		user.setFirstName(String.format("New user %s", Long.toString(System.currentTimeMillis(), 36)));
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
		System.gc();
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

	@FXML
	private void importPgpKeyFromFileButtonClicked(final ActionEvent event) {
		final File file = showOpenFileDialog("Choose file containing PGP key(s) to import");
		if (file == null)
			return;

		try {
			try (IInputStream in = FileLs.createInputStream(file)) {
				final ImportKeysResult importKeysResult = getPgp().importKeys(in);
				importUsersFromPgpKeys(importKeysResult);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	@FXML
	private void importPgpKeyFromServerButtonClicked(final ActionEvent event) {
		ImportPgpKeyFromServerWizard wizard = new ImportPgpKeyFromServerWizard();
		WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.showAndWait();
		if (wizard.getState() == WizardState.FINISHED)
			editImportedUsers(wizard.getImportPgpKeyFromServerData().getImportUsersResult());
	}

	private void importUsersFromPgpKeys(final ImportKeysResult importKeysResult) {
		assertNotNull("importKeysResult", importKeysResult);

		final Pgp pgp = getPgp();
		final Map<PgpKeyId, PgpKey> pgpKeyId2PgpKey = new HashMap<>();

		for (ImportedMasterKey importedMasterKey : importKeysResult.getPgpKeyId2ImportedMasterKey().values()) {
			final PgpKeyId pgpKeyId = importedMasterKey.getPgpKeyId();
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			assertNotNull("pgp.getPgpKey(" + pgpKeyId + ")", pgpKey);
			pgpKeyId2PgpKey.put(pgpKeyId, pgpKey);
		}

		final ImportUsersFromPgpKeysResult importUsersFromPgpKeysResult = getUserRegistry().importUsersFromPgpKeys(pgpKeyId2PgpKey.values());
		editImportedUsers(importUsersFromPgpKeysResult);
	}

	private void editImportedUsers(final ImportUsersFromPgpKeysResult importUsersFromPgpKeysResult) {
		assertNotNull("importUsersFromPgpKeysResult", importUsersFromPgpKeysResult);

		final List<User> users = new ArrayList<User>();
		for (List<ImportedUser> importedUsers : importUsersFromPgpKeysResult.getPgpKeyId2ImportedUsers().values()) {
			for (ImportedUser importedUser : importedUsers)
				users.add(importedUser.getUser());
		}
		editUserManager.edit(users);
	}

	private File showOpenFileDialog(final String title) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		final java.io.File file = fileChooser.showOpenDialog(getScene().getWindow());
		return file == null ? null : createFile(file).getAbsoluteFile();
	}

	protected synchronized UserRegistry getUserRegistry() {
		if (userRegistry == null) {
			userRegistry = UserRegistryLs.getUserRegistry();
			addWeakPropertyChangeListener(userRegistry, UserRegistry.PropertyEnum.users, usersPropertyChangeListener);
		}
		return userRegistry;
	}

	@Override
	protected void finalize() throws Throwable {
//		final UserRegistry userRegistry = this.userRegistry;
//		if (userRegistry != null) {
//			userRegistry.removePropertyChangeListener(UserRegistry.PropertyEnum.users, usersPropertyChangeListener);
//		}
		super.finalize();
	}

	protected Pgp getPgp() {
		return PgpLs.getPgpOrFail();
	}
}
