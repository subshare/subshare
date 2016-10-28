package org.subshare.gui.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.subshare.core.user.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class EditUserManager {

	private final ObservableSet<User> editedUsers = FXCollections.observableSet(new LinkedHashSet<User>());
	private final ObservableSet<User> unmodifiableEditedUsers = FXCollections.unmodifiableObservableSet(editedUsers);
	private final List<EditUserListener> editUserListeners = new CopyOnWriteArrayList<>();

	public EditUserManager() {
	}

	/**
	 * Gets the users that are currently being edited.
	 * @return the users currently being edited. Never <code>null</code>.
	 */
	public ObservableSet<User> getEditedUsers() {
		return unmodifiableEditedUsers;
	}

	public void edit(final Collection<? extends User> users) {
		assertNotNull("users", users);
		editedUsers.addAll(users);
		final EditUserEvent event = new EditUserEvent(this, users);
		for (final EditUserListener listener : editUserListeners)
			listener.onEdit(event);
	}

	public void endEditing(final Collection<? extends User> users) {
		assertNotNull("users", users);
		editedUsers.removeAll(users);
	}

	public void addEditUserListener(final EditUserListener listener) {
		assertNotNull("listener", listener);
		editUserListeners.add(listener);
	}
	public void removeEditUserListener(final EditUserListener listener) {
		editUserListeners.remove(listener);
	}
}
