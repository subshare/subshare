package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.collections.SetChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.subshare.core.user.User;
import org.subshare.gui.user.EditUserManager;
import org.subshare.gui.userlist.UserListPane;

public class UserListMainTreeItem extends MainTreeItem<String> {

	private static final Image icon = new Image(UserListMainTreeItem.class.getResource("user-list-16x16.png").toExternalForm());
	private final EditUserManager editUserManager = new EditUserManager();

	private final SetChangeListener<User> editedUsersListener = new SetChangeListener<User>() {
		@Override
		public void onChanged(final SetChangeListener.Change<? extends User> c) {
			final User userAdded = c.getElementAdded();
			if (userAdded != null) {
				addTreeItemsViewCallback(Collections.singleton(userAdded));
				selectUser(userAdded);
			}

			final User userRemoved = c.getElementRemoved();
			if (userRemoved != null)
				removeTreeItemsViewCallback(Collections.singleton(userRemoved));
		}
	};

	public UserListMainTreeItem() {
		super("Users");
		setGraphic(new ImageView(icon));

		editUserManager.getEditedUsers().addListener(editedUsersListener);
		editUserManager.addEditUserListener(event -> {
			final User user = getLast(event.getUsers());
			if (user != null)
				selectUser(user);
		});
		addTreeItemsViewCallback(editUserManager.getEditedUsers()); // currently always empty, but maybe we later store the last state from the last session
	}

	private User getLast(Collection<? extends User> users) {
		User result = null;
		for (User user : users)
			result = user;

		return result;
	}

	private void addTreeItemsViewCallback(final Collection<User> users) {
		assertNotNull("users", users);
		for (final User user : users)
			getChildren().add(new UserMainTreeItem(editUserManager, user));
	}

	private void selectUser(final User user) {
		assertNotNull("user", user);

		if (! isExpanded())
			setExpanded(true);

		final UserMainTreeItem userMainTreeItem = getUserMainTreeItem(user);
		if (userMainTreeItem != null)
			getTreeView().getSelectionModel().select(userMainTreeItem);
	}

	private UserMainTreeItem getUserMainTreeItem(final User user) {
		for (final TreeItem<String> child : getChildren()) {
			final UserMainTreeItem umti = (UserMainTreeItem) child;
			if (user.equals(umti.getValueObject()))
				return umti;
		}
		return null;
	}

	private void removeTreeItemsViewCallback(final Collection<User> users) {
		assertNotNull("users", users);
		final Set<User> userSet = users instanceof Set<?> ? cast(users) : new HashSet<User>(users);
		final List<UserMainTreeItem> itemsToRemove = new ArrayList<>(users.size());
		for (final TreeItem<String> child : getChildren()) {
			final UserMainTreeItem umti = (UserMainTreeItem) child;
			final User user = umti.getValueObject();
			if (userSet.contains(user))
				itemsToRemove.add(umti);
		}
		getChildren().removeAll(itemsToRemove);
	}

	@Override
	protected Parent createMainDetailContent() {
		return new UserListPane(editUserManager);
	}
}
