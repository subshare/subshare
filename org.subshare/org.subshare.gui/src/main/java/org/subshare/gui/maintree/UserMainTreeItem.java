package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;

import org.subshare.core.user.User;
import org.subshare.gui.user.EditUserManager;
import org.subshare.gui.user.UserPane;

import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class UserMainTreeItem extends MainTreeItem<User> {

	private static final Image icon = new Image(UserMainTreeItem.class.getResource("user_16x16.png").toExternalForm());
	private final EditUserManager editUserManager;

	public UserMainTreeItem(final EditUserManager editUserManager, final User user) {
		super(user);
		setGraphic(new ImageView(icon));
		this.editUserManager = requireNonNull(editUserManager, "editUserManager");
		requireNonNull(user, "user");
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

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
}
