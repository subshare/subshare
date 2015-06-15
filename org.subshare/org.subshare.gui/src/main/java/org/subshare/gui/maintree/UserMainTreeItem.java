package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.scene.Parent;

import org.subshare.core.user.User;
import org.subshare.gui.user.EditUserManager;
import org.subshare.gui.user.UserPane;

public class UserMainTreeItem extends MainTreeItem<User> {

	private final EditUserManager editUserManager;

	public UserMainTreeItem(final EditUserManager editUserManager, final User user) {
		super(user);
		this.editUserManager = assertNotNull("editUserManager", editUserManager);
		assertNotNull("user", user);
	}

	@Override
	protected String getValueString() {
		final User user = getValueObject();
		final StringBuilder sb = new StringBuilder();

		if (! isEmpty(user.getFirstName()))
			sb.append(user.getFirstName());

		if (! isEmpty(user.getLastName())) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append(user.getLastName());
		}

		if (! user.getEmails().isEmpty()) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append('<').append(user.getEmails().get(0)).append('>');
		}

		return sb.toString();
	}

	@Override
	protected Parent createMainDetailContent() {
		return new UserPane(editUserManager, getValueObject());
	}
}
